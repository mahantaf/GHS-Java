package Events;

import se.sics.kompics.KompicsEvent;

public class StartMessage implements KompicsEvent {
    public String dst;
    public String src;
    public String fragmentName;
    public String rootName;
    public int fragmentLevel;
    public int fragmentSize;

    public StartMessage(String src, String dst, String rootName, String fragmentName, int fragmentLevel, int fragmentSize) {
        this.dst = dst;
        this.src = src;
        this.fragmentName = fragmentName;
        this.fragmentLevel = fragmentLevel;
        this.rootName = rootName;
        this.fragmentSize = fragmentSize;
    }
}
