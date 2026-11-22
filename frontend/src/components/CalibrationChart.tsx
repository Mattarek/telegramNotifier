import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Typography, useTheme } from '@mui/material';
import type { Calibration } from '../api/types';

interface Props {
  ai?: Calibration;
  baseline?: Calibration;
}

/**
 * X = how confident the forecaster was, Y = how often it was actually right.
 * A well calibrated forecaster stays on the dashed diagonal.
 */
export function CalibrationChart({ ai, baseline }: Props) {
  const theme = useTheme();
  if (!ai || ai.count === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        Za mało rozliczonych predykcji AI, żeby ocenić kalibrację. Potrzeba kilkudziesięciu - to wymaga tygodni, nie dni.
      </Typography>
    );
  }
  const data = ai.buckets.map((bucket, index) => {
    const base = baseline?.buckets[index];
    return {
      bucket: `${Math.round(bucket.from * 100)}–${Math.round(bucket.to * 100)}%`,
      ideal: Math.round(((bucket.from + bucket.to) / 2) * 100),
      ai: bucket.count > 0 && bucket.hitRate !== null ? Math.round(bucket.hitRate * 100) : null,
      aiCount: bucket.count,
      baseline: base && base.count > 0 && base.hitRate !== null ? Math.round(base.hitRate * 100) : null,
    };
  });
  return (
    <ResponsiveContainer width="100%" height={260}>
      <LineChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
        <XAxis dataKey="bucket" tick={{ fontSize: 11 }} stroke={theme.palette.text.secondary} />
        <YAxis domain={[0, 100]} unit="%" tick={{ fontSize: 11 }} width={50} stroke={theme.palette.text.secondary} />
        <Tooltip contentStyle={{ background: theme.palette.background.paper, border: `1px solid ${theme.palette.divider}` }} />
        <Legend />
        <Line dataKey="ideal" name="Idealna kalibracja" stroke={theme.palette.text.secondary} strokeDasharray="5 5" dot={false} />
        <Line dataKey="ai" name="AI - trafność" stroke={theme.palette.primary.main} strokeWidth={2} connectNulls />
        <Line dataKey="baseline" name="Reguła bazowa" stroke={theme.palette.warning.main} connectNulls />
      </LineChart>
    </ResponsiveContainer>
  );
}
