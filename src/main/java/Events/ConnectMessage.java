package Events;

import se.sics.kompics.KompicsEvent;

public class ConnectMessage implements KompicsEvent {
    public String src;
    public String dst;
    public String fragmentName;
    public int fragmentLevel;

    public ConnectMessage(String src, String dst, String fragmentName, int fragmentLevel) {
        this.dst = dst;
        this.src = src;
        this.fragmentName = fragmentName;
        this.fragmentLevel = fragmentLevel;
    }
}
