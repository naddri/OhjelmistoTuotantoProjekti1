package com.flashcards;

import javafx.application.Application;
import javafx.geometry.Insets;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

    private static final Map<String, DeckSortOption> SORT_OPTIONS = new LinkedHashMap<>();
    static {
        SORT_OPTIONS.put("Name (A-Z)", DeckSortOption.NAME_ASC);
        SORT_OPTIONS.put("Subject (A-Z)", DeckSortOption.SUBJECT_ASC);
        SORT_OPTIONS.put("Most cards first", DeckSortOption.CARD_COUNT_DESC);
    }

    private final AuthService authService = new AuthService();
    private final DeckRepository deckRepository = new DeckRepository(List.of(
            new Deck("Cell Biology", "Biology", 6),
            new Deck("20th Century History", "History", 5),
            new Deck("Calculus", "Mathematics", 7),
            new Deck("Literary Devices", "English Lit", 5)));
    private final ReportService reportService = new ReportService(deckRepository, authService);
    private final CardBank cardBank = new CardBank();

    private Stage stage;
    private User currentUser;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        seedDemoAccounts();

        stage.setTitle("Studycard");
        stage.setMinWidth(520);
        stage.setMinHeight(500);
        showLoginScene();
        stage.show();
    }

    /** Demo accounts so reviewers can sign in immediately without registering first. */
    private void seedDemoAccounts() {
        authService.register("admin", "admin1234".toCharArray(), Role.ADMIN);
        authService.register("teacher", "teacher123".toCharArray(), Role.TEACHER);
        authService.register("student", "student123".toCharArray(), Role.STUDENT);
    }

    // ---------------------------------------------------------------- login

    private void showLoginScene() {
        boolean[] registerMode = {false};

        Label title = new Label("Welcome back");
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 28px; -fx-font-family: Georgia; -fx-font-weight: bold;");

        Label subtitle = new Label("Sign in to continue studying");
        subtitle.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        styleField(usernameField);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        styleField(passwordField);

        ComboBox<Role> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(Role.STUDENT, Role.TEACHER, Role.ADMIN);
        roleCombo.setValue(Role.STUDENT);
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setManaged(false);
        roleCombo.setVisible(false);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + ERROR + "; -fx-font-size: 11px;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        Button primaryButton = new Button("Sign in");
        primaryButton.setMaxWidth(Double.MAX_VALUE);
        primaryButton.setStyle("-fx-background-color: " + TEXT + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10;");

        Hyperlink toggleLink = new Hyperlink("New here? Create an account");
        toggleLink.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        toggleLink.setOnAction(e -> {
            registerMode[0] = !registerMode[0];
            title.setText(registerMode[0] ? "Create an account" : "Welcome back");
            subtitle.setText(registerMode[0] ? "Choose a username, password and role" : "Sign in to continue studying");
            primaryButton.setText(registerMode[0] ? "Create account" : "Sign in");
            toggleLink.setText(registerMode[0] ? "Already have an account? Sign in" : "New here? Create an account");
            roleCombo.setManaged(registerMode[0]);
            roleCombo.setVisible(registerMode[0]);
            hide(errorLabel);
        });

        primaryButton.setOnAction(e -> {
            hide(errorLabel);
            try {
                if (registerMode[0]) {
                    authService.register(usernameField.getText(), passwordField.getText().toCharArray(), roleCombo.getValue());
                }
                currentUser = authService.login(usernameField.getText(), passwordField.getText().toCharArray());
                showMainScene();
            } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
                show(errorLabel, ex.getMessage());
            }
        });

        Label demoHint = new Label("Demo accounts: admin/admin1234 · teacher/teacher123 · student/student123");
        demoHint.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px;");
        demoHint.setWrapText(true);

        VBox form = new VBox(12, title, subtitle, usernameField, passwordField, roleCombo, errorLabel, primaryButton, toggleLink, demoHint);
        form.setMaxWidth(320);
        form.setPadding(new Insets(32, 28, 32, 28));
        form.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 16;");

        VBox page = new VBox(brand(), form);
        page.setSpacing(24);
        page.setAlignment(Pos.CENTER);
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");
        page.setPadding(new Insets(40));

        stage.setScene(new Scene(page, 900, 650));
    }

    private void styleField(TextField field) {
        field.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-text-fill: " + TEXT + "; -fx-font-size: 12px; -fx-padding: 10;");
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
        String[] searchText = {""};
        String[] subjectFilter = {"All"};
        DeckSortOption[] sortOption = {DeckSortOption.NAME_ASC};

        VBox deckListHolder = new VBox();

        TextField searchField = new TextField();
        searchField.setPromptText("Search decks or subjects");
        styleField(searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ComboBox<String> subjectCombo = new ComboBox<>();
        subjectCombo.getItems().add("All");
        subjectCombo.getItems().addAll(deckRepository.subjects());
        subjectCombo.setValue("All");

        ComboBox<String> sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll(SORT_OPTIONS.keySet());
        sortCombo.setValue("Name (A-Z)");

        Runnable refreshList = () -> renderDeckList(deckListHolder, searchText[0], subjectFilter[0], sortOption[0]);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchText[0] = newVal;
            refreshList.run();
        });
        subjectCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            subjectFilter[0] = newVal;
            refreshList.run();
        });
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            sortOption[0] = SORT_OPTIONS.getOrDefault(newVal, DeckSortOption.NAME_ASC);
            refreshList.run();
        });

        HBox searchBar = new HBox(8, searchField, subjectCombo, sortCombo);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        refreshList.run();

        VBox content = new VBox(20, topBar(), heading(), searchBar, deckListHolder);
        if (currentUser.canManageContent()) {
            content.getChildren().add(addDeckForm(refreshList));
        }
        content.setMaxWidth(420);
        content.setPadding(new Insets(32, 22, 43, 22));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: " + BACKGROUND + "; -fx-background: " + BACKGROUND + ";");

        VBox page = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");

        stage.setScene(new Scene(page, 900, 650));
    }

    private HBox topBar() {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label welcome = new Label("Hi, " + currentUser.username() + " (" + currentUser.role() + ")");
        welcome.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        HBox bar = new HBox(12, brand(), spacer, welcome);
        bar.setAlignment(Pos.CENTER_LEFT);

        if (currentUser.isAdmin()) {
            Button dashboardButton = new Button("Dashboard");
            dashboardButton.setStyle(secondaryButtonStyle());
            dashboardButton.setOnAction(e -> showDashboardScene());
            bar.getChildren().add(bar.getChildren().size() - 1, dashboardButton);
        }

        Button logoutButton = new Button("Log out");
        logoutButton.setStyle(secondaryButtonStyle());
        logoutButton.setOnAction(e -> {
            currentUser = null;
            showLoginScene();
        });
        bar.getChildren().add(logoutButton);
        return bar;
    }

    private String secondaryButtonStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: " + TEXT + "; -fx-font-size: 11px; -fx-border-color: #D8D2C2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 5 10 5 10; -fx-cursor: hand;";
    }

    private HBox brand() {
        Label icon = new Label("▣");
        icon.setStyle("-fx-background-color: #171717; -fx-text-fill: white; -fx-padding: 3 5 3 5; -fx-background-radius: 3; -fx-font-size: 10px;");

        Label name = new Label("STUDYCARD");
        name.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        HBox brand = new HBox(7, icon, name);
        brand.setAlignment(Pos.CENTER_LEFT);
        return brand;
    }

    private VBox heading() {
        List<Deck> allDecks = deckRepository.all();
        int totalCards = allDecks.stream().mapToInt(Deck::cardCount).sum();

        Label title = new Label("Ready to study?");
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 34px; -fx-font-family: Georgia; -fx-font-weight: bold;");

        Label summary = new Label(totalCards + " cards across " + allDecks.size() + " decks");
        summary.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        return new VBox(4, title, summary);
    }

    private void renderDeckList(VBox holder, String query, String subjectFilter, DeckSortOption sortOption) {
        List<Deck> matches = deckRepository.search(query, subjectFilter, sortOption);

        VBox list = new VBox();
        list.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 13;");

        if (matches.isEmpty()) {
            Label empty = new Label("No decks match your search.");
            empty.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px; -fx-padding: 16;");
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
        deckName.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label details = new Label(deck.cardSummary());
        details.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        VBox labels = new VBox(2, deckName, details);
        HBox row = new HBox(labels, new Label("\u203A"));
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(labels, Priority.ALWAYS);
        row.getChildren().get(1).setStyle("-fx-text-fill: #B8B1A4; -fx-font-size: 24px;");

        Button button = new Button();
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPadding(new Insets(12, 15, 12, 15));
        button.setStyle("-fx-background-color: transparent; -fx-border-color: transparent transparent #E2DCCE transparent; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
        button.setOnAction(event -> showStudyScene(deck));
        return button;
    }

    private VBox addDeckForm(Runnable onDeckAdded) {
        Label heading = new Label("Add a new deck");
        heading.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        nameField.setPromptText("Deck name");
        styleField(nameField);

        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject");
        styleField(subjectField);

        TextField cardCountField = new TextField();
        cardCountField.setPromptText("Card count");
        styleField(cardCountField);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + ERROR + "; -fx-font-size: 11px;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        Button addButton = new Button("Add deck");
        addButton.setStyle("-fx-background-color: " + TEXT + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16 8 16;");
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
                show(errorLabel, "Card count must be a whole number");
            } catch (IllegalArgumentException ex) {
                show(errorLabel, ex.getMessage());
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
        int[] index = {0};
        boolean[] showingBack = {false};

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("\u2039 Back to decks");
        backButton.setStyle(secondaryButtonStyle());
        backButton.setOnAction(e -> showMainScene());

        HBox bar = new HBox(12, brand(), spacer, backButton);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(deck.name());
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 26px; -fx-font-family: Georgia; -fx-font-weight: bold;");

        Label progress = new Label();
        progress.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        Label sideLabel = new Label();
        sideLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label cardText = new Label();
        cardText.setWrapText(true);
        cardText.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-alignment: center;");

        Label hint = new Label("Click the card to flip it");
        hint.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px;");

        VBox cardPanel = new VBox(12, sideLabel, cardText);
        cardPanel.setAlignment(Pos.CENTER);
        cardPanel.setPadding(new Insets(40, 24, 40, 24));
        cardPanel.setMinHeight(220);
        cardPanel.setMaxWidth(Double.MAX_VALUE);
        cardPanel.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 16; -fx-cursor: hand;");

        Button prevButton = new Button("\u2039 Previous");
        prevButton.setStyle(secondaryButtonStyle());
        Button flipButton = new Button("Flip card");
        flipButton.setStyle("-fx-background-color: " + TEXT + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16 8 16;");
        Button nextButton = new Button("Next \u203A");
        nextButton.setStyle(secondaryButtonStyle());

        Runnable render = () -> {
            Card card = cards.get(index[0]);
            sideLabel.setText(showingBack[0] ? "ANSWER" : "QUESTION");
            cardText.setText(showingBack[0] ? card.back() : card.front());
            progress.setText("Card " + (index[0] + 1) + " of " + cards.size());
            prevButton.setDisable(index[0] == 0);
            nextButton.setDisable(index[0] == cards.size() - 1);
        };

        cardPanel.setOnMouseClicked(e -> {
            showingBack[0] = !showingBack[0];
            render.run();
        });
        flipButton.setOnAction(e -> {
            showingBack[0] = !showingBack[0];
            render.run();
        });
        prevButton.setOnAction(e -> {
            if (index[0] > 0) {
                index[0]--;
                showingBack[0] = false;
                render.run();
            }
        });
        nextButton.setOnAction(e -> {
            if (index[0] < cards.size() - 1) {
                index[0]++;
                showingBack[0] = false;
                render.run();
            }
        });

        render.run();

        HBox navigation = new HBox(8, prevButton, flipButton, nextButton);
        navigation.setAlignment(Pos.CENTER);

        VBox content = new VBox(16, bar, title, progress, cardPanel, hint, navigation);
        content.setMaxWidth(420);
        content.setPadding(new Insets(32, 22, 43, 22));

        VBox page = new VBox(content);
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");

        stage.setScene(new Scene(page, 900, 650));
    }

    // ------------------------------------------------------------ dashboard

    private void showDashboardScene() {
        ReportService.DashboardReport report = reportService.generate();

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("Back");
        backButton.setStyle(secondaryButtonStyle());
        backButton.setOnAction(e -> showMainScene());

        Button logoutButton = new Button("Log out");
        logoutButton.setStyle(secondaryButtonStyle());
        logoutButton.setOnAction(e -> {
            currentUser = null;
            showLoginScene();
        });

        HBox bar = new HBox(12, brand(), spacer, backButton, logoutButton);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Analytics dashboard");
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 28px; -fx-font-family: Georgia; -fx-font-weight: bold;");

        HBox statCards = new HBox(12,
                statCard("Total decks", String.valueOf(report.totalDecks())),
                statCard("Total cards", String.valueOf(report.totalCards())),
                statCard("Avg cards/deck", String.format("%.1f", report.averageCardsPerDeck())),
                statCard("Registered users", String.valueOf(report.registeredUsers())));

        VBox subjectBreakdown = new VBox(10);
        subjectBreakdown.setPadding(new Insets(16));
        subjectBreakdown.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 13;");

        Label breakdownTitle = new Label("Decks per subject");
        breakdownTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        subjectBreakdown.getChildren().add(breakdownTitle);

        int maxDecks = report.decksPerSubject().values().stream().mapToInt(Integer::intValue).max().orElse(1);
        for (Map.Entry<String, Integer> entry : report.decksPerSubject().entrySet()) {
            subjectBreakdown.getChildren().add(subjectRow(entry.getKey(), entry.getValue(), maxDecks));
        }

        VBox content = new VBox(20, bar, title, statCards, subjectBreakdown);
        content.setMaxWidth(420);
        content.setPadding(new Insets(32, 22, 43, 22));

        VBox page = new VBox(content);
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");

        stage.setScene(new Scene(page, 900, 650));
    }

    private VBox statCard(String label, String value) {
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label captionLabel = new Label(label);
        captionLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px;");

        VBox card = new VBox(4, valueLabel, captionLabel);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 10;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox subjectRow(String subject, int count, int maxCount) {
        Label subjectLabel = new Label(subject);
        subjectLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 11px;");
        subjectLabel.setMinWidth(110);

        ProgressBar bar = new ProgressBar(maxCount == 0 ? 0 : (double) count / maxCount);
        bar.setStyle("-fx-accent: " + TEXT + ";");
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        countLabel.setMinWidth(20);

        HBox row = new HBox(10, subjectLabel, bar, countLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static void main(String[] args) {
        launch(args);
    }
}