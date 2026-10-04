package in.clemo.shardsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import in.clemo.shardsearch.distributed.node.NodeSearchService;

@SpringBootTest
class ShardsearchApplicationTests {

    @MockitoBean
    private NodeSearchService nodeSearchService;

	@Test
	void contextLoads() {
	}

}
