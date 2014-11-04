import java.util.*;
import java.net.*;
import java.io.*;

/**
 * class SimPhy simulates the physical layer
 */
class SimPhy{
	/**
	 * socket
	 */
	Socket sock;

	/**
	 * input stream
	 */
	DataInputStream br;
	
	/**
	 * output stream
	 */
	OutputStream bw;
	
	/**
	 * represents whether connection present
	 */
	boolean hasConnection=false;
	
	/**
	 * buffer to send bits from physical layer to data link layer
	 */
	Buffer<ByteArray> phy2dll;
	
	/**
	 * buffer to receive frames from data link layer to physical layer
	 */
	Buffer<ByteArray> dll2phy;
	
	/**
	 * represents whether the port is up
	 */
	boolean isUp=true;

	
	/**
	 * @param deviceId device id	 	 
	 * @param p2d buffer for data from physical layer to data link layer
	 * @param d2p buffer for data from data link layer to physical layer
	 */
	SimPhy(String deviceId, Buffer<ByteArray> p2d, Buffer<ByteArray> d2p){		
		try{			
			sock = new Socket("127.0.0.1",9009); 
			hasConnection=true;
	
			br=new DataInputStream(sock.getInputStream());			
				
			bw=sock.getOutputStream();	
			
			SimPhy.writeStuffed(bw,deviceId.getBytes());			
				
			phy2dll=p2d;
			dll2phy=d2p;
			new PhySend(this);
			new PhyReceive(this);
		}
		catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	/**
	 * updates port status
	 *
	 * @param s boolean representing status of port, true if up and false otherwise
	 */
	public void setPortStatus(boolean s){
			isUp=s;
			//System.out.println("Port Status updated for "+portId+" to "+s);
	}
	
	/**
	 * returns port status
	 * @return true if port is up and false otherwise
	 */
	public boolean getPortStatus(){return isUp;}
	
	/**
	 * adds post amble to the stuffed bits
	 */
	public static void writeStuffed(OutputStream bw, byte[] f) throws Exception{						
		try{			
			byte[] temp=SimPhy.bitStuff(f);			
			ByteArray b=new ByteArray(temp.length+2);
			b.setByteVal(0,(byte)126);//here there is only post amble. Preamble may be added
			b.setAt(1,temp);
			b.setByteVal(temp.length+1,(byte)126);//here there is only post amble. Preamble may be added			
			bw.write(b.getBytes());
		}
		catch(Exception e){
			e.printStackTrace();
			throw e;
		}
	}	
	
	/**
	 * 
	 */
	public static byte[] readDeStuffed(DataInputStream br) throws Exception{					
		byte[] b=new byte[1000]; //this size is arbitrary.
		int count=0;		
		try{
			byte i=br.readByte();			
						
			while(i!=126){}//skip as long as there is no preamble
			
			i=br.readByte();			
			while(i!=126){				
				b[count++]=i;
				//add code incase count>size of b.				
				i=br.readByte();
			}
			byte[] temp=new byte[count];		
			System.arraycopy(b,0,temp,0,count);
			
			/*demonstrate the effect of bit stuff*/
			return SimPhy.bitDeStuff(temp);
			//return temp;
		}
		catch(Exception e){
			e.printStackTrace();
			throw e; //simphy may be invoked from a while(1) loop and everytime there will be an exception. Therefore to avoid it, send some sort of feedback.
		}						
	}
	
	/**
	 * handles bit stuffing
	 */
	public static byte[] bitStuff(byte[] b){		
		//To Do: Write code for bit stuffing here
		return b;//default behaviour		
	}

	/**
	 * handles bit de-stuffing
	 */
	public static byte[] bitDeStuff(byte[] b){
		//To Do: Write code for bit de-stuffing here
		return b;//default behaviour		
	}
	
	/**
	 * checks whether port is connected
	 *
	 * @return true if port is connected and false otherwise
	 */
	public boolean connected(){return hasConnection;}

	/**
	 * receives a frame from data link layer
	 */
	public byte[] receiveFromDll(){
		try{
				synchronized(dll2phy){
					if (dll2phy.empty()) dll2phy.wait();	
					byte[] f=dll2phy.get().getBytes();
					dll2phy.notify();
					return f;	
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
	 * @param f frame to be sent
	 */
	public void sendToLine(byte[] f) throws Exception{
		try{
			SimPhy.writeStuffed(bw,f);
		}
		catch (Exception e){
			//e.printStackTrace();
			throw e;
		}
	}

	/**
	 * sends frame to data link layer
	 *
	 * @param f frame to be sent
	 */
	public void sendToDll(byte[] f){
		try{
				synchronized(phy2dll){
					if (phy2dll.full()) phy2dll.wait();
					ByteArray b=new ByteArray(f);					
					phy2dll.store(b);
					phy2dll.notify();
				}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * receives bits from physical layer, converts the bits to frame and returns the converted frame
	 *
	 * @return frame formed from bits from physical layer
	 */
	public byte[] receiveFromLine() throws Exception{	
		try{					
			byte[] f=readDeStuffed(br);			
			return f;			
		}
		catch(Exception e){
			//e.printStackTrace(); //not required. already printed by simphy
			throw e;
		}		
	}
	/**
	 * class PhySend receives frame from data link layer and sends it to physical layer
	 */
	private class PhySend extends Thread{
		SimPhy simphy;		
		public PhySend(SimPhy s){
			simphy=s;			
			start();
		}
		public void run(){	
			try{
				while (true){				
					byte[] f=receiveFromDll();				
					sendToLine(f);							
				}
			}
			catch(Exception e){
			}
		}
	}
	
	/**
	 * class PhyReceive receives frame from physical layer and sends it to data link layer
	 */
	private class PhyReceive extends Thread{
		SimPhy simphy;		
		public PhyReceive(SimPhy s){
			simphy=s;			
			start();
		}
		public void run(){
			try{
				while(true){										
						byte[] f=receiveFromLine();
						if(simphy.getPortStatus()){							
							if (f!=null) sendToDll(f);
						}
				}
			}
			catch(Exception e){	
			}
		}
	}		
//=======================================================
}
