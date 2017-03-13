package com.github.voiteco.youtube.uploader;

import com.github.voiteco.youtube.uploader.auth.Auth;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Uploader {

    private static YouTube youtube;

    private static final String VIDEO_FILE_FORMAT = "video/*";

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Please provide directory with videos");
            System.out.println("Uploader \"path/to/your/directory\"");
            return;
        }

        try {
            File[] files = new File(args[0].trim()).listFiles();
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                String fileName = file.getName();
                String fullFileName = file.getAbsolutePath();

                List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.upload");
                Credential credential = Auth.authorize(scopes, "uploadvideo");
                youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                        .setApplicationName("youtube-uploader").build();

                System.out.println("Uploading: " + fileName);

                Video videoObjectDefiningMetadata = new Video();

                VideoStatus status = new VideoStatus();
                status.setPrivacyStatus("public");
                videoObjectDefiningMetadata.setStatus(status);

                VideoSnippet snippet = new VideoSnippet();

                String name = FilenameUtils.getBaseName(fileName);
                snippet.setTitle(name);

                if (name.contains("-")) {
                    List<String> tags = new ArrayList<>();
                    String[] nameParts = name.split("-");
                    for (String entry : nameParts) {
                        tags.add(entry.trim());
                    }
                    snippet.setTags(tags);
                }

                videoObjectDefiningMetadata.setSnippet(snippet);

                InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT, Uploader.class.getResourceAsStream(fullFileName));
                YouTube.Videos.Insert videoInsert = youtube.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);
                MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
                uploader.setDirectUploadEnabled(false);
                uploader.setProgressListener(new MediaHttpUploaderProgressListener() {
                    public void progressChanged(MediaHttpUploader uploader) throws IOException {
                        switch (uploader.getUploadState()) {
                            case INITIATION_STARTED:
                                System.out.println("Initiation Started");
                                break;
                            case INITIATION_COMPLETE:
                                System.out.println("Initiation Completed");
                                break;
                            case MEDIA_IN_PROGRESS:
                                System.out.println("Upload in progress");
                                System.out.println("Upload percentage: " + uploader.getProgress());
                                break;
                            case MEDIA_COMPLETE:
                                System.out.println("Upload Completed!");
                                break;
                            case NOT_STARTED:
                                System.out.println("Upload Not Started!");
                                break;
                        }
                    }
                });

                Video returnedVideo = videoInsert.execute();

                System.out.println("  - Id: " + returnedVideo.getId());
                System.out.println("  - Title: " + returnedVideo.getSnippet().getTitle());
                System.out.println("  - Tags: " + returnedVideo.getSnippet().getTags());
            }

        } catch (GoogleJsonResponseException e) {
            System.err.println(String.format("GoogleJsonResponseException: %s:%s", e.getDetails().getCode(), e.getDetails().getMessage()));
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(String.format("IOException: %s", e.getMessage()));
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println(String.format("Throwable: %s", t.getMessage()));
            t.printStackTrace();
        }

    }

}
