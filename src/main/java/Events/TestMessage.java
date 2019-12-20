package Events;

import se.sics.kompics.KompicsEvent;

public class TestMessage implements KompicsEvent {
    public String dst;
    public String src;
    public String fragmentName;
    public int fragmentLevel;

    public TestMessage(String src, String dst, String fragmentName, int fragmentLevel) {
        this.dst = dst;
        this.src = src;
        this.fragmentName = fragmentName;
        this.fragmentLevel = fragmentLevel;
    }
}
