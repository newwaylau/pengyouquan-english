package com.pengyouquan.english.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * TTS 语音服务
 * - 调用 edge-tts Python CLI 生成语音
 * - 缓存已生成的音频
 * - 提供预生成音频文件服务
 */
@Service
public class TtsService {

    private final String audioDir;
    private final String venvPython;

    private static final List<Map<String, String>> VOICES = List.of(
            Map.of("id", "en-US-JennyNeural", "name", "Jenny (US Female)", "lang", "en-US"),
            Map.of("id", "en-US-GuyNeural",   "name", "Guy (US Male)",     "lang", "en-US"),
            Map.of("id", "en-GB-SoniaNeural",  "name", "Sonia (UK Female)", "lang", "en-GB"),
            Map.of("id", "en-GB-RyanNeural",   "name", "Ryan (UK Male)",    "lang", "en-GB"),
            Map.of("id", "en-AU-NatashaNeural","name", "Natasha (AU Female)","lang", "en-AU"),
            Map.of("id", "en-AU-WilliamNeural","name", "William (AU Male)", "lang", "en-AU")
    );

    public TtsService() {
        String baseDir = System.getProperty("user.dir");
        this.audioDir = baseDir + "/data/audio";
        this.venvPython = baseDir + "/venv-tts/bin/python3";
        new File(audioDir).mkdirs();
    }

    /** 生成 TTS 音频文件，返回文件路径 */
    public File generateTts(String text, String voice) throws Exception {
        // 缓存 key = text + voice 的 MD5
        String cacheKey = md5(text + "|" + voice);
        String filename = cacheKey + ".mp3";
        File file = new File(audioDir, filename);

        if (file.exists()) {
            return file;
        }

        // 调用 edge-tts
        ProcessBuilder pb = new ProcessBuilder(
                venvPython, "-m", "edge_tts",
                "--text", text,
                "--voice", voice,
                "--write-media", file.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0 || !file.exists()) {
            throw new RuntimeException("TTS 生成失败, exit=" + exitCode);
        }

        return file;
    }

    /** 获取预生成音频文件 */
    public File getAudioFile(String filename) {
        File file = new File(audioDir, filename);
        // 防止路径穿越
        if (!file.getAbsolutePath().startsWith(new File(audioDir).getAbsolutePath())) {
            return null;
        }
        return file.exists() ? file : null;
    }

    /** 列出可用音色 */
    public List<Map<String, String>> getVoices() {
        return VOICES;
    }

    private String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.substring(0, 16);
    }
}
