import { useState } from 'react';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Chip,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { useLearning } from '../api/client';
import type { Lesson, LessonStatus, Mode } from '../api/types';
import { ErrorBox, Loading } from '../components/Loading';
import { Section } from '../components/Section';
import { CalibrationChart } from '../components/CalibrationChart';
import { PredictionsTable } from '../components/PredictionsTable';
import { date, num, prob, sourceLabel } from '../format';

const statusInfo: Record<LessonStatus, { label: string; color: 'success' | 'warning' | 'default'; description: string }> = {
  ACTIVE: { label: 'Aktywne', color: 'success', description: 'Potwierdzone kilka razy - AI traktuje je jak regułę.' },
  CANDIDATE: { label: 'W testach', color: 'warning', description: 'Hipotezy z pojedynczych przypadków - czekają na potwierdzenie.' },
  RETIRED: { label: 'Odrzucone', color: 'default', description: 'Fakty im zaprzeczyły - AI już ich nie dostaje.' },
};

export function LearningPage() {
  const { data, isLoading, error } = useLearning();
  const [mode, setMode] = useState<Mode>('INVEST');
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorBox error={error} />;

  const forMode = data.calibration.filter((c) => c.mode === mode);
  const ai = forMode.find((c) => c.source === 'AI' && c.horizonDays === null);
  const baseline = forMode.find((c) => c.source === 'BASELINE' && c.horizonDays === null);

  return (
    <Stack spacing={3}>
      <Alert severity="info" variant="outlined">
        Jak to działa: każda teza ma horyzont i pewność. Po upływie horyzontu jest rozliczana, a AI pisze post-mortem
        (trafne z właściwych powodów / przypadek / błąd rozumowania / nie do przewidzenia / szum). Tylko wyniki, które mówią
        coś o rozumowaniu, mogą wzmocnić lub osłabić lekcję. Lekcja staje się aktywna po ≥3 potwierdzeniach przy ≥65%
        zgodności i jest odrzucana, gdy fakty jej zaprzeczają. Dzięki temu system uczy się powoli i nie goni szumu.
      </Alert>

      <Section
        title="Kalibracja"
        subtitle="Czy „70% pewności” znaczy naprawdę ~70% trafień? (ostatnie 180 dni)"
        action={
          <ToggleButtonGroup size="small" exclusive value={mode} onChange={(_, value) => value && setMode(value)}>
            <ToggleButton value="INVEST">Akcje</ToggleButton>
            <ToggleButton value="TRADE">Trading</ToggleButton>
          </ToggleButtonGroup>
        }
      >
        <CalibrationChart ai={ai} baseline={baseline} />
        <Box sx={{ overflowX: 'auto', mt: 2 }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Źródło</TableCell>
                <TableCell>Horyzont</TableCell>
                <TableCell align="right">Rozliczone</TableCell>
                <TableCell align="right">Trafność</TableCell>
                <TableCell align="right">Śr. pewność</TableCell>
                <TableCell align="right">Nadmierna pewność</TableCell>
                <TableCell align="right">Brier</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {forMode.map((c) => (
                <TableRow key={`${c.source}-${c.horizonDays}`}>
                  <TableCell>{sourceLabel(c.source)}</TableCell>
                  <TableCell>{c.horizonDays === null ? 'wszystkie' : `${c.horizonDays} sesji`}</TableCell>
                  <TableCell align="right">{c.count}</TableCell>
                  <TableCell align="right">{prob(c.hitRate)}</TableCell>
                  <TableCell align="right">{prob(c.averageConfidence)}</TableCell>
                  <TableCell align="right">{c.overconfidence === null ? '–' : `${num(c.overconfidence * 100, 0)} pp`}</TableCell>
                  <TableCell align="right">{num(c.brier, 3)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      </Section>

      <Section title="Pamięć - lekcje" subtitle="Wnioski, które AI wyciągnęło ze swoich trafień i pomyłek">
        {(['ACTIVE', 'CANDIDATE', 'RETIRED'] as LessonStatus[]).map((status) => (
          <LessonGroup key={status} status={status} lessons={data.lessons.filter((lesson) => lesson.status === status)} />
        ))}
      </Section>

      <Section title="Post-mortemy" subtitle="Ostatnie przeglądy rozliczonych tez">
        <PredictionsTable predictions={data.reviews} showSymbol showTrade emptyText="Brak przeglądów - pojawią się, gdy pierwsze tezy dojdą do końca horyzontu." />
      </Section>
    </Stack>
  );
}

function LessonGroup({ status, lessons }: { status: LessonStatus; lessons: Lesson[] }) {
  const info = statusInfo[status];
  return (
    <Box sx={{ mb: 2 }}>
      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
        <Chip size="small" color={info.color} label={`${info.label} (${lessons.length})`} />
        <Typography variant="body2" color="text.secondary">
          {info.description}
        </Typography>
      </Stack>
      {lessons.map((lesson) => (
        <Accordion key={lesson.id} disableGutters variant="outlined">
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Stack spacing={0.5}>
              <Typography variant="body2">
                <b>#{lesson.id}</b> {lesson.text}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                +{lesson.supportCount} / −{lesson.contradictCount} · {lesson.scope}
                {lesson.scopeRef ? ` ${lesson.scopeRef}` : ''} · {lesson.mode ?? 'akcje i trading'} · {lesson.category ?? '–'} ·
                aktualizacja {date(lesson.updatedAt)}
              </Typography>
            </Stack>
          </AccordionSummary>
          <AccordionDetails>
            {lesson.evidence.map((evidence, index) => (
              <Typography key={index} variant="body2" sx={{ mb: 0.5 }}>
                <Chip
                  size="small"
                  sx={{ mr: 1 }}
                  color={evidence.kind === 'SUPPORT' ? 'success' : 'error'}
                  label={evidence.kind === 'SUPPORT' ? 'potwierdza' : 'zaprzecza'}
                />
                {date(evidence.createdAt)} · predykcja #{evidence.predictionId ?? '–'} · {evidence.note}
              </Typography>
            ))}
          </AccordionDetails>
        </Accordion>
      ))}
    </Box>
  );
}
