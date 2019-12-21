package Events;

import se.sics.kompics.KompicsEvent;

public class ReportReplyMessage implements KompicsEvent {
    public String dst;
    public String src;
    public String minFinder;

    public ReportReplyMessage(String src, String dst, String minFinder) {
        this.dst = dst;
        this.src = src;
        this.minFinder = minFinder;
    }
}
