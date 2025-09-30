#!/bin/bash

# Script per popolare il database con dati dimostrativi
# Genera task realistici per un utente specifico
#
# REQUISITI:
# - Database in esecuzione tramite Docker/Podman
# - Container database: privatecal-db (configurabile con DB_CONTAINER)
#
# USO:
#   ./populate_demo_data.sh
#
# VARIABILI D'AMBIENTE (opzionali):
#   DB_CONTAINER - Nome container database (default: privatecal-db)
#   DB_NAME      - Nome database (default: calendar_db)
#   DB_USER      - Utente database (default: calendar_user)
#   DB_PASSWORD  - Password database (default: calendar_pass)

set -e

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurazione database (da variabili d'ambiente o valori di default)
DB_CONTAINER="${DB_CONTAINER:-privatecal-db}"
DB_NAME="${DB_NAME:-calendar_db}"
DB_USER="${DB_USER:-calendar_user}"
DB_PASSWORD="${DB_PASSWORD:-calendar_pass}"

# Banner
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   POPOLAMENTO DATABASE CON DATI DIMOSTRATIVI          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Avviso modifiche distruttive
echo -e "${RED}⚠️  ATTENZIONE! ⚠️${NC}"
echo -e "${YELLOW}Questo script apporterà modifiche al database.${NC}"
echo -e "${YELLOW}Verranno creati numerosi task dimostrativi per l'utente specificato.${NC}"
echo ""
echo -e "${YELLOW}Se l'utente ha già dei task, questi NON verranno eliminati,${NC}"
echo -e "${YELLOW}ma verranno aggiunti nuovi task dimostrativi.${NC}"
echo ""

# Chiedi conferma
read -p "$(echo -e ${GREEN}Vuoi procedere? [y/N]:${NC} )" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Operazione annullata.${NC}"
    exit 1
fi

# Determina il comando container (docker o podman) e se serve sudo
CONTAINER_CMD="docker"
SUDO_CMD=""

# Check se serve sudo per docker/podman
if ! docker ps > /dev/null 2>&1; then
    if sudo docker ps > /dev/null 2>&1; then
        SUDO_CMD="sudo"
    elif podman ps > /dev/null 2>&1; then
        CONTAINER_CMD="podman"
    elif sudo podman ps > /dev/null 2>&1; then
        CONTAINER_CMD="podman"
        SUDO_CMD="sudo"
    else
        echo -e "${RED}❌ Errore: né docker né podman sono disponibili.${NC}"
        exit 1
    fi
fi

# Funzione per eseguire query SQL tramite container
execute_sql() {
    $SUDO_CMD $CONTAINER_CMD exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "$1" 2>/dev/null
}

# Test connessione database
echo ""
echo -e "${BLUE}Test connessione al database...${NC}"

# Verifica che il container esista e sia in esecuzione
if ! $SUDO_CMD $CONTAINER_CMD ps --format "{{.Names}}" 2>/dev/null | grep -q "^${DB_CONTAINER}$"; then
    echo -e "${RED}❌ Errore: container $DB_CONTAINER non trovato o non in esecuzione.${NC}"
    echo -e "${YELLOW}Container disponibili:${NC}"
    $SUDO_CMD $CONTAINER_CMD ps --format "table {{.Names}}\t{{.Status}}"
    echo ""
    echo -e "${YELLOW}Avvia il database con: sudo docker-compose up -d${NC}"
    exit 1
fi

# Test connessione SQL
if ! execute_sql "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${RED}❌ Errore: impossibile connettersi al database.${NC}"
    echo -e "${YELLOW}Verifica le credenziali (DB_USER=$DB_USER, DB_NAME=$DB_NAME)${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Connessione al database riuscita${NC}"

# Chiedi ID utente
echo ""
echo -e "${BLUE}Elenco utenti disponibili:${NC}"
execute_sql "SELECT id, username, email, first_name, last_name FROM users ORDER BY id;"
echo ""

read -p "$(echo -e ${GREEN}Inserisci l\'ID dell\'utente per i task dimostrativi:${NC} )" USER_ID

# Verifica che l'utente esista
USER_EXISTS=$(execute_sql "SELECT COUNT(*) FROM users WHERE id = $USER_ID;" | tr -d ' ')
if [ "$USER_EXISTS" -eq "0" ]; then
    echo -e "${RED}❌ Errore: l'utente con ID $USER_ID non esiste.${NC}"
    exit 1
fi

USER_INFO=$(execute_sql "SELECT username FROM users WHERE id = $USER_ID;" | tr -d ' ')
echo -e "${GREEN}✓ Utente selezionato: $USER_INFO (ID: $USER_ID)${NC}"

# Genera SQL per i task dimostrativi
echo ""
echo -e "${BLUE}Generazione task dimostrativi in corso...${NC}"

# Crea il file SQL temporaneo
SQL_FILE=$(mktemp)

cat > "$SQL_FILE" <<'EOSQL'
-- Script generato automaticamente per task dimostrativi
-- Data generazione: $(date)
-- User ID: $USER_ID

BEGIN;

-- Definizione dei colori per categoria
-- #2563eb - Blu - Lavoro
-- #059669 - Verde - Sport/Salute
-- #dc2626 - Rosso - Urgenze/Impegni importanti
-- #7c3aed - Viola - Hobby/Creatività
-- #ea580c - Arancione - Appuntamenti occasionali
-- #0891b2 - Ciano - Tempo libero/Sociale
-- #4f46e5 - Indaco - Formazione/Studio
-- #be123c - Rosa scuro - Famiglia

-- Funzione helper per generare timestamp (PostgreSQL)
-- Otteniamo il primo giorno del mese precedente
DO $$
DECLARE
    v_user_id BIGINT := USER_ID_PLACEHOLDER;
    v_start_date DATE := DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 month';
    v_end_date DATE := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '2 months';
    v_current_week_start DATE := DATE_TRUNC('week', CURRENT_DATE);
    v_current_week_end DATE := v_current_week_start + INTERVAL '1 week';
    v_next_week_end DATE := v_current_week_end + INTERVAL '1 week';

    -- Array di task per ogni mese
    v_tasks_month1 INTEGER := 35; -- Mese precedente
    v_tasks_month2 INTEGER := 65; -- Mese corrente (più concentrati)
    v_tasks_month3 INTEGER := 40; -- Mese successivo

    v_task_date DATE;
    v_task_start TIMESTAMP;
    v_task_end TIMESTAMP;
    v_counter INTEGER := 0;
    v_day_of_week INTEGER;
    v_week_of_month INTEGER;
BEGIN
    RAISE NOTICE 'Inizio generazione task dimostrativi per utente ID: %', v_user_id;
    RAISE NOTICE 'Periodo: % - %', v_start_date, v_end_date;

    -- ====================================================================
    -- MESE PRECEDENTE (30-40 task sparsi)
    -- ====================================================================
    RAISE NOTICE 'Generazione task per mese precedente...';

    -- Task ricorrenti: Prove band (martedì e venerdì sera)
    FOR i IN 0..4 LOOP
        -- Martedì
        v_task_date := v_start_date + (i * 7 + 1) * INTERVAL '1 day';
        IF v_task_date < DATE_TRUNC('month', CURRENT_DATE) THEN
            v_task_start := v_task_date + TIME '21:30:00';
            v_task_end := v_task_date + TIME '23:30:00';
            INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
            VALUES (v_user_id, 'Prove band', 'Sessione di prove settimanale con la band. Preparazione nuovo repertorio.',
                    v_task_start, v_task_end, '#7c3aed', 'Sala prove MusicLab');
            v_counter := v_counter + 1;
        END IF;

        -- Venerdì
        v_task_date := v_start_date + (i * 7 + 4) * INTERVAL '1 day';
        IF v_task_date < DATE_TRUNC('month', CURRENT_DATE) THEN
            v_task_start := v_task_date + TIME '21:30:00';
            v_task_end := v_task_date + TIME '23:30:00';
            INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
            VALUES (v_user_id, 'Prove band', 'Sessione di prove settimanale con la band. Preparazione nuovo repertorio.',
                    v_task_start, v_task_end, '#7c3aed', 'Sala prove MusicLab');
            v_counter := v_counter + 1;
        END IF;
    END LOOP;

    -- Task lavorativi (mese precedente)
    v_task_date := v_start_date + INTERVAL '2 days';
    v_task_start := v_task_date + TIME '09:00:00';
    v_task_end := v_task_date + TIME '11:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Riunione team planning', 'Pianificazione sprint e assegnazione task',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '3 days';
    v_task_start := v_task_date + TIME '14:00:00';
    v_task_end := v_task_date + TIME '16:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Code review progetto Alpha', 'Revisione pull request e feedback al team',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '5 days';
    v_task_start := v_task_date + TIME '10:00:00';
    v_task_end := v_task_date + TIME '12:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Sviluppo feature autenticazione', 'Implementazione JWT e refresh token',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '8 days';
    v_task_start := v_task_date + TIME '09:30:00';
    v_task_end := v_task_date + TIME '11:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Meeting con cliente', 'Presentazione milestone e raccolta feedback',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '9 days';
    v_task_start := v_task_date + TIME '15:00:00';
    v_task_end := v_task_date + TIME '17:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Refactoring database layer', 'Ottimizzazione query e indici',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '12 days';
    v_task_start := v_task_date + TIME '10:00:00';
    v_task_end := v_task_date + TIME '12:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Testing integrazione API', 'Test end-to-end servizi esterni',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    -- Sport e salute (mese precedente)
    v_task_date := v_start_date + INTERVAL '2 days';
    v_task_start := v_task_date + TIME '18:30:00';
    v_task_end := v_task_date + TIME '19:45:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Palestra', 'Allenamento upper body',
            v_task_start, v_task_end, '#059669', 'FitCenter');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '4 days';
    v_task_start := v_task_date + TIME '18:30:00';
    v_task_end := v_task_date + TIME '19:45:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Palestra', 'Allenamento lower body',
            v_task_start, v_task_end, '#059669', 'FitCenter');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '7 days';
    v_task_start := v_task_date + TIME '08:00:00';
    v_task_end := v_task_date + TIME '09:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Corsa mattutina', 'Allenamento cardio 8km',
            v_task_start, v_task_end, '#059669', 'Parco Comunale');
    v_counter := v_counter + 1;

    -- Appuntamenti e impegni occasionali (mese precedente)
    v_task_date := v_start_date + INTERVAL '6 days';
    v_task_start := v_task_date + TIME '15:00:00';
    v_task_end := v_task_date + TIME '15:45:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Visita dentista', 'Controllo semestrale e pulizia',
            v_task_start, v_task_end, '#ea580c', 'Studio Dr. Bianchi');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '13 days';
    v_task_start := v_task_date + TIME '10:30:00';
    v_task_end := v_task_date + TIME '11:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Appuntamento commercialista', 'Consegna documenti fiscali Q4',
            v_task_start, v_task_end, '#ea580c', 'Studio Rossi');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '18 days';
    v_task_start := v_task_date + TIME '14:00:00';
    v_task_end := v_task_date + TIME '14:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Revisione auto', 'Tagliando annuale',
            v_task_start, v_task_end, '#ea580c', 'Officina AutoService');
    v_counter := v_counter + 1;

    -- Tempo libero e sociale (mese precedente)
    v_task_date := v_start_date + INTERVAL '5 days';
    v_task_start := v_task_date + TIME '20:00:00';
    v_task_end := v_task_date + TIME '22:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Cena con Marco e Giulia', 'Ristorante nuovo in centro',
            v_task_start, v_task_end, '#0891b2', 'Ristorante La Pergola');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '14 days';
    v_task_start := v_task_date + TIME '21:00:00';
    v_task_end := v_task_date + TIME '23:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Cinema', 'Nuovo film di Nolan',
            v_task_start, v_task_end, '#0891b2', 'Multisala Centrale');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '21 days';
    v_task_start := v_task_date + TIME '10:00:00';
    v_task_end := v_task_date + TIME '18:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Gita a Firenze', 'Visita musei e centro storico',
            v_task_start, v_task_end, '#0891b2', 'Firenze');
    v_counter := v_counter + 1;

    -- Formazione e studio (mese precedente)
    v_task_date := v_start_date + INTERVAL '10 days';
    v_task_start := v_task_date + TIME '19:00:00';
    v_task_end := v_task_date + TIME '21:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Corso online Kubernetes', 'Modulo 3: Container orchestration',
            v_task_start, v_task_end, '#4f46e5');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '17 days';
    v_task_start := v_task_date + TIME '19:00:00';
    v_task_end := v_task_date + TIME '21:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Corso online Kubernetes', 'Modulo 4: Deployment strategies',
            v_task_start, v_task_end, '#4f46e5');
    v_counter := v_counter + 1;

    -- Famiglia (mese precedente)
    v_task_date := v_start_date + INTERVAL '7 days';
    v_task_start := v_task_date + TIME '12:30:00';
    v_task_end := v_task_date + TIME '15:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Pranzo famiglia', 'Pranzo domenicale dai nonni',
            v_task_start, v_task_end, '#be123c', 'Casa Nonni');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '14 days';
    v_task_start := v_task_date + TIME '12:30:00';
    v_task_end := v_task_date + TIME '15:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Pranzo famiglia', 'Pranzo domenicale dai nonni',
            v_task_start, v_task_end, '#be123c', 'Casa Nonni');
    v_counter := v_counter + 1;

    v_task_date := v_start_date + INTERVAL '19 days';
    v_task_start := v_task_date + TIME '18:00:00';
    v_task_end := v_task_date + TIME '19:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Compleanno sorella', 'Festa a sorpresa',
            v_task_start, v_task_end, '#be123c', 'Casa genitori');
    v_counter := v_counter + 1;

    RAISE NOTICE 'Task mese precedente: %', v_counter;

    -- ====================================================================
    -- MESE CORRENTE (50-75 task, concentrati nelle prossime 2 settimane)
    -- ====================================================================
    RAISE NOTICE 'Generazione task per mese corrente (maggiore densità)...';

    -- Task ricorrenti: Prove band (martedì e venerdì sera) - mese corrente
    FOR i IN 0..4 LOOP
        -- Martedì
        v_task_date := DATE_TRUNC('month', CURRENT_DATE) + (i * 7 + 1) * INTERVAL '1 day';
        IF v_task_date >= DATE_TRUNC('month', CURRENT_DATE) AND v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
            v_task_start := v_task_date + TIME '21:30:00';
            v_task_end := v_task_date + TIME '23:30:00';
            INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
            VALUES (v_user_id, 'Prove band', 'Sessione di prove settimanale con la band. Preparazione nuovo repertorio.',
                    v_task_start, v_task_end, '#7c3aed', 'Sala prove MusicLab');
            v_counter := v_counter + 1;
        END IF;

        -- Venerdì
        v_task_date := DATE_TRUNC('month', CURRENT_DATE) + (i * 7 + 4) * INTERVAL '1 day';
        IF v_task_date >= DATE_TRUNC('month', CURRENT_DATE) AND v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
            v_task_start := v_task_date + TIME '21:30:00';
            v_task_end := v_task_date + TIME '23:30:00';
            INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
            VALUES (v_user_id, 'Prove band', 'Sessione di prove settimanale con la band. Preparazione nuovo repertorio.',
                    v_task_start, v_task_end, '#7c3aed', 'Sala prove MusicLab');
            v_counter := v_counter + 1;
        END IF;
    END LOOP;

    -- SETTIMANA CORRENTE - Alta densità
    -- Lunedì
    v_task_date := v_current_week_start;
    IF v_task_date >= DATE_TRUNC('month', CURRENT_DATE) THEN
        v_task_start := v_task_date + TIME '09:00:00';
        v_task_end := v_task_date + TIME '10:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Daily standup meeting', 'Allineamento team su progresso task',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '11:00:00';
        v_task_end := v_task_date + TIME '13:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Sviluppo dashboard analytics', 'Implementazione grafici con Chart.js',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '14:30:00';
        v_task_end := v_task_date + TIME '17:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Bug fixing critical issues', 'Risoluzione bug segnalati in produzione',
                v_task_start, v_task_end, '#dc2626');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '18:30:00';
        v_task_end := v_task_date + TIME '19:45:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Palestra', 'Allenamento full body',
                v_task_start, v_task_end, '#059669', 'FitCenter');
        v_counter := v_counter + 1;
    END IF;

    -- Martedì
    v_task_date := v_current_week_start + INTERVAL '1 day';
    IF v_task_date >= DATE_TRUNC('month', CURRENT_DATE) THEN
        v_task_start := v_task_date + TIME '09:00:00';
        v_task_end := v_task_date + TIME '10:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Code review PR #342', 'Revisione implementazione nuova feature',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '10:30:00';
        v_task_end := v_task_date + TIME '12:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Implementazione API REST', 'Endpoint per gestione profili utente',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '15:00:00';
        v_task_end := v_task_date + TIME '16:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Meeting design review', 'Revisione mockup nuova interfaccia',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '19:30:00';
        v_task_end := v_task_date + TIME '21:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Lettura tecnica', 'Clean Architecture - Capitolo 5',
                v_task_start, v_task_end, '#4f46e5');
        v_counter := v_counter + 1;
    END IF;

    -- Mercoledì
    v_task_date := v_current_week_start + INTERVAL '2 days';
    IF v_task_date >= DATE_TRUNC('month', CURRENT_DATE) THEN
        v_task_start := v_task_date + TIME '09:30:00';
        v_task_end := v_task_date + TIME '11:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Pair programming sessione', 'Sviluppo feature complessa con junior dev',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '11:30:00';
        v_task_end := v_task_date + TIME '13:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Ottimizzazione performance', 'Analisi e ottimizzazione query database',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '14:00:00';
        v_task_end := v_task_date + TIME '15:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Documentazione tecnica', 'Aggiornamento API documentation',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '18:30:00';
        v_task_end := v_task_date + TIME '19:45:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Palestra', 'Allenamento cardio e core',
                v_task_start, v_task_end, '#059669', 'FitCenter');
        v_counter := v_counter + 1;
    END IF;

    -- Giovedì
    v_task_date := v_current_week_start + INTERVAL '3 days';
    IF v_task_date >= DATE_TRUNC('month', CURRENT_DATE) THEN
        v_task_start := v_task_date + TIME '09:00:00';
        v_task_end := v_task_date + TIME '12:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Sprint planning meeting', 'Pianificazione sprint successivo',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '14:00:00';
        v_task_end := v_task_date + TIME '16:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Sviluppo feature notifiche', 'Sistema di notifiche push real-time',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '17:00:00';
        v_task_end := v_task_date + TIME '17:45:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Fisioterapia', 'Trattamento cervicale',
                v_task_start, v_task_end, '#ea580c', 'Centro Fisio+');
        v_counter := v_counter + 1;
    END IF;

    -- Venerdì
    v_task_date := v_current_week_start + INTERVAL '4 days';
    IF v_task_date >= DATE_TRUNC('month', CURRENT_DATE) THEN
        v_task_start := v_task_date + TIME '09:00:00';
        v_task_end := v_task_date + TIME '11:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Testing e QA', 'Test suite completa pre-release',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '11:30:00';
        v_task_end := v_task_date + TIME '13:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Deploy staging environment', 'Rilascio versione su ambiente test',
                v_task_start, v_task_end, '#dc2626');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '15:00:00';
        v_task_end := v_task_date + TIME '16:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Sprint retrospective', 'Analisi sprint concluso e miglioramenti',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '20:30:00';
        v_task_end := v_task_date + TIME '23:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Aperitivo con colleghi', 'Happy hour team building',
                v_task_start, v_task_end, '#0891b2', 'Lounge Bar Downtown');
        v_counter := v_counter + 1;
    END IF;

    -- Sabato
    v_task_date := v_current_week_start + INTERVAL '5 days';
    IF v_task_date >= DATE_TRUNC('month', CURRENT_DATE) THEN
        v_task_start := v_task_date + TIME '09:00:00';
        v_task_end := v_task_date + TIME '11:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Progetto personale', 'Sviluppo side project portfolio',
                v_task_start, v_task_end, '#7c3aed');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '15:00:00';
        v_task_end := v_task_date + TIME '17:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Spesa settimanale', 'Supermercato e mercato',
                v_task_start, v_task_end, '#ea580c', 'Centro Commerciale');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '21:00:00';
        v_task_end := v_task_date + TIME '23:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Serata pizzeria', 'Cena con amici',
                v_task_start, v_task_end, '#0891b2', 'Pizzeria Da Giovanni');
        v_counter := v_counter + 1;
    END IF;

    -- Domenica
    v_task_date := v_current_week_start + INTERVAL '6 days';
    IF v_task_date >= DATE_TRUNC('month', CURRENT_DATE) THEN
        v_task_start := v_task_date + TIME '08:30:00';
        v_task_end := v_task_date + TIME '11:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Escursione montagna', 'Trekking Sentiero del Crinale',
                v_task_start, v_task_end, '#059669', 'Parco Nazionale');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '12:30:00';
        v_task_end := v_task_date + TIME '15:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Pranzo famiglia', 'Pranzo domenicale dai nonni',
                v_task_start, v_task_end, '#be123c', 'Casa Nonni');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '19:00:00';
        v_task_end := v_task_date + TIME '20:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Pianificazione settimana', 'Review obiettivi e planning task',
                v_task_start, v_task_end, '#4f46e5');
        v_counter := v_counter + 1;
    END IF;

    -- SETTIMANA SUCCESSIVA - Alta densità
    -- Lunedì
    v_task_date := v_current_week_end;
    IF v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
        v_task_start := v_task_date + TIME '09:00:00';
        v_task_end := v_task_date + TIME '10:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Weekly planning', 'Pianificazione obiettivi settimana',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '11:00:00';
        v_task_end := v_task_date + TIME '13:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Sviluppo microservizio pagamenti', 'Integrazione Stripe API',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '14:30:00';
        v_task_end := v_task_date + TIME '16:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Security audit', 'Analisi vulnerabilità e penetration test',
                v_task_start, v_task_end, '#dc2626');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '18:30:00';
        v_task_end := v_task_date + TIME '19:45:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Palestra', 'Allenamento strength',
                v_task_start, v_task_end, '#059669', 'FitCenter');
        v_counter := v_counter + 1;
    END IF;

    -- Martedì
    v_task_date := v_current_week_end + INTERVAL '1 day';
    IF v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
        v_task_start := v_task_date + TIME '09:00:00';
        v_task_end := v_task_date + TIME '11:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Implementazione caching', 'Redis integration per performance',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '11:30:00';
        v_task_end := v_task_date + TIME '13:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Technical meeting', 'Architettura nuova feature enterprise',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '15:00:00';
        v_task_end := v_task_date + TIME '17:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Migration database', 'Migrazione schema e dati versione 2.0',
                v_task_start, v_task_end, '#dc2626');
        v_counter := v_counter + 1;
    END IF;

    -- Mercoledì
    v_task_date := v_current_week_end + INTERVAL '2 days';
    IF v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
        v_task_start := v_task_date + TIME '10:00:00';
        v_task_end := v_task_date + TIME '12:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Workshop Docker', 'Training interno containerizzazione',
                v_task_start, v_task_end, '#4f46e5');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '14:00:00';
        v_task_end := v_task_date + TIME '16:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Sviluppo webhook handler', 'Gestione eventi esterni via webhook',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '18:30:00';
        v_task_end := v_task_date + TIME '19:45:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Yoga', 'Lezione yoga e stretching',
                v_task_start, v_task_end, '#059669', 'Centro Benessere');
        v_counter := v_counter + 1;
    END IF;

    -- Giovedì
    v_task_date := v_current_week_end + INTERVAL '3 days';
    IF v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
        v_task_start := v_task_date + TIME '09:00:00';
        v_task_end := v_task_date + TIME '11:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Cliente XYZ - Demo', 'Presentazione nuove funzionalità',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '14:00:00';
        v_task_end := v_task_date + TIME '17:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Refactoring frontend', 'Miglioramento struttura componenti Vue',
                v_task_start, v_task_end, '#2563eb');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '20:00:00';
        v_task_end := v_task_date + TIME '22:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Teatro', 'Spettacolo "Amleto"',
                v_task_start, v_task_end, '#0891b2', 'Teatro Comunale');
        v_counter := v_counter + 1;
    END IF;

    -- Venerdì
    v_task_date := v_current_week_end + INTERVAL '4 days';
    IF v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
        v_task_start := v_task_date + TIME '09:00:00';
        v_task_end := v_task_date + TIME '12:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Code freeze e test finali', 'Preparazione release v2.5',
                v_task_start, v_task_end, '#dc2626');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '14:00:00';
        v_task_end := v_task_date + TIME '15:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Deploy produzione', 'Release v2.5 in produzione',
                v_task_start, v_task_end, '#dc2626');
        v_counter := v_counter + 1;

        v_task_start := v_task_date + TIME '16:00:00';
        v_task_end := v_task_date + TIME '17:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Post-deploy monitoring', 'Verifica sistema e metriche',
                v_task_start, v_task_end, '#dc2626');
        v_counter := v_counter + 1;
    END IF;

    -- Task sparsi resto del mese corrente
    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '3 days';
    IF v_task_date NOT BETWEEN v_current_week_start AND v_next_week_end THEN
        v_task_start := v_task_date + TIME '10:00:00';
        v_task_end := v_task_date + TIME '11:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Appuntamento banca', 'Apertura conto deposito',
                v_task_start, v_task_end, '#ea580c', 'Banca Intesa');
        v_counter := v_counter + 1;
    END IF;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '16 days';
    IF v_task_date NOT BETWEEN v_current_week_start AND v_next_week_end AND v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
        v_task_start := v_task_date + TIME '19:00:00';
        v_task_end := v_task_date + TIME '21:30:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
        VALUES (v_user_id, 'Webinar Cloud Architecture', 'AWS Well-Architected Framework',
                v_task_start, v_task_end, '#4f46e5');
        v_counter := v_counter + 1;
    END IF;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '20 days';
    IF v_task_date NOT BETWEEN v_current_week_start AND v_next_week_end AND v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
        v_task_start := v_task_date + TIME '15:00:00';
        v_task_end := v_task_date + TIME '15:45:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Visita medico', 'Controllo annuale',
                v_task_start, v_task_end, '#ea580c', 'Poliambulatorio');
        v_counter := v_counter + 1;
    END IF;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '23 days';
    IF v_task_date NOT BETWEEN v_current_week_start AND v_next_week_end AND v_task_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' THEN
        v_task_start := v_task_date + TIME '20:00:00';
        v_task_end := v_task_date + TIME '23:00:00';
        INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
        VALUES (v_user_id, 'Concerto rock', 'Live band Foo Fighters tribute',
                v_task_start, v_task_end, '#0891b2', 'Palasport');
        v_counter := v_counter + 1;
    END IF;

    RAISE NOTICE 'Task mese corrente: %', v_counter;

    -- ====================================================================
    -- MESE SUCCESSIVO (30-45 task sparsi)
    -- ====================================================================
    RAISE NOTICE 'Generazione task per mese successivo...';

    -- Task ricorrenti: Prove band (martedì e venerdì sera) - mese successivo
    FOR i IN 0..3 LOOP
        -- Martedì
        v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + (i * 7 + 1) * INTERVAL '1 day';
        IF v_task_date < v_end_date THEN
            v_task_start := v_task_date + TIME '21:30:00';
            v_task_end := v_task_date + TIME '23:30:00';
            INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
            VALUES (v_user_id, 'Prove band', 'Sessione di prove settimanale con la band. Preparazione nuovo repertorio.',
                    v_task_start, v_task_end, '#7c3aed', 'Sala prove MusicLab');
            v_counter := v_counter + 1;
        END IF;

        -- Venerdì
        v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + (i * 7 + 4) * INTERVAL '1 day';
        IF v_task_date < v_end_date THEN
            v_task_start := v_task_date + TIME '21:30:00';
            v_task_end := v_task_date + TIME '23:30:00';
            INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
            VALUES (v_user_id, 'Prove band', 'Sessione di prove settimanale con la band. Preparazione nuovo repertorio.',
                    v_task_start, v_task_end, '#7c3aed', 'Sala prove MusicLab');
            v_counter := v_counter + 1;
        END IF;
    END LOOP;

    -- Task lavorativi mese successivo
    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '2 days';
    v_task_start := v_task_date + TIME '09:00:00';
    v_task_end := v_task_date + TIME '12:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Kickoff nuovo progetto', 'Inizio sviluppo piattaforma e-commerce',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '5 days';
    v_task_start := v_task_date + TIME '14:00:00';
    v_task_end := v_task_date + TIME '17:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Setup ambiente sviluppo', 'Configurazione CI/CD pipeline',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '9 days';
    v_task_start := v_task_date + TIME '10:00:00';
    v_task_end := v_task_date + TIME '12:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Design database schema', 'Progettazione architettura dati',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '12 days';
    v_task_start := v_task_date + TIME '09:00:00';
    v_task_end := v_task_date + TIME '11:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Implementazione auth module', 'OAuth2 e JWT authentication',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '16 days';
    v_task_start := v_task_date + TIME '14:30:00';
    v_task_end := v_task_date + TIME '17:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Sviluppo catalog service', 'Microservizio gestione prodotti',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '19 days';
    v_task_start := v_task_date + TIME '10:00:00';
    v_task_end := v_task_date + TIME '12:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Integration testing', 'Test integrazione servizi',
            v_task_start, v_task_end, '#2563eb');
    v_counter := v_counter + 1;

    -- Sport mese successivo
    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '3 days';
    v_task_start := v_task_date + TIME '18:30:00';
    v_task_end := v_task_date + TIME '19:45:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Palestra', 'Allenamento gambe',
            v_task_start, v_task_end, '#059669', 'FitCenter');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '6 days';
    v_task_start := v_task_date + TIME '07:30:00';
    v_task_end := v_task_date + TIME '09:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Running', 'Allenamento 10km',
            v_task_start, v_task_end, '#059669', 'Lungomare');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '10 days';
    v_task_start := v_task_date + TIME '18:30:00';
    v_task_end := v_task_date + TIME '19:45:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Palestra', 'Allenamento pettorali e tricipiti',
            v_task_start, v_task_end, '#059669', 'FitCenter');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '13 days';
    v_task_start := v_task_date + TIME '08:00:00';
    v_task_end := v_task_date + TIME '09:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Nuoto', 'Allenamento cardio in piscina',
            v_task_start, v_task_end, '#059669', 'Piscina Olimpia');
    v_counter := v_counter + 1;

    -- Tempo libero mese successivo
    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '7 days';
    v_task_start := v_task_date + TIME '20:00:00';
    v_task_end := v_task_date + TIME '22:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Cena ristorante', 'Cena romantica',
            v_task_start, v_task_end, '#0891b2', 'Ristorante Il Gabbiano');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '14 days';
    v_task_start := v_task_date + TIME '21:15:00';
    v_task_end := v_task_date + TIME '23:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Cinema', 'Film d''autore - rassegna europea',
            v_task_start, v_task_end, '#0891b2', 'Cinema Arthouse');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '21 days';
    v_task_start := v_task_date + TIME '10:00:00';
    v_task_end := v_task_date + TIME '18:00:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Weekend fuori porta', 'Visita Venezia',
            v_task_start, v_task_end, '#0891b2', 'Venezia');
    v_counter := v_counter + 1;

    -- Appuntamenti mese successivo
    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '8 days';
    v_task_start := v_task_date + TIME '16:00:00';
    v_task_end := v_task_date + TIME '16:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Taglio capelli', 'Appuntamento barbiere',
            v_task_start, v_task_end, '#ea580c', 'Barber Shop');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '15 days';
    v_task_start := v_task_date + TIME '11:00:00';
    v_task_end := v_task_date + TIME '11:45:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Revisione caldaia', 'Controllo annuale obbligatorio',
            v_task_start, v_task_end, '#ea580c', 'Casa');
    v_counter := v_counter + 1;

    -- Formazione mese successivo
    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '11 days';
    v_task_start := v_task_date + TIME '19:00:00';
    v_task_end := v_task_date + TIME '21:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Corso Machine Learning', 'Modulo intro Neural Networks',
            v_task_start, v_task_end, '#4f46e5');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '18 days';
    v_task_start := v_task_date + TIME '19:00:00';
    v_task_end := v_task_date + TIME '21:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color)
    VALUES (v_user_id, 'Corso Machine Learning', 'Modulo Deep Learning basics',
            v_task_start, v_task_end, '#4f46e5');
    v_counter := v_counter + 1;

    -- Famiglia mese successivo
    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '7 days';
    v_task_start := v_task_date + TIME '12:30:00';
    v_task_end := v_task_date + TIME '15:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Pranzo famiglia', 'Pranzo domenicale dai nonni',
            v_task_start, v_task_end, '#be123c', 'Casa Nonni');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '14 days';
    v_task_start := v_task_date + TIME '12:30:00';
    v_task_end := v_task_date + TIME '15:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Pranzo famiglia', 'Pranzo domenicale dai nonni',
            v_task_start, v_task_end, '#be123c', 'Casa Nonni');
    v_counter := v_counter + 1;

    v_task_date := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '21 days';
    v_task_start := v_task_date + TIME '12:30:00';
    v_task_end := v_task_date + TIME '15:30:00';
    INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color, location)
    VALUES (v_user_id, 'Pranzo famiglia', 'Pranzo domenicale dai nonni',
            v_task_start, v_task_end, '#be123c', 'Casa Nonni');
    v_counter := v_counter + 1;

    RAISE NOTICE 'Task mese successivo: %', v_counter;
    RAISE NOTICE 'TOTALE TASK GENERATI: %', v_counter;

END $$;

COMMIT;

EOSQL

# Sostituisci il placeholder con l'ID utente effettivo
sed -i "s/USER_ID_PLACEHOLDER/$USER_ID/g" "$SQL_FILE"

echo ""
echo -e "${BLUE}Esecuzione inserimenti nel database...${NC}"
echo -e "${YELLOW}Questo potrebbe richiedere alcuni secondi...${NC}"

# Esegui lo script SQL tramite container
if $SUDO_CMD $CONTAINER_CMD exec -i -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$SQL_FILE"; then
    echo ""
    echo -e "${GREEN}✓ Task dimostrativi creati con successo!${NC}"

    # Conta i task inseriti
    TASK_COUNT=$(execute_sql "SELECT COUNT(*) FROM tasks WHERE user_id = $USER_ID;" | tr -d ' ')
    echo -e "${GREEN}✓ Totale task per l'utente $USER_INFO: $TASK_COUNT${NC}"

    # Mostra distribuzione per mese
    echo ""
    echo -e "${BLUE}Distribuzione task per mese:${NC}"
    execute_sql "
    SELECT
        TO_CHAR(start_datetime, 'YYYY-MM') as mese,
        COUNT(*) as num_task
    FROM tasks
    WHERE user_id = $USER_ID
    GROUP BY TO_CHAR(start_datetime, 'YYYY-MM')
    ORDER BY mese;
    "
else
    echo ""
    echo -e "${RED}❌ Errore durante l'inserimento dei task.${NC}"
    rm -f "$SQL_FILE"
    exit 1
fi

# Pulisci file temporaneo
rm -f "$SQL_FILE"

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   OPERAZIONE COMPLETATA CON SUCCESSO!                  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"