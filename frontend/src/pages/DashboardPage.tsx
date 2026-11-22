import { Alert, Card, CardActionArea, CardContent, Chip, Stack, Typography } from '@mui/material';
import { Link } from 'react-router';
import { useDashboard } from '../api/client';
import type { Calibration, InstrumentOverview } from '../api/types';
import { Cards, Section } from '../components/Section';
import { ErrorBox, Loading } from '../components/Loading';
import { StatTile } from '../components/StatTile';
import { ReportCard } from '../components/ReportCard';
import { PredictionsTable } from '../components/PredictionsTable';
import { NewsList } from '../components/NewsList';
import { Pct } from '../components/Pct';
import { dateTime, num, prob } from '../format';

function find(calibration: Calibration[], source: 'AI' | 'BASELINE', mode: 'INVEST' | 'TRADE') {
  return calibration.find((c) => c.source === source && c.mode === mode && c.horizonDays === null);
}

export function DashboardPage() {
  const { data, isLoading, error } = useDashboard();
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorBox error={error} />;

  const aiInvest = find(data.calibration, 'AI', 'INVEST');
  const baseInvest = find(data.calibration, 'BASELINE', 'INVEST');
  const lastRun = data.lastRun;

  return (
    <Stack spacing={3}>
      {!data.aiEnabled && (
        <Alert severity="warning">
          Brak klucza <b>ANTHROPIC_API_KEY</b> - działa tylko prosta reguła bazowa. Ustaw zmienną środowiskową i zrestartuj
          backend, żeby włączyć analizę newsów i naukę.
        </Alert>
      )}

      <Cards min={200}>
        <StatTile label="Otwarte tezy" value={data.openPredictions} hint="czekają na rozliczenie" />
        <StatTile
          label="Trafność AI (akcje, 180 dni)"
          value={aiInvest && aiInvest.count > 0 ? prob(aiInvest.hitRate) : '–'}
          hint={aiInvest ? `${aiInvest.count} rozliczonych, śr. pewność ${prob(aiInvest.averageConfidence)}` : undefined}
        />
        <StatTile
          label="Reguła bazowa (akcje)"
          value={baseInvest && baseInvest.count > 0 ? prob(baseInvest.hitRate) : '–'}
          hint="AI musi ją pokonać, żeby była coś warta"
        />
        <StatTile
          label="Brier AI / bazowa"
          value={`${num(aiInvest?.brier ?? null, 3)} / ${num(baseInvest?.brier ?? null, 3)}`}
          hint="niżej = lepiej, 0,25 = rzut monetą"
        />
        <StatTile
          label="Lekcje"
          value={`${data.lessonCounts.ACTIVE ?? 0} aktywnych`}
          hint={`${data.lessonCounts.CANDIDATE ?? 0} w testach · ${data.lessonCounts.RETIRED ?? 0} odrzuconych`}
        />
        <StatTile
          label="Ostatni przebieg"
          value={lastRun ? lastRun.status : 'jeszcze nie było'}
          hint={lastRun ? `${dateTime(lastRun.startedAt)} · ~$${num(lastRun.estimatedCostUsd, 3)}` : 'kliknij „Uruchom analizę”'}
        />
      </Cards>

      <Section title="Dzisiejsze spojrzenie - akcje" subtitle="Najnowsza analiza AI każdego instrumentu względem benchmarku">
        <Cards min={360}>
          {data.instruments.map((overview) => (
            <InstrumentCard key={overview.instrument.id} overview={overview} />
          ))}
        </Cards>
      </Section>

      <Section title="Ostatnie wnioski" subtitle="Post-mortemy: dlaczego teza się sprawdziła albo nie">
        <PredictionsTable predictions={data.recentReviews} showSymbol emptyText="Jeszcze nic nie zostało rozliczone. Pierwsze wnioski pojawią się po 5 sesjach." />
      </Section>

      <Section title="Newsy rynkowe">
        <NewsList news={data.marketNews} max={12} />
      </Section>
    </Stack>
  );
}

function InstrumentCard({ overview }: { overview: InstrumentOverview }) {
  const { instrument, snapshot, aiReport, baselineReport } = overview;
  return (
    <Card variant="outlined">
      <CardActionArea component={Link} to={`/akcje/${instrument.id}`}>
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="baseline" sx={{ mb: 1 }}>
            <Typography variant="h6">
              {instrument.symbol}{' '}
              <Typography component="span" variant="body2" color="text.secondary">
                {instrument.name}
              </Typography>
            </Typography>
            <Chip size="small" label={instrument.market} />
          </Stack>
          {snapshot ? (
            <Typography variant="body2" sx={{ mb: 1 }}>
              {num(snapshot.close)} {instrument.currency} · 1d <Pct value={snapshot.return1dPct} /> · 20d{' '}
              <Pct value={snapshot.return20dPct} /> · vs rynek <Pct value={snapshot.excessReturn20dPct} />
            </Typography>
          ) : (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Brak notowań - uruchom analizę.
            </Typography>
          )}
          <ReportCard report={aiReport ?? baselineReport} compact />
        </CardContent>
      </CardActionArea>
    </Card>
  );
}
