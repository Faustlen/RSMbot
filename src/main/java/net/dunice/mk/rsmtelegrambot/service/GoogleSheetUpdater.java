package net.dunice.mk.rsmtelegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.entity.UsersList;
import net.dunice.mk.rsmtelegrambot.repository.UserListRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSheetUpdater {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final GoogleSheetDownloader googleSheetDownloader;
    private final UserListRepository userListRepository;

    public void updateSheet() {
        try {
            List<String[]> rows = googleSheetDownloader.downloadSheet();

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                try {
                    UsersList user = new UsersList();

                    user.setUserCard(parseInteger(row[5], "userCard", i));
                    // Сразу с ФИО в одну ячейку
                    user.setFullName(formatFullName(row[1], row[2], row[3], i));
                    user.setPhoneNumber(row[4] != null ? row[4].trim() : null);
                    user.setBirthDate(parseDate(row[6], "birthDate", i));

                    userListRepository.save(user);
                } catch (Exception e) {
                    log.error("Не удалось сохранить данные из Google таблицы", e);
                    log.error("Ошибка обработки строки: {}, данные: {}", i, String.join(", ", row));
                }
            }
        } catch (Exception e){
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Integer parseInteger(String value, String fieldName, int rowIndex) {
        try {
            return value != null && !value.trim().isEmpty() ? Integer.parseInt(value.trim()) : null;
        } catch (NumberFormatException e) {
            log.warn("Некорректное значение {} в строке {}: {}", fieldName, rowIndex, value);
            return null;
        }
    }

    private String parseDate(String value, String fieldName, int rowIndex) {
        try {
            return value != null && !value.trim().isEmpty()
                ? value.trim()
                : null;
        } catch (DateTimeParseException e) {
            log.warn("Некорректная дата {} в строке {}: {}", fieldName, rowIndex, value);
            return null;
        }
    }

    private String formatFullName(String lastName, String firstName, String middleName, int rowIndex) {
        if (lastName == null || firstName == null || middleName == null) {
            log.warn("Некорректное ФИО в строке {}: {}, {}, {}", rowIndex, lastName, firstName, middleName);
            return null;
        }
        return String.format("%s %s %s", lastName.trim(), firstName.trim(), middleName.trim());
    }
}

