import { CartesianGrid, Legend, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useTheme } from '@mui/material';
import type { ChartPoint } from '../api/types';
import { num } from '../format';

interface Props {
  points: ChartPoint[];
  levels?: { label: string; value: number | null; color: string }[];
  days?: number;
}

export function PriceChart({ points, levels = [], days = 180 }: Props) {
  const theme = useTheme();
  const data = points.slice(-days);
  return (
    <ResponsiveContainer width="100%" height={320}>
      <LineChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
        <XAxis dataKey="date" tick={{ fontSize: 11 }} minTickGap={40} stroke={theme.palette.text.secondary} />
        <YAxis domain={['auto', 'auto']} tick={{ fontSize: 11 }} width={60} stroke={theme.palette.text.secondary} />
        <Tooltip
          formatter={(value) => num(Number(value))}
          contentStyle={{ background: theme.palette.background.paper, border: `1px solid ${theme.palette.divider}` }}
        />
        <Legend />
        <Line type="monotone" dataKey="close" name="Kurs" stroke={theme.palette.primary.main} dot={false} strokeWidth={2} />
        <Line type="monotone" dataKey="sma20" name="SMA20" stroke={theme.palette.warning.main} dot={false} strokeWidth={1} />
        <Line type="monotone" dataKey="sma50" name="SMA50" stroke={theme.palette.secondary.main} dot={false} strokeWidth={1} />
        {levels
          .filter((level) => level.value !== null)
          .map((level) => (
            <ReferenceLine
              key={level.label}
              y={level.value as number}
              stroke={level.color}
              strokeDasharray="4 4"
              label={{ value: level.label, position: 'insideRight', fill: level.color, fontSize: 11 }}
            />
          ))}
      </LineChart>
    </ResponsiveContainer>
  );
}
