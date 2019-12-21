package Events;

import se.sics.kompics.KompicsEvent;

public class ChangeSizeMessage implements KompicsEvent {
    public String src;
    public String dst;
    public int fragmentSize;

    public ChangeSizeMessage(String src, String dst, int fragmentSize) {
        this.dst = dst;
        this.src = src;
        this.fragmentSize = fragmentSize;
    }
}
