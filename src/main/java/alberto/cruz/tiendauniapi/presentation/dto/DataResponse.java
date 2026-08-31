package alberto.cruz.tiendauniapi.presentation.dto;

import java.util.List;

public record DataResponse<T>(
        List<T> data
) {
}
