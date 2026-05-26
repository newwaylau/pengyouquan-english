import React, { useState, useEffect } from 'react';
import { cardApi } from './api/cardClient';
import './hero.css';

const SHOW_NAMES: Record<number, string> = { 1: 'GOT · 权力的游戏', 2: 'DA · 唐顿庄园' };
const SLOT_ICONS: Record<string, string> = {
  weapon: '⚔️', armor: '🛡️', trinket: '💍', tome: '📖', crown: '👑',
};

export default function HeroSelectPage({ user, onNavigate }: { user: any; onNavigate: (target: string, data?: any) => void }) {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [toastMsg, setToastMsg] = useState('');

  useEffect(() => {
    loadHeroes();
  }, []);

  const loadHeroes = async () => {
    setLoading(true);
    const res = await cardApi.getHeroes();
    if (res.code === 200) setData(res.data);
    setLoading(false);
  };

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(''), 2500);
  };

  const handleSelect = async (heroId: number) => {
    const res = await cardApi.selectHero(heroId);
    if (res.code === 200) {
      setData(res.data);
      showToast('✅ 英雄已切换');
    } else {
      showToast(res.message || '选择失败');
    }
  };

  if (loading) return <div className="page-loading">加载中...</div>;

  const heroes = data?.heroes || [];
  const activeHero = data?.activeHero;

  return (
    <div className="hero-page">
      <div className="hero-header">
        <span className="hero-title">🦸 英雄</span>
        <button className="hero-equip-btn" onClick={() => onNavigate('equipment', {})}>
          ⚔️ 装备
        </button>
      </div>

      {/* 当前激活英雄 */}
      {activeHero && (
        <div className="hero-active-card">
          <div className="hero-active-header">
            <div className="hero-active-avatar">
              {activeHero.nameCn?.charAt(0) || '?'}
            </div>
            <div className="hero-active-info">
              <div className="hero-active-name">{activeHero.nameCn}</div>
              <div className="hero-active-en">{activeHero.nameEn}</div>
              <div className="hero-active-health">❤️ {activeHero.health} {'>'} {activeHero.totalHealth || activeHero.health}</div>
            </div>
          </div>

          {/* 技能 */}
          {activeHero.skillNameCn && (
            <div className="hero-active-skill">
              <div className="hero-active-skill-name">
                ⚡ {activeHero.skillNameCn} (Lv.{activeHero.skillLevel || 1})
              </div>
              <div className="hero-active-skill-desc">{activeHero.skillDescriptionCn}</div>
              <div className="hero-active-skill-desc-en">{activeHero.skillDescriptionEn}</div>
            </div>
          )}

          {/* 装备预览 */}
          <div className="hero-active-gear">
            <div className="hero-gear-title">当前装备</div>
            <div className="hero-gear-slots">
              {['weapon', 'armor', 'trinket', 'tome', 'crown'].map(slot => {
                const item = activeHero.gear?.[slot];
                return (
                  <div key={slot} className={`hero-gear-slot ${item ? 'filled' : ''}`}>
                    <span className="hero-gear-icon">{SLOT_ICONS[slot]}</span>
                    <span className="hero-gear-name">{item ? item.nameCn : '-'}</span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* 全部英雄列表 */}
      <div className="hero-section-title">全部英雄</div>
      <div className="hero-list">
        {heroes.map((hero: any) => {
          const isActive = hero.isActive;
          return (
            <div key={hero.id} className={`hero-card ${isActive ? 'active' : ''} ${hero.unlocked ? 'unlocked' : 'locked'}`}>
              <div className="hero-card-left">
                {hero.unlocked ? (
                  <div className="hero-card-avatar unlocked" onClick={() => handleSelect(hero.id)}>
                    {hero.nameCn?.charAt(0) || '?'}
                  </div>
                ) : (
                  <div className="hero-card-avatar locked">🔒</div>
                )}
              </div>
              <div className="hero-card-info">
                <div className="hero-card-name">
                  {hero.nameCn}
                  {isActive && <span className="hero-card-active-badge">使用中</span>}
                </div>
                <div className="hero-card-en">{hero.nameEn}</div>
                <div className="hero-card-show">{SHOW_NAMES[hero.showId] || `剧集 ${hero.showId}`}</div>
                <div className="hero-card-stats">
                  <span>❤️ {hero.health}</span>
                  {hero.skillNameCn && <span>⚡ {hero.skillNameCn}</span>}
                </div>
                {hero.unlocked ? (
                  <div className="hero-card-skill-level">技能等级 {hero.skillLevel}</div>
                ) : (
                  <div className="hero-card-unlock-condition">
                    🔒 {hero.unlockCondition || '条件未知'}
                  </div>
                )}
              </div>
              <div className="hero-card-action">
                {hero.unlocked && !isActive && (
                  <button className="hero-select-btn" onClick={() => handleSelect(hero.id)}>
                    选择
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {toastMsg && (
        <div className="hero-toast">{toastMsg}</div>
      )}
    </div>
  );
}
