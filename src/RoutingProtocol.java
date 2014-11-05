import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

/**
 * class RoutingProtocol simulates the routing protocol
 */
class RoutingProtocol extends Thread {
	/**
	 * router simulated by SimRouter
	 */
	SimRouter simrouter;

	TimerClass updateTimer;
	TimerClass invalidateTimer;
	/**
	 * update timer duration
	 */
	public static int UPDATE_TIMER_VALUE = 20;

	/**
	 * invalidate timer duration
	 */
	public static int INVALID_TIMER_VALUE = 40;

	// To Do: Declare any other variables required
	// ----------------------------------------------------------------------------------------------
	public ArrayList<RouterTableRow> routingTable = new ArrayList<RoutingProtocol.RouterTableRow>();
	//public ArrayList<TimerClass> invTimerList=new ArrayList<TimerClass>();
	private HashMap<String,TimerClass> hashTimer=new HashMap<String, TimerClass>();

	/**
	 * @param s
	 *            simulated router
	 */

	final int updateBufferSize = 100;
	private int receivedPortMask;
	Buffer<ByteArray> updateBuffer = new Buffer<ByteArray>("for routing protocol", updateBufferSize);

	public void setReceivedPostMask(int m) {
		this.receivedPortMask = m;
	}

	public int getReceivedPortMask() {
		return this.receivedPortMask;
	}

	public RoutingProtocol(SimRouter s) {
		simrouter = s;
		// To Do: Do other required initialization tasks.
		updateTimer = new TimerClass(this, 1);
		start();
	}

	// ------------------------Routing
	// Function-----------------------------------------------
	/**
	 * stores the update data in a shared memory to be processed by the
	 * 'RoutingProtocol' thread later
	 * 
	 * @param p
	 *            ByteArray
	 */
	long port = -1;

	void notifyRouteUpdate(ByteArray p, long pId) {// invoked by SimRouter
		// Write code to just to stores the route update data; do not process at
		// this moment, otherwise the simrouter thread will get blocked
		port = pId;
		System.out.println("Route Update code");
		try {
			synchronized (updateBuffer) {
				if (updateBuffer.full())
					return;
				else
					updateBuffer.store(p);
				updateBuffer.notify();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// ----------------------------------------------------------------------------------------------
	/**
	 * update the routing table according to the changed status of an interface;
	 * if interface is UP (status=TRUE), add to routing table, if interface is
	 * down, remove from routing table
	 * 
	 * @param interfaceId
	 *            interface id of the router
	 * @param status
	 *            status denoting whether interface is on or off, true if on and
	 *            false otehrwise
	 */
	public void notifyPortStatusChange(int interfaceId, boolean status) {// invoked
																			// by
																			// SimRouter

		// To Do: Update the routing table according to the changed status of an
		// interface. If interface in UP (status=TRUE), add to routing table, if
		// interface is down, remove from routing table
		if (status == false) {
			simrouter.interfaces[interfaceId].setPortStatus(status);
			setPortStatus(interfaceId, status);
			IpAddress ipAddress = simrouter.interfaces[interfaceId].getIpAddress();
			int mask = simrouter.interfaces[interfaceId].subnetMask;
			String networkadd = ipAddress.getNetworkAddress(mask).getString();

			ArrayList<String> toBdelList = new ArrayList<String>();
			for (int i = 0; i < routingTable.size(); i++) {
				String nhIp = routingTable.get(i).getNextHop();
				IpAddress tIpAddress = new IpAddress(nhIp);
				int subnet = routingTable.get(i).getSubnet();
				String tempNetwork = tIpAddress.getNetworkAddress(subnet).getString();
				System.out.println("network: " + networkadd + " tep net: " + tempNetwork);
				if (tempNetwork.equals(networkadd)) {
					// routingTable.remove(i);
					toBdelList.add(routingTable.get(i).getNetwork());
				}
			}
			for (int j = 0; j < toBdelList.size(); j++) {
				String rowToBeDelete = toBdelList.get(j);
				for (int k = 0; k < routingTable.size(); k++) {
					if (rowToBeDelete.equals(routingTable.get(k).getNetwork())) {
						routingTable.remove(k);
						System.out.println("deleting....."+rowToBeDelete);
						break;
					}
				}
			}
		} else {
			simrouter.interfaces[interfaceId].setPortStatus(status);
			long id = interfaceId;
			int subnetMask = simrouter.interfaces[interfaceId].getSubnetMask();

			IpAddress ipAddress = simrouter.interfaces[interfaceId].getIpAddress();
			String networkadd = ipAddress.getNetworkAddress(subnetMask).getString();
			RouterTableRow rtRow = new RouterTableRow(id, networkadd, "", "C", ipAddress.getString(), 0, true,
					subnetMask);
			routingTable.add(rtRow);
		}

	}
	private void setPortStatus(int interfaceId, boolean status)
	{
		int count=routingTable.size();
		for(int i=0;i<count;i++)
		{
			if(routingTable.get(i).getPort()==interfaceId && routingTable.get(i).getNextHopCount()==0 )
			{
				routingTable.get(i).setPortStatus(status);
				break;
			}
		}
	}

	// ---------------------Forwarding
	// Function------------------------------------------
	/**
	 * returns an NextHop object corresponding the destination IP Address,
	 * dstIP. If route in unknown, return null
	 * 
	 * @param destination
	 *            ip
	 * @return returns an NextHop object corresponding the destination IP
	 *         Address if route is known, else returns null
	 */
	NextHop getNextHop(IpAddress dstIp) {// invoked by SimRouter

		// To Do: Write code that returns an NextHop object corresponding the
		// destination IP Address: dstIP. If route in unknown, return null

		for (int i = 0; i < routingTable.size(); i++) {
			int mask = routingTable.get(i).getSubnet();
			String network = dstIp.getNetworkAddress(mask).getString();
			System.out.println(dstIp.getString());
			if (routingTable.get(i).getNetwork().equals(network)) {
				NextHop nH = new NextHop(new IpAddress(routingTable.get(i).getNextHop()), (int) routingTable.get(i)
						.getPort());
				System.out.println("next Hop IP" + routingTable.get(i).getNextHop());
				return nH;
			}
		}
		return null; // default return value
	}

	// -------------------Routing Protocol
	// Thread--------------------------------------
	public synchronized void run() {
		// To Do 1: Populate Routing Table with directly connected interfaces
		// using the SimRouter instance. Also print this initial routing table .
		int count = simrouter.interfaceCount;
		System.out.println("count" + count);
		for (int i = 1; i <= count; i++) {
			if (simrouter.interfaces[i].isConfigured) {
				long id = i;
				int subnetMask = simrouter.interfaces[i].getSubnetMask();

				IpAddress ipAddress = simrouter.interfaces[i].getIpAddress();
				String networkadd = ipAddress.getNetworkAddress(subnetMask).getString();
				RouterTableRow rtRow = new RouterTableRow(id, networkadd, "", "C", ipAddress.getString(), 0, true,
						subnetMask);
				invalidateTimer=new TimerClass(this,2);
				invalidateTimer.startTimer(INVALID_TIMER_VALUE);
				invalidateTimer.setNet(networkadd);
				hashTimer.put(networkadd,invalidateTimer );
				routingTable.add(rtRow);
			}

		}
		// printing routing table in console
		System.out.println("Printing Routing Table.....");
		for (int i = 0; i < routingTable.size(); i++) {
			String row = "Network: " + routingTable.get(i).getNetwork() + " Port: " + routingTable.get(i).getPort()
					+ " Type: " + routingTable.get(i).type;
			System.out.println(row);

			// simrouter.WriteRTableInGUI(row);
		}
		System.out.println("Sending Packet...");
		sendRoutingUpdate();
		updateTimer.startTimer(UPDATE_TIMER_VALUE);

		// To Do 2: Send constructed routing table immediately to all the
		// neighbors. Start the update timer.

		// To Do 3: Continuously check whether there are routing updates
		// received from other neighbours.

		ByteArray p;
		try {
			while (true) {
				synchronized (updateBuffer) {
					if (updateBuffer.empty())
						updateBuffer.wait();
					p = updateBuffer.get();
					updateBuffer.notify();
				}
				// An update has been received, Now:
				checkReceivedRoutingData(p);
				// To Do 3.1: Modify routing table according to the update
				// recived.

				// To Do 3.2: Start invalidate timer for each newly
				// added/updated route if any.
				// To Do 3.3:Print the routing table if the routing table has
				// changed

				// To Do Optional 1: Send Triggered update to all the neighbors
				// and reset update timer.
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		// An update has been received, Now:
		// To Do 3.1: Modify routing table according to the update received.
		// To Do 3.2: Start invalidate timer for each newly added/updated route
		// if any.
		// To Do 3.3:Print the routing table if the routing table has changed
		// To Do Optional 1: Send Triggered update to all the neighbors and
		// reset update timer.
	}

	// Timer t=new Timer();
	private void sendRoutingUpdate() {
		ByteArray byteArray = getRoutingTable();
		int rtable = byteArray.getSize();
		ByteArray packetArray = new ByteArray(rtable + 9);
		int count = simrouter.interfaceCount;
		for (int i = 1; i <= count; i++) {
			if (simrouter.interfaces[i].isConfigured) {
				packetArray.setByteVal(0, DataLinkLayer.BROADCAST_MAC);
				packetArray.setAt(1, simrouter.interfaces[i].getIpAddress().getBytes());
				packetArray.setAt(5, new IpAddress(simrouter.RP_MULTICAST_ADDRESS).getBytes());
				packetArray.setAt(9, byteArray.getBytes());
				simrouter.sendPacketToInterface(packetArray, i);

			}

		}
	}

	private void printRoutingTable() {
		System.out.println("Printing Routing Table.....");
		for (int i = 0; i < routingTable.size(); i++) {
			String row = "Network: " + routingTable.get(i).getNetwork() + " Port: " + routingTable.get(i).getPort()
					+ " Type: " + routingTable.get(i).type;
			System.out.println(row);

			// simrouter.WriteRTableInGUI(row);
		}
	}

	public int getSize() {
		return routingTable.size();
	}

	public String getRow(int i) {
		String row = "";
		if (routingTable.get(i).getPortStatus()) {
			row = "Network: " + routingTable.get(i).getNetwork() + " Port: " + routingTable.get(i).getPort()
					+ " Type: " + routingTable.get(i).type + "NextHop: " + routingTable.get(i).nextHop
					+ " Hope Count: " + routingTable.get(i).getNextHopCount();
			System.out.println(row);
		}
		return row;
	}

	private void filterTable(ByteArray b) {
		Packet p = new Packet(b.getBytes());
		byte addr[] = new byte[4];
		int items = (p.payload.length) / 6;
		int index = 0;
		int hopes;
		int mask;
		IpAddress address;
		IpAddress srcAddrss = p.getSrcIp();
		ArrayList<String> toBdelList = new ArrayList<String>();
		// System.out.println("************************************** :");
		for (int a = 0; a < routingTable.size(); a++) {
			if (routingTable.get(a).getNextHop().equals(srcAddrss.getString())) {
				boolean isInUpdatePacket = false;
				RouterTableRow tempRow = routingTable.get(a);
				String networkId = tempRow.getNetwork();
				for (int i = 1; i <= items; i++) {
					System.arraycopy(p.payload, index, addr, 0, addr.length);
					index += 4;
					hopes = (int) p.payload[index];
					++index;
					mask = (int) p.payload[index];
					++index;
					address = new IpAddress(addr);
					System.out.println("address: " + address.getString() + " mask " + mask + " hopes:" + hopes);

					if (networkId.equals(address.getNetworkAddress(mask))) {
						isInUpdatePacket = true;
						System.out.println("hasan u value of isInUpdatePacket :" + isInUpdatePacket);
					}
				}
				if (isInUpdatePacket == false) {
					// add to delete list
					toBdelList.add(networkId);
					System.out.println("hasan u need to delete networkId :" + networkId);
				}
			}
		}
		// delete Now
		for (int j = 0; j < toBdelList.size(); j++) {
			String rowToBeDelete = toBdelList.get(j);
			for (int k = 0; k < routingTable.size(); k++) {
				if (rowToBeDelete.equals(routingTable.get(k).getNetwork())) {
					routingTable.remove(k);
				}
			}
		}

	}

	private void checkReceivedRoutingData(ByteArray b) {
		Packet p = new Packet(b.getBytes());
		byte addr[] = new byte[4];
		byte nextHopAddr[] = new byte[4];
		int items = (p.payload.length) / 10;
		int index = 0;
		int hopes;
		int mask;
		IpAddress address;
		IpAddress nAddress;
		IpAddress srcAddrss = p.getSrcIp();

		for (int i = 1; i <= items; i++) {
			System.arraycopy(p.payload, index, addr, 0, addr.length);
			index += 4;
			hopes = (int) p.payload[index];
			index++;
			mask = (int) p.payload[index];
			index++;
			address = new IpAddress(addr);
			System.arraycopy(p.payload, index, nextHopAddr, 0, nextHopAddr.length);
			index += 4;

			nAddress = new IpAddress(nextHopAddr);

			System.out.println("address: " + address.getString() + " mask " + mask + " hopes:" + hopes + "  Next Hop: "
					+ nAddress.getString());

			int num = routingTable.size();
			int flag = 1;
			int uFlag = -1;
			RouterTableRow nRow = null;
			for (int j = 0; j < num; j++) {
				RouterTableRow rRow = routingTable.get(j);
				String networkAdress = rRow.getNetwork();
				if (networkAdress.equals(address.getString())) {
					int pHopes = rRow.getNextHopCount();
					if (pHopes > (hopes + 1)) {
						uFlag = j;
						// long port = rRow.getPort();
						String network = rRow.getNetwork();
						String nextHop = srcAddrss.getString();
						String type = "N";
						int h = hopes + 1;
						nRow = new RouterTableRow(port, network, nextHop, type, "", h, true, mask);
						invalidateTimer=new TimerClass(this,2);
						invalidateTimer.setNet(networkAdress);
						invalidateTimer.startTimer(INVALID_TIMER_VALUE);
						if(hashTimer.get(networkAdress)!=null)
						{
							hashTimer.get(networkAdress).stopTimer();
						}
						hashTimer.remove(networkAdress);
						hashTimer.put(networkAdress,invalidateTimer);
					}
					else if(pHopes==(hopes+1))
					{
						System.out.println("network Adress: "+networkAdress);
						invalidateTimer=new TimerClass(this,2);
						invalidateTimer.setNet(networkAdress);
						invalidateTimer.startTimer(INVALID_TIMER_VALUE);
						if(hashTimer.get(networkAdress)!=null)
						{
							hashTimer.get(networkAdress).stopTimer();
						}
						hashTimer.remove(networkAdress);
						hashTimer.put(networkAdress,invalidateTimer);
						
					}
					flag = 0;

				}
			}
			if (uFlag != -1) {
				routingTable.remove(uFlag);
				routingTable.add(nRow);
			}
			if (flag == 1) {
				if(isDirectylyConnected(nAddress.getString()))
				{
					System.out.println("From Directly Connected Interface: "+nAddress.getString());
				}
				else
				{
				RouterTableRow row = new RouterTableRow(port, address.getString(), srcAddrss.getString(), "N", "",
						hopes + 1, true, mask);
				routingTable.add(row);
				System.out.println("Routing Table altered");
				invalidateTimer=new TimerClass(this,2);
				invalidateTimer.startTimer(INVALID_TIMER_VALUE);
				invalidateTimer.setNet(row.getNetwork());
				hashTimer.put(row.getNetwork(),invalidateTimer);
				
				System.out.println("Hashing Network: "+ row.getNetwork());
				
				
				}

			}

		}
		printRoutingTable();

	}

	private boolean isDirectylyConnected(String portIp) {
		int count = simrouter.interfaceCount;
		System.out.println("count" + count);
		for (int i = 1; i <= count; i++) {
			if (simrouter.interfaces[i].isConfigured) {
				if (simrouter.interfaces[i].getIpAddress().getString().equals(portIp))
					return true;

			}

		}
		return false;
	}

	// ----------------------Timer
	// Handler------------------------------------------------------
	/**
	 * handles what happens when update timer and invalidate timer expires
	 * 
	 * @param type
	 *            of timer: type 1- update timer and type 2- invalid timer
	 *            expired
	 */
	public void handleTimerEvent(int type, String netWork) {
		// If update timer has expired, then:
		// To Do 1: Sent routing update to all the interfaces. Use
		// simrouter.sendPacketToInterface(update, interfaceId) function to send
		// the updates.
		// To Do Optional 1: Implement split horizon rule while sending update
		// To Do 2: Start the update timer again.
		if (type == 1) {
			sendRoutingUpdate();
			updateTimer = new TimerClass(this, 1);
			updateTimer.startTimer(UPDATE_TIMER_VALUE);
		}
		// Else an invalid timer has expired, then:
		// To Do 3: Delete route from routing table for which invalidate timer
		// has expired.
		else {
			System.out.println("\t Invalid Timer Happens ");
			deleteRIPinRoutingTable(netWork);
			//notifyPortStatusChange(interfceid, false);
			// printRoutingTable_new();
		}
	}

	private void deleteRIPinRoutingTable(String netAddress)
	{
		int count=routingTable.size();
		for(int i=0;i<count;i++)
		{
			String tempNet=routingTable.get(i).getNetwork();
			int h=routingTable.get(i).getNextHopCount();
			if(tempNet.equals(netAddress) && h!=0)
			{
				routingTable.remove(i);
				hashTimer.remove(netAddress);
				System.out.println("Invalidate Timer: "+ netAddress);
				break;
			}
			
		}
	}
	private ByteArray getRoutingTable() {
		int length = routingTable.size();

		ByteArray byteArray = new ByteArray(length * 10);
		int start = 0;
		for (int i = 0; i < length; i++) {
			// System.out.println(""+start);
			byteArray.setAt(start, new IpAddress(routingTable.get(i).getNetwork()).getBytes());
			start = start + 4;
			// next Hope Count=-1 if status=false
			if (routingTable.get(i).getPortStatus())
				byteArray.setByteVal(start, (byte) routingTable.get(i).getNextHopCount());
			else {
				System.out.println("hopes -1 send");
				byteArray.setByteVal(start, (byte) -1);
			}
			start = start + 1;
			byteArray.setByteVal(start, (byte) routingTable.get(i).getSubnet());
			start = start + 1;
			byteArray.setAt(start, new IpAddress(routingTable.get(i).getNextHop()).getBytes());
			start = start + 4;

		}
		return byteArray;

	}

	// ----------------------------------------------------------------------------------------------
	public class RouterTableRow {
		private long port;
		private String network;
		private String nextHop;
		private String type;
		private String ipAddress;
		private boolean portstatus;
		private int nextHC;
		private int subnet;

		public RouterTableRow(long port, String ntwrk, String nextHop, String type, String ip, int hopeCount,
				boolean ps, int subnet) {
			this.port = port;
			this.network = ntwrk;
			this.nextHop = nextHop;
			this.type = type;
			this.ipAddress = ip;
			this.portstatus = ps;
			this.nextHC = hopeCount;
			this.subnet = subnet;

		}

		public long getPort() {
			return this.port;
		}

		public String getNetwork() {
			return this.network;
		}

		public String getNextHop() {
			return this.nextHop;
		}

		public String getType() {
			return this.type;
		}

		public String getIP() {
			return this.ipAddress;
		}

		public boolean getPortStatus() {
			return this.portstatus;
		}

		public void setPortStatus(boolean val) {
			this.portstatus = val;
		}

		public int getNextHopCount() {
			return this.nextHC;
		}

		public int getSubnet() {
			return this.subnet;
		}
	}
}
