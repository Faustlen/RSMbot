package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Stock;

@Getter
@Setter
public class ShowStocksState {
    private ShowStocksStep step = ShowStocksStep.SHOW_STOCKS_LIST;
    private Stock targetStock;
    private String editingFieldName; // поле, которое редактируем

    public enum ShowStocksStep {
        SHOW_STOCKS_LIST,
        SHOW_STOCK_DETAILS,
        HANDLE_USER_ACTION,
        SELECT_STOCK_FIELD,
        EDIT_STOCK_FIELD,
        CONFIRM_STOCK_EDIT,
        CONFIRM_STOCK_DELETE
    }
}
