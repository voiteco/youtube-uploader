package com.github.voiteco.youtube.uploader.uploader;

import com.github.voiteco.youtube.uploader.model.FileInfo;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

public class YoutubeUploader {

    private YouTube youtube;

    private static final String REFRESH_TOKEN_PROPERTY = "youtube.refresh.token";
    private static final String VIDEO_FILE_FORMAT = "video/*";

    public void init() {
        try {
            Properties values = new Properties();
            values.load(YoutubeUploader.class.getResourceAsStream("/youtube.properties"));
            String refreshToken = values.getProperty(REFRESH_TOKEN_PROPERTY);

            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            InputStreamReader isr = new InputStreamReader(YoutubeUploader.class.getResourceAsStream("/client_secrets.json"));
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, isr);
            Credential credential = new GoogleCredential.Builder().setTransport(httpTransport)
                    .setJsonFactory(jsonFactory).setClientSecrets(clientSecrets)
                    .build().setRefreshToken(refreshToken);
            youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                    .setApplicationName("youtube-uploader").build();

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

    public void upload(FileInfo fileInfo) {
        try {
            System.out.println(String.format("Uploading: %s", fileInfo.getName()));

            Video videoObjectDefiningMetadata = new Video();

            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("public");
            videoObjectDefiningMetadata.setStatus(status);

            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(fileInfo.getName());
            snippet.setTags(fileInfo.getTags());

            videoObjectDefiningMetadata.setSnippet(snippet);

            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT, new FileInputStream(fileInfo.getFile()));
            YouTube.Videos.Insert videoInsert = youtube.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);
            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(false);
            uploader.setProgressListener(new MediaHttpUploaderProgressListener() {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            System.out.println(String.format("File: %s. Initiation Started", fileInfo.getName()));
                            break;
                        case INITIATION_COMPLETE:
                            System.out.println(String.format("File: %s. Initiation Completed", fileInfo.getName()));
                            break;
                        case MEDIA_IN_PROGRESS:
                            System.out.println(String.format("File: %s. Upload in progress", fileInfo.getName()));
                            break;
                        case MEDIA_COMPLETE:
                            System.out.println(String.format("File: %s. Upload Completed!", fileInfo.getName()));
                            break;
                        case NOT_STARTED:
                            System.out.println(String.format("File: %s. Upload Not Started!", fileInfo.getName()));
                            break;
                    }
                }
            });

            Video returnedVideo = videoInsert.execute();

            System.out.println(String.format("File: %s. VideoId: %s", fileInfo.getName(), returnedVideo.getId()));

            if (fileInfo.getPlaylist() != null) {
                Playlist playlist = getOrCreatePlaylist(fileInfo.getPlaylist());

                ResourceId resourceId = new ResourceId();
                resourceId.setKind("youtube#video");
                resourceId.setVideoId(returnedVideo.getId());

                PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
                playlistItemSnippet.setPlaylistId(playlist.getId());
                playlistItemSnippet.setResourceId(resourceId);

                PlaylistItem playlistItem = new PlaylistItem();
                playlistItem.setSnippet(playlistItemSnippet);

                YouTube.PlaylistItems.Insert playlistItemsInsertCommand = youtube.playlistItems().insert("snippet", playlistItem);
                PlaylistItem returnedPlaylistItem = playlistItemsInsertCommand.execute();

                System.out.println(String.format("File: %s. Added to playlist: %s", fileInfo.getName(), playlist.getSnippet().getTitle()));
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

    private Playlist getOrCreatePlaylist(String playlistName) throws IOException {
        PlaylistListResponse playlistListResponse = youtube.playlists().list("snippet").setMine(true).execute();
        List<Playlist> playlistList = playlistListResponse.getItems();
        Playlist playlist = null;
        if (!playlistList.isEmpty()) {
            for (Playlist entry : playlistList) {
                if (entry.getSnippet().getTitle().equals(playlistName)) {
                    playlist = entry;
                    break;
                }
            }
        }
        if (playlist == null) {
            PlaylistSnippet playlistSnippet = new PlaylistSnippet();
            playlistSnippet.setTitle(playlistName);
            PlaylistStatus playlistStatus = new PlaylistStatus();
            playlistStatus.setPrivacyStatus("public");

            playlist = new Playlist();
            playlist.setSnippet(playlistSnippet);
            playlist.setStatus(playlistStatus);

            YouTube.Playlists.Insert playlistInsertCommand = youtube.playlists().insert("snippet,status", playlist);
            playlist = playlistInsertCommand.execute();
        }
        return playlist;
    }

}
