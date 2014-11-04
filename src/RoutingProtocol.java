import java.util.*;
import java.net.*;
import java.io.*;

import javax.swing.text.rtf.RTFEditorKit;

/**
 * class RoutingProtocol simulates the routing protocol
 */
class RoutingProtocol extends Thread{	
	/**
	 * router simulated by SimRouter
	 */
	SimRouter simrouter;
	/**
	 * update timer duration
	 */
	public static int UPDATE_TIMER_VALUE=30;
	
	/**
	 * invalidate timer duration
	 */
	public static int INVALID_TIMER_VALUE=90;
	
	//To Do: Declare any other variables required
	//----------------------------------------------------------------------------------------------	
	public ArrayList<RouterTableRow> routingTable=new ArrayList<RoutingProtocol.RouterTableRow>();
	/**
	 * @param s simulated router
	 */
	private int receivedPortMask;
	public void setReceivedPostMask(int m)
	{
		this.receivedPortMask=m;
	}
	public int getReceivedPortMask()
	{
		return this.receivedPortMask;
	}
	public RoutingProtocol(SimRouter s){
		simrouter=s;	
		//To Do: Do other required initialization tasks.
		start();
	}
	
	//------------------------Routing Function-----------------------------------------------	
	/**
	 * stores the update data in a shared memory to be processed by the 'RoutingProtocol' thread later
	 *
	 * @param p ByteArray
	 */
	void notifyRouteUpdate(ByteArray p){//invoked by SimRouter
		//Write code to just to stores the route update data; do not process at this moment, otherwise the simrouter thread will get blocked	
	}
	//----------------------------------------------------------------------------------------------	
	/**
	 * update the routing table according to the changed status of an interface; if interface is UP (status=TRUE), add to routing table, if interface is down, remove from routing table
	 *
	 * @param interfaceId interface id of the router
	 * @param status status denoting whether interface is on or off, true if on and false otehrwise 
	 */
	public void notifyPortStatusChange(int interfaceId, boolean status){//invoked by SimRouter
		
		//To Do: Update the routing table according to the changed status of an interface. If interface in UP (status=TRUE), add to routing table, if interface is down, remove from routing table
		routingTable.get(interfaceId-1).setPortStatus(status);
	}
	
	//---------------------Forwarding Function------------------------------------------
	/**
	 * returns an NextHop object corresponding the destination IP Address, dstIP. If route in unknown, return null
	 *
	 * @param destination ip
	 * @return returns an NextHop object corresponding the destination IP Address if route is known, else returns null
	 */
	NextHop getNextHop(IpAddress dstIp){//invoked by SimRouter
		
		//To Do: Write code that  returns an NextHop object corresponding the destination IP Address: dstIP. If route in unknown, return null
		
		String network=dstIp.getNetworkAddress(receivedPortMask).getString();
		for(int i=0;i<routingTable.size();i++)
		{
			System.out.println(dstIp.getString());
			if(routingTable.get(i).getIP().equals(dstIp.getString()))
			{
				NextHop nH=new NextHop(dstIp,(int)routingTable.get(i).getPort());
				return nH;
			}
		}
		return null; //default return value
	}	
	
	//-------------------Routing Protocol Thread--------------------------------------	
	public void run(){
		//To Do 1: Populate Routing Table with directly connected interfaces using the SimRouter instance. Also print this initial routing table	.	
		int count=simrouter.interfaceCount;
		System.out.println("count" +count);
		for(int i=1;i<=count;i++)
		{
			long id=i;
			int subnetMask=simrouter.interfaces[i].getSubnetMask();
			IpAddress ipAddress=simrouter.interfaces[i].getIpAddress();
			String networkadd=ipAddress.getNetworkAddress(subnetMask).getString();
			RouterTableRow rtRow=new RouterTableRow(id,networkadd,"","C",ipAddress.getString(),true);
			routingTable.add(rtRow);
			
		}
		//printing routing table in console
		System.out.println("Printing Routing Table.....");
		for(int i=0;i<count;i++)
		{
			String row="Network: "+routingTable.get(i).getNetwork()+" Port: "+routingTable.get(i).getPort()+" Type: " + routingTable.get(i).type;
			System.out.println(row);
		
		simrouter.WriteRTableInGUI(row);
		}
		
		//To Do 2: Send constructed routing table immediately to all the neighbors. Start the update timer.		
		
		//To Do 3: Continuously check whether there are routing updates received from other neighbours.
		//An update has been received, Now:
			//To Do 3.1: Modify routing table according to the update received. 
			//To Do 3.2: Start invalidate timer for each newly added/updated route if any.
			//To Do 3.3:Print the routing table if the routing table has changed
			//To Do Optional 1: Send Triggered update to all the neighbors and reset update timer.
	}	
	
	//----------------------Timer Handler------------------------------------------------------	
	/**
	 * handles what happens when update timer and invalidate timer expires
	 * 
	 * @param type of timer: type 1- update timer and type 2- invalid timer expired
	 */
	public void handleTimerEvent(int type){
		//If update timer has expired, then:
			//To Do 1: Sent routing update to all the interfaces. Use simrouter.sendPacketToInterface(update, interfaceId) function to  send the updates.		
			//To Do Optional 1: Implement split horizon rule while sending update
			//To Do 2: Start the update timer again.
			
		//Else an invalid timer has expired, then:
			//To Do 3:  Delete route from routing table for which invalidate timer has expired.			
	}			
	//----------------------------------------------------------------------------------------------
	public class RouterTableRow
	{
		private long port;
		private String network;
		private String nextHop;
		private  String type;
		private String ipAddress;
		private boolean portstatus;
		public RouterTableRow(long port,String ntwrk,String nextHop,String  type,String ip,boolean ps) {
			this.port=port;
			this.network=ntwrk;
			this.nextHop=nextHop;
			this.type=type;
			this.ipAddress=ip;
			this.portstatus=ps;
			
		}
		public long getPort()
		{
			return this.port;
		}
		public String getNetwork()
		{
			return this.network;
		}
		public String getNextHop()
		{
			return this.nextHop;
		}
		public String getType()
		{
			return this.type;
		}
		public String getIP()
		{
			return this.ipAddress;
		}
		public boolean getPortStatus()
		{
			return this.portstatus;
		}
		public void setPortStatus(boolean val)
		{
			this.portstatus=val;
		}
	}
}

