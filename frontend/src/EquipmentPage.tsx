import React, { useState, useEffect } from 'react';
import { cardApi } from './api/cardClient';
import './equipment.css';

const RARITY_COLORS: Record<string, string> = {
  legendary: '#ff8c00',
  epic: '#a335ee',
  rare: '#0070dd',
  common: '#9d9d9d',
};

const RARITY_CN: Record<string, string> = {
  legendary: '传说', epic: '史诗', rare: '稀有', common: '普通',
};

const SLOT_ICONS: Record<string, string> = {
  weapon: '⚔️', armor: '🛡️', trinket: '💍', tome: '📖', crown: '👑',
};

const SLOT_CN: Record<string, string> = {
  weapon: '武器', armor: '护甲', trinket: '饰品', tome: '典籍', crown: '头冠',
};

export default function EquipmentPage({ user, onBack }: { user: any; onBack: () => void }) {
  const [data, setData] = useState<any>(null);
  const [gear, setGear] = useState<any>(null);
  const [setBonuses, setSetBonuses] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [selectedSlot, setSelectedSlot] = useState<string | null>(null);
  const [detailItem, setDetailItem] = useState<any>(null);
  const [toastMsg, setToastMsg] = useState('');
  const [stardust, setStardust] = useState(0);

  useEffect(() => {
    loadEquipment();
    loadStardust();
  }, []);

  const loadEquipment = async () => {
    setLoading(true);
    const [eqRes, sdRes] = await Promise.all([
      cardApi.getEquipment(),
      cardApi.getStardust(),
    ]);
    if (eqRes.code === 200) {
      setData(eqRes.data);
      setGear(eqRes.data.currentGear);
      setSetBonuses(eqRes.data.setBonuses);
    }
    if (sdRes.code === 200) setStardust(sdRes.data?.stardust || 0);
    setLoading(false);
  };

  const loadStardust = async () => {
    const res = await cardApi.getStardust();
    if (res.code === 200) setStardust(res.data?.stardust || 0);
  };

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(''), 2500);
  };

  const handleEquip = async (ueId: number, slot: string) => {
    const res = await cardApi.equipItem(ueId, slot);
    if (res.code === 200) {
      setGear(res.data.gear);
      setSetBonuses(res.data.setBonuses);
      setSelectedSlot(null);
      showToast('✅ 装备成功');
    } else {
      showToast(res.message || '装备失败');
    }
  };

  const handleUnequip = async (slot: string) => {
    const res = await cardApi.unequipItem(slot);
    if (res.code === 200) {
      setGear(res.data.gear);
      setSetBonuses(res.data.setBonuses);
      showToast('✅ 已卸下');
    } else {
      showToast(res.message || '卸下失败');
    }
  };

  const handleUpgrade = async (ueId: number) => {
    const res = await cardApi.upgradeEquipment(ueId);
    if (res.code === 200) {
      showToast(`✅ 升级成功！等级 ${res.data.newLevel}`);
      setDetailItem(null);
      loadEquipment();
    } else {
      showToast(res.message || '升级失败');
    }
  };

  const handleReroll = async (ueId: number) => {
    const res = await cardApi.rerollEquipment(ueId);
    if (res.code === 200) {
      showToast(`✅ 重铸成功！`);
      setDetailItem(null);
      loadEquipment();
    } else {
      showToast(res.message || '重铸失败');
    }
  };

  if (loading) return <div className="page-loading">加载中...</div>;

  const allEquipment = data?.equipment || [];
  const grouped = data?.grouped || {};
  const stats = setBonuses?.activeStats || {};

  return (
    <div className="equipment-page">
      <div className="equipment-header">
        <button className="equipment-back" onClick={onBack}>← 返回</button>
        <span className="equipment-title">装备</span>
        <span style={{ color: '#fbbf24', fontSize: 13 }}>✨ {stardust}</span>
      </div>

      {/* 当前装备 */}
      <div className="equipment-gear-section">
        <div className="equipment-section-title">当前装备</div>
        <div className="equipment-slots">
          {['weapon', 'armor', 'trinket', 'tome', 'crown'].map(slot => {
            const equipped = gear?.[slot];
            return (
              <div key={slot} className={`equipment-slot-card ${equipped ? 'equipped' : ''}`}
                onClick={() => setSelectedSlot(slot)}
                style={equipped ? { borderColor: RARITY_COLORS[equipped.rarity] || 'var(--border)' } : {}}
              >
                <div className="equipment-slot-icon">{SLOT_ICONS[slot]}</div>
                <div className="equipment-slot-name">
                  {equipped ? equipped.nameCn : SLOT_CN[slot]}
                </div>
                {equipped && (
                  <>
                    <div className="equipment-slot-rarity" style={{ color: RARITY_COLORS[equipped.rarity] }}>
                      {RARITY_CN[equipped.rarity]}
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>Lv.{equipped.level || 1}</div>
                    <button className="equipment-unequip-btn"
                      onClick={(e) => { e.stopPropagation(); handleUnequip(slot); }}>
                      ✕
                    </button>
                  </>
                )}
              </div>
            );
          })}
        </div>

        {/* 统计面板 */}
        {Object.keys(stats).length > 0 && (
          <div className="equipment-stats-panel">
            <div className="equipment-section-title">属性加成</div>
            <div className="equipment-stats-row">
              {Object.entries(stats).map(([key, val]) => (
                <div key={key} className="equipment-stat-chip">
                  {key}: +{String(val)}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 套装效果 - 增强版 */}
        {setBonuses?.sets?.length > 0 && (
          <div className="equipment-sets-panel">
            <div className="equipment-section-title">套装效果</div>
            {setBonuses.sets.map((s: any, i: number) => (
              <div key={i} className={`equipment-set-item ${s.twoPieceActive ? 'active' : ''}`}
                style={{
                  borderColor: s.fivePieceActive ? '#ffd700' : s.twoPieceActive ? 'var(--teal)' : 'var(--border)',
                  background: s.fivePieceActive ? 'rgba(255,215,0,0.08)' : s.twoPieceActive ? 'rgba(20,184,166,0.08)' : undefined,
                }}
              >
                <div className="equipment-set-name">
                  {s.nameCn || s.setName}
                  {s.fivePieceActive && ' 👑'}
                  {s.twoPieceActive && !s.fivePieceActive && ' ✅'}
                </div>
                <div className="equipment-set-progress">
                  {s.count}/{s.total} 件
                </div>
                {s.twoPieceEffect && (
                  <div className="equipment-set-eff" style={{ color: s.twoPieceActive ? 'var(--teal)' : 'var(--text-secondary)', fontSize: 12, marginTop: 4 }}>
                    [2件套] {s.twoPieceEffect} {s.twoPieceActive ? '✅' : ''}
                  </div>
                )}
                {s.fivePieceEffect && (
                  <div className="equipment-set-eff" style={{ color: s.fivePieceActive ? '#ffd700' : 'var(--text-secondary)', fontSize: 12 }}>
                    [5件套] {s.fivePieceEffect} {s.fivePieceActive ? '✅' : ''}
                  </div>
                )}
                <div className="equipment-set-items" style={{ marginTop: 6 }}>
                  {s.items.map((item: any, j: number) => (
                    <span key={j} className="equipment-set-item-name">{item.nameCn}</span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 槽位选择弹窗 */}
      {selectedSlot && (
        <div className="equipment-overlay" onClick={() => setSelectedSlot(null)}>
          <div className="equipment-modal" onClick={e => e.stopPropagation()}>
            <button className="equipment-modal-close" onClick={() => setSelectedSlot(null)}>✕</button>
            <div className="equipment-modal-title">
              {SLOT_ICONS[selectedSlot]} {SLOT_CN[selectedSlot]}
            </div>
            <div className="equipment-list">
              {(grouped[selectedSlot] || []).map((item: any) => {
                const currentEquipped = gear?.[selectedSlot];
                const isEquipped = currentEquipped?.userEquipmentId === item.userEquipmentId
                  || currentEquipped?.equipmentId === item.id;
                return (
                  <div key={item.id} className={`equipment-list-item rarity-${item.rarity} ${isEquipped ? 'equipped' : ''}`}
                    style={{ borderColor: RARITY_COLORS[item.rarity] }}
                    onClick={() => {
                      if (item.owned > 0) {
                        setDetailItem({ ...item, isEquipped });
                      }
                    }}
                  >
                    <div className="equipment-list-left">
                      <div className="equipment-list-name">{item.nameCn}</div>
                      <div className="equipment-list-en">{item.nameEn}</div>
                      <div className="equipment-list-rarity" style={{ color: RARITY_COLORS[item.rarity] }}>
                        {RARITY_CN[item.rarity]} · Lv.{item.level || 1}
                      </div>
                    </div>
                    <div className="equipment-list-right">
                      {item.owned > 0 ? (
                        <span className="equipment-list-owned">×{item.owned}</span>
                      ) : (
                        <span className="equipment-list-unlock">{item.unlockCondition || '未获得'}</span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* 装备详情弹窗 - 含升级/重铸 */}
      {detailItem && (
        <div className="equipment-overlay" onClick={() => setDetailItem(null)}>
          <div className="equipment-detail-modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 360 }}>
            <button className="equipment-modal-close" onClick={() => setDetailItem(null)}>✕</button>
            <div className="equipment-detail-rarity-bar" style={{ background: RARITY_COLORS[detailItem.rarity] }} />
            <div className="equipment-detail-icon">{SLOT_ICONS[detailItem.slot]}</div>
            <div className="equipment-detail-rarity" style={{ color: RARITY_COLORS[detailItem.rarity] }}>
              {RARITY_CN[detailItem.rarity]}
            </div>
            <div className="equipment-detail-name">{detailItem.nameCn}</div>
            <div className="equipment-detail-en">{detailItem.nameEn}</div>
            <div style={{ color: 'var(--text-secondary)', fontSize: 12, marginBottom: 8 }}>
              等级 Lv.{detailItem.level || 1}
            </div>

            {/* 基础属性 */}
            {detailItem.statBonus && Object.keys(detailItem.statBonus).length > 0 && (
              <div className="equipment-detail-stats">
                {Object.entries(detailItem.statBonus).map(([k, v]) => (
                  <div key={k} className="equipment-detail-stat">
                    {k}: +{String(v)}
                  </div>
                ))}
              </div>
            )}

            {/* 附加属性（重铸） */}
            {detailItem.bonusStats && Object.keys(detailItem.bonusStats).length > 0 && (
              <div className="equipment-detail-stats" style={{ marginTop: 8, borderTop: '1px solid var(--border)', paddingTop: 8 }}>
                <div style={{ color: '#a78bfa', fontSize: 11, marginBottom: 4 }}>额外属性（可重铸）</div>
                {Object.entries(detailItem.bonusStats).map(([k, v]) => (
                  <div key={k} className="equipment-detail-stat" style={{ color: '#a78bfa' }}>
                    {k}: +{String(v)}
                  </div>
                ))}
              </div>
            )}

            {/* 套装 */}
            {detailItem.effectJson?.set && (
              <div className="equipment-detail-set">
                套装: {detailItem.effectJson.set}
              </div>
            )}

            {/* 操作按钮 */}
            <div className="equipment-detail-actions" style={{ display: 'flex', gap: 6, flexWrap: 'wrap', justifyContent: 'center' }}>
              {detailItem.isEquipped ? (
                <button className="equipment-btn unequip" onClick={() => {
                  handleUnequip(detailItem.slot);
                  setDetailItem(null);
                }}>
                  卸下
                </button>
              ) : detailItem.owned > 0 ? (
                <button className="equipment-btn equip" onClick={() => {
                  handleEquip(detailItem.id, detailItem.slot);
                  setDetailItem(null);
                }}>
                  装备
                </button>
              ) : null}
              {detailItem.owned > 0 && (
                <>
                  <button className="equipment-btn upgrade" onClick={() => handleUpgrade(detailItem.userEquipmentId)}>
                    ⬆ 升级 (100✨)
                  </button>
                  <button className="equipment-btn reroll" onClick={() => handleReroll(detailItem.userEquipmentId)}
                    style={{ background: 'rgba(167,139,250,0.2)', color: '#a78bfa', border: '1px solid #a78bfa' }}>
                    🔄 重铸 ({detailItem.rerollCount || 0}次)
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {toastMsg && (
        <div className="equipment-toast">{toastMsg}</div>
      )}
    </div>
  );
}
