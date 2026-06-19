export type BoardState = number[][];

export interface Move {
  row: number;
  col: number;
  player: number; // 1=black, 2=white
  timestamp: number;
}

export interface GameRecord {
  id: string;
  moves: Move[];
  winner: number | null; // 0=draw, 1=black, 2=white, null=ongoing
  createdAt: string;
  duration: number;
}

export interface AIConfig {
  depth: number;
  enabled: boolean;
  playerColor: number; // AI plays as this color
}

export type GameStatus = 'idle' | 'playing' | 'finished' | 'replaying';

export interface Player {
  id: string;
  name: string;
  isReady: boolean;
  color: number | null; // 1=black, 2=white, null=not assigned
}

export type RoomStatus = 'WAITING' | 'PREPARING' | 'PLAYING' | 'FINISHED';

export interface Room {
  id: string;
  name: string;
  ownerId: string;
  players: Player[];
  status: RoomStatus;
  gameId: string | null;
  maxPlayers: number;
  createdAt: number;
}

export interface CurrentUser {
  id: string;
  name: string;
}

export type AppView = 'lobby' | 'room' | 'solo';
