package MediaPlayer;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Aidan Stewart
 * @Year 2018
 * Copyright (c)
 * All rights reserved.
 */
public class Player {
    private MediaPlayer player;
    private File[][] files = new File[2][];
    private File[] currentDirectories = new File[2];
    private List<List<File>> addedDirectories = new ArrayList<>(2);
    private int directoryIndex;
    private boolean isPlaying;

    public Player(File[] rootDirectories) {
        for (int i = 0; i < rootDirectories.length; i++) {
            currentDirectories[i] = rootDirectories[i];
            files[i] = currentDirectories[i].listFiles();
            addedDirectories.add(new ArrayList<>());
            addedDirectories.get(i).clear();
        }
        directoryIndex = 0;
    }

    public void openDirectory(int selectedIndex, int buttonPressed) {
        try {
            if (files[buttonPressed][selectedIndex].isDirectory()) {
                File directorySelected = files[buttonPressed][selectedIndex];
                if (directoryIndex >= addedDirectories.get(buttonPressed).size())
                    addedDirectories.get(buttonPressed).add(directorySelected);
                else
                    addedDirectories.get(buttonPressed).set(directoryIndex, directorySelected);
            }
            files[buttonPressed] = addedDirectories.get(buttonPressed).get(directoryIndex).listFiles();
            currentDirectories[buttonPressed] = addedDirectories.get(buttonPressed).get(directoryIndex);
            directoryIndex++;
        } catch (IndexOutOfBoundsException ex){
            displayMessage(Alert.AlertType.ERROR,"Directory does not exist!");
        }
    }

    public void closeDirectory(int buttonPressed) {
        String prev = currentDirectories[buttonPressed].toString();
        if (directoryIndex != 0) {
            prev = prev.substring(0, prev.indexOf(currentDirectories[buttonPressed].getName()));
            currentDirectories[buttonPressed] = new File(prev.substring(0, prev.lastIndexOf(File.separator)));
            files[buttonPressed] = currentDirectories[buttonPressed].listFiles();
            directoryIndex--;
        } else
            displayMessage(Alert.AlertType.ERROR,"Directory does not exist!");
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

    public void displayMessage(Alert.AlertType type, String errorMsg){
        Alert alert = new Alert(type, errorMsg, ButtonType.CANCEL);
        alert.show();
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

    public File[] getCurrentDirectories() {
        return currentDirectories;
    }
}