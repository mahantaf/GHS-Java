package Components;
import Events.*;
import Ports.EdgePort;
import misc.Edge;
import misc.EdgeState;
import se.sics.kompics.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Node extends ComponentDefinition {
    Positive<EdgePort> receivePort = positive(EdgePort.class);
    Negative<EdgePort> sendPort = negative(EdgePort.class);
    public String nodeName;
    public String parentName;
    public String fragmentName;
    public String fragmentRoot;
    public int fragmentLevel;
    public int fragmentSize;

    public int minimumWeightFound;
    public String minimumNeighborFound;

    public enum State {Sleep, Find, Found};
    public State state;

    ArrayList<Edge> neighborEdges = new ArrayList<>();
    HashMap<String,Integer> neighbours = new HashMap<>();

    public Node(InitMessage initMessage) {
        System.out.println("initNode :" + initMessage.nodeName);

        this.nodeName = initMessage.nodeName;
        this.neighborEdges = initMessage.neighborEdges;
        this.neighbours = initMessage.neighbours;
        this.state = State.Sleep;
        this.fragmentName = initMessage.nodeName;
        this.fragmentRoot = initMessage.nodeName;
        this.fragmentLevel = 0;
        this.parentName = null;
        subscribe(startHandler, control);
        subscribe(reportHandler, receivePort);
    }

    public Edge findEdge (String src, String dst) {
        for (Edge e : neighborEdges)
            if(e.src.equals(src) && e.dst.equals(dst))
                return e;
        return null;
    }

    public String findMinimumWeightEdge() {
        int minimumWeight = 10000;
        String minimumNeighbor = null;

        for( Map.Entry<String, Integer> entry : neighbours.entrySet()) {
            if (entry.getValue() < minimumWeight && findEdge(nodeName, entry.getKey()).state == EdgeState.Basic) {
                minimumWeight = entry.getValue();
                minimumNeighbor = entry.getKey();
            }
        };
        return minimumNeighbor;
    }

    Handler changeRootHandler = new Handler<ChangeRootMessage>() {
        @Override
        public void handle(ChangeRootMessage event) {

        }
    };

    Handler connectHandler = new Handler<ConnectMessage>() {
        @Override
        public void handle(ConnectMessage event) {
            if (event.fragmentLevel < fragmentLevel) {
                findEdge(event.dst, event.src).state = EdgeState.Branch;
                trigger(new ChangeRootMessage(nodeName, event.src, fragmentName, fragmentLevel), sendPort);
            } else if (findEdge(nodeName, event.src).state == EdgeState.Branch) {
                fragmentLevel++;
                fragmentName = nodeName;
                trigger(new ChangeRootMessage(nodeName, event.src, fragmentName, fragmentLevel), sendPort);
                for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                    if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch) {
                        trigger(new ChangeRootMessage(nodeName, entry.getKey(), fragmentName, fragmentLevel), sendPort);
                    }
                }
                findEdge(event.dst, event.src).state = EdgeState.Branch;
            }
        }
    };

    Handler reportHandler = new Handler<ReportMessage>() {
        @Override
        public void handle(ReportMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                if (state == State.Found) {
                    if (event.src.equalsIgnoreCase(nodeName)) {
                        findEdge(nodeName, minimumNeighborFound).state = EdgeState.Branch;
                        trigger(new ConnectMessage(nodeName, minimumNeighborFound, fragmentName, fragmentLevel), sendPort);
                    } else {
                        if (event.minimumWeight < minimumWeightFound) {
                            state = State.Sleep;
                            for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                                if ((findEdge(nodeName, entry.getKey()).state == EdgeState.Branch) && (!findEdge(nodeName, entry.getKey()).dst.equalsIgnoreCase(event.src))) {
                                    trigger(new ReportMessage(event.src, entry.getKey(), event.minimumWeight), sendPort);
                                }
                            }
                        }
                    }
                } else {
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        if ((findEdge(nodeName, entry.getKey()).state == EdgeState.Branch) && (!findEdge(nodeName, entry.getKey()).dst.equalsIgnoreCase(event.src))) {
                            trigger(new ReportMessage(event.src, entry.getKey(), event.minimumWeight), sendPort);
                        }
                    }
                }
            }
        }
    };

    Handler testReplyHandler = new Handler<TestReplyMessage>() {
        @Override
        public void handle(TestReplyMessage event) {
            if (event.message.equals("reject")) {
                findEdge(event.dst, event.src).state = EdgeState.Reject;
                String minimumNeighbor = findMinimumWeightEdge();
                trigger(new TestMessage(nodeName, minimumNeighbor, fragmentName, fragmentLevel), sendPort);
            } else {
                state = State.Found;
                minimumWeightFound = neighbours.get(event.src);
                minimumNeighborFound = event.src;
                for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                    if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch) {
                        trigger(new ReportMessage(nodeName, entry.getKey(), neighbours.get(event.src)), sendPort);
                    }
                }
            }
        }
    };

    Handler testHandler = new Handler<TestMessage>() {
        @Override
        public void handle(TestMessage event) {
            if (!event.fragmentName.equals(fragmentName)) {
                if (event.fragmentLevel <= fragmentLevel)
                    trigger(new TestReplyMessage(nodeName, event.src, "accept"), sendPort);
                else {
                    // TODO: Delay
                }
            } else {
                findEdge(event.dst, event.src).state = EdgeState.Reject;
                trigger(new TestReplyMessage(nodeName, event.src, "reject"), sendPort);
            }
        }
    };

    Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            state = State.Find;
            String minimumNeighbor = findMinimumWeightEdge();
            if(minimumNeighbor != null)
                trigger(new TestMessage(nodeName, minimumNeighbor, fragmentName, fragmentLevel), sendPort);
            else {
                state = State.Sleep;
                trigger(new ReportMessage(nodeName, parentName, 10000), sendPort);
            }
        }
    };

}

