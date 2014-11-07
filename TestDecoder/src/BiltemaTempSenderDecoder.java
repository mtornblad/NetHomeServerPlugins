/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.nio.ByteBuffer;
import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.FieldValue;
import nu.nethome.util.ps.ProtocolDecoder;
import nu.nethome.util.ps.ProtocolDecoderSink;
import nu.nethome.util.ps.ProtocolMessage;

/**
 *
 * @author tornbmat
 */
@Plugin
public class BiltemaTempSenderDecoder extends ProtocolHelper implements ProtocolDecoder {
    
    private int[] waveHead = {1400};
    private int[] waveData0 = {2278, 644};
    private int[] waveData1 = {783, 2160};
    private int[] waveTail = {};
    private boolean startPulse = false;      //Markpulse
    private int dataLength = 21;
    private int pulseLengthTolerance = 10;

    //private ProtocolDecoderSink sink = null;
    private String protocolName = "BiltemaTempSender";
    private String protocolCompany = "Biltema";
    private String protocolType = "Recompiled";
   
    public BiltemaTempSenderDecoder() {
        debugLevel = 0;
        Init(BiltemaTempSenderDecoder.class, protocolName, protocolCompany, protocolType, startPulse, dataLength, pulseLengthTolerance, waveHead, waveData0, waveData1, waveTail);
    }

    @Override
    public void decode(long data) {


        ProtocolMessage message = new ProtocolMessage(protocolName, 0, 0, 3);
        
        int byte2 = (int)((data) & 0xFF);
        int byte1 = (int)((data >>= 8) & 0xFF);
        int byte0 = (int)((data >>= 8) & 0xFF);

        
        message.setRawMessageByteAt(2, byte2);
        message.setRawMessageByteAt(1, byte1);
        message.setRawMessageByteAt(0, byte0);

        message.addField(new FieldValue("byte2", byte2));
        message.addField(new FieldValue("byte1", byte1));
        message.addField(new FieldValue("byte0", byte0));
        // Report the parsed message
        sink.parsedMessage(message);
    }
}
