package bar.f0o.jlisp.lib.Net;

public abstract class IPPayload {

	public static final int UDP=17;
	public static final byte ICMP = 0;

	public abstract byte[] toByteArray();

	public abstract int getLength();

	public abstract byte getProtocol();
	
}
