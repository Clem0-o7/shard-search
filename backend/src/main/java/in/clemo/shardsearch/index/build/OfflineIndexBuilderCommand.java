package in.clemo.shardsearch.index.build;

import java.nio.file.Path;

public class OfflineIndexBuilderCommand {

    public static void main(String[] args) throws Exception {
        Path input = null;
        Path output = null;
        String name = null;
        int shards = -1;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input":
                    input = Path.of(args[++i]);
                    break;
                case "--output":
                    output = Path.of(args[++i]);
                    break;
                case "--name":
                    name = args[++i];
                    break;
                case "--shards":
                    shards = Integer.parseInt(args[++i]);
                    break;
            }
        }

        if (input == null || output == null || name == null || shards <= 0) {
            System.err.println("Usage: java ... OfflineIndexBuilderCommand --input <path> --output <path> --name <name> --shards <number>");
            System.exit(1);
        }

        System.out.println("Starting offline index build...");
        System.out.println("Input:  " + input);
        System.out.println("Output: " + output);
        System.out.println("Name:   " + name);
        System.out.println("Shards: " + shards);

        OfflineIndexBuilder builder = new OfflineIndexBuilder();
        BuildResult result = builder.build(input, output, name, shards);

        System.out.println("Build completed in " + result.elapsedMillis() + " ms");
        System.out.println("Total documents: " + result.documentCount());
        System.out.println("Shards created: " + result.shardCount());
    }
}
