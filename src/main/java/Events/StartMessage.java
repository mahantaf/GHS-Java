package Events;

import se.sics.kompics.KompicsEvent;

public class StartMessage implements KompicsEvent {
    public String dst;
    public String src;
    public String fragmentName;
    public String rootName;
    public int fragmentLevel;

    public StartMessage(String src, String dst, String rootName, String fragmentName, int fragmentLevel) {
        this.dst = dst;
        this.src = src;
        this.fragmentName = fragmentName;
        this.fragmentLevel = fragmentLevel;
        this.rootName = rootName;
    }
}
