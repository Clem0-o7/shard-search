package in.clemo.shardsearch.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenizerTest {

    private final Tokenizer tokenizer = new Tokenizer();

    @Test
    void tokenizesNormalText() {
        assertEquals(
                List.of(
                        "distributed",
                        "systems",
                        "fault",
                        "tolerant",
                        "search"
                ),
                tokenizer.tokenize(
                        "Distributed Systems: Fault-Tolerant Search!"
                )
        );
    }

    @Test
    void preservesNumbers() {
        assertEquals(
                List.of("java", "21", "spring", "boot", "4", "1"),
                tokenizer.tokenize("Java 21 / Spring Boot 4.1")
        );
    }

    @Test
    void handlesEmptyInput() {
        assertEquals(List.of(), tokenizer.tokenize(""));
        assertEquals(List.of(), tokenizer.tokenize("   "));
        assertEquals(List.of(), tokenizer.tokenize(null));
    }

    @Test
    void handlesUnicodeLetters() {
        assertEquals(
                List.of("café", "naïve"),
                tokenizer.tokenize("Café naïve")
        );
    }
}