import React, { useState, useEffect, useRef } from 'react';
import { api } from './api/client';

type UploadState = 'idle' | 'uploading' | 'parsing' | 'done' | 'error';
type BatchImportState = 'idle' | 'importing' | 'done' | 'error';

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
  const [batchDir, setBatchDir] = useState('');
  const [batchState, setBatchState] = useState<BatchImportState>('idle');
  const [batchMsg, setBatchMsg] = useState('');

  useEffect(() => {
    api.subtitleFormats().then(r => {
      if (r.code === 200) setFormats(r.data);
    });
  }, []);

  const loadHistory = () => {
    setHistoryLoading(true);
    api.subtitleHistory().then(r => {
      if (r.code === 200) setHistory(r.data);
      setHistoryLoading(false);
    });
  };

  useEffect(() => { loadHistory(); }, []);

  const handleFileSelect = (f: File) => {
    setFile(f);
    const dotIdx = f.name.lastIndexOf('.');
    const name = dotIdx > 0 ? f.name.substring(0, dotIdx) : f.name;
    setShowName(name);
    setUploadState('idle');
    setUploadMsg('');
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    if (e.dataTransfer.files.length > 0) handleFileSelect(e.dataTransfer.files[0]);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = () => setDragOver(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) handleFileSelect(e.target.files[0]);
  };

  const handleUpload = async () => {
    if (!file) return;
    setUploadState('uploading');
    setUploadMsg('正在上传...');

    const formData = new FormData();
    formData.append('file', file);
    if (showName.trim()) formData.append('showName', showName.trim());

    try {
      setUploadState('parsing');
      setUploadMsg('正在解析字幕...');
      const r = await api.subtitleUpload(formData);
      if (r.code === 200) {
        setUploadState('done');
        const data = r.data;
        const dupMsg = data.duplicateCount > 0 ? `（重复跳过 ${data.duplicateCount} 条）` : '';
        setUploadMsg(`导入完成：共导入 ${data.importedCount} 条句子${dupMsg}`);
        setFile(null);
        setShowName('');
        loadHistory();
        if (fileInputRef.current) fileInputRef.current.value = '';
      } else {
        setUploadState('error');
        setUploadMsg(`导入失败：${r.message || '未知错误'}`);
      }
    } catch {
      setUploadState('error');
      setUploadMsg('上传请求失败，请检查网络');
    }
  };

  const handleDelete = async (id: number) => {
    const r = await api.subtitleDelete(id);
    if (r.code === 200) loadHistory();
  };

  const handleBatchImport = async () => {
    setBatchState('importing');
    setBatchMsg('正在扫描目录并导入字幕...');
    try {
      const r = await api.subtitleBatchImport(batchDir.trim());
      if (r.code === 200) {
        const d = r.data;
        const parts = [`导入完成：共导入 ${d.totalImported} 句`];
        if (d.totalDuplicate > 0) parts.push(`重复跳过 ${d.totalDuplicate} 句`);
        if (d.totalFailed > 0) parts.push(`失败 ${d.totalFailed} 个文件`);
        setBatchMsg(parts.join('，'));
        setBatchState('done');
        loadHistory();
      } else {
        setBatchMsg(`导入失败：${r.message || '未知错误'}`);
        setBatchState('error');
      }
    } catch {
      setBatchMsg('请求失败，请检查网络');
      setBatchState('error');
    }
  };

  const formatTime = (t: string) => {
    try { return t.replace('T', ' ').substring(0, 19); }
    catch { return t; }
  };

  return (
    <div className="subtitle-page subtitle-v2-page">
      <header>
        <div className="page-eyebrow">SUBTITLE IMPORT</div>
        <h1 className="page-title">导入字幕</h1>
        <p className="page-sub">上传字幕文件或扫描本地目录，自动解析并导入听写句库。</p>
      </header>

      <section className="subtitle-v2-grid">
        <div className="card card-elevated subtitle-v2-upload-card">
          <div className="cap">单文件上传</div>
          <div
            className={`subtitle-v2-drop ${dragOver ? 'drag-over' : ''}`}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onClick={() => fileInputRef.current?.click()}
          >
            <input ref={fileInputRef} type="file" accept=".srt,.ass,.vtt" style={{ display: 'none' }} onChange={handleInputChange} />
            <div className="subtitle-v2-upload-icon">↑</div>
            <div className="subtitle-v2-upload-text">
              {file ? <span className="mono">{file.name} ({(file.size / 1024).toFixed(1)} KB)</span> : <>拖拽字幕文件到此处，或<strong>点击选择文件</strong></>}
            </div>
          </div>
          <div className="subtitle-v2-chips">
            <span className="chip chip-ok">时间码 ✓</span>
            <span className="chip chip-ok">双语对齐 ✓</span>
            <span className="chip chip-warn">重复自动跳过</span>
          </div>
          <label className="field">
            <span className="field-label">剧集名称</span>
            <input className="input" value={showName} onChange={e => setShowName(e.target.value)} placeholder="从文件名自动识别，可手动修改" />
          </label>
          <button className="btn btn-primary" disabled={!file || uploadState === 'uploading' || uploadState === 'parsing'} onClick={handleUpload}>
            {uploadState === 'uploading' ? '上传中...' : uploadState === 'parsing' ? '解析中...' : '导入字幕'}
          </button>
          {uploadState !== 'idle' && uploadMsg && <div className={`upload-feedback ${uploadState === 'done' ? 'success' : uploadState === 'error' ? 'error' : 'info'}`}>{uploadMsg}</div>}
        </div>

        <div className="card subtitle-v2-batch-card">
          <div className="cap">批量导入</div>
          <label className="field">
            <span className="field-label">目录路径</span>
            <input className="input" value={batchDir} onChange={e => setBatchDir(e.target.value)} placeholder="/path/to/subtitle/folder" disabled={batchState === 'importing'} />
          </label>
          <button className="btn btn-secondary" disabled={!batchDir.trim() || batchState === 'importing'} onClick={handleBatchImport}>
            {batchState === 'importing' ? '导入中...' : '扫描并导入'}
          </button>
          <div className="subtitle-v2-format-list">
            {formats.map(f => <span key={f.format} className="chip chip-teal">.{f.format} {f.name}</span>)}
          </div>
          {batchState !== 'idle' && batchMsg && <div className={`upload-feedback ${batchState === 'done' ? 'success' : 'error'}`}>{batchMsg}</div>}
        </div>
      </section>

      <section className="card subtitle-v2-history">
        <div className="row"><span className="cap">导入历史</span><span className="spacer" /><span className="chip">{history.length} 条</span></div>
        {historyLoading ? <div className="loading">加载中...</div> : history.length === 0 ? <div className="empty-state">暂无导入记录</div> : (
          <table className="table">
            <tbody>
              {history.map(record => (
                <tr key={record.id}>
                  <td><span className="chip chip-teal">{record.format.toUpperCase()}</span></td>
                  <td><div className="mono">{record.fileName}</div><div className="muted">{record.showName}</div></td>
                  <td>{record.sentenceCount} 句</td>
                  <td>{record.duplicateCount > 0 ? <span className="chip chip-warn">重复 {record.duplicateCount}</span> : <span className="chip chip-ok">无重复</span>}</td>
                  <td className="muted">{formatTime(record.importedAt)}</td>
                  <td><button className="btn btn-danger btn-sm" onClick={() => handleDelete(record.id)}>删除</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
