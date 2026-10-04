package in.clemo.shardsearch.index.storage;

import java.io.IOException;

public class ShardArtifactException extends IOException {
    
    public ShardArtifactException(String message) {
        super(message);
    }
    
    public ShardArtifactException(String message, Throwable cause) {
        super(message, cause);
    }
}
