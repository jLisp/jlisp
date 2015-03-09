package bar.f0o.jlisp.lib.Net;

import java.io.DataInputStream;

public class ICMPPacket extends IPPayload{

	public ICMPPacket(DataInputStream stream) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] toByteArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte getProtocol() {
		return IPPayload.ICMP;
	}

}
