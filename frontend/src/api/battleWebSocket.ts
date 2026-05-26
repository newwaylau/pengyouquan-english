import { Client, IMessage } from '@stomp/stompjs';

type GameCallbacks = {
  onGameStart: (data: any) => void;
  onTurnStart: (data: any) => void;
  onQuestion: (data: any) => void;
  onCardPlayResult: (data: any) => void;
  onAttackDeclared: (data: any) => void;
  onDefenseQuestion: (data: any) => void;
  onAttackResult: (data: any) => void;
  onDefenseResult: (data: any) => void;
  onGameState: (data: any) => void;
  onGameOver: (data: any) => void;
  onCombo: (data: any) => void;
  onError: (data: any) => void;
  onMatchFound: (data: any) => void;
  onOpponentAction: (data: any) => void;
  onOpponentTurn: (data: any) => void;
};

class BattleWebSocket {
  private client: Client;
  private token: string;
  private connected: boolean = false;
  private sessionId: string | null = null;
  private callbacks: GameCallbacks | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(token: string) {
    this.token = token;
    this.client = new Client({
      webSocketFactory: () => {
        const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
        const host = window.location.host;
        return new (require('sockjs-client'))(`${protocol}//${host}/ws`);
      },
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true;
        console.log('[BattleWS] Connected');
        this.resubscribe();
      },
      onDisconnect: () => {
        this.connected = false;
        console.log('[BattleWS] Disconnected');
      },
      onStompError: (frame) => {
        console.error('[BattleWS] STOMP error:', frame.headers['message']);
        this.connected = false;
      },
    });
  }

  connect() {
    if (!this.connected) {
      this.client.activate();
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.callbacks = null;
    this.sessionId = null;
    if (this.client) {
      try { this.client.deactivate(); } catch (e) { /* ignore */ }
    }
    this.connected = false;
  }

  isConnected(): boolean {
    return this.connected;
  }

  setSessionId(sessionId: string) {
    this.sessionId = sessionId;
  }

  setCallbacks(cbs: GameCallbacks) {
    this.callbacks = cbs;
  }

  private resubscribe() {
    if (!this.callbacks) return;

    // 匹配
    this.client.subscribe('/user/queue/match-found', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'MATCH_FOUND') {
        this.sessionId = data.sessionId;
        this.callbacks?.onMatchFound(data);
      }
    });

    // 游戏开始
    this.client.subscribe('/user/queue/game-start', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'GAME_START') {
        this.sessionId = data.sessionId;
        this.callbacks?.onGameStart(data);
      }
    });

    // 回合开始
    this.client.subscribe('/user/queue/turn-start', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'TURN_START') {
        this.callbacks?.onTurnStart(data);
      }
    });

    // 考题
    this.client.subscribe('/user/queue/question', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'QUESTION') {
        this.callbacks?.onQuestion(data);
      }
    });

    // 出牌结果
    this.client.subscribe('/user/queue/card-result', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'CARD_PLAY_RESULT') {
        this.callbacks?.onCardPlayResult(data);
      }
    });

    // 攻击声明
    this.client.subscribe('/user/queue/attack-declared', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'ATTACK_DECLARED') {
        this.callbacks?.onAttackDeclared(data);
      }
    });

    // 防御题
    this.client.subscribe('/user/queue/defense-question', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'DEFENSE_QUESTION') {
        this.callbacks?.onDefenseQuestion(data);
      }
    });

    // 攻击结果
    this.client.subscribe('/user/queue/attack-result', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'ATTACK_RESULT') {
        this.callbacks?.onAttackResult(data);
      }
    });

    // 防御结果
    this.client.subscribe('/user/queue/defense-result', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'ATTACK_RESULT') {
        this.callbacks?.onDefenseResult(data);
      }
    });

    // 游戏状态
    this.client.subscribe('/user/queue/game-state', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'GAME_STATE') {
        this.callbacks?.onGameState(data);
      }
    });

    // 游戏结束
    this.client.subscribe('/user/queue/game-over', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'GAME_OVER') {
        this.callbacks?.onGameOver(data);
      }
    });

    // 连击
    this.client.subscribe('/user/queue/combo', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'COMBO_BONUS') {
        this.callbacks?.onCombo(data);
      }
    });

    // 对手动作
    this.client.subscribe('/user/queue/opponent-action', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      this.callbacks?.onOpponentAction(data);
    });

    // 对手回合
    this.client.subscribe('/user/queue/opponent-turn', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'OPPONENT_TURN') {
        this.callbacks?.onOpponentTurn(data);
      }
    });

    // 错误
    this.client.subscribe('/user/queue/errors', (msg: IMessage) => {
      const data = JSON.parse(msg.body);
      if (data.type === 'ERROR') {
        this.callbacks?.onError(data);
      }
    });
  }

  // ---- 发送动作 ----

  joinQueue() {
    this.client.publish({
      destination: '/app/battle/join-queue',
      body: JSON.stringify({}),
    });
  }

  cancelQueue() {
    this.client.publish({
      destination: '/app/battle/cancel-queue',
      body: JSON.stringify({}),
    });
  }

  playCard(sessionId: string, cardId: number) {
    this.client.publish({
      destination: '/app/battle/play-card',
      body: JSON.stringify({ sessionId, cardId }),
    });
  }

  submitAnswer(sessionId: string, correct: boolean) {
    this.client.publish({
      destination: '/app/battle/submit-answer',
      body: JSON.stringify({ sessionId, correct }),
    });
  }

  attack(sessionId: string, attackerCardId: number, targetType: 'minion' | 'hero', targetId?: number) {
    this.client.publish({
      destination: '/app/battle/attack',
      body: JSON.stringify({ sessionId, attackerCardId, targetType, targetId }),
    });
  }

  submitDefense(sessionId: string, correct: boolean) {
    this.client.publish({
      destination: '/app/battle/submit-defense',
      body: JSON.stringify({ sessionId, correct }),
    });
  }

  endTurn() {
    this.client.publish({
      destination: '/app/battle/end-turn',
      body: JSON.stringify({}),
    });
  }

  concede() {
    this.client.publish({
      destination: '/app/battle/concede',
      body: JSON.stringify({}),
    });
  }
}

export default BattleWebSocket;
