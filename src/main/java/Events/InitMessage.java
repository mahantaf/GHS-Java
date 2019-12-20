package Events;

import Components.Node;
import misc.Edge;
import se.sics.kompics.Init;

import java.util.ArrayList;
import java.util.HashMap;

public class InitMessage extends Init<Node> {
    public String nodeName;
    public boolean isRoot = false;
    public HashMap<String,Integer> neighbours = new HashMap<>();
    public ArrayList<Edge> neighborEdges = new ArrayList<>();

    public InitMessage(String nodeName, boolean isRoot,
                       HashMap<String,Integer> neighbours, ArrayList<Edge> neighborEdges) {
        this.nodeName = nodeName;
        this.isRoot = isRoot;
        this.neighbours = neighbours;
        this.neighborEdges = neighborEdges;
    }
}