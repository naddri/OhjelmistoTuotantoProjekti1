package com.flashcards;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Studycard: a JavaFX flashcard prototype with authentication/authorization,
 * validated search &amp; filtering, and an administrator analytics dashboard.
 */
public final class FlashcardApp extends Application {
    private static final String BACKGROUND = "#F5F1E9";
    private static final String PANEL = "#EDE7D9";
    private static final String TEXT = "#171717";
    private static final String MUTED = "#9B978F";
    private static final String ERROR = "#B3261E";
    private static final String JAPANESE_FONT_FAMILY = "Noto Sans CJK JP";

    private static final Map<String, String> LANGUAGES = new LinkedHashMap<>();
    static {
        LANGUAGES.put("English", "en");
        LANGUAGES.put("日本語", "ja");
        LANGUAGES.put("Русский", "ru");
        LANGUAGES.put("العربية", "ar");
    }

    private final Database database = connectDatabase();
    private final AuthService authService = new AuthService(database);
    private final DeckRepository deckRepository = createDeckRepository();
    private final ReportService reportService = new ReportService(deckRepository, authService);
    private final CardBank cardBank = new CardBank(database);

    private static Database connectDatabase() {
        Database db = Database.fromEnvironment();
        if (db.isAvailable()) {
            return db;
        }
        System.err.println("Falling back to in-memory storage.");
        return null;
    }

    private DeckRepository createDeckRepository() {
        List<Deck> seeds = List.of(
                new Deck("Cell Biology", "Biology", 10),
                new Deck("20th Century History", "History", 8),
                new Deck("Calculus", "Mathematics", 10),
                new Deck("Literary Devices", "English Lit", 8));
        if (database == null) {
            return new DeckRepository(seeds);
        }
        seedDemoAccounts();
        DeckRepository repository = new DeckRepository(database, seeds);
        new CardBank(database);
        repository.reload();
        return repository;
    }

    private Stage stage;
    private Scene scene;
    private User currentUser;
    private final Localization localization = new Localization();
    private Deck currentDeck;
    private int studyIndex;
    private boolean showingAnswer;
    private boolean registerMode;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        if (database == null) {
            seedDemoAccounts();
        }

        stage.setTitle(text("app.title"));
        stage.setMinWidth(580);
        stage.setMinHeight(560);
        scene = new Scene(new VBox(), 1040, 760);
        stage.setScene(scene);
        showLoginScene();
        stage.show();
    }

    private String text(String key, Object... arguments) {
        return localization.text(key, arguments);
    }

    private String titleFontFamily() {
        return "ja".equals(localization.locale().getLanguage())
                ? JAPANESE_FONT_FAMILY : "Georgia";
    }

    private void setPageRoot(VBox pageRoot) {
        pageRoot.setNodeOrientation(localization.isRightToLeft()
                ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        if ("ja".equals(localization.locale().getLanguage())) {
            pageRoot.setStyle(pageRoot.getStyle() + "-fx-font-family: \"" + JAPANESE_FONT_FAMILY + "\";");
        }
        scene.setRoot(pageRoot);
    }

    private ComboBox<String> languageSelector(Runnable rerender) {
        ComboBox<String> selector = new ComboBox<>();
        selector.setStyle("-fx-font-family: \"" + JAPANESE_FONT_FAMILY + "\";");
        selector.getItems().addAll(LANGUAGES.keySet());
        selector.setValue(LANGUAGES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(localization.locale().getLanguage()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow());
        selector.setAccessibleText(text("language.label"));
        selector.setOnAction(event -> {
            String languageTag = LANGUAGES.get(selector.getValue());
            if (languageTag != null && !languageTag.equals(localization.locale().getLanguage())) {
                localization.setLanguage(languageTag);
                stage.setTitle(text("app.title"));
                rerender.run();
            }
        });
        return selector;
    }

    /** Wraps content in a scroll pane that keeps it centered and lets it shrink or grow with the window. */
    private ScrollPane centeredScrollPane(Region content) {
        StackPane centered = new StackPane(content);
        centered.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(centered);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: " + BACKGROUND + "; -fx-background: " + BACKGROUND + ";");
        return scrollPane;
    }

    /** Demo accounts so reviewers can sign in immediately without registering first. */
    private void seedDemoAccounts() {
        seedAccount("admin", "admin1234", Role.ADMIN);
        seedAccount("teacher", "teacher123", Role.TEACHER);
        seedAccount("student", "student123", Role.STUDENT);
    }

    private void seedAccount(String username, String password, Role role) {
        if (authService.findByUsername(username).isEmpty()) {
            authService.register(username, password.toCharArray(), role);
        }
    }

    // ---------------------------------------------------------------- login

    private void showLoginScene() {
        registerMode = false;
        showLoginScene("", "");
    }

    private void showLoginScene(String username, String password) {
        Label title = new Label(text(registerMode ? "login.registerTitle" : "login.title"));
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 32px; -fx-font-family: \"" + titleFontFamily() + "\"; -fx-font-weight: bold;");

        Label subtitle = new Label(text(registerMode ? "login.registerSubtitle" : "login.subtitle"));
        subtitle.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");

        TextField usernameField = new TextField();
        usernameField.setText(username);
        usernameField.setPromptText(text("login.username"));
        styleField(usernameField);

        PasswordField passwordField = new PasswordField();
        passwordField.setText(password);
        passwordField.setPromptText(text("login.password"));
        styleField(passwordField);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + ERROR + "; -fx-font-size: 12px;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        Button primaryButton = new Button(text(registerMode ? "login.createAccount" : "login.signIn"));
        primaryButton.setMaxWidth(Double.MAX_VALUE);
        primaryButton.setStyle("-fx-background-color: " + TEXT + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12;");

        Hyperlink toggleLink = new Hyperlink(text(registerMode ? "login.existingAccount" : "login.newAccount"));
        toggleLink.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        HBox languageRow = new HBox(8, new Label(text("language.label")),
                languageSelector(() -> showLoginScene(usernameField.getText(), passwordField.getText())));
        languageRow.setAlignment(Pos.CENTER_RIGHT);

        toggleLink.setOnAction(e -> {
            registerMode = !registerMode;
            title.setText(text(registerMode ? "login.registerTitle" : "login.title"));
            subtitle.setText(text(registerMode ? "login.registerSubtitle" : "login.subtitle"));
            primaryButton.setText(text(registerMode ? "login.createAccount" : "login.signIn"));
            toggleLink.setText(text(registerMode ? "login.existingAccount" : "login.newAccount"));
            hide(errorLabel);
        });

        primaryButton.setOnAction(e -> {
            hide(errorLabel);
            try {
                if (registerMode) {
                    authService.register(usernameField.getText(), passwordField.getText().toCharArray(), Role.STUDENT);
                }
                currentUser = authService.login(usernameField.getText(), passwordField.getText().toCharArray());
                showMainScene();
            } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
                show(errorLabel, localizedError(ex.getMessage()));
            }
        });

        VBox form = new VBox(14, title, subtitle, languageRow, usernameField, passwordField, errorLabel, primaryButton, toggleLink);
        form.setMaxWidth(360);
        form.setPadding(new Insets(36, 32, 36, 32));
        form.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 16;");

        VBox content = new VBox(form);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(460);
        content.setPadding(new Insets(40));

        ScrollPane scrollPane = centeredScrollPane(content);
        VBox page = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        page.setAlignment(Pos.CENTER);
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");

        setPageRoot(page);
    }

    private String localizedError(String message) {
        if (message == null) {
            return text("error.unknown");
        }
        if (message.startsWith("Deck not found: ")) {
            return text("error.deckNotFound", message.substring("Deck not found: ".length()));
        }
        return switch (message) {
            case "Invalid username or password" -> text("error.invalidCredentials");
            case "Username is already taken" -> text("error.usernameTaken");
            case "Username must be 3-32 characters (letters, digits, '.', '_' or '-')" -> text("error.usernameRule");
            case "Password must be at least 8 characters" -> text("error.passwordRule");
            case "Deck name cannot be blank" -> text("error.deckName");
            case "Deck subject cannot be blank" -> text("error.deckSubject");
            case "A deck needs at least one card" -> text("error.deckCardCount");
            case "Card front cannot be blank" -> text("error.cardQuestion");
            case "Card back cannot be blank" -> text("error.cardAnswer");
            case "Unable to save user" -> text("error.userSave");
            case "Unable to save deck" -> text("error.deckSave");
            case "Unable to save card" -> text("error.cardSave");
            case "Unable to hash password" -> text("error.passwordHash");
            default -> message;
        };
    }

    private void styleField(TextField field) {
        field.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-padding: 12;");
    }

    private void show(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
    }

    private void hide(Label label) {
        label.setManaged(false);
        label.setVisible(false);
    }

    // ----------------------------------------------------------------- main

    private void showMainScene() {
        showMainScene("", "All", DeckSortOption.NAME_ASC);
    }

    private void showMainScene(String initialSearch, String initialSubjectFilter, DeckSortOption initialSortOption) {
        String[] searchText = {initialSearch};
        String[] subjectFilter = {initialSubjectFilter};
        DeckSortOption[] sortOption = {initialSortOption};

        VBox deckListHolder = new VBox();

        TextField searchField = new TextField();
        searchField.setText(initialSearch);
        searchField.setPromptText(text("decks.search"));
        styleField(searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ComboBox<String> subjectCombo = new ComboBox<>();
        subjectCombo.getItems().add(text("decks.allSubjects"));
        subjectCombo.getItems().addAll(deckRepository.subjects());
        subjectCombo.setValue("All".equals(initialSubjectFilter)
                ? text("decks.allSubjects") : initialSubjectFilter);

        ComboBox<String> sortCombo = new ComboBox<>();
        Map<String, DeckSortOption> sortOptions = new LinkedHashMap<>();
        sortOptions.put(text("sort.name"), DeckSortOption.NAME_ASC);
        sortOptions.put(text("sort.subject"), DeckSortOption.SUBJECT_ASC);
        sortOptions.put(text("sort.cards"), DeckSortOption.CARD_COUNT_DESC);
        sortCombo.getItems().addAll(sortOptions.keySet());
        sortCombo.setValue(sortOptions.entrySet().stream()
                .filter(entry -> entry.getValue() == initialSortOption)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(text("sort.name")));

        Runnable refreshList = () -> renderDeckList(deckListHolder, searchText[0], subjectFilter[0], sortOption[0]);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchText[0] = newVal;
            refreshList.run();
        });
        subjectCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            subjectFilter[0] = text("decks.allSubjects").equals(newVal) ? "All" : newVal;
            refreshList.run();
        });
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            sortOption[0] = sortOptions.getOrDefault(newVal, DeckSortOption.NAME_ASC);
            refreshList.run();
        });

        HBox searchBar = new HBox(8, searchField, subjectCombo, sortCombo);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        refreshList.run();

        VBox content = new VBox(20, topBar(() -> showMainScene(
                searchText[0], subjectFilter[0], sortOption[0])), heading(), searchBar, deckListHolder);
        if (currentUser.canManageContent()) {
            content.getChildren().add(addDeckForm(refreshList));
        }
        content.setMaxWidth(460);
        content.setPadding(new Insets(32, 22, 43, 22));

        ScrollPane scrollPane = centeredScrollPane(content);
        VBox page = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");

        setPageRoot(page);
    }

    private HBox topBar(Runnable rerender) {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label welcome = new Label(text("decks.welcome", currentUser.username(), text("role." + currentUser.role().name().toLowerCase())));
        welcome.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        HBox bar = new HBox(12, spacer, welcome);
        bar.setAlignment(Pos.CENTER_LEFT);

        if (currentUser.isAdmin()) {
            Button dashboardButton = new Button(text("decks.dashboard"));
            dashboardButton.setStyle(secondaryButtonStyle());
            dashboardButton.setOnAction(e -> showDashboardScene());
            bar.getChildren().add(bar.getChildren().size() - 1, dashboardButton);
        }

        Button logoutButton = new Button(text("common.logout"));
        logoutButton.setStyle(secondaryButtonStyle());
        logoutButton.setOnAction(e -> {
            currentUser = null;
            showLoginScene();
        });
        bar.getChildren().add(languageSelector(rerender));
        bar.getChildren().add(logoutButton);
        return bar;
    }

    private String secondaryButtonStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: " + TEXT + "; -fx-font-size: 12px; -fx-border-color: #D8D2C2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 12 6 12; -fx-cursor: hand;";
    }

    private VBox heading() {
        List<Deck> allDecks = deckRepository.all();
        int totalCards = allDecks.stream().mapToInt(Deck::cardCount).sum();

        Label title = new Label(text("decks.title"));
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 38px; -fx-font-family: \"" + titleFontFamily() + "\"; -fx-font-weight: bold;");

        Label summary = new Label(text("decks.summary", localization.number(totalCards), localization.number(allDecks.size())));
        summary.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");

        return new VBox(4, title, summary);
    }

    private void renderDeckList(VBox holder, String query, String subjectFilter, DeckSortOption sortOption) {
        List<Deck> matches = deckRepository.search(query, subjectFilter, sortOption);

        VBox list = new VBox();
        list.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 13;");

        if (matches.isEmpty()) {
            Label empty = new Label(text("decks.empty"));
            empty.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px; -fx-padding: 16;");
            list.getChildren().add(empty);
        } else {
            for (Deck deck : matches) {
                list.getChildren().add(deckButton(deck));
            }
        }

        holder.getChildren().setAll(list);
    }

    private Button deckButton(Deck deck) {
        Label deckName = new Label(deck.name());
        deckName.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label details = new Label(text("decks.cardSummary", deck.subject(),
                localization.number(deck.cardCount()),
                text(deck.cardCount() == 1 ? "decks.card.one" : "decks.card.many")));
        details.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        VBox labels = new VBox(2, deckName, details);
        HBox row = new HBox(labels, new Label(localization.isRightToLeft() ? "\u2039" : "\u203A"));
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(labels, Priority.ALWAYS);
        row.getChildren().get(1).setStyle("-fx-text-fill: #B8B1A4; -fx-font-size: 26px;");

        Button button = new Button();
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPadding(new Insets(12, 15, 12, 15));
        button.setStyle("-fx-background-color: transparent; -fx-border-color: transparent transparent #E2DCCE transparent; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
        button.setOnAction(event -> {
            if (!deck.equals(currentDeck)) {
                studyIndex = 0;
                showingAnswer = false;
            }
            currentDeck = deck;
            showStudyScene(deck);
        });
        return button;
    }

    private VBox addDeckForm(Runnable onDeckAdded) {
        Label heading = new Label(text("deckForm.title"));
        heading.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        nameField.setPromptText(text("deckForm.name"));
        styleField(nameField);

        TextField subjectField = new TextField();
        subjectField.setPromptText(text("deckForm.subject"));
        styleField(subjectField);

        TextField cardCountField = new TextField();
        cardCountField.setPromptText(text("deckForm.cardCount"));
        styleField(cardCountField);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + ERROR + "; -fx-font-size: 12px;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        Button addButton = new Button(text("deckForm.add"));
        addButton.setStyle("-fx-background-color: " + TEXT + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 18 9 18;");
        addButton.setOnAction(e -> {
            hide(errorLabel);
            try {
                int cardCount = Integer.parseInt(cardCountField.getText().trim());
                deckRepository.addDeck(nameField.getText(), subjectField.getText(), cardCount);
                nameField.clear();
                subjectField.clear();
                cardCountField.clear();
                onDeckAdded.run();
            } catch (NumberFormatException ex) {
                show(errorLabel, text("error.wholeNumber"));
            } catch (IllegalArgumentException ex) {
                show(errorLabel, localizedError(ex.getMessage()));
            }
        });

        HBox fields = new HBox(8, nameField, subjectField, cardCountField, addButton);
        fields.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, heading, fields, errorLabel);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 13;");
        return box;
    }

    // ----------------------------------------------------------------- study

    private void showStudyScene(Deck deck) {
        List<Card> cards = cardBank.cardsFor(deck);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        currentDeck = deck;

        Button backButton = new Button(text("study.backToDecks"));
        backButton.setStyle(secondaryButtonStyle());
        backButton.setOnAction(e -> showMainScene());

        HBox bar = new HBox(12, spacer, backButton);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(deck.name());
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 28px; -fx-font-family: \"" + titleFontFamily() + "\"; -fx-font-weight: bold;");

        Label progress = new Label();
        progress.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");

        Label sideLabel = new Label();
        sideLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label cardText = new Label();
        cardText.setWrapText(true);
        cardText.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-alignment: center;");

        Label hint = new Label(text("study.hint"));
        hint.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        VBox cardPanel = new VBox(12, sideLabel, cardText);
        cardPanel.setAlignment(Pos.CENTER);
        cardPanel.setPadding(new Insets(44, 26, 44, 26));
        cardPanel.setMinHeight(240);
        cardPanel.setMaxWidth(Double.MAX_VALUE);
        cardPanel.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 16; -fx-cursor: hand;");

        Button prevButton = new Button(text("study.previous"));
        prevButton.setStyle(secondaryButtonStyle());
        Button flipButton = new Button(text("study.flip"));
        flipButton.setStyle("-fx-background-color: " + TEXT + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 18 9 18;");
        Button nextButton = new Button(text("study.next"));
        nextButton.setStyle(secondaryButtonStyle());

        Runnable render = () -> {
            Card card = cards.get(studyIndex);
            sideLabel.setText(text(showingAnswer ? "study.answer" : "study.question"));
            cardText.setText(showingAnswer ? card.back() : card.front());
            progress.setText(text("study.progress", localization.number(studyIndex + 1), localization.number(cards.size())));
            prevButton.setDisable(studyIndex == 0);
            nextButton.setDisable(studyIndex == cards.size() - 1);
        };

        cardPanel.setOnMouseClicked(e -> {
            showingAnswer = !showingAnswer;
            render.run();
        });
        flipButton.setOnAction(e -> {
            showingAnswer = !showingAnswer;
            render.run();
        });
        prevButton.setOnAction(e -> {
            if (studyIndex > 0) {
                studyIndex--;
                showingAnswer = false;
                render.run();
            }
        });
        nextButton.setOnAction(e -> {
            if (studyIndex < cards.size() - 1) {
                studyIndex++;
                showingAnswer = false;
                render.run();
            }
        });

        render.run();

        HBox navigation = new HBox(8, prevButton, flipButton, nextButton);
        navigation.setAlignment(Pos.CENTER);

        bar.getChildren().add(languageSelector(() -> showStudyScene(deck)));
        VBox content = new VBox(16, bar, title, progress, cardPanel, hint, navigation);
        if (currentUser.canManageContent()) {
            content.getChildren().add(addCardForm(deck, cards, render));
        }
        content.setMaxWidth(460);
        content.setPadding(new Insets(32, 22, 43, 22));

        ScrollPane scrollPane = centeredScrollPane(content);
        VBox page = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");

        setPageRoot(page);
    }

    private VBox addCardForm(Deck deck, List<Card> cards, Runnable render) {
        Label heading = new Label(text("cardForm.title"));
        heading.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        TextField frontField = new TextField();
        frontField.setPromptText(text("study.question"));
        styleField(frontField);
        HBox.setHgrow(frontField, Priority.ALWAYS);

        TextField backField = new TextField();
        backField.setPromptText(text("study.answer"));
        styleField(backField);
        HBox.setHgrow(backField, Priority.ALWAYS);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + ERROR + "; -fx-font-size: 12px;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        Button addButton = new Button(text("cardForm.add"));
        addButton.setStyle("-fx-background-color: " + TEXT + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 18 9 18;");
        addButton.setOnAction(e -> {
            hide(errorLabel);
            try {
                cardBank.addCard(deck, frontField.getText(), backField.getText());
                deckRepository.updateCardCount(deck, cards.size());
                frontField.clear();
                backField.clear();
                studyIndex = cards.size() - 1;
                showingAnswer = false;
                render.run();
            } catch (IllegalArgumentException ex) {
                show(errorLabel, localizedError(ex.getMessage()));
            }
        });

        HBox fields = new HBox(8, frontField, backField, addButton);
        fields.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, heading, fields, errorLabel);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 13;");
        return box;
    }

    // ------------------------------------------------------------ dashboard

    private void showDashboardScene() {
        ReportService.DashboardReport report = reportService.generate();

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button(text("common.back"));
        backButton.setStyle(secondaryButtonStyle());
        backButton.setOnAction(e -> showMainScene());

        Button logoutButton = new Button(text("common.logout"));
        logoutButton.setStyle(secondaryButtonStyle());
        logoutButton.setOnAction(e -> {
            currentUser = null;
            showLoginScene();
        });

        HBox bar = new HBox(12, spacer, backButton, languageSelector(() -> showDashboardScene()), logoutButton);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(text("dashboard.title"));
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 32px; -fx-font-family: \"" + titleFontFamily() + "\"; -fx-font-weight: bold;");

        HBox statCards = new HBox(12,
                statCard(text("dashboard.totalDecks"), localization.number(report.totalDecks())),
                statCard(text("dashboard.totalCards"), localization.number(report.totalCards())),
                statCard(text("dashboard.averageCards"), localization.decimal(report.averageCardsPerDeck())),
                statCard(text("dashboard.registeredUsers"), localization.number(report.registeredUsers())));

        VBox subjectBreakdown = new VBox(10);
        subjectBreakdown.setPadding(new Insets(16));
        subjectBreakdown.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 13;");

        Label breakdownTitle = new Label(text("dashboard.subjectBreakdown"));
        breakdownTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        subjectBreakdown.getChildren().add(breakdownTitle);
        Label generatedAt = new Label(text("dashboard.generatedAt", localization.dateTime(Instant.now())));
        generatedAt.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        subjectBreakdown.getChildren().add(generatedAt);

        int maxDecks = report.decksPerSubject().values().stream().mapToInt(Integer::intValue).max().orElse(1);
        for (Map.Entry<String, Integer> entry : report.decksPerSubject().entrySet()) {
            subjectBreakdown.getChildren().add(subjectRow(entry.getKey(), entry.getValue(), maxDecks));
        }

        VBox content = new VBox(20, bar, title, statCards, subjectBreakdown);
        content.setMaxWidth(460);
        content.setPadding(new Insets(32, 22, 43, 22));

        ScrollPane scrollPane = centeredScrollPane(content);
        VBox page = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");

        setPageRoot(page);
    }

    private VBox statCard(String label, String value) {
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label captionLabel = new Label(label);
        captionLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        VBox card = new VBox(4, valueLabel, captionLabel);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 10;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox subjectRow(String subject, int count, int maxCount) {
        Label subjectLabel = new Label(subject);
        subjectLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12px;");
        subjectLabel.setMinWidth(110);

        ProgressBar bar = new ProgressBar(maxCount == 0 ? 0 : (double) count / maxCount);
        bar.setStyle("-fx-accent: " + TEXT + ";");
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label countLabel = new Label(localization.number(count));
        countLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
        countLabel.setMinWidth(20);

        HBox row = new HBox(10, subjectLabel, bar, countLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static void main(String[] args) {
        launch(args);
    }
}