package bar.f0o.jlisp.lib.Net;

public abstract class IPPacket {

	public abstract byte[] toByteArray();
	public abstract void addPayload(IPPayload payload);
}
