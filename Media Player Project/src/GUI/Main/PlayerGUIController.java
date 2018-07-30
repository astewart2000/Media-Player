package GUI.Main;

import MainClass.Main;
import MediaPlayer.Player;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author Aidan Stewart
 * @Year 2018
 * Copyright (c)
 * All rights reserved.
 */
public class PlayerGUIController implements Initializable {
    @FXML
    private Button musicBrowse, videoBrowse;
    @FXML
    private ImageView play, skip, back, rewind, fullscreen, loop, randomize, openDir, closeDir;
    @FXML
    private CheckBox loopingCBox, randomizeCBox;
    @FXML
    private Slider slider;
    @FXML
    private TextField musicRoot, videoRoot, directoryDisplay;
    @FXML
    private Label songDuration, songEnd;
    @FXML
    private VBox listViewContainer, settings;
    @FXML
    private ListView<String> listView;
    @FXML
    private MediaView mediaView;
    @FXML
    private StackPane mediaViewContainer;
    private final String[] saveFileNames = {"Music", "Videos"};
    private final TextField[] rootTextFields = new TextField[2];
    private List<List<Integer>> shuffledIndices = new ArrayList<>(2);
    private Player player;
    private int buttonPressed = 0;
    private int shuffledIndicesIndex;
    private boolean isChooserOpen;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (int i = 0; i < 2; i++)
            shuffledIndices.add(new ArrayList<>());
        setTextFieldToArray();
        setDefaults();
        mediaView.fitHeightProperty().bind(mediaViewContainer.heightProperty());
        mediaView.fitWidthProperty().bind(mediaViewContainer.widthProperty());
    }

    private void setTextFieldToArray() {
        rootTextFields[0] = musicRoot;
        rootTextFields[1] = videoRoot;
    }

    private void setDefaults() {
        String separator = File.separator;
        for (int i = 0; i < saveFileNames.length; i++) {
            File file = new File(saveFileNames[i] + ".txt");
            try {
                rootTextFields[i].setText(Files.readAllLines(file.toPath()).get(0));
            } catch (IOException e) {
                e.printStackTrace();
                String home = System.getProperty("user.home");
                File dir = new File(home + separator + saveFileNames[i]);
                saveToFile(file, dir.getAbsolutePath());
                rootTextFields[i].setText(dir.getAbsolutePath());
            }
        }
        setPlayer();
        refreshDisplay();
    }

    private void setPlayer() {
        Path[] directories = new Path[2];
        for (int i = 0; i < directories.length; i++) {
            directories[i] = new File(rootTextFields[i].getText()).toPath();
            listenForDirectoryChanges(new File(rootTextFields[i].getText()).toPath());
        }
        player = new Player(directories);
    }

    @FXML
    private void listViewMouseEvent(MouseEvent e) {
        if (e.getClickCount() == 2) {
            playNewMedia();
            openNewDirectory();
        }
    }

    @FXML
    private void anchorPaneKeyEvent(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            playNewMedia();
            openNewDirectory();
        } else if (e.getCode() == KeyCode.ESCAPE && Main.getMainStage().isFullScreen())
            toggleFullscreen();
        else if (e.getCode() == KeyCode.SPACE)
            playOrPauseMedia();
    }

    @FXML
    private void mediaButtonEvent(ActionEvent e) {
        int prevButton = buttonPressed;
        String buttonText = ((Button) e.getSource()).getText();
        switch (buttonText) {
            case "Music":
                buttonPressed = 0;
                break;
            case "Video":
                buttonPressed = 1;
                break;
            case "Settings":
                buttonPressed = 2;
                break;
        }
        if (prevButton != buttonPressed) {
            refreshDisplay();
        }
    }

    @FXML
    public void imageViewClickEvent(MouseEvent e) {
        String buttonText = ((ImageView) e.getSource()).getId();
        switch (buttonText) {
            case "play":
                playOrPauseMedia();
                break;
            case "skip":
                skipOrBack(true);
                break;
            case "back":
                skipOrBack(false);
                break;
            case "openDir":
                player.openDirectory(buttonPressed, false);
                refreshDisplay();
                break;
            case "closeDir":
                player.closeDirectory(buttonPressed);
                refreshDisplay();
                break;
            case "rewind":
                player.restartMedia();
                break;
            case "fullscreen":
                toggleFullscreen();
                break;
            case "randomize":
                toggleRandomizing();
                break;
            case "loop":
                toggleLooping();
                break;
        }
    }

    @FXML
    private void browseButtonEvent(ActionEvent e) {
        String buttonText = ((Button) e.getSource()).getId();
        int browseButtonPressed = -1;
        switch (buttonText) {
            case "musicBrowse":
                browseButtonPressed = 0;
                break;
            case "videoBrowse":
                browseButtonPressed = 1;
                break;
        }
        openDirectoryChooser(browseButtonPressed);
    }

    @FXML
    private void sliderChangeEvent() {
        player.updateMediaValue(slider);
    }

    private void listenForDirectoryChanges(Path path) {
        new Thread(() -> {
            try (WatchService service = FileSystems.getDefault().newWatchService()) {
                Map<WatchKey, Path> keyPathMap = new HashMap<>();
                keyPathMap.put(path.register(service,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_CREATE)
                        ,path);
                WatchKey watchKey;
                do {
                    watchKey = service.take();
                    for (WatchEvent<?> ignored : watchKey.pollEvents()) {
                        player.refreshFiles(buttonPressed);
                        Platform.runLater(this::refreshDisplay);
                    }
                } while (watchKey.reset());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void refreshDisplay() {
        if (listView != null)
            listView.getItems().clear();
        if (buttonPressed != 2) {
            player.refreshFiles(buttonPressed);
            File[] files = player.getFiles()[buttonPressed];
            for (File file : files)
                listView.getItems().add((file.isFile() ? "" : "Folder: ") + file.getName());
            listViewContainer.setVisible(true);
            directoryDisplay.setText(String.valueOf(player.getCurrentDirectories()[buttonPressed]));
            refreshShuffledIndices();
        } else
            listViewContainer.setVisible(false);
    }

    private void refreshShuffledIndices() {
        int length = player.getFiles()[buttonPressed].length;
        int[] randomIndex = ThreadLocalRandom.current().ints(0, length).distinct().limit(length).toArray();
        shuffledIndices.get(buttonPressed).clear();
        for (int i = 0; i < length; i++)
            shuffledIndices.get(buttonPressed).add(randomIndex[i]);
        shuffledIndicesIndex = 0;
    }

    private void saveToFile(File file, String text) {
        try {
            Files.write(file.toPath(), text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playNewMedia() {
        if (isSelectedFile()) {
            player.playNewMedia(buttonPressed, selectedListViewIndex());
            mediaView.setMediaPlayer(player.getPlayer());
            openMediaView();
            songDuration();
        }
    }

    private void playOrPauseMedia() {
        player.playOrPauseMedia();
        songDuration();
        openMediaView();
    }

    private void openNewDirectory() {
        if (isSelectedDirectory()) {
            if (!player.doesContain(selectedListViewIndex(), buttonPressed))
                listenForDirectoryChanges(player.getCurrentDirectories()[buttonPressed]);
            player.openDirectory(buttonPressed, true);
            System.out.println(Arrays.toString(player.getCurrentDirectories()));
            refreshDisplay();
        }
    }

    private void openMediaView() {
        boolean visibility = buttonPressed == 1 && player.isPlaying();
        mediaViewContainer.setVisible(visibility);
    }

    private void songDuration() {
        MediaPlayer mediaPlayer = player.getPlayer();
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) ->
        {
            Duration duration = mediaPlayer.getTotalDuration();
            slider.setMax(duration.toSeconds());
            slider.setValue(newTime.toSeconds());
            songEnd.setText(createDurationString((int) duration.toSeconds()));
            songDuration.setText(createDurationString((int) newTime.toSeconds()));
            mediaPlayer.setOnEndOfMedia(() -> {
                if (loopingCBox.isSelected())
                    player.restartMedia();
                else
                    skipOrBack(true);
            });
        });
    }

    private String createDurationString(int seconds) {
        int hours = seconds / (60 * 60) % 24;
        int mins = seconds / 60 % 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    private void skipOrBack(boolean skip){
        if (randomizeCBox.isSelected())
            skipOrBackShuffle(skip);
        else
            setSelectedListViewIndex(selectedListViewIndex() + (skip ? 1 : -1));
        if (player.isPlaying())
            playNewMedia();
    }

    private void skipOrBackShuffle(boolean skip) {
        shuffledIndicesIndex += skip ? +1 : -1;
        try {
            int randomIndex = shuffledIndices.get(buttonPressed).get(shuffledIndicesIndex);
            setSelectedListViewIndex(randomIndex);
            if (player.isPlaying())
                playNewMedia();
        } catch (IndexOutOfBoundsException ex) {
            shuffledIndicesIndex += !skip ? +1 : -1;
        }
    }

    private void toggleFullscreen() {
        boolean isFull = Main.getMainStage().isFullScreen();
        Main.getMainStage().setFullScreen(!isFull);
    }

    private void toggleLooping() {
        boolean prev = loopingCBox.isSelected();
        clearBoxes();
        loopingCBox.setSelected(!prev);
    }

    private void toggleRandomizing() {
        boolean prev = randomizeCBox.isSelected();
        clearBoxes();
        randomizeCBox.setSelected(!prev);
    }

    private void clearBoxes() {
        loopingCBox.setSelected(false);
        randomizeCBox.setSelected(false);
    }

    private void openDirectoryChooser(int browseButtonPressed) {
        if (!isChooserOpen) {
            isChooserOpen = true;
            DirectoryChooser chooser = new DirectoryChooser();
            File dir = chooser.showDialog(new Stage());
            if (dir != null) {
                player.stopMedia();
                File file = new File(saveFileNames[browseButtonPressed] + ".txt");
                saveToFile(file, dir.getAbsolutePath());
                setDefaults();
            }
            isChooserOpen = false;
        }
    }

    private int selectedListViewIndex() {
        return listView.getSelectionModel().getSelectedIndex();
    }

    private void setSelectedListViewIndex(int index) {
        listView.getSelectionModel().select(index);
    }

    private boolean isSelectedFile() {
        return player.getFiles()[buttonPressed][selectedListViewIndex()].isFile();
    }

    private boolean isSelectedDirectory() {
        return player.getFiles()[buttonPressed][selectedListViewIndex()].isDirectory();
    }
}

