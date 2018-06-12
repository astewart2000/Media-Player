package MediaPlayer;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.io.File;
import java.nio.file.*;
import java.util.*;

/**
 * @Author Aidan Stewart
 * @Year 2018
 * Copyright (c)
 * All rights reserved.
 */
public class Player {
    private MediaPlayer player;
    private File[][] files = new File[2][];
    private Path[] currentDirectories = new Path[2];
    private List<List<Path>> addedDirectories = new ArrayList<>(2);
    private int directoryIndex;
    private boolean isPlaying;

    public Player(Path rootDirectories[]) {
        for (int i = 0; i < rootDirectories.length; i++) {
            rootDirectories[i] = rootDirectories[i];
            currentDirectories[i] = rootDirectories[i];
            addedDirectories.add(new ArrayList<>());
        }
    }

    public boolean doesContain(int selectedIndex, int buttonPressed){
        currentDirectories[buttonPressed] = files[buttonPressed][selectedIndex].toPath();
        return addedDirectories.get(buttonPressed).contains(currentDirectories[buttonPressed]);
    }

    public void openDirectory(int buttonPressed, boolean addingDir) {
        try {
            if (addingDir) {
                if (directoryIndex >= addedDirectories.get(buttonPressed).size())
                    addedDirectories.get(buttonPressed).add(currentDirectories[buttonPressed]);
                else
                    addedDirectories.get(buttonPressed).set(directoryIndex, currentDirectories[buttonPressed]);
            }
            currentDirectories[buttonPressed] = addedDirectories.get(buttonPressed).get(directoryIndex);
            directoryIndex++;
        } catch (IndexOutOfBoundsException ex){
            displayMessage(Alert.AlertType.ERROR,"Directory does not exist!");
        }
    }

    public void closeDirectory(int buttonPressed) {
        String prev = currentDirectories[buttonPressed].toString();
        if (directoryIndex != 0) {
            prev = prev.substring(0, prev.indexOf(currentDirectories[buttonPressed].toFile().getName()));
            currentDirectories[buttonPressed] = new File(prev.substring(0, prev.lastIndexOf(File.separator))).toPath();
            directoryIndex--;
        } else
            displayMessage(Alert.AlertType.ERROR,"Directory does not exist!");
    }

    private void displayMessage(Alert.AlertType type, String errorMsg){
        Alert alert = new Alert(type, errorMsg, ButtonType.CANCEL);
        alert.show();
    }

    public void refreshFiles(int buttonPressed){
        files[buttonPressed] = currentDirectories[buttonPressed].toFile().listFiles();
    }

    public void playNewMedia(int buttonPressed, int selectedIndex) {
        String file = files[buttonPressed][selectedIndex].toURI().toString();
        stopMedia();
        try {
            Media media = new Media(file);
            player = new MediaPlayer(media);
            player.play();
            isPlaying = true;
        } catch (MediaException ex){
            displayMessage(Alert.AlertType.ERROR ,"Not a playable file type!");
        }
    }


    public void playOrPauseMedia() {
        if (isPlaying)
            player.pause();
        else
            player.play();
        isPlaying = !isPlaying;
    }

    public void updateMediaValue(Slider slider) {
        player.pause();
        player.seek(Duration.seconds(slider.getValue()));
        if (!slider.isValueChanging() && isPlaying)
            player.play();
    }

    public void stopMedia() {
        if (player != null)
            player.stop();
        isPlaying = false;
    }

    public void restartMedia() {
        player.seek(player.getStartTime());
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public File[][] getFiles() {
        return files;
    }

    public Path[] getCurrentDirectories() {
        return currentDirectories;
    }

    public List<List<Path>> getAddedDirectories() {
        return addedDirectories;
    }

}