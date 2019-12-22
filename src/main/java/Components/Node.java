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
    public int reportCount;

    public int minimumWeightFound;
    public String minimumNeighborFound;

    public enum State {Sleep, Find, Found};
    public State state;

    ArrayList<Edge> neighborEdges = new ArrayList<>();
    HashMap<String,Integer> neighbours = new HashMap<>();
    HashMap<String, Integer> minimumWeights = new HashMap<>();
    ArrayList<KompicsEvent> testCallbacks = new ArrayList<>();
    ArrayList<KompicsEvent> connectCallbacks = new ArrayList<>();

    public Node(InitMessage initMessage) {
        System.out.println("initNode :" + initMessage.nodeName);

        this.nodeName = initMessage.nodeName;
        this.neighborEdges = initMessage.neighborEdges;
        this.neighbours = initMessage.neighbours;
        this.state = State.Sleep;
        this.fragmentName = initMessage.nodeName;
        this.fragmentRoot = initMessage.nodeName;
        this.fragmentLevel = 1;
        this.parentName = null;
        this.reportCount = 0;

        subscribe(startHandler, control);
        subscribe(reportHandler, receivePort);
        subscribe(changeRootHandler, receivePort);
        subscribe(testHandler, receivePort);
        subscribe(testReplyHandler, receivePort);
        subscribe(connectHandler, receivePort);
        subscribe(initHandler, receivePort);
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
    public int numberOfBranchEdges() {
        int count = 0;
        for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
            if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch)
                count++;
        }
        return count;
    }
    public String findMinimumLeaf() {
        int minWeight = 10000;
        String minFinder = null;
        for (Map.Entry<String, Integer> entry : minimumWeights.entrySet()) {
            if(entry.getValue() < minWeight) {
                minWeight = entry.getValue();
                minFinder = entry.getKey();
            }
        }
        return minFinder;
    }
    public int findMinimumLeafWeight() {
        int minWeight = 10000;
        for (Map.Entry<String, Integer> entry : minimumWeights.entrySet())
            if(entry.getValue() < minWeight)
                minWeight = entry.getValue();
        return minWeight;
    }
    public void callConnectEventCallbacks() {
        System.out.println("Connect callback in " + nodeName + " size is: " + connectCallbacks.size());
        for (int i = 0; i < connectCallbacks.size(); i++) {
            KompicsEvent e = connectCallbacks.get(i);
            connectCallbacks.remove(i);
            connectHandler.handle(e);
            i--;
        }
    }
    public void callTestEventCallbacks() {
        System.out.println("Test callback in " + nodeName + " size is: " + testCallbacks.size());
        for (int i = 0; i < testCallbacks.size(); i++) {
            KompicsEvent e = testCallbacks.get(i);
            testCallbacks.remove(i);
            testHandler.handle(e);
            i--;
        }
    }

    Handler connectHandler = new Handler<ConnectMessage>() {
        @Override
        public void handle(ConnectMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {

                System.out.println("Node " + nodeName + " has received CONNECT from " + event.src);

                if (event.fragmentLevel < fragmentLevel && findEdge(event.dst, event.src).state != EdgeState.Branch) {
                    System.out.println("Node " + nodeName + " and " + event.src + " are in ABSORB phase");

                    // Absorb phase
                    findEdge(event.dst, event.src).state = EdgeState.Branch;
                    System.out.println("Node " + nodeName + " has sent INIT_MESSAGE to node " + event.src);
                    trigger(new StartMessage(nodeName, event.src, fragmentRoot, fragmentName, fragmentLevel), sendPort);
                    callConnectEventCallbacks();

                } else if (findEdge(nodeName, event.src).state == EdgeState.Branch) {

                    // Merge phase
                    fragmentLevel = event.fragmentLevel + 1;
                    fragmentRoot = (nodeName.charAt(0) - event.src.charAt(0)) > 0 ? nodeName : event.src;
                    fragmentName = fragmentRoot;
                    if (fragmentRoot.equalsIgnoreCase(nodeName))
                        parentName = null;
                    else
                        parentName = fragmentRoot;

                    System.out.println("Node " + nodeName + " and " + event.src + " are in MERGE phase with root " + fragmentRoot +
                                        " and fragment level " + fragmentLevel);

//                    findEdge(event.dst, event.src).state = EdgeState.Branch;
                    trigger(new StartMessage(nodeName, event.src, fragmentRoot, fragmentName, fragmentLevel), sendPort);
                } else {
                    System.out.println("Message is postponed via connect callback");
                    connectCallbacks.add(event);
                }
            }
        }
    };

    Handler changeRootHandler = new Handler<ChangeRootMessage>() {
        @Override
        public void handle(ChangeRootMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " has received CHANGE_ROOT from " + event.src);
                if(nodeName.equalsIgnoreCase(event.minFinder)) {
                    findEdge(nodeName, minimumNeighborFound).state = EdgeState.Branch;
                    trigger(new ConnectMessage(nodeName, minimumNeighborFound, fragmentName, fragmentLevel), sendPort);
                } else {
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch &&
                                !findEdge(nodeName, entry.getKey()).dst.equalsIgnoreCase(event.src)) {
                            trigger(new ChangeRootMessage(nodeName, entry.getKey(), event.minFinder), sendPort);
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
                System.out.println("Node " + nodeName + " has received REPORT from " + event.src + " with weight " + event.minimumWeight);
                if(fragmentLevel > 1) {
                    minimumWeights.put(event.minFinder, event.minimumWeight);
                    reportCount++;
                    if (reportCount == (1 + (numberOfBranchEdges() + (parentName == null ? 0 : -1)))) {
                        String minFinder = findMinimumLeaf();
                        if (parentName != null) {
                            System.out.println("Node " + nodeName + " is sending report to its parent");
                            trigger(new ReportMessage(nodeName, parentName, minFinder, findMinimumLeafWeight()), sendPort);
                        } else {
                            System.out.println("Root " + nodeName + " has received all the reports.");
                            if (findMinimumLeafWeight() != 10000) {
                                System.out.println("Root " + nodeName + " is sending CHANGE_ROOT with weight " + findMinimumLeafWeight()
                                        + " And node " + minFinder);
                                if (minFinder.equalsIgnoreCase(nodeName)) {
                                    changeRootHandler.handle(new ChangeRootMessage(nodeName, nodeName, nodeName));
                                } else {
                                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                                        if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch) {
                                            trigger(new ChangeRootMessage(nodeName, entry.getKey(), minFinder), sendPort);
                                        }
                                    }
                                }
                            } else {
                                System.out.println("Node " + nodeName + " should terminate the process.");
                                // TODO: It should terminate the process and print the tree.
                            }
                        }
                        reportCount = 0;
                        minimumWeights.clear();
                    }
                } else {
                    changeRootHandler.handle(new ChangeRootMessage(nodeName, nodeName, nodeName));
                }
            }
        }
    };

    final Handler testReplyHandler = new Handler<TestReplyMessage>() {
        @Override
        public void handle(TestReplyMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {

                System.out.println("Node " + nodeName + " has received TEST_REPLY from " + event.src + " with value " + event.message);

                if (event.message.equals("reject")) {
                    // If message contains reject find next best edge and send a test message through it.
                    findEdge(event.dst, event.src).state = EdgeState.Reject;

                    String minimumNeighbor = findMinimumWeightEdge();
                    if (minimumNeighbor != null) {
                        System.out.println("Node " + minimumNeighbor + " is a candidate for node " + nodeName);
                        trigger(new TestMessage(nodeName, minimumNeighbor, fragmentName, fragmentLevel), sendPort);
                    } else {
                        state = State.Sleep;
                        System.out.println("Node " + nodeName + " has found no candidate edge.");
                        reportHandler.handle(new ReportMessage(nodeName, nodeName, nodeName, 10000));
                    }
                } else {
                    // If message contains accept change the node state to found and send a report message to the root via parent.
                    state = State.Found;
                    minimumWeightFound = neighbours.get(event.src);
                    minimumNeighborFound = event.src;

                    reportHandler.handle(new ReportMessage(nodeName, nodeName, nodeName, neighbours.get(event.src)));
                }
            }
        }
    };

    Handler testHandler = new Handler<TestMessage>() {
        @Override
        public void handle(TestMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {

                System.out.println("Node " + nodeName + " with fragment " + fragmentName
                        + " and level " + fragmentLevel + " has received TEST message from "
                        + event.src + " with fragment " + event.fragmentName + " and level " + event.fragmentLevel);

                if (!event.fragmentName.equals(fragmentName)) {
                    // If nodes are from different fragments.
                    if (event.fragmentLevel <= fragmentLevel)
                        trigger(new TestReplyMessage(nodeName, event.src, "accept"), sendPort);
                    else {
                        // If requested node fragment size is bigger than receiver it should wait.
                        System.out.println("Node " + event.src + " has a bigger fragment than node " + nodeName);
                        testCallbacks.add(event);
                    }
                } else {
                    // If nodes are from the same fragment send reject.
                    findEdge(event.dst, event.src).state = EdgeState.Reject;
                    trigger(new TestReplyMessage(nodeName, event.src, "reject"), sendPort);
                }
            }
        }
    };

    Handler initHandler = new Handler<StartMessage>() {
        @Override
        public void handle(StartMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " has received INIT_MESSAGE from " + event.src);

                /**
                 * This functions as change root message
                 * Who receive this message first update its parent, root and fragment
                 * Then it sends test message to through its basic edges to its neighbors.
                 * Send this message through it Branch edges.
                 */
                fragmentName = event.fragmentName;
                fragmentRoot = event.rootName;
                fragmentLevel = event.fragmentLevel;
                if (fragmentRoot.equalsIgnoreCase(nodeName))
                    parentName = null;
                else
                    parentName = event.src;

                state = State.Find;

                // Send start message to branch edge neighbors.
                for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                    if (findEdge(nodeName, entry.getKey()).state == EdgeState.Branch &&
                            !findEdge(nodeName, entry.getKey()).dst.equalsIgnoreCase(event.src)) {
                        System.out.println("Node " + nodeName + " is sending INIT_MESSAGE to node " + entry.getKey());
                        trigger(new StartMessage(nodeName, entry.getKey(), fragmentRoot, fragmentName, fragmentLevel), sendPort);
                    }
                }
                callConnectEventCallbacks();
                callTestEventCallbacks();
                String minimumNeighbor = findMinimumWeightEdge();

                // Send an initial test message on candidate edge.
                if (minimumNeighbor != null) {
                    System.out.println("Node " + minimumNeighbor + " is a candidate for node " + nodeName);
                    trigger(new TestMessage(nodeName, minimumNeighbor, fragmentName, fragmentLevel), sendPort);
                } else {
                    // If no edge found send report message to root via parent with report message that contains infinity weight.
                    state = State.Sleep;
                    System.out.println("Node " + nodeName + " has found no candidate edge.");
//                    trigger(new ReportMessage(nodeName, parentName, 10000), sendPort);
                    reportHandler.handle(new ReportMessage(nodeName, nodeName, nodeName,10000));
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
//                trigger(new ReportMessage(nodeName, parentName, 10000), sendPort);
                reportHandler.handle(new ReportMessage(nodeName, nodeName, nodeName,10000));
            }
        }
    };

}

