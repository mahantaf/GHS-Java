package Events;

import se.sics.kompics.KompicsEvent;

public class ConnectMessage implements KompicsEvent {
    public String src;
    public String dst;
    public String fragmentName;
    public int fragmentLevel;
    public int fragmentSize;

    public ConnectMessage(String src, String dst, String fragmentName, int fragmentLevel, int fragmentSize) {
        this.dst = dst;
        this.src = src;
        this.fragmentName = fragmentName;
        this.fragmentLevel = fragmentLevel;
        this.fragmentSize = fragmentSize;
    }
}
