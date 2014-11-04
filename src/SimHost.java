import java.util.*;
import java.net.*;
import java.io.*;

/**
 * class SimHost simulates an end device
 */
class SimHost{
	/**
	 * ip address of the end device encapsulated in instance of IpAddress
	 */
	IpAddress ipAddr;
	
	/**
	 * subnet mask
	 */
	int mask;
	
	/**
	 * ip address of default gateway encapsulated in instance of IpAddress
	 */
	IpAddress gateway;
	
	/**
	 * device id of the end deivce or router or switch at the other end of a connection
	 */
	String otherEnd;
	
	/**
	 * mac address of the end device in byte format
	 */
	byte macAddress;
	
	/**
	 * ARP table
	 */
	Hashtable arpTable;
	
	/**
	 * data link layer
	 */
	public DataLinkLayer dll;

	/**
	 * used to read contents of configuration files
	 */
	public BufferedReader br;
	
	/**
	 * buffer containing data to be sent from data link layer to network layer
	 */
	Buffer<ByteArray> dll2nl;
	
	/**
	 * buffer containing data to be sent from network layer to data link layer
	 */
	Buffer<ByteArray> nl2dll;
	
	/**
	 * reads configurations of end devices; and connections among end devices, routers and switches
	 *
	 * @param deviceId string representation of the device, Hn for end devices, Rn for routers, Sn for switches; n being 1,2,3,...
	 */
	public void loadParameters(String deviceId) throws Exception{		
		BufferedReader br=new BufferedReader(new FileReader("Config//Config.txt"));		
		String line;
		while((line=br.readLine())!=null){
			String[] tokens= line.split(":");			
			if(tokens[0].compareTo(deviceId)==0){
				String paramName=tokens[1];
				/*if(paramName.compareTo("CONNECTSTO")==0){
					otherEnd=tokens[2];
					if (tokens.length>3) otherEnd=otherEnd+tokens[3];
				}*/
				if(paramName.compareTo("IPADDRESS")==0){
					ipAddr=new IpAddress(tokens[2]);
					System.out.println("IP ADDRESS: "+ipAddr.getString());
				}
				else if(paramName.compareTo("SUBNETMASK")==0){
					mask=Integer.parseInt(tokens[2]);
					System.out.println("SUBNET MASK LENGTH: "+mask);
				}
				else if(paramName.compareTo("DEFAULTGATEWAY")==0){
					gateway=new IpAddress(tokens[2]);
					System.out.println("Default Gateway: "+gateway.getString());;
				}
				else if(paramName.compareTo("MACADDRESS")==0){
					macAddress=(byte)Integer.parseInt(tokens[2]);
					System.out.println("MAC ADDRESS: "+(int)((byte)macAddress & 0xFF));
				}				
			}
		}
	}
	
	/**
	 * reads in the ARP table from text file, loads in hashtable indexed by ip address having mac address as the content
	 */
	public void loadArpTable() throws Exception{	
		arpTable=new Hashtable();
		BufferedReader br=new BufferedReader(new FileReader("Config//ArpTable.txt"));		
		String line;
		while((line=br.readLine())!=null){
			String[] tokens= line.split(":");
			IpAddress dst=new IpAddress(tokens[0]);
			if(ipAddr.sameSubnet(dst, mask)){
				arpTable.put(tokens[0],tokens[1]);
			}
		}
	}
	
	/**
	 * loads configurations, arp table, allows input from console
	 */
	public SimHost(String deviceId){
		try{
			loadParameters(deviceId);
			loadArpTable();
		}catch(Exception e){}
		
		br= new BufferedReader(new InputStreamReader(System.in));
		
		dll2nl=new Buffer<ByteArray>("Dll2Nl Buffer",1);
		nl2dll=new Buffer<ByteArray>("Nl2Dll Buffer",1);
		dll=new DataLinkLayer(deviceId, "", macAddress, dll2nl, nl2dll);							
		
		new NlSend(this);
		new NlReceive(this);
	}
	
	/**
	 * receives a frame from data link layer and converts it into a packet
	 *
	 * @return Packet
	 */
	public Packet receiveFromDll(){
		try{
				synchronized(dll2nl){
					if (dll2nl.empty()) dll2nl.wait();	
					Packet p=new Packet((dll2nl.get()).getBytes());
					dll2nl.notify();
					return p;	
				}											
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * sends a ByteArray to data link layer
	 *
	 * @param p ByteArray
	 */
	public void sendToDll(ByteArray p){		
		try{
				synchronized(nl2dll){
					if (nl2dll.full()) nl2dll.wait();	
					nl2dll.store(p);
					nl2dll.notify();
				}											
		}
		catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	/**
	 * returns mac address from ip address
	 *
	 * @param ip ip address encapsulated in an instance of IpAddress
	 * @return returns integer representation of mac address
	 */
	int getMacFromArpTable(IpAddress ip){
	    String macAd=(String) arpTable.get(ip.getString());
		if (macAd!=null) return Integer.parseInt(macAd);
		else return -1;
	}
	
	/**
	 * class NlSend takes input from command line in IP:MESSAGE format, converts the message into a packet and sends the packet to data link layer
	 */
	private class NlSend extends Thread{
		SimHost nl;		
		public NlSend(SimHost n){
			nl=n;			
			start();
		}
		public void run(){
			try{			
				while(true){	
					System.out.println("Wating for input...");
					String input=br.readLine();
					System.out.println("Got input:  "+input);
					String[] tokens= input.split(":");			
					if(tokens.length<2){
						System.out.println("Input parameter missing");
						continue;
					}
				
					IpAddress dstIp=new IpAddress(tokens[0]);

					if (ipAddr.sameIp(dstIp)){
						System.out.println("Ping to own interface...");
						continue;
					}	
					
					int dstMac;
					if(ipAddr.sameSubnet(dstIp, mask)){ 
						dstMac=getMacFromArpTable(dstIp);
					}
					else dstMac=getMacFromArpTable(gateway);
					
					if(dstMac<0) {
						System.out.println("MAC Address of Destination not found");
						continue;
					}
					//Everything fine; send the packet					
					String text=tokens[1];					
					Packet p=new Packet(ipAddr, dstIp, text.getBytes());
					ByteArray temp=new ByteArray((p.getBytes()).length+1);
					temp.setByteVal(0,(byte)dstMac);
					temp.setAt(1,p.getBytes());
					sendToDll(temp);
				}			
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * class NlReceive receives a packet by converting a frame received from data link layer
	 */
	private class NlReceive extends Thread{
		SimHost nl;		
		public NlReceive(SimHost n){
			nl=n;			
			start();
		}
		public void run(){
			while(true){
				//Packet p=nl.receiveFromDll();
				Packet p=receiveFromDll();
				if(p.getDstIp().sameIp(ipAddr)) System.out.println("\tShowing Packet: "+p.getString()+"\n");
			}
		}
	}
	
	
	public static void main(String args[]) throws Exception{					
		String deviceId="H2";
		
		int argCount=args.length;		
		if (argCount>0)deviceId=args[0];
		
		SimHost simHost=new SimHost(deviceId);
		
		while(true){}
	}
}

