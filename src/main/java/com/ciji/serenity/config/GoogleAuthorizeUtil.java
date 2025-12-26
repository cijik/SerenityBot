package com.ciji.serenity.config;

import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class GoogleAuthorizeUtil {
    public static GoogleCredentials authorize() throws IOException {
        if (Files.exists(Paths.get("./credentials.json"))) {
            try(InputStream inputSteam = new FileInputStream("./credentials.json")) {
                return GoogleCredentials.fromStream(inputSteam).createScoped(List.of(SheetsScopes.SPREADSHEETS));
            }
        } else {
            try(InputStream inputSteam = GoogleAuthorizeUtil.class.getResourceAsStream("/credentials.json")) {
                assert inputSteam != null;
                return GoogleCredentials.fromStream(inputSteam).createScoped(List.of(SheetsScopes.SPREADSHEETS));
            }
        }

    }
}
