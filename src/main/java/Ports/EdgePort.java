package Ports;

import Events.*;
import se.sics.kompics.PortType;

public class EdgePort extends PortType {{
    positive(StartMessage.class);
    positive(ReportMessage.class);
    positive(TestMessage.class);
    positive(ChangeRootMessage.class);
    positive(ConnectMessage.class);
    positive(TestReplyMessage.class);

    negative(StartMessage.class);
    negative(ReportMessage.class);
    negative(TestMessage.class);
    negative(ChangeRootMessage.class);
    negative(ConnectMessage.class);
    negative(TestReplyMessage.class);
}}
