import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import { useInstruments, useSaveInstrument } from '../api/client';
import type { Instrument, InstrumentRequest } from '../api/types';
import { ErrorBox, Loading } from '../components/Loading';
import { Section } from '../components/Section';

const emptyForm: InstrumentRequest = {
  symbol: '',
  name: '',
  market: 'GPW',
  type: 'STOCK',
  currency: 'PLN',
  yahooSymbol: '',
  newsQuery: '',
  benchmarkId: null,
  analyzed: true,
  tradingEnabled: false,
  active: true,
};

export function InstrumentsPage() {
  const { data, isLoading, error } = useInstruments();
  const [editing, setEditing] = useState<{ id?: number; form: InstrumentRequest } | null>(null);
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorBox error={error} />;

  const openEdit = (instrument: Instrument) => {
    setEditing({
      id: instrument.id,
      form: {
        symbol: instrument.symbol,
        name: instrument.name,
        market: instrument.market,
        type: instrument.type,
        currency: instrument.currency,
        yahooSymbol: instrument.yahooSymbol,
        newsQuery: instrument.newsQuery,
        benchmarkId: instrument.benchmarkId,
        analyzed: instrument.analyzed,
        tradingEnabled: instrument.tradingEnabled,
        active: instrument.active,
      },
    });
  };

  return (
    <Section
      title="Instrumenty"
      subtitle="Symbol Yahoo: GPW z końcówką .WA (np. CBF.WA, PKN.WA), USA bez końcówki (NVDA), Xetra .DE. Benchmarki mają wyłączoną analizę."
      action={
        <Button startIcon={<AddIcon />} variant="outlined" onClick={() => setEditing({ form: emptyForm })}>
          Dodaj
        </Button>
      }
    >
      <Box sx={{ overflowX: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Symbol</TableCell>
              <TableCell>Nazwa</TableCell>
              <TableCell>Rynek</TableCell>
              <TableCell>Yahoo</TableCell>
              <TableCell>Zapytanie newsów</TableCell>
              <TableCell>Benchmark</TableCell>
              <TableCell>Analiza</TableCell>
              <TableCell>Trading</TableCell>
              <TableCell>Aktywny</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {data.map((instrument) => (
              <TableRow key={instrument.id} hover>
                <TableCell sx={{ fontWeight: 600 }}>{instrument.symbol}</TableCell>
                <TableCell>{instrument.name}</TableCell>
                <TableCell>{instrument.market}</TableCell>
                <TableCell>{instrument.yahooSymbol}</TableCell>
                <TableCell>{instrument.newsQuery ?? '–'}</TableCell>
                <TableCell>{instrument.benchmarkSymbol ?? '–'}</TableCell>
                <TableCell>{instrument.analyzed ? 'tak' : 'nie'}</TableCell>
                <TableCell>{instrument.tradingEnabled ? 'tak' : 'nie'}</TableCell>
                <TableCell>{instrument.active ? 'tak' : 'nie'}</TableCell>
                <TableCell>
                  <IconButton size="small" onClick={() => openEdit(instrument)} aria-label="edytuj">
                    <EditIcon fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>
      {editing && (
        <InstrumentDialog
          key={editing.id ?? 'new'}
          id={editing.id}
          initial={editing.form}
          benchmarks={data.filter((instrument) => instrument.id !== editing.id)}
          onClose={() => setEditing(null)}
        />
      )}
    </Section>
  );
}

interface DialogProps {
  id?: number;
  initial: InstrumentRequest;
  benchmarks: Instrument[];
  onClose: () => void;
}

function InstrumentDialog({ id, initial, benchmarks, onClose }: DialogProps) {
  const [form, setForm] = useState<InstrumentRequest>(initial);
  const save = useSaveInstrument();
  const set = <K extends keyof InstrumentRequest>(key: K, value: InstrumentRequest[K]) => setForm({ ...form, [key]: value });

  const submit = () =>
    save.mutate(
      { id, body: { ...form, newsQuery: form.newsQuery || null, currency: form.currency || null } },
      { onSuccess: onClose },
    );

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{id ? `Edycja ${initial.symbol}` : 'Nowy instrument'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {save.error && <Alert severity="error">{save.error.message}</Alert>}
          <TextField label="Symbol" value={form.symbol} disabled={!!id} onChange={(e) => set('symbol', e.target.value)} />
          <TextField label="Nazwa" value={form.name} onChange={(e) => set('name', e.target.value)} />
          <Stack direction="row" spacing={2}>
            <TextField select fullWidth label="Rynek" value={form.market} onChange={(e) => set('market', e.target.value as InstrumentRequest['market'])}>
              <MenuItem value="GPW">GPW</MenuItem>
              <MenuItem value="US">USA</MenuItem>
              <MenuItem value="OTHER">Inny</MenuItem>
            </TextField>
            <TextField select fullWidth label="Typ" value={form.type} onChange={(e) => set('type', e.target.value as InstrumentRequest['type'])}>
              <MenuItem value="STOCK">Akcja</MenuItem>
              <MenuItem value="ETF">ETF</MenuItem>
              <MenuItem value="INDEX">Indeks</MenuItem>
            </TextField>
            <TextField fullWidth label="Waluta" value={form.currency ?? ''} onChange={(e) => set('currency', e.target.value)} />
          </Stack>
          <TextField label="Symbol Yahoo" helperText="np. CBF.WA, NVDA, SXR8.DE, ^GSPC" value={form.yahooSymbol} onChange={(e) => set('yahooSymbol', e.target.value)} />
          <TextField label="Zapytanie do Google News" helperText="np. nazwa spółki; puste = bez newsów" value={form.newsQuery ?? ''} onChange={(e) => set('newsQuery', e.target.value)} />
          <TextField
            select
            label="Benchmark"
            value={form.benchmarkId ?? ''}
            onChange={(e) => set('benchmarkId', e.target.value === '' ? null : Number(e.target.value))}
          >
            <MenuItem value="">brak</MenuItem>
            {benchmarks.map((benchmark) => (
              <MenuItem key={benchmark.id} value={benchmark.id}>
                {benchmark.symbol} - {benchmark.name}
              </MenuItem>
            ))}
          </TextField>
          <Stack direction="row">
            <FormControlLabel control={<Checkbox checked={form.analyzed} onChange={(e) => set('analyzed', e.target.checked)} />} label="Analizuj codziennie" />
            <FormControlLabel control={<Checkbox checked={form.tradingEnabled} onChange={(e) => set('tradingEnabled', e.target.checked)} />} label="Zakładka trading" />
            <FormControlLabel control={<Checkbox checked={form.active} onChange={(e) => set('active', e.target.checked)} />} label="Aktywny" />
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Anuluj</Button>
        <Button variant="contained" onClick={submit} disabled={save.isPending}>
          Zapisz
        </Button>
      </DialogActions>
    </Dialog>
  );
}
