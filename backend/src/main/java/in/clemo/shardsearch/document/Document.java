package in.clemo.shardsearch.document;

public record Document(
    long id,
    String title,
    String text,
    String source
)
{

}