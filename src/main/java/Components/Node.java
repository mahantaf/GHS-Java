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
    HashMap<String, Integer> minimumWeights = new HashMap<>();

    public Node(InitMessage initMessage) {
        System.out.println("initNode :" + initMessage.nodeName);

        this.nodeName = initMessage.nodeName;
        this.neighborEdges = initMessage.neighborEdges;
        this.neighbours = initMessage.neighbours;
        this.state = State.Sleep;
        this.fragmentName = initMessage.nodeName;
        this.fragmentRoot = initMessage.nodeName;
        this.fragmentLevel = 0;
        this.fragmentSize = 1;
        this.parentName = initMessage.nodeName;

        subscribe(startHandler, control);
        subscribe(reportHandler, receivePort);
        subscribe(changeRootHandler, receivePort);
//        subscribe(changeSizeHandler, receivePort);
        subscribe(reportReplyHandler, receivePort);
        subscribe(testHandler, receivePort);
        subscribe(testReplyHandler, receivePort);
        subscribe(connectHandler, receivePort);
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
    Handler changeSizeHandler = new Handler<ChangeSizeMessage>() {
        @Override
        public void handle(ChangeSizeMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " has received CHANGE_SIZE from " + event.src);
                fragmentSize = event.fragmentSize;
                if (!nodeName.equalsIgnoreCase(parentName)) {
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch) {
                            trigger(new ChangeSizeMessage(nodeName, entry.getKey(), fragmentSize), sendPort);
                        }
                    }
                } else {
                    minimumWeights.clear();
                }
            }
        }
    };
    Handler changeRootHandler = new Handler<ChangeRootMessage>() {
        @Override
        public void handle(ChangeRootMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " has received CHANGE_ROOT from " + event.src);
                fragmentLevel = event.fragmentLevel;
                fragmentName = event.fragmentName;
                fragmentRoot = event.rootName;
                fragmentSize = event.fragmentSize;
                parentName = event.src;
                minimumWeights.clear();
                for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                    if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch && !findEdge(nodeName, entry.getKey()).dst.equalsIgnoreCase(event.src)) {
                        trigger(new ChangeRootMessage(nodeName, entry.getKey(), fragmentRoot, fragmentName, fragmentLevel, fragmentSize), sendPort);
                    }
                }
            }
        }
    };

    Handler connectHandler = new Handler<ConnectMessage>() {
        @Override
        public void handle(ConnectMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " has received CONNECT from " + event.src);
                if (event.fragmentLevel < fragmentLevel) {
                    System.out.println("Node " + nodeName + " and " + event.src + " are in ABSORB phase");
                    // Absorb phase
                    fragmentSize += event.fragmentSize;
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch) {
                            trigger(new ChangeSizeMessage(nodeName, entry.getKey(), fragmentSize), sendPort);
                        }
                    }
                    findEdge(event.dst, event.src).state = EdgeState.Branch;
                    trigger(new ChangeRootMessage(nodeName, event.src, fragmentRoot, fragmentName, fragmentLevel, fragmentSize), sendPort);
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch) {
                            trigger(new StartMessage(nodeName, entry.getKey(), fragmentRoot, fragmentName, fragmentLevel, fragmentSize), sendPort);
                        }
                    }
                } else if (findEdge(nodeName, event.src).state == EdgeState.Branch) {
                    // Merge phase
                    fragmentLevel++;
                    fragmentName = nodeName;
                    fragmentSize += event.fragmentSize;
                    fragmentRoot = (nodeName.charAt(0) - event.src.charAt(0)) > 0 ? nodeName : event.src;
                    parentName = nodeName;
                    System.out.println("Node " + nodeName + " and " + event.src + " are in MERGE phase with root " + fragmentRoot);
                    trigger(new ChangeRootMessage(nodeName, event.src, fragmentRoot, fragmentName, fragmentLevel, fragmentSize), sendPort);
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch &&
                                !findEdge(nodeName, entry.getKey()).dst.equalsIgnoreCase(event.src)) {
                            trigger(new ChangeRootMessage(nodeName, entry.getKey(), fragmentRoot, fragmentName, fragmentLevel, fragmentSize), sendPort);
                        }
                    }
                    findEdge(event.dst, event.src).state = EdgeState.Branch;
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        trigger(new StartMessage(nodeName, entry.getKey(), fragmentRoot, fragmentName, fragmentLevel, fragmentSize), sendPort);
                    }
                }
            }
        }
    };
    Handler reportReplyHandler = new Handler<ReportReplyMessage>() {
        @Override
        public void handle(ReportReplyMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " has received REPORT_REPLY from " + event.src);
                if (nodeName.equalsIgnoreCase(event.dst)) {
                    // Check if this is the selected node
                    if (nodeName.equalsIgnoreCase(event.minFinder)) {
                        findEdge(nodeName, minimumNeighborFound).state = EdgeState.Branch;
                        trigger(new ConnectMessage(nodeName, minimumNeighborFound, fragmentName, fragmentLevel, fragmentSize), sendPort);
                    } else {
                        for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                            if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch &&
                                    (!findEdge(nodeName, entry.getKey()).dst.equalsIgnoreCase(event.src))) {
                                trigger(new ReportReplyMessage(nodeName, entry.getKey(), event.minFinder), sendPort);
                            }
                        }
                    }
                }
            }
        }
    };

    Handler reportHandler = new Handler<ReportMessage>() {
        @Override
        public void handle(ReportMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " has received REPORT from " + event.src);
                // Check if this node is root or not
                if (nodeName.equalsIgnoreCase(parentName)) {
                    // If parent it should add this node and weight to the HashMap and if
                    // the size of HashMap is equal to fragmentSize - 1 it should get the
                    // minimum over all the values in the HashMap and send a report reply
                    // message to the node with minimum edge that it has found.
                    minimumWeights.put(event.src, event.minimumWeight);
                    if (minimumWeights.size() == fragmentSize) {
                        int minWeight = 10000;
                        String minFinder = null;
                        for (Map.Entry<String, Integer> entry : minimumWeights.entrySet()) {
                            if(entry.getValue() < minWeight) {
                                minWeight = entry.getValue();
                                minFinder = entry.getKey();
                            }
                        }
                        for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                            if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch) {
                                trigger(new ReportReplyMessage(nodeName, entry.getKey(), minFinder), sendPort);
                            }
                        }
                    }

                } else {
                    // If node is not a root send the message to parent.
                    event.dst = parentName;
                    trigger(event, sendPort);
                }
            }
        }
    };

    Handler testReplyHandler = new Handler<TestReplyMessage>() {
        @Override
        public void handle(TestReplyMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " has received TEST_REPLY from " + event.src + " with value " + event.message);
                if (event.message.equals("reject")) {
                    // If message contains reject find next best edge and send a test message through it.
                    findEdge(event.dst, event.src).state = EdgeState.Reject;
                    String minimumNeighbor = findMinimumWeightEdge();
                    trigger(new TestMessage(nodeName, minimumNeighbor, fragmentName, fragmentLevel), sendPort);
                } else {
                    // If message contains accept change the node state to found and send a report message to the root via parent.
                    state = State.Found;
                    minimumWeightFound = neighbours.get(event.src);
                    minimumNeighborFound = event.src;
                    if (fragmentSize == 1) {
                        System.out.println("Node " + nodeName + " is sending a connect message to node " + minimumNeighborFound);
                        findEdge(nodeName, minimumNeighborFound).state = EdgeState.Branch;
                        trigger(new ConnectMessage(nodeName, minimumNeighborFound, fragmentName, fragmentLevel, fragmentSize), sendPort);
                    } else {
                        trigger(new ReportMessage(nodeName, parentName, neighbours.get(event.src)), sendPort);
                    }
                }
            }
        }
    };

    Handler testHandler = new Handler<TestMessage>() {
        @Override
        public void handle(TestMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " has received TEST message from " + event.src);
                if (!event.fragmentName.equals(fragmentName)) {
                    // If nodes are from different fragments.
                    if (event.fragmentLevel <= fragmentLevel)
                        trigger(new TestReplyMessage(nodeName, event.src, "accept"), sendPort);
                    else {
                        // If requested node fragment size is bigger than receiver it should wait.
                        // TODO: Delay
                    }
                } else {
                    // If nodes are from the same fragment send reject.
                    findEdge(event.dst, event.src).state = EdgeState.Reject;
                    trigger(new TestReplyMessage(nodeName, event.src, "reject"), sendPort);
                }
            }
        }
    };

    Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            System.out.println("Node " + nodeName + " has received start message");
            state = State.Find;
            String minimumNeighbor = findMinimumWeightEdge();

            // Send an initial test message on candidate edge.
            if(minimumNeighbor != null) {
                System.out.println("Node " + nodeName + " is sending a test message to " + minimumNeighbor);
                trigger(new TestMessage(nodeName, minimumNeighbor, fragmentName, fragmentLevel), sendPort);
            }
            else {
                // If no edge found send report message to root via parent with report message that contains infinity weight.
                state = State.Sleep;
                trigger(new ReportMessage(nodeName, parentName, 10000), sendPort);
            }
        }
    };

    Handler initHandler = new Handler<StartMessage>() {
        @Override
        public void handle(StartMessage event) {
            fragmentName = event.fragmentName;
            fragmentSize = event.fragmentSize;
            fragmentRoot = event.rootName;
            fragmentLevel = event.fragmentLevel;

            state = State.Find;
            String minimumNeighbor = findMinimumWeightEdge();

            for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                trigger(new StartMessage(nodeName, entry.getKey(), fragmentRoot, fragmentName, fragmentLevel, fragmentSize), sendPort);
            }

            // Send an initial test message on candidate edge.
            if(minimumNeighbor != null)
                trigger(new TestMessage(nodeName, minimumNeighbor, fragmentName, fragmentLevel), sendPort);
            else {
                // If no edge found send report message to root via parent with report message that contains infinity weight.
                state = State.Sleep;
                trigger(new ReportMessage(nodeName, parentName, 10000), sendPort);
            }
        }
    };

}

