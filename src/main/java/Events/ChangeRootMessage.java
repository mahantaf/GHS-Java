package Events;

import se.sics.kompics.KompicsEvent;

public class ChangeRootMessage implements KompicsEvent {
    public String src;
    public String dst;
    public String rootName;
    public String fragmentName;
    public int fragmentLevel;
    public int fragmentSize;

    public ChangeRootMessage(String src, String dst, String rootName, String fragmentName, int fragmentLevel, int fragmentSize) {
        this.src = src;
        this.dst = dst;
        this.rootName = rootName;
        this.fragmentName = fragmentName;
        this.fragmentLevel = fragmentLevel;
        this.fragmentSize = fragmentSize;
    }
}
