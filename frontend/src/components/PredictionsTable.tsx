import { Fragment, useState } from 'react';
import {
  Box,
  Chip,
  Collapse,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import type { Prediction } from '../api/types';
import { date, num, prob, sourceLabel, verdictColors, verdictLabels } from '../format';
import { DirectionChip } from './DirectionChip';
import { Pct } from './Pct';

interface Props {
  predictions: Prediction[];
  showSymbol?: boolean;
  showTrade?: boolean;
  emptyText?: string;
}

export function PredictionsTable({ predictions, showSymbol = false, showTrade = false, emptyText }: Props) {
  if (predictions.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        {emptyText ?? 'Brak predykcji.'}
      </Typography>
    );
  }
  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell />
            {showSymbol && <TableCell>Spółka</TableCell>}
            <TableCell>Data</TableCell>
            <TableCell>Źródło</TableCell>
            <TableCell>Teza</TableCell>
            <TableCell align="right">Cena</TableCell>
            <TableCell>Wynik</TableCell>
            <TableCell align="right">Ruch oceniany</TableCell>
            {showTrade && <TableCell align="right">Transakcja</TableCell>}
            <TableCell>Wniosek AI</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {predictions.map((prediction) => (
            <PredictionRow key={prediction.id} prediction={prediction} showSymbol={showSymbol} showTrade={showTrade} />
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

function PredictionRow({ prediction: p, showSymbol, showTrade }: { prediction: Prediction; showSymbol: boolean; showTrade: boolean }) {
  const [open, setOpen] = useState(false);
  const columns = 8 + (showSymbol ? 1 : 0) + (showTrade ? 1 : 0);
  return (
    <Fragment>
      <TableRow hover sx={{ '& > td': { borderBottom: open ? 'none' : undefined } }}>
        <TableCell padding="checkbox">
          <IconButton size="small" onClick={() => setOpen(!open)} aria-label="szczegóły">
            {open ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
          </IconButton>
        </TableCell>
        {showSymbol && <TableCell sx={{ fontWeight: 600 }}>{p.symbol}</TableCell>}
        <TableCell>{date(p.referenceDate)}</TableCell>
        <TableCell>
          <Chip size="small" label={sourceLabel(p.source)} color={p.source === 'AI' ? 'primary' : 'default'} variant="outlined" />
        </TableCell>
        <TableCell>
          <DirectionChip direction={p.direction} mode={p.mode} confidence={p.confidence} horizonDays={p.horizonDays} />
        </TableCell>
        <TableCell align="right">
          {num(p.referencePrice)}
          {p.resolvedPrice !== null && ` → ${num(p.resolvedPrice)}`}
        </TableCell>
        <TableCell>
          {p.status === 'OPEN' ? (
            <Chip size="small" label="w toku" />
          ) : (
            <Chip size="small" label={p.hit ? 'trafiona' : 'chybiona'} color={p.hit ? 'success' : 'error'} />
          )}
        </TableCell>
        <TableCell align="right">
          <Pct value={p.measuredReturnPct} />
        </TableCell>
        {showTrade && (
          <TableCell align="right">
            {p.tradeOutcome ? (
              <Typography
                component="span"
                variant="body2"
                color={(p.rMultiple ?? 0) > 0 ? 'success.main' : 'error.main'}
              >
                {p.tradeOutcome} {num(p.rMultiple)}R
              </Typography>
            ) : p.stopPrice !== null ? (
              `SL ${num(p.stopPrice)} / TP ${num(p.targetPrice)}`
            ) : (
              '–'
            )}
          </TableCell>
        )}
        <TableCell>
          {p.review ? (
            <Chip size="small" color={verdictColors[p.review.verdict] ?? 'default'} label={verdictLabels[p.review.verdict] ?? p.review.verdict} />
          ) : (
            '–'
          )}
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell sx={{ py: 0 }} colSpan={columns}>
          <Collapse in={open} unmountOnExit>
            <Box sx={{ py: 1.5, display: 'grid', gap: 1 }}>
              <Typography variant="body2">
                <b>Uzasadnienie:</b> {p.rationale ?? '–'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Oczekiwany ruch: {p.expectedReturnPct === null ? '–' : <Pct value={p.expectedReturnPct} />} · zmiana kursu:{' '}
                <Pct value={p.actualReturnPct} /> · benchmark: <Pct value={p.benchmarkReturnPct} /> · Brier: {num(p.brier, 3)} ·
                pewność {prob(p.confidence)}
                {p.resolvedDate && ` · rozliczona ${date(p.resolvedDate)}`}
              </Typography>
              {p.review && (
                <>
                  <Typography variant="body2">
                    <b>Post-mortem:</b> {p.review.explanation}
                  </Typography>
                  <Typography variant="body2">
                    <b>Co naprawdę ruszyło kursem:</b> {p.review.actualDriver}
                  </Typography>
                </>
              )}
              {p.lessonsUsed.length > 0 && (
                <Typography variant="caption" color="text.secondary">
                  Użyte lekcje: {p.lessonsUsed.map((id) => `#${id}`).join(', ')}
                </Typography>
              )}
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </Fragment>
  );
}
