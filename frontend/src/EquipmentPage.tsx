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

  useEffect(() => {
    loadEquipment();
  }, []);

  const loadEquipment = async () => {
    setLoading(true);
    const res = await cardApi.getEquipment();
    if (res.code === 200) {
      setData(res.data);
      setGear(res.data.currentGear);
      setSetBonuses(res.data.setBonuses);
    }
    setLoading(false);
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
      showToast(`✅ 装备成功`);
    } else {
      showToast(res.message || '装备失败');
    }
  };

  const handleUnequip = async (slot: string) => {
    const res = await cardApi.unequipItem(slot);
    if (res.code === 200) {
      setGear(res.data.gear);
      setSetBonuses(res.data.setBonuses);
      showToast(`✅ 已卸下`);
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

  if (loading) return <div className="page-loading">加载中...</div>;

  const allEquipment = data?.equipment || [];
  const grouped = data?.grouped || {};

  const stats = setBonuses?.activeStats || {};

  return (
    <div className="equipment-page">
      <div className="equipment-header">
        <button className="equipment-back" onClick={onBack}>← 返回</button>
        <span className="equipment-title">装备</span>
      </div>

      {/* 当前装备配置 */}
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

        {/* 套装效果 */}
        {setBonuses?.sets?.length > 0 && (
          <div className="equipment-sets-panel">
            <div className="equipment-section-title">套装效果</div>
            {setBonuses.sets.map((s: any, i: number) => (
              <div key={i} className={`equipment-set-item ${s.active ? 'active' : ''}`}>
                <div className="equipment-set-name">
                  {s.setName} {s.active ? '✅' : ''}
                </div>
                <div className="equipment-set-progress">
                  {s.count}/{s.total} 件
                </div>
                <div className="equipment-set-items">
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
                        // 查找 userEquipmentId
                        setDetailItem({ ...item, isEquipped });
                      }
                    }}
                  >
                    <div className="equipment-list-left">
                      <div className="equipment-list-name">{item.nameCn}</div>
                      <div className="equipment-list-en">{item.nameEn}</div>
                      <div className="equipment-list-rarity" style={{ color: RARITY_COLORS[item.rarity] }}>
                        {RARITY_CN[item.rarity]}
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

      {/* 装备详情弹窗 */}
      {detailItem && (
        <div className="equipment-overlay" onClick={() => setDetailItem(null)}>
          <div className="equipment-detail-modal" onClick={e => e.stopPropagation()}>
            <button className="equipment-modal-close" onClick={() => setDetailItem(null)}>✕</button>
            <div className="equipment-detail-rarity-bar" style={{ background: RARITY_COLORS[detailItem.rarity] }} />
            <div className="equipment-detail-icon">{SLOT_ICONS[detailItem.slot]}</div>
            <div className="equipment-detail-rarity" style={{ color: RARITY_COLORS[detailItem.rarity] }}>
              {RARITY_CN[detailItem.rarity]}
            </div>
            <div className="equipment-detail-name">{detailItem.nameCn}</div>
            <div className="equipment-detail-en">{detailItem.nameEn}</div>

            {detailItem.statBonus && Object.keys(detailItem.statBonus).length > 0 && (
              <div className="equipment-detail-stats">
                {Object.entries(detailItem.statBonus).map(([k, v]) => (
                  <div key={k} className="equipment-detail-stat">
                    {k}: +{String(v)}
                  </div>
                ))}
              </div>
            )}

            {detailItem.effectJson?.set && (
              <div className="equipment-detail-set">
                套装: {detailItem.effectJson.set}
              </div>
            )}

            <div className="equipment-detail-actions">
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
