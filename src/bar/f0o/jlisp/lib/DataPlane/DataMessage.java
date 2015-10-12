/******************************************************************************
 * Copyright (c) 2015 by                                                      *
 * Andreas Stockmayer <stockmay@f0o.bar> and                                  *
 * Mark Schmidt <schmidtm@f0o.bar>                                            *
 *                                                                            *
 * This file (DataMessage.java) is part of JLISP.                                *
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

package bar.f0o.jlisp.lib.DataPlane;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import bar.f0o.jlisp.lib.Net.IPPacket;
import sun.misc.IOUtils;
import sun.nio.ch.IOUtil; 

/**
 * Dataplane Message
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |N|L|E|V|I|flags|            Nonce/Map-Version                  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |             Instance ID / Locator-Status-Bits                 |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class DataMessage {

    /**
     * N: Nonce-present bit.
     * L: The L-bit is the 'Locator-Status-Bits' field enabled bit.
     * E: Echo-nonce-request bit. This bit MUST be ignored if N-bit is set to 0 otherwise indicates wether NONCE should be sent back.
     * V: Map-Version present bit.  When this bit is set to 1, the N-bit MUST be 0.
     * I: Instance ID bit. When this bit is set to 1, the 'Locator-Status-Bits' field is reduced to 8 bits and the high-order 24 bits are used as an Instance ID.
     * flags: reserved for future use.
     * Nonce: The LISP 'Nonce' field is a 24-bit value that is randomly generated by an ITR
     * Locator-Status-Bits: 'Locator-Status-Bits' field in the LISP header is set by an ITR to indicate to an ETR the up/down status of the Locators in the source site.
     * <p>
     * When doing ITR/PITR encapsulation:
     *   o  The outer-header 'Time to Live' field (or 'Hop Limit' field, in the case of IPv6) SHOULD be copied from the inner-header 'Time to Live' field.
     *   o  The outer-header 'Type of Service' field (or the 'Traffic Class' field, in the case of IPv6) SHOULD be copied from the inner-header 'Type of Service' field (with one exception; see below).
     *
     * When doing ETR/PETR decapsulation:
     *   o  The inner-header 'Time to Live' field (or 'Hop Limit' field, in the case of IPv6) SHOULD be copied from the outer-header 'Time to Live' field, when the Time to Live value of the outer header is less than the Time to Live value of the inner header.  Failing to perform this check can cause the Time to Live of the inner header to increment across encapsulation/decapsulation cycles.  This check is also performed when doing initial encapsulation, when a packet comes to an ITR or PITR destined for a LISP site.
     *   o  The inner-header 'Type of Service' field (or the 'Traffic Class' field, in the case of IPv6) SHOULD be copied from the outer-header 'Type of Service' field (with one exception; see below).
     * </p>
     */
    private static final byte type = 2;

    private boolean nBit, lBit, eBit, vBit, iBit;
    private int nonce;
    private int lsBits;
    private int instanceID;
    private IPPacket payload;

    
    public DataMessage(byte[] data) {
		byte flags = data[0];
		this.nBit = (flags & 64) != 0;
        this.lBit = (flags & 32) != 0;
        this.eBit = (flags & 16) != 0;
        this.vBit = (flags & 8)  != 0;
        this.iBit = (flags & 4)  != 0;
        this.nonce = (data[1] << 16) + (data[2] << 8) + data[3];
        if (!this.iBit) {
            this.instanceID = 0;
            this.lsBits = (data[4] << 24)+(data[5] << 16)+(data[6] <<8)+data[7];
        }
        else {
            this.instanceID = (data[4] << 16)+(data[5] <<8)+data[6];
            this.lsBits =  data[7];
        }
        byte[] pl = new byte[data.length-8];
        System.arraycopy(data, 8, pl, 0, pl.length);
        this.payload = IPPacket.fromByteArray(pl);
	}
    
    public DataMessage(boolean nBit, boolean lBit, boolean eBit, boolean vBit, boolean iBit, int nonce, int lsBits, IPPacket payload) {
        this(nBit, lBit, eBit, vBit, iBit, nonce, 0, lsBits,  payload);
    }
 
    public DataMessage(boolean nBit, boolean lBit, boolean eBit, boolean vBit, boolean iBit, int nonce, int instanceId, int lsBits, IPPacket payload) {
        this.nBit = nBit;
        this.lBit = lBit;
        this.eBit = eBit;
        this.vBit = vBit;
        this.iBit = iBit;
        this.nonce = nonce;
        this.instanceID = instanceId;
        this.lsBits = lsBits;
        this.payload = payload;
    }

    
    
    
	public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        try {
            byte flagsTypeTmp = 0;
            if (this.nBit)
                flagsTypeTmp |= 128;
            if (this.lBit)
                flagsTypeTmp |= 64;
            if (this.eBit)
                flagsTypeTmp |= 32;
            if (this.vBit)
               flagsTypeTmp |= 16;
            if ( this.iBit)
               flagsTypeTmp |= 8;
            int flagsAndNonce = (flagsTypeTmp << 24) + (this.nonce & 0b0000000011111111111111111111111111111111);
            stream.writeInt(flagsAndNonce);
            if (this.iBit) {
                int instAndLS = (this.instanceID << 8) + (this.lsBits & 0b00000000000000000000000011111111);
                stream.writeInt(instAndLS);
            } else
                stream.writeInt(this.lsBits);
            stream.write(payload.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteStream.toByteArray();
    }

    @Override
    public String toString() {
        String ret = "DataMessage [nBit=" + nBit + ", lBit=" + lBit + ", eBit=" + eBit + ", vBit=" + vBit + ", iBit=" + iBit
                     + ", nonce=" + nonce + ", instanceID=" + instanceID
                     + ", lsBits=" + lsBits + ", payload=" + payload;
        return ret;
    }
	public boolean isnBit() {
		return this.nBit;
	}
	public boolean islBit() {
		return this.lBit;
	}
	public boolean iseBit() {
		return this.eBit;
	}
	public boolean isvBit() {
		return this.vBit;
	}
	public boolean isiBit() {
		return this.iBit;
	}
	public int getNonce() {
		return this.nonce;
	}
	public int getLsBits() {
		return this.lsBits;
	}
	public int getInstanceID() {
		return this.instanceID;
	}
	public IPPacket getPayload() {
		return this.payload;
	}


}
