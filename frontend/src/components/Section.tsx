import { Paper, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface Props {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
}

export function Section({ title, subtitle, action, children }: Props) {
  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack direction="row" alignItems="flex-start" justifyContent="space-between" sx={{ mb: 1.5 }}>
        <div>
          <Typography variant="h6">{title}</Typography>
          {subtitle && (
            <Typography variant="body2" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </div>
        {action}
      </Stack>
      {children}
    </Paper>
  );
}

/** Simple responsive CSS grid - avoids depending on the Grid API details. */
export function Cards({ min = 320, children }: { min?: number; children: ReactNode }) {
  return (
    <Stack
      sx={{
        display: 'grid',
        gap: 2,
        gridTemplateColumns: `repeat(auto-fill, minmax(${min}px, 1fr))`,
      }}
    >
      {children}
    </Stack>
  );
}
