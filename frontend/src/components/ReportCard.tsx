import { Alert, Box, Chip, Divider, List, ListItem, ListItemIcon, ListItemText, Stack, Typography } from '@mui/material';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import RemoveCircleOutlineIcon from '@mui/icons-material/RemoveCircleOutline';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import type { News, Report } from '../api/types';
import { date, num } from '../format';
import { DirectionChip } from './DirectionChip';

interface Props {
  report: Report | null;
  news?: News[];
  compact?: boolean;
}

function sentimentChip(sentiment: number | null) {
  if (sentiment === null) return null;
  const label = sentiment > 0.25 ? 'pozytywny' : sentiment < -0.25 ? 'negatywny' : 'neutralny';
  const color = sentiment > 0.25 ? 'success' : sentiment < -0.25 ? 'error' : 'default';
  return <Chip size="small" color={color} label={`sentyment ${label} (${num(sentiment)})`} />;
}

export function ReportCard({ report, news = [], compact = false }: Props) {
  if (!report) {
    return (
      <Alert severity="info" variant="outlined">
        Brak analizy AI. Uruchom analizę (przycisk na górze) - bez klucza ANTHROPIC_API_KEY działa tylko reguła bazowa.
      </Alert>
    );
  }
  const newsById = new Map(news.map((item) => [item.id, item]));
  return (
    <Stack spacing={1.5}>
      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center">
        <Typography variant="caption" color="text.secondary">
          Analiza z {date(report.referenceDate)} {report.model && `· ${report.model}`}
        </Typography>
        {sentimentChip(report.sentiment)}
        {report.predictions.map((p) => (
          <DirectionChip key={p.id} direction={p.direction} mode={p.mode} confidence={p.confidence} horizonDays={p.horizonDays} />
        ))}
      </Stack>

      <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>
        {report.summary}
      </Typography>

      {report.mode === 'TRADE' && report.tradeBias && (
        <Box sx={{ p: 1.5, borderRadius: 1, bgcolor: 'action.hover' }}>
          <Typography variant="subtitle2">
            Setup: {report.setupType ?? '–'} · kierunek {report.tradeBias}
          </Typography>
          {report.stopPrice !== null && report.targetPrice !== null && report.entryPrice !== null ? (
            <Typography variant="body2">
              Odniesienie {num(report.entryPrice)} · stop {num(report.stopPrice)} · cel {num(report.targetPrice)} · R:R{' '}
              {num(Math.abs(report.targetPrice - report.entryPrice) / Math.abs(report.entryPrice - report.stopPrice))}
            </Typography>
          ) : (
            <Typography variant="body2" color="text.secondary">
              Brak poziomów - model nie widzi przewagi.
            </Typography>
          )}
        </Box>
      )}

      {!compact && report.keyFactors.length > 0 && (
        <>
          <Divider />
          <Typography variant="subtitle2">Kluczowe czynniki</Typography>
          <List dense disablePadding>
            {report.keyFactors.map((factor, index) => (
              <ListItem key={index} disableGutters alignItems="flex-start">
                <ListItemIcon sx={{ minWidth: 32, mt: 0.5 }}>
                  {factor.impact === 'POSITIVE' ? (
                    <AddCircleOutlineIcon color="success" fontSize="small" />
                  ) : factor.impact === 'NEGATIVE' ? (
                    <RemoveCircleOutlineIcon color="error" fontSize="small" />
                  ) : (
                    <RadioButtonUncheckedIcon fontSize="small" />
                  )}
                </ListItemIcon>
                <ListItemText
                  primary={factor.factor}
                  secondary={(factor.newsIds ?? [])
                    .map((id) => newsById.get(id)?.title)
                    .filter(Boolean)
                    .join(' | ') || undefined}
                />
              </ListItem>
            ))}
          </List>
        </>
      )}

      {!compact && report.risks.length > 0 && (
        <>
          <Typography variant="subtitle2">Ryzyka</Typography>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            {report.risks.map((risk, index) => (
              <li key={index}>
                <Typography variant="body2">{risk}</Typography>
              </li>
            ))}
          </ul>
        </>
      )}

      {!compact && report.invalidation && (
        <Alert severity="warning" variant="outlined">
          <b>Co obali tę tezę:</b> {report.invalidation}
        </Alert>
      )}
    </Stack>
  );
}
