/******************************************************************************
 * Copyright (c) 2015 by                                                      *
 * Andreas Stockmayer <stockmay@f0o.bar> and                                  *
 * Mark Schmidt <schmidtm@f0o.bar>                                            *
 *                                                                            *
 * This file (IPv4Locator.java) is part of JLISP.                             *
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

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by schmidtm on 04.02.15.
 */
public class IPv4Locator implements Locator {
    private byte[] locator = new byte[4];

    public IPv4Locator(DataInputStream stream) throws IOException {
        stream.read(locator);
    }

    @Override
    public ControlMessage.AfiType getType() {
        return ControlMessage.AfiType.IPv4;
    }

    @Override
    public byte[] toByteArray() {
        return locator;
    }

    @Override
    public String toString() {
        return String.format("|%15s|\n", this.locator);
    }
}
