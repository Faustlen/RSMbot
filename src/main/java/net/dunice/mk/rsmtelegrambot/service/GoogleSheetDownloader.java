package net.dunice.mk.rsmtelegrambot.service;

import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleSheetDownloader {
    private static final String SPREADSHEET_ID = "1rJR5XI14UvcIP2wiJp52GMErTwL4129iDfzcktivNh0";
    private static final String SHEET_NAME = "Лист1";
    private static final String URL_TEMPLATE = "https://docs.google.com/spreadsheets/d/%s/gviz/tq?tqx=out:csv&sheet=%s";

    public List<String[]> downloadSheet() {
        try {
            String sheetUrl = URL_TEMPLATE.formatted(SPREADSHEET_ID, SHEET_NAME);
            URL url = new URI(sheetUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            InputStream inputStream = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to download file: " + connection.getResponseMessage());
            }

            CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
            List<String[]> allRows = reader.readAll();

            // Пропускаем первые три лишние строки
            return allRows.stream()
                .skip(2)
                .toList();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
