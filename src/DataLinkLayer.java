import java.util.*;
import java.net.*;
import java.io.*;

/**
 * class DataLinkLayer simulates data link layer
 */
class DataLinkLayer{

	/**
	 * underlying physical layer
	 */
	SimPhy simPhy;
	
	/**
	 * mac address of source in byte format 
	 */
	byte srcMac;
	
	/**
	 * buffer containing data from physical layer to data link layer
	 */
	Buffer<ByteArray> phy2dll;
	
	/**
	 * buffer containing data from data link layer to physical layer
	 */
	Buffer<ByteArray> dll2phy;
	
	/**
	 * buffer containing data from data link layer to network layer
	 */
	Buffer<ByteArray> dll2nl;
	
	/**
	 * buffer containing data from network layer to data link layer
	 */
	Buffer<ByteArray> nl2dll;	
	
	/**
	 * buffer size of frames
	 */
	public static int frameBufferSize=10;
	
	/**
	 * buffer size of packets
	 */
	public static int packetBufferSize=10;
	
	/**
	 * broadcast mac address in byte format
	 */
	public static byte BROADCAST_MAC=(byte)255;
	
	
	/**
	 * port id
	 */
	String portId="";

	/**
	 * @param deviceId device id
	 * @param pId port id
	 * @param otherId port id of the device at the other end
	 * @param mac mac address in byte format
	 * @param d buffer of frames
	 * @param n buffer of packets
	 */
	DataLinkLayer(String deviceId, String pId, byte mac, Buffer<ByteArray> d, Buffer<ByteArray> n){			
		portId=pId;
		dll2nl=d;
		nl2dll=n;
		srcMac=mac;

		phy2dll=new Buffer<ByteArray>("Phy2Dll Buffer",1);
		dll2phy=new Buffer<ByteArray>("Dll2Phy Buffer",1);
				
		String deviceWithPortId=deviceId;
		if (portId!="") deviceWithPortId=deviceId+"-"+portId;
		simPhy=new SimPhy(deviceWithPortId, phy2dll, dll2phy);			
		
		
		//if (srcMac==0) simPhy.setPortStatus(false); //a port will be down without mac address; default could have been used instead;

		
		
		//System.out.println("Interface Mac: "+mac);
		new DllSend(this);
		new DllReceive(this);
	}
	/**
	 * get port status
	 */
	public boolean getPortStatus(){return simPhy.getPortStatus();}
	/**
	 * set port status
	 *
	 * @param s true if port is up and false otherwise
	 */
	public void setPortStatus(boolean s){simPhy.setPortStatus(s);}
	/**
	 * set mac address
	 *

	 * @param s true if port is up and false otherwise
	 */
	public void setMacAddress(byte macAddress){
		srcMac=macAddress;		
		//simPhy.setPortStatus(true);
	}
	/**
	 * get mac address
	 *
	 * @return mac address
	 */
	byte getMacAddress(){return srcMac;}

	/**
	 * receives ByteArray from network layer
	 *
	 * @return byte array from network layer
	 */
	public ByteArray receiveFromNl(){
		try{
				synchronized(nl2dll){
					if (nl2dll.empty()) nl2dll.wait();	
					ByteArray p=nl2dll.get();
					nl2dll.notify();
					return p;
				}												
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * sends frame to physical layer
	 *
	 * @param f frame to be sent to physical layer
	 */
	public void sendToPhy(Frame f){		
		try{
				synchronized(dll2phy){
					if (dll2phy.full()) dll2phy.wait();	
					dll2phy.store(new ByteArray(f.getBytes()));
					dll2phy.notify();
				}											
		}
		catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	/**
	 * sends packets to network layer
	 * 
	 * @param p packet to be sent
	 */
	public void sendToNl(byte[] p){		
		try{
				synchronized(dll2nl){
					if (dll2nl.full()) dll2nl.wait();					
					ByteArray b=new ByteArray(p);					
					dll2nl.store(b);
					dll2nl.notify();
				}
				//System.out.println("\tPacket sent to NL");	
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * receives frame from physical layer
	 *
	 * @return frame from physical layer
	 */
	public Frame receiveFromPhy(){		
		try{
			synchronized(phy2dll){
				if (phy2dll.empty()) phy2dll.wait();	
				ByteArray b=phy2dll.get();
				Frame f=new Frame(b.getBytes());;										
				phy2dll.notify();
				return f;	
			}											
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}	
	
	/**
	 * class DllSend receives packets from network layer and sends frame to data link layer
	 */
	private class DllSend extends Thread{
		DataLinkLayer dll;		
		public DllSend(DataLinkLayer d){
			dll=d;			
			start();
		}
		public void run(){
			try{
				while (true){
					//ByteArray p=dll.receiveFromNl();
					ByteArray pktWithDstMac=receiveFromNl();
											
					byte[] p=pktWithDstMac.getAt(1,pktWithDstMac.getSize()-1);
					int dstMac=pktWithDstMac.getByteVal(0);

					Frame f=new Frame(srcMac, dstMac, p); /*create frame	*/
	  				sendToPhy(f);	/* transmit the frame */

					//debug message	  
					if(portId.compareTo("")==0) System.out.println("Frame Sent: "+f.getString()+"\n");
					else System.out.println("Sent Frame Through Port= "+portId+" : "+f.getString()+"\n");
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * class DllReceive receives frames from physical layer
	 */
	private class DllReceive extends Thread{
		DataLinkLayer dll;		
		public DllReceive(DataLinkLayer d){
			dll=d;			
			start();
		}
		public void run(){
			try{
				while(true){
					//Frame f=dll.receiveFromPhy();
					Frame f=receiveFromPhy();
					//debug message
					if(portId.compareTo("")==0)System.out.println("\tFrame Received: "+f.getString()+"\n");
					else System.out.println("\tReceived Frame on Port= "+portId+" : "+f.getString()+"\n");
					
					//check if the frame is valid and if so, send to upper layer
					if(f.getDstMac()==srcMac || f.getDstMac()==BROADCAST_MAC){
						if (f.hasCheckSumError()){
							System.out.println("\tChecksum Erron in Frame: "+f.getString()+"\n");
						}
						else sendToNl(f.getPayload());			
					}
					else System.out.println("\tMac Mismatch. Dropping Frame: "+f.getString()+"\n");	
					
					/*synchronized(dll){
						dll.receiveHandler(f);						
					}*/
				}
			}
			catch(Exception e){}
		}
	}		
}
