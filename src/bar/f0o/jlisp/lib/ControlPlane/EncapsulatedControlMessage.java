/******************************************************************************
 * Copyright (c) 2015 by                                                      *
 * Andreas Stockmayer <stockmay@f0o.bar> and                                  *
 * Mark Schmidt <schmidtm@f0o.bar>                                            *
 *                                                                            *
 * This file (EncapsulatedControlMessage.java) is part of jlisp.              *
 *                                                                            *
 * jlisp is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * jlisp is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the                *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with $project.name.If not, see <http://www.gnu.org/licenses/>.       *
 ******************************************************************************/

package bar.f0o.jlisp.lib.ControlPlane;

import bar.f0o.jlisp.lib.Net.IPPacket;
import bar.f0o.jlisp.lib.Net.IPv4Packet;
import bar.f0o.jlisp.lib.Net.UDPPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EncapsulatedControlMessage extends ControlMessage {

    /**
     * 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |Type=8 |S|                  Reserved                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * / |                       IPv4 or IPv6 Header                     |
     * IH  |                  (uses RLOC or EID addresses)                 |
     * \ |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * / |       Source Port = xxxx      |       Dest Port = yyyy        |
     * UDP +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * \ |           UDP Length          |        UDP Checksum           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                      LISP Control Message                     |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    

    private ControlMessage message;
    private byte[]         srcAddr;
    private byte[]         dstAddr;
    private short          srcPort;
    private short          dstPort;
    private boolean        sBit;

    @SuppressWarnings("unused")
    private EncapsulatedControlMessage() {
    }

    public EncapsulatedControlMessage(byte[] srcAddr, byte[] dstAddr, short srcPort, short dstPort,
                                      ControlMessage message) {
        this.srcAddr = srcAddr;
        this.dstAddr = dstAddr;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.message = message;
    }

    public EncapsulatedControlMessage(DataInputStream stream, byte version) throws IOException{
    	this.type = 8;
    	this.sBit = (version&0b00001000)!=0;
    	stream.skipBytes(3);
    	IPv4Packet innerIP = new IPv4Packet(stream);
    	this.srcAddr = innerIP.getSrcIP();
    	this.dstAddr = innerIP.getDstIP();
    	UDPPacket innerUDP = new UDPPacket(innerIP.getPayload().toByteArray());
    	this.srcPort = innerUDP.getSrcPort();
    	this.dstPort = innerUDP.getDstPort();
    	DataInputStream innerControl = new DataInputStream(new ByteArrayInputStream(innerUDP.getPayload()));
    	message = ControlMessage.fromStream(innerControl);
    }


    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        try {
            int type = 0b10000000000000000000000000000000;
            stream.writeInt(type);
            UDPPacket packet = new UDPPacket(srcAddr, srcPort, dstAddr, dstPort, message.toByteArray());
            IPPacket ippacket = new IPv4Packet(srcAddr, dstAddr);
            ippacket.addPayload(packet);
            stream.write(ippacket.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteStream.toByteArray();
    }

    

    public ControlMessage getMessage() {
        return message;
    }

    public byte[] getSrcAddr() {
        return srcAddr;
    }

    public byte[] getDstAddr() {
        return dstAddr;
    }

    public short getSrcPort() {
        return srcPort;
    }

    public short getDstPort() {
        return dstPort;
    }

    public boolean issBit() {
        return sBit;
    }


}
