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
    private ImageView play, skip, back, rewind, fullscreen, loop, shuffle, openDir, closeDir;
    @FXML
    private CheckBox loopingCBox, shuffleCBox;
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
    private int shuffledIndicesIndex, menuButtonPressed = 0;
    private boolean isChooserOpen;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (int i = 0; i < 2; i++)
            shuffledIndices.add(new ArrayList<>());
        setTextFieldToArray();
        tryToSetRootFields();
        mediaView.fitHeightProperty().bind(mediaViewContainer.heightProperty());
        mediaView.fitWidthProperty().bind(mediaViewContainer.widthProperty());
    }

    private void setTextFieldToArray() {
        rootTextFields[0] = musicRoot;
        rootTextFields[1] = videoRoot;
    }

    private void tryToSetRootFields(){
        for (int i = 0; i < 2; i++) {
            File file = new File(saveFileNames[i] + ".txt");
            try {
                rootTextFields[i].setText(Files.readAllLines(file.toPath()).get(0));
            } catch (IOException | NullPointerException ex) {
                setRootFieldsToDefault(i);
            }
            setPlayer();
        }
    }

    private void setRootFieldsToDefault(int saveFile) {
        String home = System.getProperty("user.home");
        File file = new File(saveFileNames[saveFile] + ".txt");
        File defaultDir = new File(home + File.separator + saveFileNames[saveFile]);
        saveToFile(file, defaultDir.getAbsolutePath());
        rootTextFields[saveFile].setText(defaultDir.getAbsolutePath());
    }

    private void setPlayer() {
        Path[] directories = new Path[2];
        for (int i = 0; i < directories.length; i++) {
            directories[i] = new File(rootTextFields[i].getText()).toPath();
            tryToListenForDisplayedDirChanges(new File(rootTextFields[i].getText()).toPath());
        }
        player = new Player(directories);
        refreshDisplay();
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
        int prevButton = menuButtonPressed;
        String buttonText = ((Button) e.getSource()).getText();
        switch (buttonText) {
            case "Music":
                menuButtonPressed = 0;
                break;
            case "Video":
                menuButtonPressed = 1;
                break;
            case "Settings":
                menuButtonPressed = 2;
                break;
                default:
                    break;
        }
        if (prevButton != menuButtonPressed)
            refreshDisplay();
    }

    @FXML
    public void imageViewClickEvent(MouseEvent e) {
        String buttonText = ((ImageView) e.getSource()).getId();
        switch (buttonText) {
            case "play":
                playOrPauseMedia();
                break;
            case "skip":
                changeMedia(true);
                break;
            case "back":
                changeMedia(false);
                break;
            case "openDir":
                player.tryToOpenDirectory(menuButtonPressed, false);
                refreshDisplay();
                break;
            case "closeDir":
                player.closeDirectory(0);
                refreshDisplay();
                break;
            case "rewind":
                player.getPlayer().seek(player.getPlayer().getStartTime());
                break;
            case "fullscreen":
                toggleFullscreen();
                break;
            case "shuffle":
                toggleShuffle();
                break;
            case "loop":
                toggleLooping();
                break;
                default:
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
                default:
                    break;
        }
        openDirectoryChooser(browseButtonPressed);
    }

    @FXML
    private void sliderChangeEvent() {
        player.updateMediaValue(slider);
    }

    private void tryToListenForDisplayedDirChanges(Path path) {
        new Thread(() -> {
            try {
                listenForDisplayedDirChanges(path);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void listenForDisplayedDirChanges(Path path) throws IOException, InterruptedException {
        WatchService service = FileSystems.getDefault().newWatchService();
        Map<WatchKey, Path> keyPathMap = new HashMap<>();
        keyPathMap.put(path.register(service,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_CREATE)
                ,path);
        WatchKey watchKey;
        do {
            watchKey = service.take();
            for (WatchEvent<?> ignored : watchKey.pollEvents()) {
                player.refreshFiles(menuButtonPressed);
                System.out.println("Test Completed");
                Platform.runLater(this::refreshDisplay);
            }
        } while (watchKey.reset());
    }

    private void refreshDisplay() {
        if (listView != null)
            listView.getItems().clear();
        if (menuButtonPressed != 2) {
            player.refreshFiles(menuButtonPressed);
            File[] files = player.getFiles()[menuButtonPressed];
            for (File file : files)
                listView.getItems().add((file.isFile() ? "" : "Folder: ") + file.getName());
            listViewContainer.setVisible(true);
            directoryDisplay.setText(String.valueOf(player.getCurrentDirectories()[menuButtonPressed]));
            refreshShuffledIndices();
        } else
            listViewContainer.setVisible(false);
    }

    private void refreshShuffledIndices() {
        int length = player.getFiles()[menuButtonPressed].length;
        int[] randomIndex = ThreadLocalRandom.current().ints(0, length).distinct().limit(length).toArray();
        shuffledIndices.get(menuButtonPressed).clear();
        for (int i = 0; i < length; i++)
            shuffledIndices.get(menuButtonPressed).add(randomIndex[i]);
        shuffledIndicesIndex = 0;
    }

    private void playNewMedia() {
        if (isSelectedFile()) {
            player.tryToPlayNewMedia(menuButtonPressed, selectedListViewIndex());
            mediaView.setMediaPlayer(player.getPlayer());
            openMediaView();
            determineMediaDuration();
        }
    }

    private void playOrPauseMedia() {
        player.mediaPlayState();
        determineMediaDuration();
        openMediaView();
    }

    private void openNewDirectory() {
        if (isSelectedDirectory()) {
            if (!player.doesContain(selectedListViewIndex(), menuButtonPressed))
                tryToListenForDisplayedDirChanges(player.getCurrentDirectories()[menuButtonPressed]);
            player.tryToOpenDirectory(menuButtonPressed, true);
            refreshDisplay();
        }
    }

    private void openMediaView() {
        boolean visibility = menuButtonPressed == 1 && player.isPlaying();
        mediaViewContainer.setVisible(visibility);
    }

    private void determineMediaDuration() {
        MediaPlayer mediaPlayer = player.getPlayer();
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            setTimeStamps(newTime);
            mediaPlayer.setOnEndOfMedia(() -> {
                if (loopingCBox.isSelected())
                    player.getPlayer().seek(player.getPlayer().getStartTime());
                else
                    changeMedia(true);
            });
        });
    }

    private void setTimeStamps(Duration currentTime){
        MediaPlayer mediaPlayer = player.getPlayer();
        Duration duration = mediaPlayer.getTotalDuration();
        slider.setMax(duration.toSeconds());
        slider.setValue(currentTime.toSeconds());
        songEnd.setText(createDurationString((int) duration.toSeconds()));
        songDuration.setText(createDurationString((int) currentTime.toSeconds()));
    }

    private String createDurationString(int seconds) {
        int hours = seconds / (60 * 60) % 24;
        int mins = seconds / 60 % 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    private void changeMedia(boolean skip){
        if (shuffleCBox.isSelected())
           tryToChangeShuffledMedia(skip);
        else
            setSelectedListViewIndex(selectedListViewIndex() + (skip ? 1 : -1));
        if (player.isPlaying())
            playNewMedia();
    }

    private void tryToChangeShuffledMedia(boolean skip){
        try {
            changeShuffledMedia(skip);
        } catch (IndexOutOfBoundsException e){
            shuffledIndicesIndex += -1;
        }
    }

    private void changeShuffledMedia(boolean skip) throws IndexOutOfBoundsException {
        shuffledIndicesIndex += skip ? +1 : -1;
        int indexSelectedInShuffleList = shuffledIndices.get(menuButtonPressed).get(shuffledIndicesIndex);
        setSelectedListViewIndex(indexSelectedInShuffleList);

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

    private void toggleShuffle() {
        boolean prev = shuffleCBox.isSelected();
        clearBoxes();
        shuffleCBox.setSelected(!prev);
    }

    private void clearBoxes() {
        loopingCBox.setSelected(false);
        shuffleCBox.setSelected(false);
    }

    private void openDirectoryChooser(int browseButtonPressed) {
        if (!isChooserOpen) {
            isChooserOpen = true;
            DirectoryChooser chooser = new DirectoryChooser();
            File dir = chooser.showDialog(new Stage());
            if (dir != null)
               setNewDirectoryInSaveFile(dir, browseButtonPressed);
            isChooserOpen = false;
        }
    }

    private void setNewDirectoryInSaveFile(File dir, int browseButtonPressed) {
        if (player.getPlayer() != null)
            player.getPlayer().stop();
        File file = new File(saveFileNames[browseButtonPressed] + ".txt");
        saveToFile(file, dir.getAbsolutePath());
        tryToSetRootFields();
    }

    private void saveToFile(File file, String text) {
        try {
            Files.write(file.toPath(), text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private int selectedListViewIndex() {
        return listView.getSelectionModel().getSelectedIndex();
    }

    private void setSelectedListViewIndex(int index) {
        listView.getSelectionModel().select(index);
    }

    private boolean isSelectedFile() {
        return player.getFiles()[menuButtonPressed][selectedListViewIndex()].isFile();
    }

    private boolean isSelectedDirectory() {
        return player.getFiles()[menuButtonPressed][selectedListViewIndex()].isDirectory();
    }
}

