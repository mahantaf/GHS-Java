package Events;

import se.sics.kompics.KompicsEvent;

public class ReportMessage implements KompicsEvent {

    public String dst;
    public String src;
    public String minFinder;
    public int minimumWeight;

    public ReportMessage(String src, String dst, String minFinder, int minimumWeight) {
        this.dst = dst;
        this.src = src;
        this.minFinder = minFinder;
        this.minimumWeight = minimumWeight;
    }
}
