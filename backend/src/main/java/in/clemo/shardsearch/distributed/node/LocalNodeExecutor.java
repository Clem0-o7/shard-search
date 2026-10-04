package in.clemo.shardsearch.distributed.node;

import in.clemo.shardsearch.analysis.Tokenizer;
//import in.clemo.shardsearch.distributed.GlobalCorpusStatistics;
import in.clemo.shardsearch.distributed.Shard;
import in.clemo.shardsearch.search.Bm25Scorer;
import in.clemo.shardsearch.search.SearchEngine;
import in.clemo.shardsearch.search.SearchResponse;

public class LocalNodeExecutor
        implements NodeExecutor {

    private final LocalShardRegistry shardRegistry;
    private final Tokenizer tokenizer;
    private final Bm25Scorer scorer;
    private final in.clemo.shardsearch.search.CorpusStatistics corpusStatistics;

    public LocalNodeExecutor(
            LocalShardRegistry shardRegistry,
            Tokenizer tokenizer,
            Bm25Scorer scorer,
            in.clemo.shardsearch.search.CorpusStatistics corpusStatistics
    ) {
        this.shardRegistry = shardRegistry;
        this.tokenizer = tokenizer;
        this.scorer = scorer;
        this.corpusStatistics = corpusStatistics;
    }

    @Override
    public SearchResponse execute(
            NodeDescriptor node,
            NodeSearchRequest request
    ) {

        Shard shard =
                shardRegistry.getShard(request.shardId());

        SearchEngine shardEngine =
                new SearchEngine(
                        shard.getIndex(),
                        tokenizer,
                        scorer,
                        corpusStatistics
                );

        SearchResponse response =
                shardEngine.searchWithTrace(
                        request.query(),
                        request.limit()
                );

        return response;
    }
}