# Studycard

The first JavaFX screen follows the supplied Figma-style reference: a warm neutral background, centered content, deck summary, and clickable study decks.

## Run the app

From this folder:

```text
mvn javafx:run
```

Clicking a deck currently opens a small confirmation dialog. This is the first interactive screen; the study-card view can be added next.

## Test and coverage

```text
mvn clean verify
```

Tests run with JUnit 5. JaCoCo creates `target/site/jacoco/index.html` and `target/site/jacoco/jacoco.xml`.

To publish the report into the repository's public folder on Windows:

```text
mvn clean verify
mkdir public-html\coverage
xcopy /E /I /Y target\site\jacoco public-html\coverage
```