package com.flashcards;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public final class FlashcardApp extends Application {
    private static final String BACKGROUND = "#F5F1E9";
    private static final String PANEL = "#EDE7D9";
    private static final String TEXT = "#171717";
    private static final String MUTED = "#9B978F";

    private final List<Deck> decks = List.of(
            new Deck("Cell Biology", "Biology", 6),
            new Deck("20th Century History", "History", 5),
            new Deck("Calculus", "Mathematics", 7),
            new Deck("Literary Devices", "English Lit", 5));

    @Override
    public void start(Stage stage) {
        VBox content = new VBox(28, brand(), heading(), deckList());
        content.setMaxWidth(398);
        content.setPadding(new Insets(43, 22, 43, 22));

        VBox page = new VBox(content);
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: " + BACKGROUND + ";");

        Scene scene = new Scene(page, 900, 650);
        stage.setTitle("Studycard");
        stage.setMinWidth(520);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
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
        Label title = new Label("Ready to study?");
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 34px; -fx-font-family: Georgia; -fx-font-weight: bold;");

        Label summary = new Label(totalCards() + " cards across " + decks.size() + " decks");
        summary.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        return new VBox(4, title, summary);
    }

    private VBox deckList() {
        VBox list = new VBox();
        list.setStyle("-fx-background-color: " + PANEL + "; -fx-background-radius: 13;");

        for (Deck deck : decks) {
            list.getChildren().add(deckButton(deck));
        }
        return list;
    }

    private Button deckButton(Deck deck) {
        Label deckName = new Label(deck.name());
        deckName.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label details = new Label(deck.cardSummary());
        details.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        VBox labels = new VBox(2, deckName, details);
        HBox row = new HBox(labels, new Label("›"));
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(labels, javafx.scene.layout.Priority.ALWAYS);
        row.getChildren().get(1).setStyle("-fx-text-fill: #B8B1A4; -fx-font-size: 24px;");

        Button button = new Button();
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPadding(new Insets(12, 15, 12, 15));
        button.setStyle("-fx-background-color: transparent; -fx-border-color: transparent transparent #E2DCCE transparent; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
        button.setOnAction(event -> showDeckMessage(deck));
        return button;
    }

    private void showDeckMessage(Deck deck) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Studycard");
        alert.setHeaderText(deck.name());
        alert.setContentText("Starting " + deck.cardCount() + " cards in " + deck.subject() + ".");
        alert.showAndWait();
    }

    private int totalCards() {
        return decks.stream().mapToInt(Deck::cardCount).sum();
    }

    public static void main(String[] args) {
        launch(args);
    }
}