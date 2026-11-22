import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type {
  DashboardData,
  Instrument,
  InstrumentDetail,
  InstrumentRequest,
  LearningData,
  Mode,
  PipelineRun,
  TradingData,
} from './types';

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!response.ok) {
    let detail = response.statusText;
    try {
      const body = await response.json();
      detail = body.detail ?? detail;
    } catch {
      // no JSON body
    }
    throw new ApiError(response.status, detail);
  }
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export const queryKeys = {
  dashboard: ['dashboard'] as const,
  instruments: ['instruments'] as const,
  detail: (id: number, mode: Mode) => ['detail', id, mode] as const,
  trading: ['trading'] as const,
  learning: ['learning'] as const,
  runs: ['runs'] as const,
};

export function useDashboard() {
  return useQuery({
    queryKey: queryKeys.dashboard,
    queryFn: () => request<DashboardData>('/api/dashboard'),
    // Poll faster while the pipeline is running, so the log and results show up live
    refetchInterval: (query) => (query.state.data?.pipelineRunning ? 3000 : 60000),
  });
}

export function useInstruments() {
  return useQuery({ queryKey: queryKeys.instruments, queryFn: () => request<Instrument[]>('/api/instruments') });
}

export function useInstrumentDetail(id: number, mode: Mode) {
  return useQuery({
    queryKey: queryKeys.detail(id, mode),
    queryFn: () => request<InstrumentDetail>(`/api/instruments/${id}/detail?mode=${mode}`),
  });
}

export function useTrading() {
  return useQuery({ queryKey: queryKeys.trading, queryFn: () => request<TradingData>('/api/trading') });
}

export function useLearning() {
  return useQuery({ queryKey: queryKeys.learning, queryFn: () => request<LearningData>('/api/learning') });
}

export function useRuns() {
  return useQuery({
    queryKey: queryKeys.runs,
    queryFn: () => request<PipelineRun[]>('/api/pipeline/runs'),
    refetchInterval: (query) => (query.state.data?.[0]?.status === 'RUNNING' ? 3000 : false),
  });
}

export function useRunPipeline() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => request<PipelineRun>('/api/pipeline/run', { method: 'POST' }),
    onSettled: () => queryClient.invalidateQueries(),
  });
}

export function useSaveInstrument() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: InstrumentRequest }) =>
      request<Instrument>(id ? `/api/instruments/${id}` : '/api/instruments', {
        method: id ? 'PUT' : 'POST',
        body: JSON.stringify(body),
      }),
    onSuccess: () => queryClient.invalidateQueries(),
  });
}
