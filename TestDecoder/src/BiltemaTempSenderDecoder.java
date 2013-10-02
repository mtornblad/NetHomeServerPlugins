/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.nio.ByteBuffer;
import nu.nethome.util.plugin.Plugin;
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
    private String protocolType = "QUE?";
    private int[] splitPositions = {4};
   
    public BiltemaTempSenderDecoder() {
        debugLevel = 1;
        Init(BiltemaTempSenderDecoder.class, protocolName, protocolCompany, protocolType, startPulse, dataLength, pulseLengthTolerance, waveHead, waveData0, waveData1, waveTail);
    }

    @Override
    public void decode(long data) {

        int[] bits = bitSplit(data, splitPositions);

        ProtocolMessage message = new ProtocolMessage(protocolName, 0, 0, bits.length);
        for (int i = 0; i < bits.length; i++) {
            message.setRawMessageByteAt(i, bits[i]);
        }

        // Report the parsed message
        sink.parsedMessage(message);
    }
}
