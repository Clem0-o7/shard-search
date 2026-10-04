package in.clemo.shardsearch.distributed.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SearchRequestDto(
        @NotNull
        @NotBlank
        String query,

        @Min(1)
        @Max(100)
        int limit
) {
}
