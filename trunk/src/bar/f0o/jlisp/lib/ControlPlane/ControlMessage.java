/******************************************************************************
 * Copyright (c) 2015 by                                                      *
 * Andreas Stockmayer <stockmay@f0o.bar> and                                  *
 * Mark Schmidt <schmidtm@f0o.bar>                                            *
 *                                                                            *
 * This file (ControlMessage.java) is part of JLISP.                          *
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

/**
 * Control Message
 */
public interface ControlMessage {

    //AFI Type
    public enum AfiType {
        NONE(0),
        IPv4(1),
        IPv6(2),
        LCAF(16387);

        private final int val;

        private AfiType(int x) {
            this.val = x;
        }

        public int getVal() {
            return val;
        }

        public static AfiType fromInt(int x) {
            switch (x) {
                case 0:
                    return NONE;
                case 1:
                    return IPv4;
                case 2:
                    return IPv6;
                case 16387:
                    return LCAF;

            }
            return null;
        }


        //Get number of octets
        public static int length(AfiType a) {
            switch (a) {
                case NONE:
                    return 0;
                case IPv4:
                    return 4;
                case IPv6:
                    return 16;
                case LCAF:
                    return 16387;
            }
            return -1;
        }

        public static int length(int a) {
            return length(fromInt(a));
        }
    }


    public byte[] toByteArray();
}
