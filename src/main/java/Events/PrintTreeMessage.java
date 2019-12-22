package Events;

import se.sics.kompics.KompicsEvent;

public class PrintTreeMessage implements KompicsEvent {
    public String src;
    public String dst;

    public PrintTreeMessage(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }
}
