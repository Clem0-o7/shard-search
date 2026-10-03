package in.clemo.shardsearch.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("[\\p{L}\\p{N}]+");

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.toLowerCase(Locale.ROOT);

        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        List<String> tokens = new ArrayList<>();

        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        return tokens;
    }
}