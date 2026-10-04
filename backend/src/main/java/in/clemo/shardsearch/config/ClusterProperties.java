package in.clemo.shardsearch.config;

import in.clemo.shardsearch.distributed.node.ShardRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;

@ConfigurationProperties(prefix = "shardsearch.cluster")
@Validated
public record ClusterProperties(
        @NotEmpty @Valid List<Node> nodes
) {

    public record Node(
            @NotBlank String id,
            @NotNull URI endpoint,
            @NotEmpty @Valid List<Shard> shards
    ) {
    }

    public record Shard(
            @NotNull Integer id,
            @NotNull ShardRole role
    ) {
    }
}
