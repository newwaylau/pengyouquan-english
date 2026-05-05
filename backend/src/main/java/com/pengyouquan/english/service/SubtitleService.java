package com.pengyouquan.english.service;

import com.pengyouquan.english.model.Sentence;
import com.pengyouquan.english.model.Show;
import com.pengyouquan.english.model.SubtitleImportLog;
import com.pengyouquan.english.repository.SentenceRepository;
import com.pengyouquan.english.repository.ShowRepository;
import com.pengyouquan.english.repository.SubtitleImportLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字幕解析导入服务
 * 支持 SRT / ASS / VTT 三种格式
 */
@Service
public class SubtitleService {

    private static final Logger log = LoggerFactory.getLogger(SubtitleService.class);

    private final ShowRepository showRepository;
    private final SentenceRepository sentenceRepository;
    private final SubtitleImportLogRepository importLogRepository;

    public SubtitleService(ShowRepository showRepository,
                           SentenceRepository sentenceRepository,
                           SubtitleImportLogRepository importLogRepository) {
        this.showRepository = showRepository;
        this.sentenceRepository = sentenceRepository;
        this.importLogRepository = importLogRepository;
    }

    /**
     * 解析并导入字幕文件
     *
     * @param fileName  原始文件名
     * @param content   字幕文件内容
     * @param showName  指定剧集名（null 时从文件名自动提取）
     * @return 导入结果
     */
    @Transactional
    public ImportResult importSubtitle(String fileName, String content, String showName) {
        // 1. 自动识别格式
        String format = detectFormat(fileName, content);
        if (format == null) {
            return new ImportResult(0, 0, "不支持的字幕格式，支持：srt, ass, vtt");
        }

        // 2. 解析字幕内容
        List<SubtitleEntry> entries = parse(content, format);
        if (entries.isEmpty()) {
            return new ImportResult(0, 0, "未解析到任何字幕条目");
        }

        // 3. 确定剧集名（用 final 变量支持 lambda）
        final String finalShowName;
        if (showName == null || showName.isBlank()) {
            finalShowName = extractShowName(fileName);
        } else {
            finalShowName = showName;
        }
        final String finalFileName = fileName;

        // 4. 查找或创建剧集
        Show show = showRepository.findByNameContaining(finalShowName)
                .stream()
                .filter(s -> s.getName().equals(finalShowName))
                .findFirst()
                .orElseGet(() -> {
                    Show newShow = new Show();
                    newShow.setName(finalShowName);
                    newShow.setSourceFile(finalFileName);
                    return showRepository.save(newShow);
                });

        // 5. 逐条导入（去重）
        int importedCount = 0;
        int duplicateCount = 0;

        for (SubtitleEntry entry : entries) {
            if (entry.text == null || entry.text.isBlank()) continue;

            String cleanText = entry.text.trim();

            // 去重：同一 showId + 同文本不重复
            boolean exists = sentenceRepository.findByShowIdOrderById(show.getId())
                    .stream()
                    .anyMatch(s -> s.getText().equals(cleanText));

            if (exists) {
                duplicateCount++;
                continue;
            }

            Sentence sentence = new Sentence();
            sentence.setShowId(show.getId());
            sentence.setText(cleanText);
            sentence.setStartTime(entry.startTime);
            sentence.setEndTime(entry.endTime);
            sentenceRepository.save(sentence);
            importedCount++;
        }

        // 6. 记录导入日志
        SubtitleImportLog logEntry = new SubtitleImportLog();
        logEntry.setShowId(show.getId());
        logEntry.setFileName(fileName);
        logEntry.setFormat(format);
        logEntry.setSentenceCount(importedCount);
        logEntry.setDuplicateCount(duplicateCount);
        importLogRepository.save(logEntry);

        log.info("字幕导入完成：{} -> 剧集[{}]，导入{}条，重复{}条", fileName, showName, importedCount, duplicateCount);

        return new ImportResult(importedCount, duplicateCount, null);
    }

    /**
     * 从文件名提取剧集名称
     * 去掉 .srt / .ass / .vtt 后缀
     */
    public String extractShowName(String fileName) {
        if (fileName == null) return "未知剧集";
        // 去掉扩展名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    /**
     * 自动检测字幕格式
     */
    public String detectFormat(String fileName, String content) {
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".srt")) return "srt";
            if (lower.endsWith(".ass")) return "ass";
            if (lower.endsWith(".vtt")) return "vtt";
        }
        // 根据内容特征判断
        if (content != null) {
            if (content.contains("WEBVTT")) return "vtt";
            if (content.contains("[Script Info]") || content.contains("Format:")) return "ass";
            if (content.matches("(?s).*\\d+\\s*\\n\\d{2}:\\d{2}:\\d{2}[,.]\\d{3}\\s*-->.*")) return "srt";
        }
        return null;
    }

    // ── 解析器 ──

    /**
     * 根据格式解析字幕内容
     */
    public List<SubtitleEntry> parse(String content, String format) {
        return switch (format) {
            case "srt" -> parseSrt(content);
            case "ass" -> parseAss(content);
            case "vtt" -> parseVtt(content);
            default -> List.of();
        };
    }

    /**
     * SRT 格式解析
     * 格式：
     * 1
     * 00:00:01,000 --> 00:00:04,000
     * Hello world / 你好世界
     */
    public List<SubtitleEntry> parseSrt(String content) {
        List<SubtitleEntry> result = new ArrayList<>();
        if (content == null || content.isBlank()) return result;

        // 按空行分隔块
        String[] blocks = content.split("\\n\\s*\\n");
        Pattern timePattern = Pattern.compile(
                "(\\d{2}):(\\d{2}):(\\d{2})[,.]?(\\d{1,3})?\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[,.]?(\\d{1,3})?"
        );

        for (String block : blocks) {
            String[] lines = block.trim().split("\\n", 3);
            if (lines.length < 2) continue;

            // 找到时间轴行
            Matcher timeMatcher = timePattern.matcher(block);
            if (!timeMatcher.find()) continue;

            double startTime = parseTime(timeMatcher.group(1), timeMatcher.group(2),
                    timeMatcher.group(3), timeMatcher.group(4));
            double endTime = parseTime(timeMatcher.group(5), timeMatcher.group(6),
                    timeMatcher.group(7), timeMatcher.group(8));

            // 提取文本（时间行之后的内容）
            int timeEnd = timeMatcher.end();
            String textBlock = block.substring(timeEnd).trim();

            if (textBlock.isEmpty()) continue;

            // 处理中英双语（含 / 分隔符）
            String text = processBilingualText(textBlock);

            result.add(new SubtitleEntry(startTime, endTime, text));
        }

        return result;
    }

    /**
     * ASS 格式解析
     * Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,Hello world
     */
    public List<SubtitleEntry> parseAss(String content) {
        List<SubtitleEntry> result = new ArrayList<>();
        if (content == null || content.isBlank()) return result;

        // 先尝试找到 Format 行确定列映射
        String[] lines = content.split("\\n");

        // Dialogue: 格式示例:
        // Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,Hello world / 你好世界
        // 时间格式: H:MM:SS.xx 或 0:00:01.00
        // 文本在最后一个逗号之后（注意可能有逗号在文本内）
        Pattern dialoguePattern = Pattern.compile(
                "^Dialogue:\\s*(\\d+),(\\d?\\d?:\\d{2}:\\d{2}[.,]\\d{2})," +
                "(\\d?\\d?:\\d{2}:\\d{2}[.,]\\d{2}),[^,]*,[^,]*,[^,]*,[^,]*,[^,]*,(.+)$",
                Pattern.MULTILINE
        );

        Matcher matcher = dialoguePattern.matcher(content);
        while (matcher.find()) {
            String startStr = matcher.group(2);
            String endStr = matcher.group(3);
            String text = matcher.group(4).trim();

            if (text.isEmpty()) continue;

            double startTime = parseAssTime(startStr);
            double endTime = parseAssTime(endStr);

            // 处理 ASS 中的换行标记
            text = text.replace("\\N", " / ").replace("\\n", " / ");
            text = processBilingualText(text);

            result.add(new SubtitleEntry(startTime, endTime, text));
        }

        return result;
    }

    /**
     * WebVTT 格式解析
     * 00:00:01.000 --> 00:00:04.000
     * Hello world / 你好世界
     */
    public List<SubtitleEntry> parseVtt(String content) {
        List<SubtitleEntry> result = new ArrayList<>();
        if (content == null || content.isBlank()) return result;

        // 去掉 WEBVTT header 和元数据
        String body = content;
        int headerEnd = body.indexOf("-->");
        if (headerEnd < 0) return result;

        // 找到第一个 time 行之前的内容跳过
        String[] parts = body.split("(?=\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s*-->)");
        // 也可以用和 SRT 类似的块分割方式，但 VTT 使用 . 而不是 ,
        // 先尝试按块分割
        // 简化处理：从第一个时间行开始处理
        int firstTime = -1;
        String[] lines = body.split("\\n");
        for (int i = 0; i < Math.min(lines.length, 20); i++) {
            if (lines[i].matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s*-->.*")) {
                firstTime = i;
                break;
            }
        }

        if (firstTime < 0) return result;

        StringBuilder sb = new StringBuilder();
        for (int i = firstTime; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }

        // 现在用 SRT 类似的方式解析
        String vttContent = sb.toString();
        String[] blocks = vttContent.split("\\n\\s*\\n");
        Pattern timePattern = Pattern.compile(
                "(\\d{2}):(\\d{2}):(\\d{2})[.,](\\d{1,3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[.,](\\d{1,3})"
        );

        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;

            // 跳过序号行（VTT 有时也有）
            String[] blockLines = block.split("\\n");
            int timeLineIdx = 0;
            for (int i = 0; i < blockLines.length; i++) {
                if (blockLines[i].contains("-->")) {
                    timeLineIdx = i;
                    break;
                }
            }

            Matcher timeMatcher = timePattern.matcher(block);
            if (!timeMatcher.find()) continue;

            double startTime = parseTime(timeMatcher.group(1), timeMatcher.group(2),
                    timeMatcher.group(3), timeMatcher.group(4));
            double endTime = parseTime(timeMatcher.group(5), timeMatcher.group(6),
                    timeMatcher.group(7), timeMatcher.group(8));

            int afterTime = block.indexOf("-->") + 3;
            // 找到时间行结束
            int textStart = block.indexOf('\n', afterTime);
            if (textStart < 0) continue;

            String textBlock = block.substring(textStart).trim();
            if (textBlock.isEmpty()) continue;

            // 去掉可能的头部标签如 <c> </c>
            textBlock = textBlock.replaceAll("<[^>]+>", "");

            String text = processBilingualText(textBlock);
            result.add(new SubtitleEntry(startTime, endTime, text));
        }

        return result;
    }

    // ── 工具方法 ──

    /**
     * 处理中英双语文本
     * 如果行中含 " / " 分隔符，合并为一行
     */
    private String processBilingualText(String text) {
        if (text == null || text.isBlank()) return text;

        // 去除每行首尾空白
        String[] textLines = text.split("\\n");
        StringBuilder merged = new StringBuilder();
        for (String line : textLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            // 去掉 HTML 标签
            trimmed = trimmed.replaceAll("<[^>]+>", "");
            if (trimmed.isEmpty()) continue;
            if (merged.length() > 0) {
                merged.append(" / ");
            }
            merged.append(trimmed);
        }

        return merged.toString();
    }

    /**
     * 解析时间 HH:MM:SS,mmm → 秒
     */
    private double parseTime(String h, String m, String s, String ms) {
        double hours = h != null ? Double.parseDouble(h) : 0;
        double minutes = m != null ? Double.parseDouble(m) : 0;
        double seconds = s != null ? Double.parseDouble(s) : 0;
        double millis = 0;
        if (ms != null && !ms.isEmpty()) {
            // 可能只有1-3位
            millis = Double.parseDouble(ms);
            int len = ms.length();
            if (len == 1) millis *= 100;
            else if (len == 2) millis *= 10;
        }
        return hours * 3600 + minutes * 60 + seconds + millis / 1000.0;
    }

    /**
     * 解析 ASS 时间格式: 0:00:01.00
     */
    private double parseAssTime(String timeStr) {
        // 格式: H:MM:SS.xx 或 0:00:01.00
        String[] parts = timeStr.split("[:.]");
        if (parts.length >= 3) {
            double hours = Double.parseDouble(parts[0]);
            double minutes = Double.parseDouble(parts[1]);
            double seconds = Double.parseDouble(parts[2]);
            double millis = 0;
            if (parts.length >= 4) {
                String ms = parts[3];
                // 可能只有2位（百分秒）
                millis = Double.parseDouble(ms);
                int len = ms.length();
                if (len == 2) millis *= 10;
            }
            return hours * 3600 + minutes * 60 + seconds + millis / 1000.0;
        }
        return 0;
    }

    /**
     * 批量导入目录下所有字幕文件
     *
     * @param directoryPath 服务器上的目录路径
     * @return 批量导入汇总结果
     */
    @Transactional
    public BatchImportResult batchImport(String directoryPath) {
        Path dir = Paths.get(directoryPath);

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return new BatchImportResult(0, 0, 0, "目录不存在或不是文件夹: " + directoryPath);
        }

        // 提取目录名作为默认剧集名
        String defaultShowName = dir.getFileName().toString();

        List<Path> subtitleFiles = new ArrayList<>();
        try (var stream = Files.walk(dir, 3)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> {
                      String name = p.getFileName().toString().toLowerCase();
                      return name.endsWith(".srt") || name.endsWith(".ass") || name.endsWith(".vtt");
                  })
                  .forEach(subtitleFiles::add);
        } catch (IOException e) {
            return new BatchImportResult(0, 0, 0, "遍历目录失败: " + e.getMessage());
        }

        if (subtitleFiles.isEmpty()) {
            return new BatchImportResult(0, 0, 0, "目录下未找到 .srt / .ass / .vtt 文件");
        }

        int totalSuccess = 0;
        int totalDuplicate = 0;
        int totalFailed = 0;
        List<String> errors = new ArrayList<>();

        for (Path filePath : subtitleFiles) {
            try {
                byte[] bytes = Files.readAllBytes(filePath);
                String content = new String(bytes, StandardCharsets.UTF_8);
                if (content.contains("\uFFFD")) {
                    content = new String(bytes, Charset.forName("GBK"));
                }

                String fileName = filePath.getFileName().toString();
                ImportResult result = importSubtitle(fileName, content, defaultShowName);

                totalSuccess += result.importedCount;
                totalDuplicate += result.duplicateCount;

                if (!result.isSuccess()) {
                    totalFailed++;
                    errors.add(fileName + ": " + result.error);
                }
            } catch (IOException e) {
                totalFailed++;
                errors.add(filePath.getFileName().toString() + ": 读取失败 - " + e.getMessage());
            }
        }

        return new BatchImportResult(totalSuccess, totalDuplicate, totalFailed, errors.isEmpty() ? null : String.join("\n", errors));
    }

    /**
     * 解析导入记录 DTO
     */
    public List<Map<String, Object>> getAllImportHistory() {
        List<SubtitleImportLog> logs = importLogRepository.findAllByOrderByImportedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SubtitleImportLog log : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("showId", log.getShowId());
            item.put("fileName", log.getFileName());
            item.put("format", log.getFormat());
            item.put("sentenceCount", log.getSentenceCount());
            item.put("duplicateCount", log.getDuplicateCount());
            item.put("importedAt", log.getImportedAt() != null ? log.getImportedAt().toString() : "");
            // 获取剧集名称
            String showName = showRepository.findById(log.getShowId())
                    .map(Show::getName).orElse("未知");
            item.put("showName", showName);
            result.add(item);
        }
        return result;
    }

    /**
     * 删除导入记录（同时删除关联剧集？不，只删除记录本身）
     */
    @Transactional
    public void deleteImportLog(Long id) {
        importLogRepository.deleteById(id);
    }

    // ── 内部类 ──

    /** 字幕条目 */
    public static class SubtitleEntry {
        public final double startTime;
        public final double endTime;
        public final String text;

        public SubtitleEntry(double startTime, double endTime, String text) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.text = text;
        }
    }

    /** 导入结果 */
    public static class ImportResult {
        public final int importedCount;
        public final int duplicateCount;
        public final String error;

        public ImportResult(int importedCount, int duplicateCount, String error) {
            this.importedCount = importedCount;
            this.duplicateCount = duplicateCount;
            this.error = error;
        }

        public boolean isSuccess() {
            return error == null;
        }
    }

    /** 批量导入汇总结果 */
    public static class BatchImportResult {
        public final int totalImported;
        public final int totalDuplicate;
        public final int totalFailed;
        public final String error;

        public BatchImportResult(int totalImported, int totalDuplicate, int totalFailed, String error) {
            this.totalImported = totalImported;
            this.totalDuplicate = totalDuplicate;
            this.totalFailed = totalFailed;
            this.error = error;
        }

        public boolean isSuccess() {
            return error == null;
        }
    }
}
