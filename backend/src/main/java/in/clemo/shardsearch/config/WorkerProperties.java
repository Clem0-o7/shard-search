package in.clemo.shardsearch.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "shardsearch.worker")
@Validated
public record WorkerProperties(
        @NotBlank String nodeId,
        @NotBlank String indexDirectory
) {
}
