package Events;

import se.sics.kompics.KompicsEvent;

public class ChangeRootMessage implements KompicsEvent {
    public String src;
    public String dst;
    public String minFinder;


    public ChangeRootMessage(String src, String dst, String minFinder) {
        this.src = src;
        this.dst = dst;
        this.minFinder = minFinder;
    }
}
