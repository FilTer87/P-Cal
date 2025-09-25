# 🧪 Testing & Code Coverage

Questa guida spiega come eseguire i test e analizzare la copertura del codice per il backend di PrivateCal.

## 📋 Prerequisiti

- Java 17+
- Maven 3.8+
- Database PostgreSQL (per l'ambiente di produzione)
- Database H2 (automaticamente configurato per i test)

## 🚀 Comandi di Test

### Esecuzione Test Standard

```bash
# Esegui tutti i test
mvn test

# Esegui un test specifico
mvn test -Dtest=TaskIntegrationTest

# Esegui test con output verboso
mvn test -X
```

### Test con Coverage

```bash
# Esegui test con code coverage
mvn clean test

# Genera solo il report HTML (dopo aver eseguito i test)
mvn jacoco:report

# Visualizza summary della coverage
./coverage-summary.sh
```

### Profili Maven

```bash
# Esegui test con profilo coverage esteso
mvn test -Pcoverage
```

## 📊 Report di Coverage

### Metriche Principali

La copertura del codice viene misurata su:

- **Instructions Coverage**: Percentuale di istruzioni bytecode eseguite
- **Branch Coverage**: Percentuale di branch condizionali testati
- **Line Coverage**: Percentuale di linee di codice coperte
- **Method Coverage**: Percentuale di metodi testati

### Soglie di Coverage Configurate

```xml
<!-- Soglie minime definite in pom.xml -->
<limits>
    <limit>
        <counter>INSTRUCTION</counter>
        <value>COVEREDRATIO</value>
        <minimum>0.70</minimum> <!-- 70% istruzioni -->
    </limit>
    <limit>
        <counter>BRANCH</counter>
        <value>COVEREDRATIO</value>
        <minimum>0.60</minimum> <!-- 60% branch -->
    </limit>
</limits>
```

### Esclusioni dalla Coverage

Le seguenti classi sono **escluse** dal calcolo della coverage:

- `**/PrivateCalApplication.class` - Classe main dell'applicazione
- `**/config/**` - Classi di configurazione Spring
- `**/dto/**` - Data Transfer Objects (principalmente getter/setter)
- `**/entity/**` - Entità JPA (principalmente boilerplate)

## 📁 Struttura dei Report

```
target/site/jacoco/
├── index.html              # 🌐 Report HTML principale
├── jacoco.xml              # 📄 Report XML per tool esterni
├── jacoco.csv              # 📊 Report CSV per analisi
├── jacoco-sessions.html    # 📋 Dettaglio sessioni di test
└── com.privatecal.*/       # 📂 Report dettagliati per package
```

## 🎯 Come Migliorare la Coverage

### 1. Identifica Classes con Bassa Coverage

```bash
# Visualizza summary con classi che necessitano attenzione
./coverage-summary.sh
```

### 2. Analizza Report Dettagliati

1. Apri `target/site/jacoco/index.html` nel browser
2. Naviga nei package con coverage bassa
3. Clicca sulle classi per vedere linee non coperte (in rosso)

### 3. Scrivi Test Mirati

Per ogni linea rossa nel report HTML:

```java
@Test
void shouldHandleSpecificScenario() {
    // Arrange: Setup test data

    // Act: Execute the uncovered code path

    // Assert: Verify expected behavior
}
```

## 📈 Coverage Attuale

### 📊 Overview (Ultima Esecuzione)

- **Instructions**: 13.3% (1163/8735)
- **Branches**: 8.2% (55/669)
- **Lines**: 13.3% (269/2027)
- **Methods**: 76.4% (268/351)

### 🏆 Classi Meglio Coperte

1. `SecurityConfig` - 89.2%
2. `TaskService` - 43.2%
3. `JwtAuthenticationFilter` - 39.2%

### ⚠️ Classi che Richiedono Attenzione

1. `AuthController` - 0.5%
2. `AuthService` - 1.1%
3. `JwtUtils` - 1.3%

## 🔧 Configurazione Avanzata

### Modifica Soglie Coverage

Edita `pom.xml` sezione JaCoCo plugin:

```xml
<limits>
    <limit>
        <counter>INSTRUCTION</counter>
        <value>COVEREDRATIO</value>
        <minimum>0.80</minimum> <!-- Aumenta a 80% -->
    </limit>
</limits>
```

### Aggiungi Esclusioni

```xml
<excludes>
    <exclude>**/MyNewClass.class</exclude>
</excludes>
```

### Integrazione CI/CD

```yaml
# Esempio GitHub Actions
- name: Run tests with coverage
  run: mvn clean test

- name: Upload coverage reports
  uses: codecov/codecov-action@v3
  with:
    file: ./target/site/jacoco/jacoco.xml
```

## 🎨 Script Utili

### Coverage Summary

```bash
# Esegui lo script per visualizzare summary
./coverage-summary.sh
```

### Cleanup

```bash
# Pulisci report precedenti
mvn clean

# Reset completo
rm -rf target/site/jacoco/
```

## ⚡ Tips per Testing Efficace

### 1. **Test di Integrazione**
- I test integration coprono più codice per test
- Usa `@SpringBootTest` per test full-stack

### 2. **Test Unitari Mirati**
- Scrivi unit test per logica di business complessa
- Mock le dipendenze esterne

### 3. **Test Data Builders**
```java
public class TaskRequestBuilder {
    public static TaskRequest validTask() {
        return new TaskRequest("Test Task",
                             Instant.now(),
                             Instant.now().plusSeconds(3600));
    }
}
```

### 4. **Test Parametrizzati**
```java
@ParameterizedTest
@ValueSource(strings = {"", " ", "   "})
void shouldRejectBlankTitles(String title) {
    TaskRequest request = TaskRequestBuilder.validTask();
    request.setTitle(title);

    assertThrows(ValidationException.class,
                () -> taskService.createTask(request));
}
```

---

📝 **Nota**: Questo documento viene aggiornato automaticamente ad ogni esecuzione dei test con coverage.

🎯 **Obiettivo**: Raggiungiamo almeno il 70% di instruction coverage e 60% di branch coverage!