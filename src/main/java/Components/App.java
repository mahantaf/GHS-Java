package Components;


import Events.InitMessage;
import Ports.EdgePort;
import misc.Edge;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class App extends ComponentDefinition {

    String startNode = "";
    ArrayList<Edge> edges = new ArrayList<>();
    Map<String, Component> components = new HashMap<String, Component>();

    public App() {
        readTable();
    }

    public static void main(String[] args) throws InterruptedException {
        Kompics.createAndStart(App.class);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            System.exit(1);
        }
        Kompics.shutdown();
//        Kompics.waitForTermination();

    }

    public void readTable() {
        File resourceFile = new File("src/main/java/tables.txt");
        try (Scanner scanner = new Scanner(resourceFile)) {
            int i = 0;
            while (scanner.hasNext()) {
                String line = scanner.nextLine();

                if (i > 0) {
                    if (line.split(",").length > 1) {
                        int weight = Integer.parseInt(line.split(",")[1]);
                        String rel = line.split(",")[0];
                        String src = rel.split("-")[0];
                        String dst = rel.split("-")[1];
                        edges.add(new Edge(src, dst, weight));
                    } else {
                        startNode = line;
                        for (Edge edge : edges) {
                            if (!components.containsKey(edge.src)) {
                                ArrayList<Edge> neighborEdges = new ArrayList<>();
                                HashMap<String, Integer> nb = findNeighbours(edge.src, neighborEdges);
                                Component c = create(Node.class, new InitMessage(edge.src, edge.src.equalsIgnoreCase
                                        (startNode), nb, neighborEdges));
                                components.put(edge.src, c);
                            }
                            if (!components.containsKey(edge.dst)) {
                                ArrayList<Edge> neighborEdges = new ArrayList<>();
                                HashMap<String, Integer> nb = findNeighbours(edge.src, neighborEdges);
                                Component c = create(Node.class, new InitMessage(edge.dst, edge.dst.equalsIgnoreCase
                                        (startNode), nb, neighborEdges));
                                components.put(edge.dst, c);
                            }
                            connect(components.get(edge.src).getPositive(EdgePort.class),
                                    components.get(edge.dst).getNegative(EdgePort.class), Channel.TWO_WAY);
                            connect(components.get(edge.src).getNegative(EdgePort.class),
                                    components.get(edge.dst).getPositive(EdgePort.class), Channel.TWO_WAY);
                        }
                    }
                }
                i++;
            }


            System.out.println(startNode);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private HashMap<String, Integer> findNeighbours(String node, ArrayList<Edge> neighborEdges) {
        HashMap<String, Integer> nb = new HashMap<String, Integer>();
        for (Edge tr : edges) {
            if (tr.src.equalsIgnoreCase(node) && !nb.containsKey(tr.dst)) {
                nb.put(tr.dst, tr.weight);
                neighborEdges.add(tr);
            } else if (tr.dst.equalsIgnoreCase(node) && !nb.containsKey(tr.src)) {
                nb.put(tr.src, tr.weight);
            }
        }
        return nb;
    }
}
