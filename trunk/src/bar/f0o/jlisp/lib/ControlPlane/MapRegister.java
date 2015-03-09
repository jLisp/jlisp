/******************************************************************************
 * Copyright (c) 2015 by                                                      *
 * Andreas Stockmayer <stockmay@f0o.bar> and                                  *
 * Mark Schmidt <schmidtm@f0o.bar>                                            *
 *                                                                            *
 * This file (MapRegister.java) is part of JLISP.                             *
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
 * Map Register
 * <p/>
 * 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |Type=3 |P|            Reserved               |M| Record Count  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Nonce . . .                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         . . . Nonce                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            Key ID             |  Authentication Data Length   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * ~                     Authentication Data                       ~
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
public class MapRegister implements ControlMessage {

    public enum HmacType {
        NONE(0), HMAC_SHA_1_96(1), HMAC_SHA_256_128(2);
        private final int val;

        private HmacType(int x) {
            this.val = x;
        }

        public int getVal() {
            return val;
        }

        public static HmacType fromInt(int x) {
            switch (x) {
                case 0:
                    return NONE;
                case 1:
                    return HMAC_SHA_1_96;
                case 2:
                    return HMAC_SHA_256_128;

            }
            return null;
        }
    }

    /**
     * P: Proxy Map Reply bit: if 1 etr sends map register requesting the map server to proxy a map reply.
     * M: Want map notify bit: if 1 ETR wants a map notify as response to this
     * Record Count: Number of records in this message
     * Nonce 64 bit (not currently used)
     * Key ID: Type of key (0 none, 1 HMAC-SHA-1-96, 2 HMAC-SHA-256-128)
     * AuthenticationDataLength length in octets of the Authentication Data
     * AuthenticationData: the key
     */
    private static final byte type = 3;

    private boolean pFlag, mFlag;
    private byte     recordCount;
    private long     nonce;
    private HmacType keyId;
    private short    authenticationDataLength;
    private byte[]   authenticationData;

    private ArrayList<Record> records = new ArrayList<>();


    public MapRegister(DataInputStream stream) throws IOException {
        byte flag = stream.readByte();
        this.pFlag = (flag & 8) != 0;
        short reserved = stream.readShort();
        this.mFlag = (reserved & 1) != 0;
        this.recordCount = stream.readByte();
        this.nonce = stream.readLong();
        short key = stream.readShort();
        this.keyId = HmacType.fromInt(key);
        this.authenticationDataLength = stream.readShort();
        this.authenticationData = new byte[authenticationDataLength];
        stream.read(authenticationData);
        for (int i = 0; i < recordCount; i++)
            this.records.add(new Record(stream));

    }

    public MapRegister(boolean pFlag, boolean mFlag, long nonce, HmacType keyId,
                       byte[] authenticationData, ArrayList<Record> records) {
        this.pFlag = pFlag;
        this.mFlag = mFlag;
        this.recordCount = (byte) records.size();
        this.nonce = nonce;
        this.keyId = keyId;
        this.authenticationDataLength = (short) authenticationData.length;
        this.authenticationData = authenticationData;
        this.records = records;
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        try {
            byte typeFlagTmp = 48;
            if (this.pFlag)
                typeFlagTmp |= 8;
            stream.writeByte(typeFlagTmp);
            short reserved = (short) (this.mFlag ? 1 : 0);
            stream.writeShort(reserved);
            stream.writeByte(this.recordCount);
            stream.writeLong(this.nonce);
            stream.writeShort(this.keyId.getVal());
            stream.writeShort(this.authenticationDataLength);
            stream.write(this.authenticationData);
            for (Record r : this.records)
                stream.write(r.toByteArray());


        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteStream.toByteArray();
    }

    public static byte getType() {
        return type;
    }

    public boolean ispFlag() {
        return pFlag;
    }

    public boolean ismFlag() {
        return mFlag;
    }

    public byte getRecordCount() {
        return recordCount;
    }

    public long getNonce() {
        return nonce;
    }

    public HmacType getKeyId() {
        return keyId;
    }

    public short getAuthenticationDataLength() {
        return authenticationDataLength;
    }

    public byte[] getAuthenticationData() {
        return authenticationData;
    }

    public ArrayList<Record> getRecords() {
        return records;
    }


}
