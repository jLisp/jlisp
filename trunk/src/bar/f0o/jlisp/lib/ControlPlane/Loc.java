/******************************************************************************
 * Copyright (c) 2015 by                                                      *
 * Andreas Stockmayer <stockmay@f0o.bar> and                                  *
 * Mark Schmidt <schmidtm@f0o.bar>                                            *
 *                                                                            *
 * This file (Loc.java) is part of JLISP.                                     *
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

/**
 * Loc fuer Message
 * 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-|
 * /|    Priority   |    Weight     |  M Priority   |   M Weight     |
 * L +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * o |        Unused Flags     |L|p|R|           Loc-AFI             |
 * c +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  \|                             Locator                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */


public class Loc {
    /**
     * Priority: smaller -> higher priority 255 means not used for unicast forwarding
     * Weight: Balance of traffic when priority is the same
     * M-Priority: Priority for multicast
     * M-Weight: Weight for unicast
     * Unused Flags:0
     * L: local locator (0 if proxy reply)
     * p: for probing, this is the probed locator
     * R: Sender has route to the locator in the locator record
     * Loc-afi: Type of the address IPv4 IPv6
     * Locator: IPv4 or IPv6 address assigned to an ETR
     */
    private byte priority, weight, mPriority, mWeight;
    private boolean lFlag, pFlag, rFlag;
    private AfiType locAFI;
    private Locator locator;


    private Loc() {
    }

    public Loc(DataInputStream stream) throws IOException {
        this.priority = stream.readByte();
        this.weight = stream.readByte();
        this.mPriority = stream.readByte();
        this.mWeight = stream.readByte();
        short flags = stream.readShort();
        this.rFlag = (flags & 1) != 0;
        this.pFlag = (flags & 2) != 0;
        this.lFlag = (flags & 4) != 0;
        int afiType = stream.readShort();
        this.locAFI = AfiType.fromInt(afiType);

        if (this.locAFI == AfiType.IPv4)
            this.locator = new IPv4Locator(stream);
        else if (this.locAFI == AfiType.IPv6)
            this.locator = new IPv6Locator(stream);
        else if (this.locAFI == AfiType.LCAF)
            this.locator = new LCAFLocator(stream);
        else
            throw new IOException("Wrong AfiType");
        //     this.lFlag = stream.readBoolean();
        //     this.pFlag = stream.readBoolean();
    }

    public Loc(byte priority, byte weight, byte mPriority, byte mWeight, boolean lFlag, boolean pFlag,
               boolean rFlag,
               AfiType locAFI, Locator locator) {
        this.priority = priority;
        this.weight = weight;
        this.mPriority = mPriority;
        this.mWeight = mWeight;
        this.rFlag = rFlag;
        this.pFlag = pFlag;
        this.lFlag = lFlag;
        this.locAFI = locAFI;
        this.locator = locator;
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        try {
            stream.writeByte(this.priority);
            stream.writeByte(this.weight);
            stream.writeByte(this.mPriority);
            stream.writeByte(this.mWeight);
            short flags = 0;
            if (rFlag)
                flags |= 0b0000000000000001;
            if (pFlag)
                flags |= 0b000000000000010;
            if (lFlag)
                flags |= 0b000000000000100;
            stream.writeShort(flags);
            stream.writeByte(this.locAFI.getVal());
            stream.write(this.locator.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteStream.toByteArray();
    }

    @Override
    public String toString() {
        String res = "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n";
        res += "|Priority|Weight  |M-Priority|M-Weight|\n";
        res += String.format("|%8d|%8d|%10d|%8d|\n", this.priority, this.weight, this.mPriority, this.mWeight);
        res += "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n";
        res += "|Unused-Flag|L|p|R|Loc-Afi            |\n";
        res += String.format("|%11d|%1d|%1d|%1d|%19d|\n", 0, lFlag ? 1 : 0, pFlag ? 1 : 0, rFlag ? 1 : 0,
                             locAFI.getVal());
        res += "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n";
        res += "|             Locator                 |\n";
        res += String.format("|%37s|\n", this.locator);
        res += "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n";
        return res;
    }


    public byte getPriority() {
        return priority;
    }

    public byte getWeight() {
        return weight;
    }

    public byte getmPriority() {
        return mPriority;
    }

    public byte getmWeight() {
        return mWeight;
    }

    public boolean islFlag() {
        return lFlag;
    }

    public boolean ispFlag() {
        return pFlag;
    }

    public boolean isrFlag() {
        return rFlag;
    }

    public AfiType getLocAFI() {
        return locAFI;
    }

    public Locator getLocator() {
        return locator;
    }


}
