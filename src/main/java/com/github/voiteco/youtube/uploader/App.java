package com.github.voiteco.youtube.uploader;

import com.github.voiteco.youtube.uploader.model.FileInfo;
import com.github.voiteco.youtube.uploader.uploader.YoutubeUploader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class App {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide directory with videos");
            System.out.println("YoutubeUploader \"path/to/your/directory\"");
            return;
        }

        File directory = new File(args[0].trim());
        if (!directory.isDirectory()) {
            System.out.println("No such file or directory");
            return;
        }

        List<FileInfo> fileInfoList = new ArrayList<>();

        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                FileInfo fileInfo = new FileInfo(file);
                fileInfoList.add(fileInfo);
            }
        }

        YoutubeUploader uploader = new YoutubeUploader();
        uploader.init();
        for (FileInfo fileInfo : fileInfoList) {
            uploader.upload(fileInfo);
        }
    }

}
