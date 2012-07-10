package voldemort.store.routed.action;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import voldemort.TestUtils;
import voldemort.cluster.Cluster;
import voldemort.cluster.Node;
import voldemort.cluster.failuredetector.FailureDetector;
import voldemort.cluster.failuredetector.FailureDetectorConfig;
import voldemort.cluster.failuredetector.ThresholdFailureDetector;
import voldemort.routing.RouteToAllLocalPrefStrategy;
import voldemort.routing.RoutingStrategy;
import voldemort.store.routed.BasicPipelineData;
import voldemort.store.routed.Pipeline;
import voldemort.store.routed.Pipeline.Event;
import voldemort.store.routed.Pipeline.Operation;
import voldemort.utils.ByteArray;

import com.google.common.collect.ImmutableList;

public class ConfigureNodesLocalHostTest {

    protected final ByteArray aKey = TestUtils.toByteArray("vold");
    protected String currentHost = "";

    private List<Node> getTestNodes() {
        try {
            currentHost = InetAddress.getLocalHost().getHostName();
        } catch(UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ImmutableList.of(node(0, "some-node-1", 2, 7, 14),
                                node(1, "some-node-2", 1, 10, 13),
                                node(2, currentHost, 3, 5, 17),
                                node(3, "some-node-3", 0, 11, 16),
                                node(4, "some-node-4", 6, 9, 15),
                                node(5, "some-node-5", 4, 8, 12));
    }

    private Node node(int id, String hostName, int... tags) {
        List<Integer> list = new ArrayList<Integer>(tags.length);
        for(int tag: tags)
            list.add(tag);
        return new Node(id, hostName, 8080, 6666, 6667, list);
    }

    @Test
    public void testConfigureNodesLocalHost() throws Exception {
        List<Node> nodes = getTestNodes();
        Cluster cluster = new Cluster("test-route-all-local-pref-cluster", nodes);
        FailureDetector failureDetector = new ThresholdFailureDetector(new FailureDetectorConfig().setNodes(nodes));
        RoutingStrategy routingStrategy = new RouteToAllLocalPrefStrategy(cluster.getNodes());
        BasicPipelineData<byte[]> pipelineData = new BasicPipelineData<byte[]>();
        ConfigureNodesLocalHost<byte[], BasicPipelineData<byte[]>> action = new ConfigureNodesLocalHost<byte[], BasicPipelineData<byte[]>>(pipelineData,
                                                                                                                                           Event.COMPLETED,
                                                                                                                                           failureDetector,
                                                                                                                                           1,
                                                                                                                                           routingStrategy,
                                                                                                                                           aKey);
        Pipeline pipeline = new Pipeline(Operation.GET, 10000, TimeUnit.MILLISECONDS);
        pipeline.addEventAction(Event.STARTED, action);
        pipeline.addEvent(Event.STARTED);
        pipeline.execute();

        if(pipelineData.getFatalError() != null)
            throw pipelineData.getFatalError();

        assertEquals(cluster.getNodes().size(), pipelineData.getNodes().size());
        assertEquals(pipelineData.getNodes().get(0).getHost(), currentHost);
    }
}
