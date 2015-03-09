/******************************************************************************
 * Copyright (c) 2015 by                                                      *
 * Andreas Stockmayer <stockmay@f0o.bar> and                                  *
 * Mark Schmidt <schmidtm@f0o.bar>                                            *
 *                                                                            *
 * This file (Record.java) is part of JLISP.                                  *
 *                                                                            *
 * JLISP is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * JLISP is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with JLISP.  If not, see <http://www.gnu.org/licenses/>.             *
 ******************************************************************************/

package bar.f0o.jlisp.lib.ControlPlane;

import bar.f0o.jlisp.lib.ControlPlane.ControlMessage.AfiType;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Record for Control Messages
 * 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   |                          Record TTL                           |
 * |   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * R   | Loc Count | EID mask-len  | ACT |A|      Reserved         |
 * e   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * c   | Rsvd  |  Map-Version Number   |        EID-Prefix-AFI         |
 * o   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * r   |                          EID-Prefix                           |
 * d   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  /|    Priority   |    Weight     |  M Priority   |   M Weight    |
 * | L +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | o |        Unused Flags     |L|p|R|           Loc-AFI             |
 * | c +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  \|                             Loc                           |
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Record {

    /**
     * Record TTL: Store for n minutes 0 means removed immediately 0xffffffff recipient can decide
     * Loc-Count: Number of loc entries: 0 no entries
     * EID-Mask-Len: mask length for the prefix
     * ACT: For negative replies:
     * (0) No-Action:  The map-cache is kept alive, and no packet
     * encapsulation occurs.
     * <p/>
     * (1) Natively-Forward:  The packet is not encapsulated or dropped
     * but natively forwarded.
     * <p/>
     * (2) Send-Map-Request:  The packet invokes sending a Map-Request.
     * <p/>
     * (3) Drop:  A packet that matches this map-cache entry is dropped.
     * An ICMP Destination Unreachable message SHOULD be sent.
     * A: set to 1 by ETR set to 0 for proxy reply
     * Map-Version-Number: if not 0 informs what version number of the eid is contained
     * EID-Prefix-AFI: Family of the address (1 ipv4, 2 ipv6)
     * EID-Prefix: Prefix either IPv4 or IPv6
     */

    private int  recordTTL;
    private byte locatorCount, eidMaskLen, act, rsvd;
    private boolean aFlag;
    private short   reserved;
    private short   versionNumber;
    private AfiType eidPrefixAfi;
    private byte[]  eidPrefix;
    private ArrayList<Loc> locs = new ArrayList<>();

    @SuppressWarnings("unused")
    private Record() {
    }

    public Record(DataInputStream stream) throws IOException {
        this.recordTTL = stream.readInt();
        this.locatorCount = stream.readByte();
        this.eidMaskLen = stream.readByte();
        short reservedTmp = stream.readShort(); //TODO
        this.reserved = (short) (reservedTmp & 0b0000111111111111);
        this.act = (byte) ((reservedTmp & 0b1110000000000000) >> 14);
        this.aFlag = (reservedTmp & 0b0001000000000000) != 0;
        this.versionNumber = stream.readShort();
        this.eidPrefixAfi = AfiType.fromInt(stream.readShort());
        byte[] buffer = new byte[AfiType.length(this.eidPrefixAfi)];
        stream.read(buffer);
        this.eidPrefix = buffer;
        for (int i = 0; i < this.locatorCount; i++)
            this.locs.add(new Loc(stream));
    }

    public Record(int recordTTL, byte eidMaskLen, byte act, boolean aFlag, short versionNumber, AfiType eidPrefixAfi,
                  byte[] eidPrefix, ArrayList<Loc> locs) {
        this.recordTTL = recordTTL;
        this.locatorCount = (byte) locs.size();
        this.eidMaskLen = eidMaskLen;
        this.act = act;
        this.aFlag = aFlag;
        this.versionNumber = versionNumber;
        this.eidPrefixAfi = eidPrefixAfi;
        this.eidPrefix = eidPrefix;
        this.locs = locs;
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        try {
            stream.writeInt(this.recordTTL);
            stream.writeByte(this.locatorCount);
            stream.writeByte(this.eidMaskLen);
            short reservedTmp = (short) (reserved | (act << 14));
            if (aFlag)
                reservedTmp |= 0b0010000000000000;
            stream.writeByte(reservedTmp); //TODO
            short versionNumberTmp = (short) (versionNumber | rsvd);
            stream.writeShort(versionNumberTmp);
            stream.writeShort(this.eidPrefixAfi.getVal());
            stream.write(this.eidPrefix);
            for (Loc loc : locs)
                stream.write(loc.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteStream.toByteArray();
    }
 /*
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                          Record TTL                           |
 *     |-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-|
 * R   | Loc Count | EID mask-len  | ACT |A|      Reserved         |
 * e   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * c   | Rsvd  |  Map-Version Number   |        EID-Prefix-AFI         |
 * o   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * r   |                          EID-Prefix                           |
 * d   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  /|    Priority   |    Weight     |  M Priority   |   M Weight    |
 * | L +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | o |        Unused Flags     |L|p|R|           Loc-AFI             |
 * | c +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  \|                             Loc                           |
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */

    @Override
    public String toString() {
        String ret = "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n";
        ret += "|               Record-TTL            |\n";
        ret += String.format("|%37d|\n", this.recordTTL);
        ret += "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n";
        ret += "|Loc-Count|Eid mask-len|ACT|A|Reserved|\n";
        ret += String.format("|%9d|%12d|%3d|%1d|%8d|\n", this.locatorCount, this.eidMaskLen, this.act, aFlag ? 0 : 1,
                             this.reserved);
        ret += "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n";
        ret += "|Rsvd|Map-Version-nr|  Eid-Prefix-Afi |\n";
        ret += String.format("|%4d|%14d|%17d|\n", this.rsvd, this.versionNumber, this.eidPrefixAfi.getVal());
        ret += "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n";
        ret += "|              Loc                |\n";
        try {
            ret += String.format("|%37s|\n", InetAddress.getByAddress(eidPrefix).getHostAddress());
        } catch (UnknownHostException e) {
            ret += String.format("|%37s|", "Not IPv4 or IPv6");
        }
        ret += "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n";
        ret += "\n||||||||||\n|Locators|\n||||||||||\n";
        for (Loc loc : locs) {
            ret += "\n";
            ret += loc.toString();
        }
        ret += "\n";
        return ret;
    }

    public int getRecordTTL() {
        return recordTTL;
    }

    public byte getLocatorCount() {
        return locatorCount;
    }

    public byte getEidMaskLen() {
        return eidMaskLen;
    }

    public byte getAct() {
        return act;
    }

    public byte getRsvd() {
        return rsvd;
    }

    public boolean isaFlag() {
        return aFlag;
    }

    public short getReserved() {
        return reserved;
    }

    public short getVersionNumber() {
        return versionNumber;
    }

    public AfiType getEidPrefixAfi() {
        return eidPrefixAfi;
    }

    public byte[] getEidPrefix() {
        return eidPrefix;
    }

    public ArrayList<Loc> getLocs() {
        return locs;
    }


}
