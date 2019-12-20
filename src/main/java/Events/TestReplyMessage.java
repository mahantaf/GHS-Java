package Events;

import se.sics.kompics.KompicsEvent;

public class TestReplyMessage implements KompicsEvent {
    public String src;
    public String dst;
    public String message;

    public TestReplyMessage(String src, String dst, String message) {
        this.src = src;
        this.dst = dst;
        this.message = message;
    }
}
