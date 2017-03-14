package com.github.voiteco.youtube.uploader.model;

import org.apache.commons.io.FilenameUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileInfo {

    private String fileName;
    private String name;
    private String playlist;
    private List<String> tags;
    private File file;

    public FileInfo() {
    }

    public FileInfo(File file) {
        this.file = file;
        this.fileName = file.getName();
        this.name = FilenameUtils.getBaseName(this.fileName);

        this.tags = new ArrayList<>();
        if (this.name.contains("-")) {
            String[] parts = this.name.split("-");
            this.playlist = parts[0];
            for (String entry : parts) {
                tags.add(entry.trim());
            }
        } else {
            tags.add(this.name);
            this.playlist = this.name;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaylist() {
        return playlist;
    }

    public void setPlaylist(String playlist) {
        this.playlist = playlist;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

}
