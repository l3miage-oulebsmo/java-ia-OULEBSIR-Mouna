package com.moodtune.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class JamendoConfig {
    private JamendoConfig() {
    }

    public static Path defaultPath() {
        return Path.of("config", "jamendo.properties");
    }

    public static String loadClientId(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String clientId = properties.getProperty("client_id");
        return clientId == null || clientId.isBlank() ? null : clientId.trim();
    }
}
