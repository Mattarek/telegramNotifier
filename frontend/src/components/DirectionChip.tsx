import { Chip } from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import TrendingFlatIcon from '@mui/icons-material/TrendingFlat';
import type { Direction, Mode } from '../api/types';
import { directionLabel, prob } from '../format';

interface Props {
  direction: Direction;
  mode: Mode;
  confidence?: number;
  horizonDays?: number;
  size?: 'small' | 'medium';
}

export function DirectionChip({ direction, mode, confidence, horizonDays, size = 'small' }: Props) {
  const icon = direction === 'UP' ? <TrendingUpIcon /> : direction === 'DOWN' ? <TrendingDownIcon /> : <TrendingFlatIcon />;
  const color = direction === 'UP' ? 'success' : direction === 'DOWN' ? 'error' : 'default';
  const parts = [
    horizonDays ? `${horizonDays}d` : null,
    directionLabel(direction, mode),
    confidence !== undefined ? prob(confidence) : null,
  ].filter(Boolean);
  return <Chip size={size} icon={icon} color={color} variant="outlined" label={parts.join(' · ')} />;
}
