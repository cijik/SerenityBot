package com.ciji.serenity.service;

import com.ciji.serenity.config.GoogleAuthorizeUtil;
import com.google.api.client.googleapis.apache.v2.GoogleApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SheetsServiceUtil {
    private static final String APPLICATION_NAME = "Serenity Bot";

    public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleAuthorizeUtil.authorize();
        return new Sheets.Builder(
                GoogleApacheHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
