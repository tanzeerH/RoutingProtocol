import java.util.*;
import java.net.*;
import java.io.*;
//+++++++++++++++++Class: SimPort++++++++++++++++++++++++++++++++++++
class SimPort extends Thread{	
	int portId;
	Buffer<ByteArray> swMemory;	
	Buffer<ByteArray> portBuffer;
	Buffer<ByteArray> fromPhy;	
	SimPhy simPhy;
	//-----------------------------------------------------------------	
	SimPort(String dId, int pid, Buffer<ByteArray> mem, Buffer<ByteArray> pbuf){		
		portId=pid;
		swMemory=mem;
		portBuffer=pbuf;
		fromPhy=new Buffer<ByteArray>("Phy2Dll Buffer",1);
		simPhy=new SimPhy(dId+"-"+pid, fromPhy, pbuf);	
		start();
	}
	public boolean getPortStatus(){return simPhy.getPortStatus();}	
	public void run(){	
		try{
			ByteArray b;
			synchronized(fromPhy){
				if (fromPhy.empty()) fromPhy.wait();	
				ByteArray p=fromPhy.get();			
				fromPhy.notify();
				b=new ByteArray(p.getSize()+1);
				b.setByteVal(0,(byte)portId);
				b.setAt(1,p.getBytes());
			}			
			synchronized(swMemory){
					if (swMemory.full()) swMemory.wait();														
					swMemory.store(b);
					swMemory.notify();
			}		
		}catch(Exception e){}
	}
}
//+++++++++++++++++Class: SimSwitch++++++++++++++++++++++++++++++++++++
class SimSwitch extends Thread{
	public Hashtable macTable;	
	public SimPort ports[];
	public int portCount;	
	Buffer<ByteArray> swMemory;	
	Buffer<ByteArray>portBuffers[];
	
	public static int MEMORY_SIZE=10;
	public static int MACTABLE_SIZE=10;	//not used
	
	BufferedReader br;
	//-----------------------------------------------------------------	
	public void loadParameters(String deviceId) throws Exception{		
		BufferedReader br=new BufferedReader(new FileReader("..\\Config\\Config.txt"));		
		String line;
		while((line=br.readLine())!=null){
			String[] tokens= line.split(":");			
			if(tokens[0].compareTo(deviceId)==0){
				String paramName=tokens[1];
				if(paramName.compareTo("NUMOFPORTS")==0){
					portCount=Integer.parseInt(tokens[2]);
					ports=new SimPort[portCount+1];		
					portBuffers=(Buffer<ByteArray>[])new Buffer[portCount+1]; //assuming port count is already loaded
					for (int i=1; i<=portCount; i++){
						portBuffers[i]=new Buffer<ByteArray>("Port Buffer for Port="+"i",1);			
						ports[i]=new SimPort(deviceId, i, swMemory, portBuffers[i]); //each port take instance of the switch and its id			
					}
				}											
			}
		}
	}	
	//-----------------------------------------------------------------	
	public SimSwitch(String deviceId){
		swMemory=new Buffer<ByteArray>("Switch Memory",MEMORY_SIZE);					
		macTable=new Hashtable();
		try{
			loadParameters(deviceId);
		}catch(Exception e){}
		
		start();
	}
	//=======================================================
	public int getPortCount(){return portCount;}	
	//=======================================================
	public void run(){ //works as the switching engine
		try{			
			while(true){
				ByteArray pd=getPortData();				
				int inPort=pd.getByteVal(0); 
				Frame f=new Frame(pd.getAt(1, pd.getSize()-1));					
				//--------Learning 			 
				int srcMac=f.getSrcMac();
				if(macTable.get(srcMac)==null){
					macTable.put(srcMac, inPort);
					System.out.println(macTable);
				}
				//--------Forwarding 			 
				int dstMac=f.getDstMac();
				if(macTable.get(dstMac)!=null){
						int outPort=((Integer)(macTable.get(dstMac))).intValue();						
						sendFrameToPort(f, outPort);
				}					
				else broadcastFrame(f, inPort); //default broadcast											
			}			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}				
	//=======================================================
	ByteArray getPortData(){
		try{
				synchronized(swMemory){
					if (swMemory.empty()) swMemory.wait();	
					ByteArray pd=swMemory.get();
					swMemory.notifyAll();					
					return pd;
				}											
		}
		catch(Exception e){
			e.printStackTrace();
		}		
		return null;
	}
	//=======================================================
	void broadcastFrame(Frame f, int inPort){
		try{
			for(int i=1; i<=portCount;i++){
				if(i!=inPort) sendFrameToPort(f,i);
			}
		}
		catch(Exception e){e.printStackTrace();}
	}
	//=======================================================
	void sendFrameToPort(Frame f, int outPort){
		try{
				if (ports[outPort].getPortStatus()){
					synchronized(portBuffers[outPort]){
						if (portBuffers[outPort].full()) return;	//frame dropped
						else portBuffers[outPort].store(new ByteArray(f.getBytes()));
						portBuffers[outPort].notify();					
					}
				}		
		}
		catch(Exception e){
			e.printStackTrace();
		}		
	}		
	//=======================================================
	public static void main(String args[]) throws Exception{	
		
		String deviceId="Default";
		
		int argCount=args.length;		
		if (argCount>0)deviceId=args[0];
		
		SimSwitch simswitch=new SimSwitch(deviceId);			
		simswitch.join();
	}		
	//=======================================================		
}