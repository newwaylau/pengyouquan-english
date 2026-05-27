/** 卡牌响应 DTO — 对应后端 CardResponse.java */
export interface CardResponse {
  id: number;
  showId: number;
  showName: string;
  nameCn: string;
  nameEn: string;
  cardType: 'minion' | 'spell' | 'equipment' | 'location';
  rarity: 'common' | 'rare' | 'epic' | 'legendary';
  cost: number;
  attack: number | null;
  health: number | null;
  effectJson: string | null;
  keywords: string | null;
  challengeType: string | null;
  faction: string | null;
  race: 'dragon' | 'undead' | 'human' | 'beast' | 'ice' | 'holy' | null;
  element: 'fire' | 'ice' | 'shadow' | 'light' | 'nature' | 'metal' | null;
  quoteText: string | null;
  quantity: number;
  hasGolden: boolean;
  golden: boolean;
  goldenAttackBonus: number;
  goldenHealthBonus: number;
}
