package Events;

import se.sics.kompics.KompicsEvent;

public class ReportMessage implements KompicsEvent {

    public String dst;
    public String src;
    public int minimumWeight;

    public ReportMessage(String src, String dst, int minimumWeight) {
        this.dst = dst;
        this.src = src;
        this.minimumWeight = minimumWeight;
    }
}
