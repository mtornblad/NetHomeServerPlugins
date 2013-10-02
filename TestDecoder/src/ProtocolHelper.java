/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.ArrayList;
import nu.nethome.util.ps.ProtocolDecoderSink;
import nu.nethome.util.ps.ProtocolInfo;
import nu.nethome.util.ps.PulseLength;

/**
 *
 * @author tornbmat
 */
abstract class ProtocolHelper {

    private enum WavePart {

        Idle, Head, Data, Tail, Ready
    }
    private PulseLength[] waveHead;
    private PulseLength[] waveData0;
    private PulseLength[] waveData1;
    private PulseLength[] waveTail;
    private boolean startPulse = true;      //Markpulse
    private int dataLength = 0;             //Datalength = 0, dynamic length, requires endpuls!  TODO!!!
    private WavePart wavePart = WavePart.Idle;
    private boolean matches0 = true;
    private boolean matches1 = true;
    private int pulseCounter = 0;
    private int bitCounter = 0;
    private long data = 0;
    private String protocolName;
    private String protocolCompany;
    private String protocolType;        //Change to ENUM???
    
    protected int debugLevel = 0;
    protected ProtocolDecoderSink sink = null;

    abstract void decode(long data);

    protected void debug(int Level, String Message) {
        if (debugLevel >= Level) {
            System.out.println(protocolName + ": " + Message);
        }
    }

    protected void Init(Class decoder, String protocolName, String protocolCompany, String protocolType, boolean startPulse, int dataLength, int pulseLengthTolerance, int[] waveHead, int[] waveData0, int[] waveData1, int[] waveTail) {
        this.startPulse = startPulse;
        this.dataLength = dataLength;
        this.protocolCompany = protocolCompany;
        this.protocolName = protocolName;
        this.protocolType = protocolType;
        this.waveHead = new PulseLength[waveHead.length];
        this.waveData0 = new PulseLength[waveData0.length];
        this.waveData1 = new PulseLength[waveData1.length];
        this.waveTail = new PulseLength[waveTail.length];


        for (int x = 0; x < waveHead.length; x++) {
            this.waveHead[x] = new PulseLength(decoder, "Head:" + x, waveHead[x], waveHead[x] * pulseLengthTolerance / 100);
        }
        for (int x = 0; x < waveData0.length; x++) {
            this.waveData0[x] = new PulseLength(decoder, "Data0:" + x, waveData0[x], waveData0[x] * pulseLengthTolerance / 100);
        }
        for (int x = 0; x < waveData1.length; x++) {
            this.waveData1[x] = new PulseLength(decoder, "Data1:" + x, waveData1[x], waveData1[x] * pulseLengthTolerance / 100);
        }
        for (int x = 0; x < waveTail.length; x++) {
            this.waveTail[x] = new PulseLength(decoder, "Tail:" + x, waveTail[x], waveTail[x] * pulseLengthTolerance / 100);
        }
        debug(1, "Plugin initiated!");
    }

    public void setTarget(ProtocolDecoderSink sink) {
        this.sink = sink;
    }

    public ProtocolInfo getInfo() {
        return new ProtocolInfo(protocolName, protocolType, protocolCompany, dataLength);
    }

    public int parse(double pulse, boolean state) {
        debug(3, "Parsing, state: " + wavePart.toString());
        debug(3, "Parsing, pulseCounter: " + pulseCounter);
        debug(3, "Parsing, bitCounter: " + bitCounter);
        switch (wavePart) {
            case Idle: {
                if ((waveHead[0].matches(pulse)) && (state == startPulse)) {
                    bitCounter = 0;
                    data = 0;
                    if (waveHead.length > 1) {
                        wavePart = WavePart.Head;
                        debug(2, "Switched state to " + wavePart.toString());
                        pulseCounter = 1;
                    } else {
                        wavePart = WavePart.Data;
                        debug(2, "Switched state to " + wavePart.toString());
                        pulseCounter = 0;
                    }
                }
                break;
            }
            case Head: {
                debug(3, "Parsing, pulseCounter: " + pulseCounter);
                debug(3, "Parsing, bitCounter: " + bitCounter);
                if (waveHead[pulseCounter].matches(pulse)) {
                    pulseCounter++;
                    if (pulseCounter == waveHead.length) {
                        wavePart = WavePart.Data;
                        debug(2, "Switched state to " + wavePart.toString());
                        pulseCounter = 0;
                    }
                } else {
                    pulseCounter = 0;
                    wavePart = WavePart.Idle;
                    debug(2, "Switched state to " + wavePart.toString());
                }
                break;
            }

            case Data: {
                debug(2, "Parsing, pulseCounter: " + pulseCounter);
                debug(2, "Parsing, bitCounter: " + bitCounter);
                debug(2, "Parsing, matches0: " + matches0);
                debug(2, "Parsing, matches1: " + matches1);
                // Kolla om vi matchar en puls för etta elle nolla, om tidigare serien också stämt
                if (!(waveData0[pulseCounter].matches(pulse) && matches0)) {
                    matches0 = false;
                }
                if (!(waveData1[pulseCounter].matches(pulse) && matches1)) {
                    matches1 = false;
                }
                debug(2, "Parsing, matches0: " + matches0);
                debug(2, "Parsing, matches1: " + matches1);

                //Matchar vi fortfarande någon databit så räkna upp 
                if (matches0 || matches1) {
                    pulseCounter++;

                    //Om vi fått alla pulser för en bit så lägger vi till den pch nollställer pulsräknare
                    if (matches0 && (pulseCounter == waveData0.length)) {
                        addBit(0);
                        pulseCounter = 0;
                        matches1 = true;
                    }

                    if (matches1 && (pulseCounter == waveData1.length)) {
                        addBit(1);
                        pulseCounter = 0;
                        matches0 = true;
                    }

                    //Har vi fått alla bitar vi väntar på?  (Kanske borde göras om så man kollar stop puls om det finns variabel paket längd
                    if ((bitCounter == dataLength) && (waveTail.length == 0)) {
                        wavePart = wavePart.Ready;
                        debug(2, "Switched state to " + wavePart.toString());
                    }
                    if ((bitCounter == dataLength) && (waveTail.length > 0)) {
                        wavePart = wavePart.Tail;
                        debug(2, "Switched state to " + wavePart.toString());
                    }
                } else {
                    wavePart = wavePart.Idle;
                    debug(2, "Switched state to " + wavePart.toString());
                }
                break;
            }

            case Tail: {
                //TODO: addera koden som behövs här
                break;
            }
            case Ready: {
                wavePart = WavePart.Idle;
                debug(2, "Switched state to " + wavePart.toString());
                decode(data);
                debug(1, Long.toBinaryString(data));
                break;
            }
        }
        return wavePart.ordinal();
    }

    private void addBit(int b) {
        data <<= 1;
        data |= b;
        bitCounter++;
    }

    public int[] bitSplit(long data, int[] splitPosition) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        int pos = splitPosition.length-1;
        int x =0, y=0 ;
       
        while (data > 0){
            x = (int)(Math.pow(2, splitPosition[pos]))-1;
            y = (int)(data & x);
            //System.out.println(Long.toBinaryString(data));
            //System.out.println(x);
            //System.out.println(Long.toBinaryString(y));
            result.add(y);
            data >>= splitPosition[pos];
            if (pos > 0) pos++;
                       
        }
        
        int resultArray[] = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            resultArray[i] = result.get(i);
        }
        
        return resultArray;
    }
}
