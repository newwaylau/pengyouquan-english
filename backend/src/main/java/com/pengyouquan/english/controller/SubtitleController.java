package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.service.SubtitleService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字幕导入接口
 * 支持 SRT / ASS / VTT 格式上传解析
 */
@RestController
@RequestMapping("/api/subtitle")
public class SubtitleController {

    private final SubtitleService subtitleService;

    public SubtitleController(SubtitleService subtitleService) {
        this.subtitleService = subtitleService;
    }

    /**
     * 上传字幕文件
     * POST /api/subtitle/upload
     * 支持 multipart/form-data: file, showName
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "showName", required = false) String showName) {

        if (file.isEmpty()) {
            return ApiResponse.badRequest("文件为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            return ApiResponse.badRequest("文件名无效");
        }

        // 读取文件内容（尝试 UTF-8 和 GBK 两种编码）
        String content = readFileContent(file);

        if (content == null || content.isBlank()) {
            return ApiResponse.badRequest("无法读取文件内容");
        }

        // 执行导入
        SubtitleService.ImportResult result = subtitleService.importSubtitle(fileName, content, showName);

        if (!result.isSuccess()) {
            return ApiResponse.badRequest(result.error);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("importedCount", result.importedCount);
        data.put("duplicateCount", result.duplicateCount);
        data.put("fileName", fileName);
        data.put("showName", showName != null && !showName.isBlank() ? showName : subtitleService.extractShowName(fileName));

        return ApiResponse.success(data);
    }

    /**
     * 获取导入历史
     * GET /api/subtitle/history
     */
    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history() {
        return ApiResponse.success(subtitleService.getAllImportHistory());
    }

    /**
     * 删除导入记录
     * DELETE /api/subtitle/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subtitleService.deleteImportLog(id);
        return ApiResponse.success();
    }

    /**
     * 批量导入目录下所有字幕文件
     * POST /api/subtitle/batch-import
     * 请求体: { "directoryPath": "/path/to/subtitles" }
     */
    @PostMapping("/batch-import")
    public ApiResponse<Map<String, Object>> batchImport(@RequestBody Map<String, String> request) {
        String directoryPath = request.get("directoryPath");
        if (directoryPath == null || directoryPath.isBlank()) {
            return ApiResponse.badRequest("请提供目录路径");
        }

        SubtitleService.BatchImportResult result = subtitleService.batchImport(directoryPath.trim());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalImported", result.totalImported);
        data.put("totalDuplicate", result.totalDuplicate);
        data.put("totalFailed", result.totalFailed);
        data.put("directoryPath", directoryPath);

        if (!result.isSuccess()) {
            return ApiResponse.badRequest(result.error);
        }

        return ApiResponse.success(data);
    }

    /**
     * 获取支持的字幕格式
     * GET /api/subtitle/formats
     */
    @GetMapping("/formats")
    public ApiResponse<List<Map<String, Object>>> formats() {
        List<Map<String, Object>> formatList = List.of(
            Map.of("format", "srt", "name", "SubRip (.srt)", "description", "最通用的字幕格式，支持中英双语"),
            Map.of("format", "ass", "name", "Advanced SubStation Alpha (.ass)", "description", "高级字幕格式，支持样式和特效"),
            Map.of("format", "vtt", "name", "WebVTT (.vtt)", "description", "Web 视频字幕格式")
        );
        return ApiResponse.success(formatList);
    }

    /**
     * 读取文件内容（UTF-8 优先，失败时尝试 GBK）
     */
    private String readFileContent(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            // 先尝试 UTF-8
            String content = new String(bytes, StandardCharsets.UTF_8);
            // 检查是否有非法字符
            if (!content.contains("�")) {
                return content;
            }
            // 有乱码，尝试 GBK
            return new String(bytes, Charset.forName("GBK"));
        } catch (IOException e) {
            return null;
        }
    }
}
