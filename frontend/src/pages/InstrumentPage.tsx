import { Button, Stack, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { Link, useParams } from 'react-router';
import { useInstrumentDetail } from '../api/client';
import type { Mode } from '../api/types';
import { ErrorBox, Loading } from '../components/Loading';
import { Cards, Section } from '../components/Section';
import { StatTile } from '../components/StatTile';
import { PriceChart } from '../components/PriceChart';
import { ReportCard } from '../components/ReportCard';
import { PredictionsTable } from '../components/PredictionsTable';
import { NewsList } from '../components/NewsList';
import { Pct } from '../components/Pct';
import { num } from '../format';

export function InstrumentPage({ mode }: { mode: Mode }) {
  const id = Number(useParams().id);
  const { data, isLoading, error } = useInstrumentDetail(id, mode);
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorBox error={error} />;

  const { instrument, snapshot, aiReport, baselineReport } = data;
  const backPath = mode === 'INVEST' ? '/akcje' : '/trading';
  const levels =
    mode === 'TRADE' && aiReport
      ? [
          { label: 'stop', value: aiReport.stopPrice, color: '#ef5350' },
          { label: 'cel', value: aiReport.targetPrice, color: '#66bb6a' },
        ]
      : [];

  return (
    <Stack spacing={3}>
      <Stack direction="row" spacing={2} alignItems="center">
        <Button component={Link} to={backPath} startIcon={<ArrowBackIcon />}>
          Wróć
        </Button>
        <Typography variant="h5">
          {instrument.symbol} · {instrument.name}
        </Typography>
        <Typography color="text.secondary">
          {instrument.market} · {mode === 'INVEST' ? `benchmark ${instrument.benchmarkSymbol ?? 'brak'}` : 'trading 1–5 sesji'}
        </Typography>
      </Stack>

      {snapshot && (
        <Cards min={160}>
          <StatTile label="Kurs" value={`${num(snapshot.close)} ${instrument.currency ?? ''}`} hint={snapshot.date} />
          <StatTile label="1 dzień" value={<Pct value={snapshot.return1dPct} variant="h5" />} />
          <StatTile label="20 dni" value={<Pct value={snapshot.return20dPct} variant="h5" />} />
          <StatTile label="vs benchmark 20d" value={<Pct value={snapshot.excessReturn20dPct} variant="h5" />} />
          <StatTile label="RSI 14" value={num(snapshot.rsi14, 0)} hint=">70 wykupienie, <30 wyprzedanie" />
          <StatTile label="ATR 14" value={`${num(snapshot.atrPct)}%`} hint="typowy dzienny zakres" />
          <StatTile label="Zmienność roczna" value={`${num(snapshot.volatility20Pct, 0)}%`} />
          <StatTile label="Od szczytu 52t" value={<Pct value={snapshot.high52w && snapshot.close ? (snapshot.close / snapshot.high52w - 1) * 100 : null} variant="h5" />} />
        </Cards>
      )}

      <Section title="Wykres" subtitle="Kurs zamknięcia ze średnimi 20 i 50 sesji">
        <PriceChart points={data.chart} levels={levels} />
      </Section>

      <Cards min={420}>
        <Section title="Analiza AI" subtitle={mode === 'INVEST' ? 'Względem benchmarku' : 'Setup krótkoterminowy'}>
          <ReportCard report={aiReport} news={data.news} />
        </Section>
        <Section title="Reguła bazowa" subtitle="Prosty punkt odniesienia, który AI ma pokonać">
          {baselineReport ? <ReportCard report={baselineReport} compact /> : <Typography color="text.secondary">Brak.</Typography>}
        </Section>
      </Cards>

      <Section title="Historia tez" subtitle="Rozwiń wiersz, żeby zobaczyć uzasadnienie i post-mortem">
        <PredictionsTable predictions={data.predictions} showTrade={mode === 'TRADE'} />
      </Section>

      <Section title="Newsy" subtitle="Numer # to identyfikator, na który powołuje się AI">
        <NewsList news={data.news} max={40} />
      </Section>
    </Stack>
  );
}
