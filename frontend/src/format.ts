import type { Direction, Mode } from './api/types';

const numberFormat = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 2, minimumFractionDigits: 2 });

export function num(value: number | null | undefined, digits = 2): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '–';
  return digits === 2
    ? numberFormat.format(value)
    : new Intl.NumberFormat('pl-PL', { maximumFractionDigits: digits, minimumFractionDigits: digits }).format(value);
}

export function pct(value: number | null | undefined, digits = 2, withSign = true): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '–';
  const sign = withSign && value > 0 ? '+' : '';
  return `${sign}${num(value, digits)}%`;
}

/** 0.62 -> "62%" */
export function prob(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '–';
  return `${Math.round(value * 100)}%`;
}

export function date(value: string | null | undefined): string {
  if (!value) return '–';
  return new Date(value).toLocaleDateString('pl-PL');
}

export function dateTime(value: string | null | undefined): string {
  if (!value) return '–';
  return new Date(value).toLocaleString('pl-PL', { dateStyle: 'short', timeStyle: 'short' });
}

export function directionLabel(direction: Direction, mode: Mode): string {
  if (mode === 'INVEST') {
    return { UP: 'Lepiej od rynku', DOWN: 'Gorzej od rynku', FLAT: 'Jak rynek' }[direction];
  }
  return { UP: 'Wzrost', DOWN: 'Spadek', FLAT: 'Bez wyraźnego ruchu' }[direction];
}

export const verdictLabels: Record<string, string> = {
  CORRECT_FOR_RIGHT_REASONS: 'Trafne z właściwych powodów',
  CORRECT_BY_LUCK: 'Trafne przez przypadek',
  WRONG_BAD_REASONING: 'Błąd w rozumowaniu',
  WRONG_UNFORESEEABLE: 'Nie do przewidzenia',
  NOISE: 'Szum rynkowy',
};

export const verdictColors: Record<string, 'success' | 'warning' | 'error' | 'default' | 'info'> = {
  CORRECT_FOR_RIGHT_REASONS: 'success',
  CORRECT_BY_LUCK: 'warning',
  WRONG_BAD_REASONING: 'error',
  WRONG_UNFORESEEABLE: 'info',
  NOISE: 'default',
};

export const sourceLabel = (source: 'AI' | 'BASELINE') => (source === 'AI' ? 'AI' : 'Reguła bazowa');
