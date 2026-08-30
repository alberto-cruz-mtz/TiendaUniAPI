package alberto.cruz.tiendauniapi.presentation.dto;

import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PostRequestParams {

    private final String cursor;
    private final Pageable pageable;
    private final String search;

    @Getter
    private final boolean isOutOfStock;

    public PostRequestParams(String cursor, Pageable pageable, String search, Boolean isOutOfStock) {
        boolean isOutOfStockValue = isOutOfStock != null && isOutOfStock;

        this.cursor = cursor;
        this.pageable = pageable;
        this.search = search;
        this.isOutOfStock = isOutOfStockValue;
    }

    public String cursor() {
        return this.cursor;
    }

    public Pageable pageable() {
        return this.pageable;
    }

    public String search() {
        return this.search;
    }
}
