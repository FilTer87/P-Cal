# Testing Strategy: H2 vs PostgreSQL

## Problema
I test con H2 in-memory database hanno comportamenti diversi rispetto a PostgreSQL in produzione, specialmente per operazioni che coinvolgono:
- Transaction isolation
- Timing delle operazioni DELETE/INSERT
- Constraint enforcement
- Cascading operations

## Soluzione Implementata

### 1. Fix del Codice Produzione
Nel `TaskService.updateTask()` abbiamo implementato:
```java
// Get existing reminders for comparison and explicit deletion
List<Reminder> existingReminders = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);

// Delete existing reminders explicitly
if (!existingReminders.isEmpty()) {
    reminderRepository.deleteAll(existingReminders);
    reminderRepository.flush(); // Force immediate execution in PostgreSQL
}

// Add new reminders
for (ReminderRequest reminderRequest : taskRequest.getReminders()) {
    reminderService.createReminderForTask(taskId, reminderRequest);
}

// Force flush of inserts as well
reminderRepository.flush();
```

### 2. Strategia di Testing Multi-Database

#### Test Veloci (CI/CD) - H2
- **Comando**: `mvn test`
- **Database**: H2 in-memory
- **Scopo**: Test funzionali rapidi, verifica logica business
- **Profilo**: `test` (default)

#### Test di Integrazione - PostgreSQL (Opzionale)
- **Comando**: `DOCKER_AVAILABLE=true mvn test -P postgres-tests`
- **Database**: PostgreSQL via Testcontainers (richiede Docker)
- **Scopo**: Verifica comportamenti specifici del database produzione
- **Profilo**: `postgres-tests`
- **Nota**: Si attiva solo se Docker è disponibile

#### Test Completi
- **Comando**: `mvn verify -P integration-tests`
- **Include**: Tutti i test di integrazione con database reale

### 3. Configurazione Maven Profiles

```xml
<!-- Test rapidi con H2 (default) -->
<profile>
    <id>default</id>
    <!-- usa application-test.yml con H2 -->
</profile>

<!-- Test PostgreSQL realistici -->
<profile>
    <id>postgres-tests</id>
    <!-- usa Testcontainers + application-postgres-test.yml -->
</profile>

<!-- Test di integrazione completi -->
<profile>
    <id>integration-tests</id>
    <!-- include tutti i test *IntegrationTest.java e *PostgresTest.java -->
</profile>
```

### 4. Naming Convention

- `*Test.java` - Test unitari rapidi (H2)
- `*WithFlushTest.java` - Test del fix per PostgreSQL (non richiede Docker)
- `*PostgresTest.java` - Test con PostgreSQL reale (richiede Docker)
- `*IntegrationTest.java` - Test di integrazione end-to-end

### 5. Pipeline CI/CD Consigliata

```yaml
# .github/workflows/ci.yml
test-fast:
  run: mvn test  # Test rapidi con H2

test-integration:
  run: mvn verify -P integration-tests  # Test completi con PostgreSQL

deploy:
  needs: [test-fast, test-integration]
  # Deploy solo se entrambi i test passano
```

## Vantaggi

1. **Velocità**: Test quotidiani veloci con H2
2. **Accuratezza**: Test pre-deploy accurati con PostgreSQL reale
3. **Debugging**: Test PostgreSQL riproducono bug produzione
4. **CI/CD**: Pipeline flessibile con test rapidi + test approfonditi

## Comandi Utili

```bash
# Test rapidi (sviluppo quotidiano) - include test del fix
mvn test

# Test specifico del fix reminder senza Docker
mvn test -Dtest=TaskReminderUpdateWithFlushTest

# Test con PostgreSQL reale (solo se Docker disponibile)
DOCKER_AVAILABLE=true mvn test -P postgres-tests

# Test completi pre-deployment
mvn verify -P integration-tests

# Test specifico con debug PostgreSQL
DOCKER_AVAILABLE=true mvn test -Dtest=TaskReminderPostgresTest -P postgres-tests
```

## Senza Docker

Se Docker non è disponibile, puoi comunque:

1. **Testare il fix**: `mvn test -Dtest=TaskReminderUpdateWithFlushTest`
2. **Verificare logica**: Tutti i test H2 includono il comportamento del fix
3. **Deploy con fiducia**: Il fix è stato progettato specificamente per PostgreSQL