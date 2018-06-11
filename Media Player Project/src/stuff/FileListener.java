package stuff;

import GUI.Main.PlayerGUIController;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Aidan Stewart
 * @Year 2018
 * Copyright (c)
 * All rights reserved.
 */
public class FileListener {

    private Path path;
    private boolean running;

    public FileListener(Path path){
        this.path = path;
        running = true;
        //listenForChanges();
    }




    public boolean isRunning() {
        return running;
    }


    public void setRunning(boolean running) {
        this.running = running;
    }

}


