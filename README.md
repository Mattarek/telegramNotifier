# MarketMind - osobisty towarzysz giełdowy z pamięcią

Aplikacja tylko dla Ciebie. Codziennie pobiera notowania i newsy, a AI (Claude) analizuje każdą spółkę
i zapisuje **sprawdzalne tezy** (kierunek, horyzont, pewność w %). Kiedy horyzont minie, system sam rozlicza
tezę, AI pisze **post-mortem** („dlaczego się sprawdziło / nie sprawdziło”) i powoli buduje **pamięć lekcji**,
z której korzysta przy kolejnych analizach.

> To narzędzie do myślenia, nie porada inwestycyjna. Decyzje i ryzyko są Twoje.

## Zakładki

| Zakładka | Co tam jest |
|---|---|
| **Pulpit** | trafność AI vs reguła bazowa, otwarte tezy, dzisiejsze spojrzenie na każdą spółkę, ostatnie wnioski, newsy rynkowe |
| **Akcje** | horyzont 5 i 20 sesji, ocena **względem benchmarku** (WIG20 / S&P 500); szczegóły: wykres, analiza, czynniki, ryzyka, „co obali tezę”, historia tez z post-mortemami |
| **Trading** | horyzont 1 i 5 sesji, setup LONG/SHORT/brak, stop i cel, papierowe transakcje rozliczane w R |
| **Nauka i wnioski** | wykres kalibracji, statystyki, lekcje (aktywne / w testach / odrzucone) z dowodami, post-mortemy |
| **Instrumenty** | dodawanie i edycja obserwowanych spółek |
| **Przebiegi** | log każdego uruchomienia i szacowany koszt AI |

## Jak działa „uczenie się” (i dlaczego powoli)

Codzienny przebieg (domyślnie pn-pt 22:30, po zamknięciu USA, albo przycisk **Uruchom analizę**):

1. **Notowania** - dzienne świece z Yahoo Finance (endpoint `v8/chart`, bez klucza).
2. **Newsy** - Google News RSS (zapytanie z pola „Zapytanie newsów”) + Yahoo RSS dla spółek z USA.
3. **Rozliczenie** - teza z horyzontem N sesji jest rozliczana po N kolejnych świecach.
   - Akcje: liczy się zwrot **minus zwrot benchmarku** (czy pobiła rynek).
   - Trading: liczy się zwrot bezwzględny + symulacja transakcji (wejście na otwarciu, stop/cel, wynik w R).
   - Ruch w paśmie ±X% (`app.scoring.flat-band-pct`) = FLAT.
4. **Post-mortem** - AI ocenia każdą rozliczoną tezę: trafna z właściwych powodów / przypadek / błąd rozumowania /
   nie do przewidzenia / szum. **Szum i zdarzenia nie do przewidzenia nie zmieniają lekcji** - to chroni przed
   „uczeniem się” przypadków.
5. **Nowe analizy** - AI dostaje: dane cenowe, newsy, swoją historię na tej spółce, swoją kalibrację, wynik reguły
   bazowej i lekcje z pamięci.

Zasady lekcji (`LessonPolicy`): nowa lekcja startuje jako **kandydat**, staje się **aktywna** po ≥3 potwierdzeniach
przy ≥65% zgodności, wraca do testów, gdy zgodność spadnie, i jest **odrzucana**, gdy fakty jej zaprzeczają
(≥3 zaprzeczenia i <40% zgodności).

**Reguła bazowa** (momentum / trend) działa równolegle na tych samych spółkach. Jeśli po kilkudziesięciu
rozliczeniach AI jej nie pokonuje, to jej „wnioski” nie są warte więcej niż prosta reguła - i to jest uczciwa
informacja, którą ta aplikacja ma Ci dać.

**Kalibracja**: jeśli AI mówi „70%”, powinna mieć rację w ~70% przypadków. **Brier score**: 0 = idealnie,
0,25 = rzut monetą. Wiarygodne wnioski wymagają kilkudziesięciu rozliczonych tez, czyli tygodni, nie dni.

## Uruchomienie (Mac)

Wymagania: Docker Desktop, JDK 25, Node 20+.

```bash
# 1. Database (port 5433, so it does not clash with bankapp on 5432)
docker compose up -d

# 2. Backend - the API key is read from the environment, never commit it
export ANTHROPIC_API_KEY=sk-ant-...
cd backend
./gradlew bootRun

# 3. Frontend (new terminal tab: ⌘T)
cd frontend
npm install
npm run dev
```

Otwórz http://localhost:5173 i kliknij **Uruchom analizę**. Pierwszy przebieg pobiera 2 lata notowań.

W IntelliJ: otwórz folder `backend` jako projekt Gradle, a w konfiguracji uruchomienia `MarketMindApplication`
dodaj zmienną środowiskową `ANTHROPIC_API_KEY` (Run → Edit Configurations… → Environment variables).

Bez klucza aplikacja też działa, ale tylko z regułą bazową (bez analizy newsów i nauki).

## Koszt AI

Jedna analiza to ok. 4-7 tys. tokenów wejścia i 1-2 tys. wyjścia. Przy 4 spółkach (2 z tradingiem) to ~6 analiz
dziennie plus post-mortemy - zwykle kilka dolarów miesięcznie przy cenach z `application.yml`
(`app.ai.input-price-per-mtok` / `output-price-per-mtok` - **sprawdź aktualny cennik** i popraw, bo z nich liczony jest
koszt w zakładce Przebiegi). Model zmienisz w `app.ai.model`.

## Konfiguracja (`backend/src/main/resources/application.yml`)

- `app.pipeline.cron` - kiedy ma się uruchamiać (strefa `Europe/Warsaw`).
- `app.news.general-feeds` - ogólne kanały RSS (makro / rynek).
- `app.scoring.flat-band-pct` - pasmo „bez ruchu” dla horyzontów 1/5/20.
- `app.market.history-range` - ile historii pobierać.

## Startowa lista

CBF (cyber_Folks), ACP (Asseco Poland), NVDA, SXR8 (iShares Core S&P 500 na Xetra) + benchmarki WIG20 i S&P 500.
Jeśli Twój ETF to inny ticker, popraw go w zakładce Instrumenty. Jeśli w logu przebiegu WIG20 ma błąd pobierania,
zmień jego symbol Yahoo (np. na ETF na WIG20 notowany na GPW).

## Struktura

```
backend/   Spring Boot 4.1, Java 25, PostgreSQL, Liquibase
  instrument/  obserwowane spółki
  market/      notowania (Yahoo), wskaźniki (SMA, RSI, ATR...) - czysta logika
  news/        RSS (Google News, Yahoo)
  ai/          klient Claude API, schematy odpowiedzi (tool use = zawsze JSON)
  prediction/  tezy, rozliczanie, kalibracja, symulacja transakcji, reguła bazowa
  learning/    lekcje, dowody, post-mortemy, zasady awansu/odrzucenia
  analysis/    budowanie promptów i zapis analiz, post-mortemy
  pipeline/    codzienny przebieg + harmonogram
  dashboard/   widoki dla frontendu
frontend/  React 19, TypeScript, MUI 7, React Query, Recharts
```

Testy czystej logiki: `cd backend && ./gradlew test`.

## Ograniczenia, o których warto wiedzieć

- Yahoo `v8/chart` to nieoficjalne API - może się zmienić. Źródło cen jest za interfejsem `PriceProvider`, więc
  można je podmienić (Stooq od 2026 wymaga klucza API).
- AI widzi **tytuły** newsów, nie pełne artykuły. Raporty ESPI/EBI trafiają tylko, jeśli pojawią się w Google News.
- Dane dzienne - nie ma intraday. Symulacja tradingu nie liczy prowizji, spreadu ani poślizgu.
- Analiza uruchomiona w trakcie sesji bierze bieżącą cenę jako cenę odniesienia; jedna analiza na spółkę dziennie.
# telegramNotifier
# telegramNotifier
