# 🚀 Guida Ottimizzazione Docker Build - Backend

## 📋 Indice
1. [Problema Attuale](#problema-attuale)
2. [Come Funziona la Cache Docker](#come-funziona-la-cache-docker)
3. [Soluzioni Proposte](#soluzioni-proposte)
4. [Confronto Performance](#confronto-performance)
5. [Quale Dockerfile Usare](#quale-dockerfile-usare)

---

## 🔍 Problema Attuale

### Dockerfile Attuale
Il tuo `Dockerfile` attuale è **già ottimizzato con layer caching**, ma ha alcune limitazioni:

```dockerfile
# ✅ BUONO: Copia pom.xml prima del codice
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

# ✅ BUONO: Copia codice dopo le dipendenze
COPY src ./src
RUN ./mvnw clean package -DskipTests
```

### Perché a volte è veloce e a volte lento?

**Docker invalida la cache quando:**
- Modifichi `pom.xml` → Riscarica TUTTE le dipendenze ✅ *Corretto*
- Modifichi file in `.mvn/` → Riscarica TUTTE le dipendenze ⚠️ *Raro*
- Modifichi `mvnw` → Riscarica TUTTE le dipendenze ⚠️ *Raro*
- Modifichi qualsiasi file in `src/` → **NON** riscarica dipendenze ✅ *Ottimizzato!*

**I due download che vedi:**
1. **`dependency:go-offline`** (riga 14): Scarica dipendenze del progetto
2. **`mvnw clean package`** (riga 20): Può scaricare plugin Maven non coperti da `go-offline`

---

## 🧠 Come Funziona la Cache Docker

Docker costruisce le immagini a **layer**:

```
Layer 1: Base image (eclipse-temurin:17)
Layer 2: COPY mvnw + .mvn         ← Cache invalidata RARAMENTE
Layer 3: COPY pom.xml             ← Cache invalidata se modifichi dipendenze
Layer 4: RUN dependency:go-offline ← Re-eseguito solo se Layer 3 cambia
Layer 5: COPY src/                ← Cache invalidata SPESSO (sviluppo)
Layer 6: RUN mvnw package         ← Re-eseguito solo se Layer 5 cambia
```

**Regola d'oro:** Se un layer cambia, tutti i layer successivi vengono ricostruiti.

---

## 💡 Soluzioni Proposte

Ho creato **3 versioni** del Dockerfile con ottimizzazioni crescenti:

### 1. `Dockerfile` (Attuale) - ⭐⭐⭐
**Cosa fa:**
- Layer caching base
- Download dipendenze solo se cambia `pom.xml`

**Pro:**
- Semplice, funziona ovunque
- Già ottimizzato per la maggior parte dei casi

**Contro:**
- Scarica plugin Maven ad ogni build (`mvnw package`)
- Nessuna cache persistente

---

### 2. `Dockerfile.optimized` - ⭐⭐⭐⭐
**Novità rispetto all'attuale:**
```dockerfile
# Scarica anche i plugin Maven (non solo dipendenze)
RUN ./mvnw dependency:resolve-plugins -B

# Build in modalità offline (usa solo cache locale)
RUN ./mvnw clean package -DskipTests -o
```

**Pro:**
- Elimina il secondo download (plugin)
- Modalità offline più veloce
- JVM ottimizzata per container
- Health check integrato

**Contro:**
- Se aggiungi nuove dipendenze, devi rimuovere `-o` temporaneamente

**Quando usarlo:** Produzione e sviluppo standard

---

### 3. `Dockerfile.buildkit` - ⭐⭐⭐⭐⭐
**Novità RIVOLUZIONARIA:**
```dockerfile
# Cache mount persistente del repository Maven locale
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B
```

**Pro:**
- ✨ Cache Maven **persistente** tra build diverse
- ✨ Anche modificando `pom.xml`, riusa dipendenze già scaricate
- ✨ Build successive **drasticamente più veloci**
- ✨ Cache sopravvive anche se cancelli l'immagine

**Contro:**
- Richiede Docker BuildKit (abilitato di default da Docker 23+)
- Sintassi leggermente più complessa

**Quando usarlo:** SEMPRE, se Docker >= 23 (consigliato!)

---

## 📊 Confronto Performance

| Scenario | Dockerfile Attuale | Dockerfile.optimized | Dockerfile.buildkit |
|----------|-------------------|---------------------|---------------------|
| **Prima build** (cache vuota) | ~5-8 min | ~5-8 min | ~5-8 min |
| **Rebuild senza modifiche** | ~1-2 sec | ~1-2 sec | ~1-2 sec |
| **Modifica `src/`** (codice) | ~30-60 sec | ~20-40 sec | ~20-40 sec |
| **Modifica `pom.xml`** | ~5-8 min ⚠️ | ~5-8 min ⚠️ | ~30-60 sec ✅ |

**Il vantaggio di BuildKit è enorme quando modifichi `pom.xml`!**

---

## 🎯 Quale Dockerfile Usare?

### Verifica versione Docker
```bash
docker --version
# Se >= 23.0: BuildKit è abilitato di default
# Se < 23.0: devi abilitarlo manualmente
```

### Opzione 1: Sostituisci Dockerfile attuale (CONSIGLIATO)
```bash
cd backend/
mv Dockerfile Dockerfile.old
mv Dockerfile.buildkit Dockerfile
```

### Opzione 2: Usa docker-compose con BuildKit
```bash
# Abilita BuildKit temporaneamente
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker-compose build backend

# Oppure in una sola riga
DOCKER_BUILDKIT=1 docker-compose build
```

### Opzione 3: Abilita BuildKit permanentemente
```bash
# Aggiungi a ~/.docker/config.json
{
  "features": {
    "buildkit": true
  }
}

# Oppure in /etc/docker/daemon.json (system-wide)
{
  "features": {
    "buildkit": true
  }
}
```

---

## 🔧 Come Testare

### 1. Test con Dockerfile.optimized
```bash
cd backend/
docker build -f Dockerfile.optimized -t privatecal-backend:optimized .

# Test rebuild modificando codice
touch src/main/java/com/privatecal/Application.java
docker build -f Dockerfile.optimized -t privatecal-backend:optimized .
# Dovrebbe essere veloce (~20-40 sec)
```

### 2. Test con Dockerfile.buildkit
```bash
# Prima build (lenta, popola cache)
DOCKER_BUILDKIT=1 docker build -f Dockerfile.buildkit -t privatecal-backend:buildkit .

# Modifica pom.xml (aggiungi un commento)
echo "<!-- test -->" >> pom.xml

# Rebuild (dovrebbe essere MOLTO più veloce!)
DOCKER_BUILDKIT=1 docker build -f Dockerfile.buildkit -t privatecal-backend:buildkit .
# Dovrebbe essere ~1-2 min invece di ~5-8 min!
```

---

## 🐛 Troubleshooting

### Problema: Build fallisce in modalità offline
```
[ERROR] Failed to execute goal ... artifact not found
```

**Soluzione:** Rimuovi temporaneamente `-o` da `Dockerfile.optimized`:
```dockerfile
# Invece di:
RUN ./mvnw clean package -DskipTests -o

# Usa:
RUN ./mvnw clean package -DskipTests
```

### Problema: BuildKit non funziona
```
ERROR: BuildKit not enabled
```

**Soluzione 1:** Abilita BuildKit:
```bash
export DOCKER_BUILDKIT=1
```

**Soluzione 2:** Usa syntax directive nel Dockerfile:
```dockerfile
# syntax=docker/dockerfile:1.4
FROM eclipse-temurin:17
```

### Problema: Cache BuildKit troppo grande
```bash
# Vedi dimensione cache
docker buildx du

# Pulisci cache BuildKit
docker buildx prune
```

---

## 📈 Best Practices Aggiuntive

### 1. .dockerignore
Crea un file `.dockerignore` nella cartella `backend/`:
```
target/
.mvn/wrapper/maven-wrapper.jar
*.log
.git
.gitignore
README.md
.env
.env.local
```

### 2. Multi-stage build per dev
```dockerfile
# Stage per development con hot-reload
FROM eclipse-temurin:17 as dev
WORKDIR /app
COPY pom.xml .
RUN ./mvnw dependency:go-offline
CMD ["./mvnw", "spring-boot:run"]
```

### 3. Dimensione immagini
```bash
# Confronta dimensioni
docker images | grep privatecal-backend

# RISULTATO ATTESO:
# Multi-stage build: ~250-300 MB (solo JRE Alpine)
# Build singolo: ~500-600 MB (JDK completo)
```

---

## 🎓 Approfondimenti

### Perché `dependency:go-offline` non scarica tutto?
Il goal Maven `dependency:go-offline` ha limitazioni note:
- Non scarica plugin usati solo durante build
- Non scarica dipendenze di alcuni plugin specifici
- Alcuni artifact vengono scaricati dinamicamente

**Soluzione:** Aggiungi `dependency:resolve-plugins` come in `Dockerfile.optimized`

### Come funziona BuildKit cache mount?
```dockerfile
RUN --mount=type=cache,target=/root/.m2 COMANDO
```
- Docker crea un volume persistente mappato a `/root/.m2`
- Il volume **NON** viene copiato nell'immagine finale
- Persiste tra build diverse e cancellazioni immagini
- Ogni utente Docker ha la propria cache separata

---

## ✅ Raccomandazione Finale

**Per il tuo progetto, consiglio:**

1. **Immediate:** Usa `Dockerfile.optimized` (sostituisci quello attuale)
   - Risolve subito il doppio download
   - Nessun requisito speciale
   - Migliora build del 20-30%

2. **Prossimo step:** Migra a `Dockerfile.buildkit`
   - Verifica Docker >= 23
   - Testa in ambiente di sviluppo
   - Migliora build del 60-80% quando modifichi pom.xml

3. **Bonus:** Crea `.dockerignore`
   - Riduce context size
   - Build ancora più veloci

---

**Domande? Problemi? Apri una issue!** 🚀