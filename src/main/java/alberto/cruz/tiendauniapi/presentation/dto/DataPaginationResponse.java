package alberto.cruz.tiendauniapi.presentation.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"hasNext", "cursor", "data"})
public record DataPaginationResponse<T>(
        List<T> data,
        String cursor,
        boolean hasNext
) {
}
