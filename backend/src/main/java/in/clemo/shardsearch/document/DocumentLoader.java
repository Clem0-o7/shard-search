package in.clemo.shardsearch.document;

import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class DocumentLoader {

    private final ObjectMapper objectMapper;

    public DocumentLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public long load(Path corpusPath, Consumer<Document> documentConsumer)
            throws IOException {

        long documentCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(corpusPath)) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                Document document =
                        objectMapper.readValue(line, Document.class);

                documentConsumer.accept(document);
                documentCount++;
            }
        }

        return documentCount;
    }
}