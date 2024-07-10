package com.ciji.serenity.config;

import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class GoogleAuthorizeUtil {
    public static GoogleCredentials authorize() throws IOException {
        try(InputStream inputSteam = GoogleAuthorizeUtil.class.getResourceAsStream("/credentials.json")) {
            return GoogleCredentials.fromStream(inputSteam).createScoped(List.of(SheetsScopes.SPREADSHEETS));
        }
    }
}
