package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.service.TtsService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * TTS 语音接口
 * - 生成并播放边缘语音
 * - 列出可用音色
 * - 播放预生成音频
 */
@RestController
@RequestMapping("/api")
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    /** 获取 TTS 音频 */
    @GetMapping("/tts")
    public ResponseEntity<Resource> tts(
            @RequestParam String text,
            @RequestParam(defaultValue = "en-GB-RyanNeural") String voice) {
        try {
            java.io.File audio = ttsService.generateTts(text, voice);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .body(new FileSystemResource(audio));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** 列出声色 */
    @GetMapping("/tts/voices")
    public ApiResponse<List<Map<String, String>>> listVoices() {
        return ApiResponse.success(ttsService.getVoices());
    }

    /** 播放预生成音频 */
    @GetMapping("/audio/{filename:.+}")
    public ResponseEntity<Resource> serveAudio(@PathVariable String filename) {
        java.io.File audio = ttsService.getAudioFile(filename);
        if (audio == null || !audio.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(new FileSystemResource(audio));
    }
}
