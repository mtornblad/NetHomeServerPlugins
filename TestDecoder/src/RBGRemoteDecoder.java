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
public class RBGRemoteDecoder extends ProtocolHelper implements ProtocolDecoder {
    private int[] waveHead = {8925, 4400};
    private int[] waveData0 = {635, 460};
    private int[] waveData1 = {635, 1590};
    private int[] waveTail = {};
    private boolean startPulse = true;      //Markpulse
    private int dataLength = 32;
    private int pulseLengthTolerance = 10;
    //private ProtocolDecoderSink sink = null;
    private String protocolName = "RGBRemote";
    private String protocolCompany = "RGBRemoteCompany";
    private String protocolType = "Space Length";
    private int[] splitPositions = {4};

    public RBGRemoteDecoder() {
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
