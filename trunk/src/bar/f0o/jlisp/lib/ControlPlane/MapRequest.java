/******************************************************************************
 * Copyright (c) 2015 by                                                      *
 * Andreas Stockmayer <stockmay@f0o.bar> and                                  *
 * Mark Schmidt <schmidtm@f0o.bar>                                            *
 *                                                                            *
 * This file (MapRequest.java) is part of JLISP.                              *
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
import java.util.HashMap;
import java.util.Map;


/**
 * Map Request Message
 * <p/>
 * 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |Type=1 |A|M|P|S|p|s|    Reserved     |   IRC   | Record Count  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Nonce . . .                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         . . . Nonce                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Source-EID-AFI        |   Source EID Address  ...     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         ITR-RLOC-AFI 1        |    ITR-RLOC Address 1  ...    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                              ...                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         ITR-RLOC-AFI n        |    ITR-RLOC Address n  ...    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * / |   Reserved    | EID mask-len  |        EID-Prefix-AFI         |
 * Rec +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * \ |                       EID-Prefix  ...                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                   Map-Reply Record  ...                       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class MapRequest implements ControlMessage {

    /**
     * A: Authoritative bit: 0: UDP based Map Requests bei ITR 1: Destination site should return address
     * M: Map data present set if map reply is included
     * P: if set should be treated as probe with nonce copied from request
     * S: SMR bit
     * p: PITR bit 1 if sent by pitr
     * s: smr invoked 1 if answer to smr
     * IRC: Number of (ITR-RLOC-AFI / ITR-RLOC- Address) pairs -1 max 31
     * Record count: Number of records (only one allowed for sender)
     * Nonce: 64bit random
     * Source-EID-AFI:Address family of the Source EID
     * Source EID Address: Host that originated map request (refreshing or probing AFI 0 and this field length 0)
     * ITR-RLOC-AFI: Address family of ITR
     * ITR-RLOC Address: Give ETR option to choose between address families if more than one type given
     * EID mask len: mask length for the prefix
     * EID-prefix-AFI: prefix address family
     * EID-Prefix: the request prefix
     * Map-Reply: optional reply included in Request
     */

    private static final byte TYPE = 1;
    private boolean aFlag, mFlag, pFlag, smrBit, pitrBit, smrInvoked;
    private byte irc, recordCount;
    private long    nonce;
    private AfiType sourceEidAfi;
    private byte[]  sourceEIDAddress;
    private Map<Short, byte[]> itrRlocPairs = new HashMap<>();
    private ArrayList<Rec>     recs         = new ArrayList<>();
    private MapReply reply;

    @SuppressWarnings("unused")
    private MapRequest() {
    }

    public MapRequest(DataInputStream stream) throws IOException {
        byte flags = stream.readByte();

        this.aFlag = (flags & 1) != 0;
        this.mFlag = (flags & 2) != 0;
        this.pFlag = (flags & 4) != 0;
        this.smrBit = (flags & 8) != 0;
        this.pitrBit = (flags & 16) != 0;
        this.smrInvoked = (flags & 32) != 0;
        this.irc = stream.readByte();
        this.recordCount = stream.readByte();
        this.nonce = stream.readLong();
        this.sourceEidAfi = AfiType.fromInt(stream.readShort());
        byte[] buffer = new byte[AfiType.length(this.sourceEidAfi)];
        stream.read(buffer);
        this.sourceEIDAddress = buffer;
        this.itrRlocPairs = new HashMap<>();
        for (int i = 0; i < this.irc; i++) {
            short t = stream.readShort();
            buffer = new byte[AfiType.length(AfiType.fromInt(t))];
            this.itrRlocPairs.put(t, buffer);
        }
        this.recs = new ArrayList<>();
        for (int i = 0; i < this.recordCount; i++)
            this.recs.add(new Rec(stream));
        if (this.mFlag)
            this.reply = new MapReply(stream);
    }

    public MapRequest(boolean aFlag, boolean mFlag, boolean pFlag, boolean smrBit, boolean pitrBit,
                      boolean smrInvoked, long nonce, AfiType sourceEidAfi, byte[] sourceEIDAddress,
                      Map<Short, byte[]> itrRlocPairs, ArrayList<Rec> recs, MapReply reply) {
        this.aFlag = aFlag;
        this.mFlag = mFlag;
        this.pFlag = pFlag;
        this.smrBit = smrBit;
        this.pitrBit = pitrBit;
        this.smrInvoked = smrInvoked;
        this.irc = (byte) (itrRlocPairs.size() - 1);
        this.recordCount = (byte) recs.size();
        this.nonce = nonce;
        this.sourceEidAfi = sourceEidAfi;
        this.sourceEIDAddress = sourceEIDAddress;
        this.itrRlocPairs = itrRlocPairs;
        this.recs = recs;
        this.reply = reply;
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        try {
            short flagsTypeTmp = 4096; //Type = 1
            if (this.aFlag)
                flagsTypeTmp |= 64;
            if (this.mFlag)
                flagsTypeTmp |= 128;
            if (this.pFlag)
                flagsTypeTmp |= 256;
            if (this.smrBit)
                flagsTypeTmp |= 512;
            if (this.pitrBit)
                flagsTypeTmp |= 1024;
            if (this.smrInvoked)
                flagsTypeTmp |= 2048;

            stream.writeShort(flagsTypeTmp);
            stream.writeByte(this.irc);
            stream.writeByte(this.recordCount);
            stream.writeLong(this.nonce);
            stream.writeShort(this.sourceEidAfi.getVal());
            stream.write(this.sourceEIDAddress);
            for (Map.Entry<Short, byte[]> entry : this.itrRlocPairs.entrySet()) {
                stream.writeShort(entry.getKey());
                stream.write(entry.getValue());
            }
            for (Rec rec : this.recs) {
                stream.write(rec.toByteArray());
            }
            if (reply != null)
                stream.write(this.reply.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteStream.toByteArray();
    }

    /**
     * Map Request Message
     * <p/>
     * 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |Type=1 |A|M|P|S|p|s|    Reserved     |   IRC   | Record Count  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         Nonce . . .                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         . . . Nonce                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |         Source-EID-AFI        |   Source EID Address  ...     |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |         ITR-RLOC-AFI 1        |    ITR-RLOC Address 1  ...    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                              ...                              |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |         ITR-RLOC-AFI n        |    ITR-RLOC Address n  ...    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * / |   Reserved    | EID mask-len  |        EID-Prefix-AFI         |
     * Rec +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * \ |                       EID-Prefix  ...                         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                   Map-Reply Record  ...                       |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    public String toString() {
        String rsvp = "";
        String res = ""; /* String.format("+-+\n" +
                                   "|%032d|\n" +
                                   "-+\n" +
                                   "|%08d|%08d%|\n" +
                                   "+-+\n" +
                                   "|%08d|%08d%|\n" +
                                   "+-+\n" +
                                   "|%s|\n", recordTTL, locatorCount, eidMaskLen, act, aFlag, rsvp, versionNumber,eidPrefixAfi , eidPrefix);
        for(Loc l : locs)
            res += locs.toString();   */
        return res;
    }

    //Getter

    public static byte getType() {
        return TYPE;
    }

    public boolean isaFlag() {
        return aFlag;
    }

    public boolean ismFlag() {
        return mFlag;
    }

    public boolean ispFlag() {
        return pFlag;
    }

    public boolean isSmrBit() {
        return smrBit;
    }

    public boolean isPitrBit() {
        return pitrBit;
    }

    public boolean isSmrInvoked() {
        return smrInvoked;
    }

    public byte getIrc() {
        return irc;
    }

    public byte getRecordCount() {
        return recordCount;
    }

    public long getNonce() {
        return nonce;
    }

    public AfiType getSourceEidAfi() {
        return sourceEidAfi;
    }

    public byte[] getSourceEIDAddress() {
        return sourceEIDAddress;
    }

    public Map<Short, byte[]> getItrRlocPairs() {
        return itrRlocPairs;
    }

    public ArrayList<Rec> getRecs() {
        return recs;
    }

    public MapReply getReply() {
        return reply;
    }


}