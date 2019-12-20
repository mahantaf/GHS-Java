package Events;

import se.sics.kompics.KompicsEvent;

public class ChangeRootMessage implements KompicsEvent {
    public String src;
    public String dst;
    public String fragmentName;
    public int fragmentLevel;
//    String newRoot;

    public ChangeRootMessage(String src, String dst, String fragmentName, int fragmentLevel) {
        this.src = src;
        this.dst = dst;
        this.fragmentName = fragmentName;
        this.fragmentLevel = fragmentLevel;
    }
}
