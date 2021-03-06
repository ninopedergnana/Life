package ch.zhaw.pm2.life.controller;

import ch.zhaw.pm2.life.controller.dialogs.ColorPickerDialog;
import ch.zhaw.pm2.life.controller.dialogs.ScanRadiusDialog;
import ch.zhaw.pm2.life.controller.dialogs.SpawnSpeciesDialog;
import ch.zhaw.pm2.life.controller.dialogs.StopConditionDialog;
import ch.zhaw.pm2.life.model.Board;
import ch.zhaw.pm2.life.model.GameObject;
import ch.zhaw.pm2.life.model.GameProperties;
import ch.zhaw.pm2.life.model.lifeform.animal.AnimalObject;
import ch.zhaw.pm2.life.util.ValidationUtil;
import ch.zhaw.pm2.life.view.BoardView;
import ch.zhaw.pm2.life.view.StatisticView;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for the main window.
 */
public class LifeWindowController {

    private static final int ROWS = 16;
    private static final int COLUMNS = 16;
    private final StatisticView statisticView = new StatisticView();
    private BoardView boardView;
    private Game game;
    private SetupController setupController;
    private GameProperties gameProperties;
    private Board boardObject;
    private boolean fullscreen;
    @FXML private BorderPane root;
    @FXML private Pane board;
    @FXML private TextArea messageField;
    @FXML private Button nextRoundButton;
    @FXML private Button stopSimButton;
    @FXML private Menu editMenu;

    /**
     * Initializes everything after the JavaFX components are injected.
     */
    @FXML
    public void initialize() {
        try {
            boardObject = new Board(ROWS, COLUMNS);
            boardView = new BoardView(boardObject);
            board.getChildren().add(boardView);
            board.widthProperty().addListener(observable -> updateSize());
            board.heightProperty().addListener(observable -> updateSize());
            root.addEventHandler(KeyEvent.KEY_PRESSED, keyHandler());
        } catch (Exception e) {
            messageField.appendText(e.getMessage() + "\n");
        }
    }

    @FXML
    private void nextRound() {
        if (game == null) {
            return;
        }

        if (game.isOngoing()) {
            messageField.appendText(game.nextMove() + "\n");
            boardView.draw();
        } else {
            nextRoundButton.setDisable(true);
            stopSimButton.setDisable(true);
            showStatistics();
        }
    }

    @FXML
    private void stopSimulation() {
        if (game == null) {
            return;
        }
        game.stop();
        nextRoundButton.setDisable(true);
        stopSimButton.setDisable(true);
        showStatistics();
    }

    @FXML
    private void updateSize() {
        double newWidth = board.getWidth();
        double newHeight = board.getHeight();
        if (newHeight > 0.0 && newWidth > 0.0) {
            boardView.updateDimension(newWidth, newHeight);
        }
    }

    /**
     * Changes the window in to a fullscreen mode.
     */
    @FXML
    public void toggleFullscreen() {
        fullscreen = !fullscreen;
        ((Stage) root.getScene().getWindow()).setFullScreen(fullscreen);
    }

    /**
     * Allows to hide the message log at the bottom of the window.
     */
    @FXML
    public void toggleTextField() {
        boolean status = messageField.isVisible();
        if (status) {
            messageField.setVisible(false);
            messageField.setManaged(false);
        } else {
            messageField.setManaged(true);
            messageField.setVisible(true);
        }
    }

    /**
     * Shows an popup with a link to the manual.
     */
    @FXML
    public void showManual() {
        String link = "https://github.zhaw.ch/PM2-IT19bWIN-benf-runm/gruppe01-ncpl-projekt2-life/wiki";
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Hilfe");
        dialog.setHeaderText("Die Anleitung zur Simulation finden Sie unter");
        Hyperlink hyperlink = new Hyperlink(link);
        dialog.getDialogPane().setContent(hyperlink);
        hyperlink.setOnAction(event -> setupController.getHostServices().showDocument(link));
        dialog.showAndWait();
    }

    /**
     * Allows to create a new species.
     */
    @FXML
    public void createNewSpecies() {
        SpawnSpeciesDialog dialog = new SpawnSpeciesDialog("Neue Spezies erstellen");
        Optional<Set<GameObject>> response = dialog.showAndWait();

        response.ifPresent(gameObjects -> gameObjects.forEach(gameObject -> {
            Optional<GameObject> duplicate = boardObject.getGameObjects().stream()
                    .filter(go -> go.getName().equalsIgnoreCase(gameObject.getName()))
                    .findAny();
            if (duplicate.isEmpty()) {
                boardObject.addGameObject(gameObject, boardObject.getRandomPosition());
                messageField.appendText(String.format("%s wurde zur Simulation hinzugefuegt.%n", gameObject.getName()));
                boardView.draw();
            }
        }));

    }

    private EventHandler<KeyEvent> keyHandler() {
        return event -> {
            switch (event.getCode()) {
                case F11:
                    toggleFullscreen();
                    break;

                case ESCAPE:
                    fullscreen = false;
                    break;

                case F1:
                    showManual();
                    break;

                default:
                    break;
            }
        };
    }

    private void showStatistics() {
        statisticView.setStartLifeForms(game.getStartLifeForms());
        statisticView.setBornLifeForms(game.getBornLifeForms());
        statisticView.setDiedLifeForms(game.getDeadLifeForms());
        statisticView.setSurvivedLifeForms(game.getSurvivedLifeForms());
        statisticView.setSpawnLifeForms(game.getSpawnedLifeForms());

        Set<String> species = setupController.getGameObjects()
                .keySet()
                .stream()
                .map(GameObject::getName)
                .collect(Collectors.toSet());

        statisticView.initChart((Stage) board.getScene().getWindow(), species);
        statisticView.show();
    }

    /**
     * Draws the board.
     */
    public void drawBoard() {
        if (game == null) {
            return;
        }
        boardView.draw();
    }

    /**
     * Sets a reference of the {@link SetupController}.
     * @param setupController {@link SetupController}
     */
    public void setSetupController(SetupController setupController) {
        this.setupController = Objects.requireNonNull(setupController, "The setup controller cannot be null.");
    }

    /**
     * Initializes the game.
     */
    public void initGame() {
        if (boardObject == null) {
            return;
        }
        try {
            gameProperties = new GameProperties(setupController.getGameObjects());
            game = new Game(boardObject, gameProperties);
            game.setSpeciesToWatch(setupController.getStopCondition());
        } catch (Exception e) {
            messageField.appendText(e.getMessage() + "\n");
        }
    }

    /**
     * Generates the edit menu depending on the.
     * {@link GameObject}'s read from the config file.
     */
    public void initEditMenu() {
        setupController.getGameObjects().forEach((gameObject, amount) -> {
            if (amount > 0) {
                Menu lifeform = new Menu();
                lifeform.setText(gameObject.getName());

                lifeform.getItems().add(changeColor(gameObject));
                lifeform.getItems().add(changeName(gameObject));
                lifeform.getItems().add(changeEnergy(gameObject));

                if (gameObject instanceof AnimalObject) {
                    AnimalObject animalObject = (AnimalObject) gameObject;
                    lifeform.getItems().add(changeScanRadius(animalObject));
                }
                editMenu.getItems().add(lifeform);

            }
        });
        editMenu.getItems().add(changeStopCondition());
    }

    private MenuItem changeStopCondition() {
        MenuItem item = new MenuItem("Stoppbedingung aendern");
        item.setOnAction(event -> {
            StopConditionDialog dialog = new StopConditionDialog();
            dialog.setUpComboBox(boardObject.getGameObjects());
            Optional<String> response = dialog.showAndWait();
            response.ifPresent(species -> {
                messageField.appendText(String.format("Simulation beendet, sobald nur noch %s lebt%n", species));
                game.setSpeciesToWatch(species);
            });
        });
        return item;
    }

    private MenuItem changeScanRadius(AnimalObject animalObject) {
        MenuItem radiusItem = new MenuItem();
        radiusItem.setText("Sichtweite aendern");
        radiusItem.setOnAction(event -> {
            ScanRadiusDialog dialog = new ScanRadiusDialog();
            Optional<Integer> response = dialog.showAndWait();
            response.ifPresent(radius -> boardObject.getGameObjects().stream()
                    .filter(AnimalObject.class::isInstance)
                    .map(AnimalObject.class::cast)
                    .forEach(ao -> {
                        if (ao.getName().equals(animalObject.getName())) {
                            messageField.appendText(String.format("Sichtweite von %s wurde von %s auf %s geaendert%n",
                                                                  ao.getName(), ao.getScanRadius(), radius));
                            ao.setScanRadius(radius);
                        }
                    }));
        });
        return radiusItem;
    }

    private MenuItem changeColor(GameObject gameObject) {
        MenuItem colorItem = new MenuItem();
        colorItem.setText("Farbe aendern");
        colorItem.setOnAction(event -> {
            ColorPickerDialog dialog = new ColorPickerDialog();
            Optional<String> response = dialog.showAndWait();
            response.ifPresent(color -> boardObject.getGameObjects().forEach(go -> {
                if (go.getName().equals(gameObject.getName())) {
                    messageField.appendText(String.format("Farbe von %s von %s auf %s geaendert%n",
                                                          go.getName(), go.getColor(), color));
                    go.setColor(color);
                    boardView.draw();
                }
            }));
        });
        return colorItem;
    }

    private MenuItem changeName(GameObject gameObject) {
        MenuItem nameItem = new MenuItem();
        nameItem.setText("Name aendern");
        nameItem.setOnAction(event -> {

            TextInputDialog dialog = new TextInputDialog(gameObject.getName());
            dialog.setTitle("Neuer Name");
            dialog.getEditor().setTextFormatter(ValidationUtil.getNameFormatter());
            Optional<String> response = dialog.showAndWait();

            response.ifPresent(name -> boardObject.getGameObjects().stream()
                    .filter(go -> go.getName().equals(gameObject.getName()))
                    .forEach(go -> editMenu.getItems().stream()
                            .filter(menuItem -> menuItem.getText().equals(gameObject.getName()))
                            .forEach(menuItem -> {
                                if (name.isBlank()) {
                                    return;
                                }
                                menuItem.setText(name);
                                messageField.appendText(String.format(
                                        "Name von %s wurde auf %s geaendert%n", gameObject.getName(), name));
                                gameObject.setName(name);
                                go.setName(name);

                            })));
        });
        return nameItem;
    }

    private MenuItem changeEnergy(GameObject gameObject) {
        MenuItem energyItem = new MenuItem();
        energyItem.setText("Energie aendern");
        energyItem.setOnAction(event -> {

            TextInputDialog dialog = new TextInputDialog(String.valueOf(gameObject.getEnergy()));
            dialog.setTitle("Neuer Energiewert");
            dialog.getEditor().setTextFormatter(ValidationUtil.getEnergyFormatter());
            Optional<String> response = dialog.showAndWait();

            response.ifPresent(energy -> boardObject.getGameObjects().stream()
                    .filter(go -> go.getName().equals(gameObject.getName()))
                    .forEach(go -> {
                        if (energy.matches("\\d+")) {
                            go.setEnergy(Integer.parseInt(energy));
                            gameProperties.getEnergyProperty(go.getName()).set(Integer.parseInt(energy));
                            messageField.appendText(String.format(
                                    "Energie von %s wurde auf %s gesetzt%n", go.getName(), go.getEnergy()));
                            boardView.draw();
                        }
                    }));
        });
        return energyItem;
    }

}
