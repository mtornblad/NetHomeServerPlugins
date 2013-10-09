
/**
 * Copyright (C) 2007-2011, Stefan Strömberg <stestr@nethome.nu>
 *
 * This file is part of OpenNetHome.
 *
 * OpenNetHome is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenNetHome is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;
import java.io.*;

/**
 * This is a very simple demo of how a protocol decoder is built with the
 * nethome decoder framework. The protocol decoders can be used with the
 * ProtocolAnalyzer and the NethomeServer do decode RF and IR protocols. This
 * RubicsonGrillDecoder does not decode any real protocol, it simply interprets
 * lengths of pulses and spaces as 0 and 1. It serves to demonstrate how a real
 * decoder would be written
 *
 * Note that the class is annotated as a "Plugin" which makes it possible for
 * the ProtocolAnalyzer to load it dynamically. All you have to do is to pack
 * the class in a jar and place the jar in the "plugins" folder.
 *
 * @author Stefan Strömberg
 *
 */
@Plugin
public class RubicsonGrillDecoder implements ProtocolDecoder {

    private static final int IDLE = 0;
    private static final int WAIT_MARK = 1;
    private static final int WAIT_SPACE = 2;
    private static final int MESSAGE_LENGTH = 36;
    private int state = IDLE;
    private long data = 0;
    private long lastData = 0;
    private int bitCounter = 0;
    private int repeatCount = 0;
    private ProtocolDecoderSink sink = null;
    private double lastPulse;

    public void setTarget(ProtocolDecoderSink sink) {
        this.sink = sink;
    }

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("RubicsonGrill", "Space Length", "Mattias", MESSAGE_LENGTH, 1);
    }

    public int parse(double pulse, boolean state) {
        switch (this.state) {
            case IDLE: {
                if ((pulse > 7900) && (pulse < 7950) && (lastPulse > 600) && (lastPulse < 700) && !state) {
                    this.state = WAIT_MARK;
                    //System.out.println("Set state WAIT_MARK for Rubicson");
                }
                break;
            }
            case WAIT_SPACE: {
                if ((pulse > 1650) && (pulse < 1950)) {
                    addBit(0);
                    this.state = WAIT_MARK;
                    // System.out.println("Set state WAIT_MARK, pulses:" + bitCounter);
                } else if ((pulse > 3700) && (pulse < 4300)) {
                    addBit(1);
                    this.state = WAIT_MARK;
                    // System.out.println("Set state WAIT_MARK, pulses:" + bitCounter);
                } else {
                    this.state = IDLE;
                    data = 0;
                    bitCounter = 0;
                }
                break;
            }
            case WAIT_MARK: {
                if ((pulse > 600) && (pulse < 700)) {
                    this.state = WAIT_SPACE;
                    // System.out.println("Set state WAIT_SPACE, pulses:" + bitCounter);
                } else {
                    this.state = IDLE;
                    data = 0;
                    bitCounter = 0;
                }
                break;
            }
        }
        lastPulse = pulse;
        return this.state;
    }

    private void addBit(int b) {
        data <<= 1;
        data |= b;
        bitCounter++;
        // Check if this is a complete message
        if (bitCounter == MESSAGE_LENGTH) {

            //System.out.println("Rubicson: " + Long.toBinaryString(data));

            try {
                FileWriter fstream = new FileWriter("c:\\temp\\grilldata.txt", true); //true tells to append data.
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(Long.toBinaryString(data) + "\n");
                out.close();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            // It is, read the parameters

            long decode = data;

            int n1 = (int) decode & 0xF;
            decode >>= 4;	// Unknown
            int n2 = (int) decode & 0xF;
            decode >>= 4;	// Device id generated at start?
            int n3 = (int) decode & 0xF;
            decode >>= 4;  // Device id generated at start?
            int n4 = (int) decode & 0xF;
            decode >>= 4;  // ?
            int n5 = (int) decode & 0xF;
            decode >>= 4;  // ?
            int n6 = (int) decode & 0xF;
            decode >>= 4;	// 0
            int n7 = (int) decode & 0xF;
            decode >>= 4;	// Temp high nibble
            int n8 = (int) decode & 0xF;
            decode >>= 4;	// Temp middle nibble
            int n9 = (int) decode & 0xF;
            decode >>= 4;	// Temp low nibble

            int command = 0;
            int address = n3 + (n2 << 4);

            int tempF = n9 + (n8 << 4) + (n7 << 8) - 90;
            int tempC = (int) ((tempF - 32) / 1.8);

            // Create the message
            ProtocolMessage message = new ProtocolMessage("RubicsonGrill", command, address, 9);
            message.setRawMessageByteAt(8, n1);
            message.setRawMessageByteAt(7, n2);
            message.setRawMessageByteAt(6, n3);
            message.setRawMessageByteAt(5, n4);
            message.setRawMessageByteAt(4, n5);
            message.setRawMessageByteAt(3, n6);
            message.setRawMessageByteAt(2, n7);
            message.setRawMessageByteAt(1, n8);
            message.setRawMessageByteAt(0, n9);

            message.addField(new FieldValue("Address", address));
            message.addField(new FieldValue("TempC", tempC));
            message.addField(new FieldValue("TempF", tempF));
            message.addField(new FieldValue("N4", n4));
            message.addField(new FieldValue("N5", n5));

            System.out.println("Rubicson iGrill :-) Farenheit: " + tempF);

            // Check if this is a repeat
            if (data == lastData) {
                repeatCount++;
                message.setRepeat(repeatCount);
            } else {
                repeatCount = 0;
            }

            // Report the parsed message
            sink.parsedMessage(message);
            lastData = data;
            data = 0;
            bitCounter = 0;
            state = IDLE;
        }
    }
    

}
