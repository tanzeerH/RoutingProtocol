import java.util.*;
import java.net.*;
import java.io.*;

import javax.swing.JFrame;

/**
 * class NextHop stores the ip address and port id of the next router interface
 * packets should be routed to
 */
class NextHop {
	/**
	 * ip address
	 */
	IpAddress ip;

	/**
	 * interface id
	 */
	int interfaceId;

	/**
	 * @param i
	 *            ip address
	 * @param id
	 *            port id
	 */
	public NextHop(IpAddress i, int id) {
		ip = i;
		interfaceId = id;
	}

	/**
	 * returns ip address of next hop
	 * 
	 * @return ip address of next hop
	 */
	IpAddress getIp() {
		return ip;
	}

	/**
	 * returns interface id of next hop
	 * 
	 * @return interface id of next hop
	 */
	int getInterfaceId() {
		return interfaceId;
	}
}

/**
 * class SimInterface simulates interface of router
 */
class SimInterface extends Thread {
	/**
	 * device id of the router
	 */
	String deviceId;

	/**
	 * interface id of the router interface
	 */
	int interfaceId;

	/**
	 * buffer of frames
	 */
	Buffer<ByteArray> rMemory;
	Buffer<ByteArray> fromDll;

	/**
	 * buffer of packets
	 */
	Buffer<ByteArray> interfaceBuffer;

	/**
	 * underlying data link layer
	 */
	DataLinkLayer dll;

	/**
	 * indicates whether the interface is up, true if up and false otherwise
	 */
	boolean isUp = true;

	/**
	 * denotes whether interface is configured, true if configured and false
	 * otherwise
	 */
	boolean isConfigured = false;

	/**
	 * configuration of the interface
	 */

	/**
	 * ip address of the router interface
	 */
	IpAddress ipAddr;

	/**
	 * subnet mask of the router interface
	 */
	int subnetMask;

	/**
	 * mac address of the router interface
	 */
	// byte macAddress;

	/**
	 * sets ip address of the router interface
	 * 
	 * @param ip
	 *            address of the router interface
	 */
	void setIpAddress(IpAddress ip) {
		ipAddr = ip;
	}

	/**
	 * gets ip address of the router interface
	 * 
	 * @return ip address of the router interface
	 */
	IpAddress getIpAddress() {
		return ipAddr;
	}

	/**
	 * sets mac address of the router interface
	 * 
	 * @param mac
	 *            address of the router interface
	 */
	void setMacAddress(byte mac) {
		// macAddress=mac;
		dll.setMacAddress(mac);
	}

	/**
	 * gets mac address of the router interface
	 * 
	 * 
	 * @return mac address of the router interface
	 */
	byte getMacAddress() {
		return dll.getMacAddress();
	}

	/**
	 * sets subnet mask of the router interface
	 * 
	 * @param subnet
	 *            mask of the router interface
	 */
	void setSubnetMask(int mask) {
		subnetMask = mask;
	}

	/**
	 * gets subnet mask of the router interface
	 * 
	 * 
	 * @return subnet mask of the router interface
	 */
	int getSubnetMask() {
		return subnetMask;
	}

	/**
	 * 
	 * set the configuration status of the interface
	 * 
	 * 
	 * @param b
	 *            boolean indicating status of the interface, true if configured
	 *            and false otehrwise
	 */
	void setIsConfigured(boolean b) {
		isConfigured = b;
	}

	/**
	 * gets the configuration status of the interface
	 * 
	 * @return boolean indicating status of the interface, true if configured
	 *         and false otehrwise
	 */
	boolean getIsConfigured() {
		return isConfigured;
	}

	/**
	 * 
	 * returns status of the interface
	 * 
	 * 
	 * @return true if interface is up and false otherwise
	 */
	public boolean getPortStatus() {
		return isUp;
	}

	/**
	 * sets status of the interface
	 * 
	 * @param s
	 *            true if interface is up and false otherwise
	 */
	public void setPortStatus(boolean s) {
		isUp = s;
		dll.setPortStatus(s);
	}

	/**
	 * @param dId
	 *            device id
	 * @param iId
	 *            interface id
	 * @param ifcfg
	 *            interface configuration
	 * @param buffer
	 *            of frames
	 * @param buffer
	 *            of packets
	 */
	public SimInterface(String dId, int iId, Buffer<ByteArray> mem,
			Buffer<ByteArray> pbuf) {
		deviceId = dId;
		interfaceId = iId;

		rMemory = mem;
		interfaceBuffer = pbuf;
		fromDll = new Buffer<ByteArray>("dll2interface Buffer", 1);

		dll = new DataLinkLayer(deviceId, Integer.toString(interfaceId),
				(byte) 0, fromDll, pbuf);
		start();
	}

	public void run() {
		try {
			while (true) {
				ByteArray b;
				synchronized (fromDll) {
					if (fromDll.empty())
						fromDll.wait();
					ByteArray p = fromDll.get();
					fromDll.notify();
					b = new ByteArray(p.getSize() + 1);
					b.setByteVal(0, (byte) interfaceId);
					b.setAt(1, p.getBytes());
					// System.out.println("\tPacket received from DLL in siminterface");
				}
				synchronized (rMemory) {
					if (rMemory.full())
						rMemory.wait();
					rMemory.store(b);
					rMemory.notify();
					// System.out.println("\tPacket stored in router memory");
				}
			}
		} catch (Exception e) {
		}
	}
}

/**
 * class SimRouter simulates router
 */
class SimRouter extends Thread {
	/**
	 * number of interfaces of the router
	 */
	public int interfaceCount;

	/**
	 * array of interfaces of the router
	 */
	public SimInterface interfaces[];

	/**
	 * array of buffers for the interfaces
	 */
	Buffer<ByteArray> interfaceBuffers[];

	/**
	 * buffer from frames // router memory
	 */
	Buffer<ByteArray> rMemory;

	/**
	 * arp table
	 */
	Hashtable arpTable;

	/**
	 * routing protocol
	 */
	RoutingProtocol rProto;
	/**
	 * memory size
	 */
	public static int MEMORY_SIZE = 100;

	/**
	 * multicast address
	 */
	public String RP_MULTICAST_ADDRESS = "224.0.0.0";

	/**
	 * @param deviceId
	 *            device id of the router
	 */
	//static GUIRouter gui = new GUIRouter("Router");

	public SimRouter(String deviceId) {
		rMemory = new Buffer<ByteArray>("Router Memory", MEMORY_SIZE);
		arpTable = new Hashtable();

		try {
			loadParameters(deviceId);
			loadArpTable();
		} catch (Exception e) {
			e.printStackTrace();
		}

		rProto = new RoutingProtocol(this);
		new ConsoleInput(this);
		start();
	}

	/**
	 * loads configuration info for the router
	 * 
	 * @param deviceId
	 *            device id of the router
	 */
	public void loadParameters(String deviceId) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(
				"Config//Config.txt"));
		// System.out.println("Loading Parameters.");
		String line;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(":");
			if (tokens[0].compareTo(deviceId) == 0) {
				// System.out.println(line);
				String paramName = tokens[1];
				if (paramName.compareTo("NUMOFPORTS") == 0) {
					interfaceCount = Integer.parseInt(tokens[2]);
					interfaces = new SimInterface[interfaceCount + 1]; // to
																		// start
																		// index
																		// from
																		// 1
					interfaceBuffers = (Buffer<ByteArray>[]) new Buffer[interfaceCount + 1]; // assuming
																								// port
																								// count
																								// is
																								// already
																								// loaded
					for (int i = 1; i <= interfaceCount; i++) {
						interfaceBuffers[i] = new Buffer<ByteArray>(
								"Port Buffer for Port=" + "i", 1);
						interfaces[i] = new SimInterface(deviceId, i, rMemory,
								interfaceBuffers[i]); // each port take instance
														// of the switch and its
														// id
					}
				} else if (paramName.compareTo("PORTIPMASK") == 0) {
					int interfaceId = Integer.parseInt(tokens[2]);
					interfaces[interfaceId].setIpAddress(new IpAddress(
							tokens[3]));
					interfaces[interfaceId].setSubnetMask(Integer
							.parseInt(tokens[4]));

					interfaces[interfaceId].setIsConfigured(true);
					System.out.println("Port IP Mask: "
							+ interfaceId
							+ " "
							+ interfaces[interfaceId].getIpAddress()
									.getString() + " "
							+ interfaces[interfaceId].getSubnetMask());
				} else if (paramName.compareTo("PORTMAC") == 0) {
					int interfaceId = Integer.parseInt(tokens[2]);
					interfaces[interfaceId].setMacAddress((byte) Integer
							.parseInt(tokens[3]));
					System.out.println("Port " + interfaceId + " MAC: "
							+ (interfaces[interfaceId].getMacAddress() & 0xFF));
					// System.out.println("PORTMAC:" +tokens[3]);
				}
			}
		}
	}

	/**
	 * loads the ARP table
	 */
	public void loadArpTable() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(
				"Config//ArpTable.txt"));
		String line;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(":");
			IpAddress dst = new IpAddress(tokens[0]);
			for (int i = 1; i <= interfaceCount; i++) {
				if (interfaces[i].getIpAddress() != null) {
					if (dst.sameSubnet(interfaces[i].getIpAddress(),
							interfaces[i].getSubnetMask())) {
						arpTable.put(tokens[0], tokens[1]);
						break;
					}
				}
			}
		}
	}

	/**
	 * returns the corresponding mac address of supplied ip address
	 * 
	 * @return mac address
	 */
	int getMacFromArpTable(IpAddress ip) {
		if (arpTable.get(ip.getString()) != null) {
			return Integer.parseInt((String) arpTable.get(ip.getString()));
		}
		return -1; // arp entry not found
	}

	/**
	 * sets status of the specified interface
	 * 
	 * @param interfaceId
	 *            id of the interface
	 * @param status
	 *            status of the interface, true if interface is on and false
	 *            otherwise
	 */
	public void setPortStatus(int interfaceId, boolean status) {
		if (interfaceId > 0 && interfaceId <= interfaceCount) {
			interfaces[interfaceId].setPortStatus(status);
			rProto.notifyPortStatusChange(interfaceId, status);
		} else
			System.out.println("Invalid port number");
	}

	/**
	 * gets packets from interfaces stored in the shared memory
	 * 
	 * @return packet in the form of ByteArray
	 */
	public ByteArray receivePacketsFromPorts() {
		try {
			synchronized (rMemory) {
				if (rMemory.empty())
					rMemory.wait();
				ByteArray p = rMemory.get();
				rMemory.notify();
				return p;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * sends ByteArray representing packets to interface
	 * 
	 * @param p
	 *            ByteArray representing packets
	 * @param outInterface
	 *            interface id to which the data should be forwarded to
	 */
	void sendPacketToInterface(ByteArray p, int outInterface) {
		try {
			if (interfaces[outInterface].getPortStatus() == true) {
				synchronized (interfaceBuffers[outInterface]) {
					if (interfaceBuffers[outInterface].full())
						return; // frame dropped
					else
						interfaceBuffers[outInterface].store(new ByteArray(p
								.getBytes()));
					interfaceBuffers[outInterface].notify();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ----------------------------------------------------------
	public void run() {
		try {
			while (true) {
				ByteArray b = receivePacketsFromPorts();
				String temps = "\tPacket received by routing engine";
				System.out.println(temps);
				//gui.updateGUI(temps);
				int interfaceId = b.getByteVal(0);
				//gui.updateGUI("int "+ interfaceId);

				Packet p = new Packet(b.getAt(1, b.getSize() - 1));
				IpAddress dst = p.getDstIp();

				int dstMac;
				ByteArray temp;
				int portMask = interfaces[interfaceId].getSubnetMask();
				rProto.setReceivedPostMask(portMask);

				// Check if packet is from an active port
				if (interfaces[interfaceId].getPortStatus() == true) {
					// Now handle packet

					// 1. Check if it is a route update packet
					if (dst.sameIp(new IpAddress(RP_MULTICAST_ADDRESS))) {
						rProto.notifyRouteUpdate(new ByteArray(p.getBytes()));
					}

					// 2. else if it is data packet, then route
					else {
						temps = "Received Packet: " + p.getString();
						System.out.println(temps);
						//gui.updateGUI(temps);
						// NextHop nextHop=rTable.getNextHop(dst);
						NextHop nextHop = rProto.getNextHop(dst);

						// 2.1 If No route available, then send destination
						// unreachable to sender
						if (nextHop == null) {// unreachable
							temps = "Destination Unreachable";
							System.out.println(temps);
							//gui.updateGUI(temps);
							String text = "Destination Unreachable";
							Packet rep = new Packet(
									interfaces[interfaceId].getIpAddress(),
									p.getSrcIp(), text.getBytes());

							nextHop = rProto.getNextHop(p.getSrcIp());

							if (nextHop == null) {
								/*temps = "Source Unreachable";
								System.out.println(temps);
								gui.updateGUI(temps);*/
								text = "Destination Unreachable.";
								 rep = new Packet(
										interfaces[interfaceId].getIpAddress(),
										p.getSrcIp(), text.getBytes());
								dstMac = getMacFromArpTable(p.getSrcIp());
								System.out.println("Reply to: "
										+ p.getSrcIp());
								//gui.updateGUI("Reply To: " + p.getSrcIp()+"  message:="+text);
								if (dstMac >= 0) {
									temp = new ByteArray(
											(rep.getBytes()).length + 1);
									temp.setByteVal(0, (byte) dstMac);
									temp.setAt(1, rep.getBytes());

									sendPacketToInterface(temp,interfaceId);
								}

							} else {
								dstMac = getMacFromArpTable(nextHop.getIp());
								if (dstMac >= 0) {
									temp = new ByteArray(
											(rep.getBytes()).length + 1);
									temp.setByteVal(0, (byte) dstMac);
									temp.setAt(1, rep.getBytes());

									sendPacketToInterface(temp,
											nextHop.getInterfaceId());
								} else {
									temps = "MAC not found in ARP Table; Dropping Packet";
									System.out.println(temps);
									//gui.updateGUI(temps);

								}
							}

						}

						// 2.2 A route found for the packet
						else {
							// 2.2.1 check if packet is for router itself
							if ((interfaces[nextHop.getInterfaceId()]
									.getIpAddress()).sameIp(nextHop.getIp())) {
								temps = "\tShowing Packet: " + p.getString();
								System.out.println(temps);
							//	gui.updateGUI(temps);
								String text = "Reply from router.";
								Packet rep = new Packet(
										interfaces[interfaceId].getIpAddress(),
										p.getSrcIp(), text.getBytes());
								dstMac = getMacFromArpTable(p.getSrcIp());
								System.out.println("Reply to: "
										+ nextHop.getIp()+"  message:="+text);
								//gui.updateGUI("Reply To: " + nextHop.getIp());
								if (dstMac >= 0) {
									temp = new ByteArray(
											(rep.getBytes()).length + 1);
									temp.setByteVal(0, (byte) dstMac);
									temp.setAt(1, rep.getBytes());

									sendPacketToInterface(temp,interfaceId);
								} else {
									temps = "MAC not found in ARP Table; Dropping Packet";
									System.out.println(temps);
									//gui.updateGUI(temps);

								}
							}

							// 2.2.2 else forward packet to next hop
							else {
								temps="Routing Packet: "
										+ p.getString();
								System.out.println(temps);
								//gui.updateGUI(temps);
								dstMac = getMacFromArpTable(nextHop.getIp());
								temp = new ByteArray((p.getBytes()).length + 1);
								temp.setByteVal(0, (byte) dstMac);
								temp.setAt(1, p.getBytes());

								sendPacketToInterface(temp,
										nextHop.getInterfaceId());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void WriteRTableInGUI(String row) {
		//gui.writeRoutingTable(row);
	}

	// ----------------------------------------------------------
	public static void main(String args[]) throws Exception {
		String deviceId = "R3";

		int argCount = args.length;
		if (argCount > 0)
			deviceId = args[0];

		// GUIRouter ui=new GUIRouter(deviceId);
		//gui.setVisible(true);
		//gui.setSize(500, 500);
		// gui.writeRoutingTable();

		SimRouter simrouter = new SimRouter(deviceId);
		simrouter.join();

	}
}

/**
 * class ConsoleInput deals with ping commands issued by the user from SimHost
 */
class ConsoleInput extends Thread {
	/**
	 * SimRouter representing the simulated router
	 */
	SimRouter sRouter;

	/**
	 * @param s
	 *            SimRouter representing the simulated router
	 */
	public ConsoleInput(SimRouter s) {
		sRouter = s;
		start();
	}

	public void run() {
		String userInput;
		try {
			BufferedReader inFromUser = new BufferedReader(
					new InputStreamReader(System.in));
			while (true) {
				userInput = inFromUser.readLine();
				System.out.println("Input " + userInput);

				int colon = userInput.indexOf(':');
				String command = userInput.substring(0, colon);

				if (command.compareTo("Down") == 0) {
					int interfaceId = Integer.parseInt(userInput
							.substring(colon + 1));
					sRouter.setPortStatus(interfaceId, false);
				}
				if (command.compareTo("Up") == 0) {
					int interfaceId = Integer.parseInt(userInput
							.substring(colon + 1));
					sRouter.setPortStatus(interfaceId, true);
				}
			}
		} catch (Exception e) {
		}
	}
}
