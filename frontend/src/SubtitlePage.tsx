import React, { useState, useEffect, useRef } from 'react';
import { api } from './api/client';

/** 上传状态 */
type UploadState = 'idle' | 'uploading' | 'parsing' | 'done' | 'error';

interface ImportRecord {
  id: number;
  showId: number;
  showName: string;
  fileName: string;
  format: string;
  sentenceCount: number;
  duplicateCount: number;
  importedAt: string;
}

export default function SubtitlePage() {
  const [dragOver, setDragOver] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [showName, setShowName] = useState('');
  const [uploadState, setUploadState] = useState<UploadState>('idle');
  const [uploadMsg, setUploadMsg] = useState('');
  const [formats, setFormats] = useState<{ format: string; name: string }[]>([]);
  const [history, setHistory] = useState<ImportRecord[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 加载支持的字幕格式
  useEffect(() => {
    api.subtitleFormats().then(r => {
      if (r.code === 200) setFormats(r.data);
    });
  }, []);

  // 加载导入历史
  const loadHistory = () => {
    setHistoryLoading(true);
    api.subtitleHistory().then(r => {
      if (r.code === 200) setHistory(r.data);
      setHistoryLoading(false);
    });
  };

  useEffect(() => { loadHistory(); }, []);

  // 文件选择
  const handleFileSelect = (f: File) => {
    setFile(f);
    // 自动提取剧集名（去掉扩展名）
    const dotIdx = f.name.lastIndexOf('.');
    const name = dotIdx > 0 ? f.name.substring(0, dotIdx) : f.name;
    setShowName(name);
    setUploadState('idle');
    setUploadMsg('');
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    if (e.dataTransfer.files.length > 0) {
      handleFileSelect(e.dataTransfer.files[0]);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = () => setDragOver(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      handleFileSelect(e.target.files[0]);
    }
  };

  // 上传
  const handleUpload = async () => {
    if (!file) return;
    setUploadState('uploading');
    setUploadMsg('正在上传...');

    const formData = new FormData();
    formData.append('file', file);
    if (showName.trim()) {
      formData.append('showName', showName.trim());
    }

    try {
      setUploadState('parsing');
      setUploadMsg('正在解析字幕...');
      const r = await api.subtitleUpload(formData);
      if (r.code === 200) {
        setUploadState('done');
        const data = r.data;
        const dupMsg = data.duplicateCount > 0 ? `（重复跳过 ${data.duplicateCount} 条）` : '';
        setUploadMsg(`✅ 导入完成！共导入 ${data.importedCount} 条句子${dupMsg}`);
        setFile(null);
        setShowName('');
        // 刷新历史
        loadHistory();
        // 重置文件输入
        if (fileInputRef.current) fileInputRef.current.value = '';
      } else {
        setUploadState('error');
        setUploadMsg(`❌ 导入失败：${r.message || '未知错误'}`);
      }
    } catch (err) {
      setUploadState('error');
      setUploadMsg('❌ 上传请求失败，请检查网络');
    }
  };

  // 删除记录
  const handleDelete = async (id: number) => {
    const r = await api.subtitleDelete(id);
    if (r.code === 200) {
      loadHistory();
    }
  };

  // 格式化时间
  const formatTime = (t: string) => {
    try {
      return t.replace('T', ' ').substring(0, 19);
    } catch {
      return t;
    }
  };

  return (
    <div className="subtitle-page">
      <h2>📄 导入字幕</h2>

      {/* 上传区 */}
      <div
        className={`upload-zone ${dragOver ? 'drag-over' : ''}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onClick={() => fileInputRef.current?.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".srt,.ass,.vtt"
          style={{ display: 'none' }}
          onChange={handleInputChange}
        />
        <div className="upload-icon">📁</div>
        <div className="upload-text">
          {file ? (
            <span className="file-selected">{file.name} ({(file.size / 1024).toFixed(1)} KB)</span>
          ) : (
            <>拖拽字幕文件到此处，或<strong>点击选择文件</strong></>
          )}
        </div>
        <div className="upload-hint">
          支持格式：
          {formats.map((f, i) => (
            <span key={f.format} className="format-badge">
              .{f.format}
            </span>
          ))}
        </div>
      </div>

      {/* 剧集名输入 */}
      <div className="subtitle-form-row">
        <label>剧集名称</label>
        <input
          className="subtitle-input"
          value={showName}
          onChange={e => setShowName(e.target.value)}
          placeholder="从文件名自动识别，可手动修改"
        />
      </div>

      {/* 上传按钮 */}
      <button
        className="upload-btn"
        disabled={!file || uploadState === 'uploading' || uploadState === 'parsing'}
        onClick={handleUpload}
      >
        {uploadState === 'uploading' ? '🔄 上传中...' :
         uploadState === 'parsing' ? '🔄 解析中...' : '⬆️ 导入字幕'}
      </button>

      {/* 进度反馈 */}
      {uploadState !== 'idle' && uploadMsg && (
        <div className={`upload-feedback ${uploadState === 'done' ? 'success' : uploadState === 'error' ? 'error' : 'info'}`}>
          {uploadMsg}
        </div>
      )}

      {/* 导入历史 */}
      <div className="history-section">
        <h3>📋 导入历史</h3>
        {historyLoading ? (
          <div className="loading">加载中...</div>
        ) : history.length === 0 ? (
          <div className="empty-state">暂无导入记录</div>
        ) : (
          <div className="history-list">
            {history.map(record => (
              <div key={record.id} className="history-item">
                <div className="history-info">
                  <div className="history-show">{record.showName}</div>
                  <div className="history-meta">
                    <span className="format-tag">{record.format.toUpperCase()}</span>
                    <span>{record.sentenceCount} 句</span>
                    {record.duplicateCount > 0 && <span className="dup-badge">重复 {record.duplicateCount}</span>}
                    <span className="history-time">{formatTime(record.importedAt)}</span>
                  </div>
                  <div className="history-file">{record.fileName}</div>
                </div>
                <button className="small-btn delete-btn" onClick={() => handleDelete(record.id)}>
                  🗑️ 删除
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
