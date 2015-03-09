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


package bar.f0o.jlisp.lib.tools;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import bar.f0o.jlisp.lib.ControlPlane.EncapsulatedControlMessage;
import bar.f0o.jlisp.lib.ControlPlane.Loc;
import bar.f0o.jlisp.lib.ControlPlane.MapReply;
import bar.f0o.jlisp.lib.ControlPlane.MapRequest;
import bar.f0o.jlisp.lib.ControlPlane.ControlMessage.AfiType;
import bar.f0o.jlisp.lib.ControlPlane.Rec;
import bar.f0o.jlisp.lib.ControlPlane.Record;


public class jLig {

	
	
	public static void main(String args[]) throws Exception {
		//Read parameters
		String ms = args[0];
		String eid = args[1];
		//Parse requestet EID
		byte[] eidBytes;
		AfiType eidType = AfiType.IPv4;
		if(ms.contains(":")){
			eidBytes = (Inet6Address.getByName(eid).getAddress());
			eidType = AfiType.IPv6;
		}
		else
			eidBytes = (Inet4Address.getByName(eid).getAddress());
		
		//Generate Message
		Rec r = new Rec((byte)(AfiType.length(eidType)*8), eidType, eidBytes);
		ArrayList<Rec> recs = new ArrayList<Rec>();
		recs.add(r);
		HashMap<Short,byte[]> itrs = new HashMap<Short, byte[]>();
		
		itrs.put((short) 1, Inet4Address.getLocalHost().getAddress());
		
		byte src[] = {};
		MapRequest req = new MapRequest( false,false, false, false, false,false 
				, new Random().nextLong(), 
				AfiType.NONE, src,
				itrs, recs, null);
				
		EncapsulatedControlMessage message = new EncapsulatedControlMessage(Inet4Address.getLocalHost().getAddress(),
				Inet4Address.getByName(eid).getAddress(),(short)60573,(short)4342,req);	
		
		byte[] ligBytes = message.toByteArray();
	
		//Send Message
		DatagramPacket ligPacket = new DatagramPacket(ligBytes, ligBytes.length, InetAddress.getByName(ms), 4342);
		DatagramSocket sock = new DatagramSocket(60573);
		sock.send(ligPacket);
		//Receive answer
		byte[] answer = new byte[128];
		DatagramPacket ligAnswer = new DatagramPacket(answer, answer.length);;
		sock.receive(ligAnswer);
		sock.close();
		DataInputStream answerStream = new DataInputStream(new ByteArrayInputStream(answer));

		MapReply rep = new MapReply(answerStream);
		
		//Print result
		if(rep.getRecordCount() == 0)
		{
			System.out.println("No result");
			return;
		}
		Record result = rep.getRecords().get(0);
		System.out.println("RLocs for EID: " + InetAddress.getByAddress(result.getEidPrefix()).getHostAddress());
		for(Loc loc : result.getLocs()){
			System.out.println(loc.getLocator());
		}
	
	
	}
}
