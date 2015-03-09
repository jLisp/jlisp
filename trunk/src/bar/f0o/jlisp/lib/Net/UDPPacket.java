package bar.f0o.jlisp.lib.Net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class UDPPacket extends IPPayload {

	private short srcPort;
	private short dstPort;
	private short length;
	private short checksum;
	private byte[] payload;
	
	public UDPPacket(byte[] srcAddresss,short srcPort,byte[] dstAddress,short dstPort,byte[] payload){
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.payload = payload;
		this.length = (short) (8 + payload.length);
		if(srcAddresss.length == 4)
			generateChecksumV4(srcAddresss, dstAddress);
		else if(srcAddresss.length == 32)
			generateChecksumV6(srcAddresss, dstAddress);
		else{
			System.err.println("Wrong number of octets in src address");
		}
	}
	
	
	public UDPPacket(DataInputStream stream) throws IOException {
		this.srcPort = stream.readShort();
		this.dstPort = stream.readShort();
		this.length = stream.readShort();
		this.checksum = stream.readShort();
		stream.readFully(payload);
	}

	@Override
	public byte[] toByteArray() {
		 ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(byteStream);
	        try {
	        	stream.writeShort(srcPort);
	            stream.writeShort(dstPort);
	            stream.writeShort(length);
	            stream.writeShort(checksum);
	            stream.write(payload);
	            System.out.println("payload");
	        } catch (IOException e) {
	            e.printStackTrace();
	        }

	        return byteStream.toByteArray();	
	}

	@Override
	public int getLength() {
		return (payload.length/4)+2;
	}

	@Override
	public byte getProtocol() {
		return IPPayload.UDP;
	}
	
	private void generateChecksumV4(byte[] src,byte[] dst){
		int sum = 0;
		sum += (short) ((src[1] + (src[0] << 8)) ^ 0xFFFF); 
		sum += (short) ((dst[1] + (dst[0] << 8)) ^ 0xFFFF); 
		sum += ((short) 17) ^ 0xFFFF;
		sum += this.length ^ 0xFFFF;
		for(int i=0;i<payload.length;i+=2){
			if(i+1 == payload.length)
				sum += (payload[i] << 16) ^ 0xFFFF;
			else
				sum += ((payload[i] << 16) +payload[i+1]) ^ 0xFFFF;
		}
		while((sum & 0xFFFF0000) != 0){
			int carry = (sum & 0xFFFF0000) >> 16;
		    sum &= 0x0000FFFF;
		    sum += carry;
		}
		
	}
	
	private void generateChecksumV6(byte[] src,byte[] dst){
		
	}
}
