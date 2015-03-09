/******************************************************************************
 * Copyright (c) 2015 by                                                      *
 * Andreas Stockmayer <stockmay@f0o.bar> and                                  *
 * Mark Schmidt <schmidtm@f0o.bar>                                            *
 *                                                                            *
 * This file (MapReply.java) is part of JLISP.                                *
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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Map Reply
 * 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |Type=2 |P|E|S|          Reserved               | Record Count  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Nonce . . .                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         . . . Nonce                           |
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   |                          Record TTL                           |
 * |   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * R   | Loc Count | EID mask-len  | ACT |A|      Reserved         |
 * e   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * c   | Rsvd  |  Map-Version Number   |       EID-Prefix-AFI          |
 * o   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * r   |                          EID-Prefix                           |
 * d   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  /|    Priority   |    Weight     |  M Priority   |   M Weight    |
 * | L +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | o |        Unused Flags     |L|p|R|           Loc-AFI             |
 * | c +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  \|                             Loc                           |
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * <p/>
 * <p/>
 * <p/>
 * <p/>
 * S-Bit 1: (For further study)
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    AD Type    |       Authentication Data Content . . .       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class MapReply implements ControlMessage {

    /**
     * P: probe bit: set if response to locator reachability probe
     * E: Site is enabled for echo-nonce-locator-reachability algorithm
     * S: Security bit if set additional Security information is Attached at the end
     * Record Count: Number of records in this reply
     * Nonce: 64bit Value from Map-Request or 24 bit value set in a data probe packet
     */
    private static final byte type = 2;

    private boolean pBit, eBit, sBit;
    private long  nonce;
    private byte  recordCount;
    private short reserved;
    private ArrayList<Record> records = new ArrayList<>();

    //optional
    private byte adType;
    private int  adContent;


    public MapReply(DataInputStream stream) throws IOException {
        byte flags = stream.readByte();
        this.sBit = (flags & 1) != 0;
        this.eBit = (flags & 2) != 0;
        this.pBit = (flags & 4) != 0;
        this.reserved = stream.readShort();
        this.recordCount = stream.readByte();
        this.nonce = stream.readLong();
        for (int i = 0; i < this.recordCount; i++)
            records.add(new Record(stream));
        if (sBit) {
            int ad = stream.readInt();
            adType = (byte) (ad >> 24);
            adContent = ad & 0x00FFFFFF;
        }
    }

    public MapReply(boolean pBit, boolean eBit, boolean sBit, long nonce, ArrayList<Record> records) {
        this.sBit = sBit;
        this.eBit = eBit;
        this.pBit = pBit;
        this.recordCount = (byte) records.size();
        this.nonce = nonce;
        this.records = records;
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        try {
            short flagsTypeTmp = 16; //Type = 2
            if (this.sBit)
                flagsTypeTmp |= 1;
            if (this.eBit)
                flagsTypeTmp |= 2;
            if (this.pBit)
                flagsTypeTmp |= 4;
            stream.writeShort(flagsTypeTmp);
            stream.writeShort(this.reserved);
            stream.writeByte(this.recordCount);
            stream.writeLong(this.nonce);
            for (Record r : records)
                stream.write(r.toByteArray());
            if (sBit) {
                stream.writeInt(((adType << 24) | adContent));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteStream.toByteArray();
    }

    @Override
    public String toString() {
        String ret = "MapReply [pBit=" + pBit + ", eBit=" + eBit + ", sBit=" + sBit
                     + ", nonce=" + nonce + ", recordCount=" + recordCount
                     + ", reserved=" + reserved + ", records=";
        for (Record rec : records)
            ret += "{" + rec.toString() + "}";
        ret += ", adType=" + adType + ", adContent=" + adContent + "]";
        return ret;
    }

    //Getter

    public static byte getType() {
        return type;
    }

    public boolean ispBit() {
        return pBit;
    }

    public boolean iseBit() {
        return eBit;
    }

    public boolean issBit() {
        return sBit;
    }

    public long getNonce() {
        return nonce;
    }

    public byte getRecordCount() {
        return recordCount;
    }

    public short getReserved() {
        return reserved;
    }

    public ArrayList<Record> getRecords() {
        return records;
    }

    public byte getAdType() {
        return adType;
    }

    public int getAdContent() {
        return adContent;
    }


}
