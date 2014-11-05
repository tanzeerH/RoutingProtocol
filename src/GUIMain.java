import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.omg.CORBA.TCKind;
import org.omg.CosNaming.NamingContextExtPackage.AddressHelper;

public class GUIMain extends JFrame {
	private JButton btn;
	private JLabel jLabel;
	private JFrame uiFrame;
	private int count = 0;
	private ArrayList<JButton> btnList = new ArrayList<JButton>();
	private ArrayList<String> btnTexList = new ArrayList<String>();
	private ConnectionDaemon conDaemon;

	GUIMain() {
		super("Main Gui");
		uiFrame = this;
		setLayout(new FlowLayout());
		try {
			loadDevices();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void setConDaemon(ConnectionDaemon con) {
		this.conDaemon = con;
	}

	public void setDevicesOnUI(String deviceId) {
		System.out.println("Update UI:  " + deviceId);
		btn = new JButton(deviceId);
		jLabel = new JLabel("                                            ");
		btnList.add(btn);
		btnTexList.add(deviceId);
		btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int sourceId = btnList.indexOf(e.getSource());
				String dId = btnTexList.get(sourceId);
				System.out.println(dId + " clicked");
				if (dId.contains("H"))
					getHostInfoById(dId);
				else if (dId.contains("R")) {
					String[] tokens = dId.split("-");
					getRouterInfoById(tokens[0]);
				}
			}
		});
		uiFrame.add(btn);
		uiFrame.add(jLabel);

	}

	private void getHostInfoById(String hId) {
		SimHost simHost = null;
		ArrayList<SimHost> hostList = conDaemon.getSimHostList();
		for (int i = 0; i < hostList.size(); i++) {
			String str = hostList.get(i).deviceId;
			if (str.equals(hId)) {
				simHost = hostList.get(i);
				break;
			}
		}
		if (simHost != null) {
			HostUI hu = new HostUI(hId, simHost);
			hu.setVisible(true);
			hu.setSize(300, 300);

			System.out.println(simHost.ipAddr.getString() + " " + simHost.macAddress);
		}
	}

	private void getRouterInfoById(String rId) {
		SimRouter simRouter = null;
		ArrayList<SimRouter> rList = conDaemon.getRouterList();
		for (int i = 0; i < rList.size(); i++) {
			String str = rList.get(i).deviceId;
			if (str.equals(rId)) {
				simRouter = rList.get(i);
				break;
			}
		}
		if (simRouter != null) {
			RoutertUI hu = new RoutertUI(rId, simRouter);
			hu.setVisible(true);
			hu.setSize(500, 500);

			System.out.println(simRouter.deviceId);
		}
	}

	public void loadDevices() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("Config//Config.txt"));
		// System.out.println("Loading Topology.");
		String line;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(":");
			if (tokens.length > 1 && tokens[1].compareTo("CONNECTSTO") == 0) {
				// System.out.println(line);
				String oneEnd = tokens[0];
				String otherEnd = tokens[2];
				if (!btnTexList.contains(oneEnd)) {
					setDevicesOnUI(oneEnd);
				}
				if (!btnTexList.contains(otherEnd)) {
					setDevicesOnUI(otherEnd);
				}

			}
		}
	}

	private class HostUI extends JFrame {
		JTextField txt_Ip = new JTextField(15);
		JTextField txt_Mac = new JTextField();
		JTextField txt_Mask = new JTextField(15);
		JTextField txt_gateWay = new JTextField(15);
		JLabel labelIp = new JLabel("Ip Address:  ");
		JLabel labelMac = new JLabel("Mac:   ");
		JLabel labelMask = new JLabel("Mask:   ");
		JLabel labelGateWay = new JLabel("Gateway:   ");
		JTextField txt_ping = new JTextField(15);
		JLabel labelcommand = new JLabel("Command:   ");
		JButton btnUpdate = new JButton("Update                 ");
		JButton btnGo = new JButton("Go");

		public HostUI(String id, SimHost simHost) {
			super(id);
			setLayout(new FlowLayout());

			txt_Ip.setText(simHost.ipAddr.getString());
			txt_Mask.setText("" + simHost.mask);
			txt_gateWay.setText(simHost.gateway.getString());
			// txt_Mac.setText(simHost.macAddress);
			add(labelIp);
			add(txt_Ip);

			// add(labelMac);
			// add(txt_Mac);
			add(labelGateWay);
			add(txt_gateWay);
			add(labelMask);
			add(txt_Mask);
			add(btnUpdate);
			add(labelcommand);
			add(txt_ping);
			add(btnGo);

		}
	}

	public class RoutertUI extends JFrame {

		public JTextArea txtrtTable;
		public JCheckBox jbox;
		public ArrayList<JCheckBox> chkList = new ArrayList<JCheckBox>();
		public ArrayList<Long> idList = new ArrayList<Long>();

		public RoutertUI(String id, final SimRouter simRouter) {
			super(id);
			setLayout(new FlowLayout());
			txtrtTable = new JTextArea(5, 40);
			int max = simRouter.rProto.getSize();
			for (int i = 0; i < max; i++) {
				String str = simRouter.rProto.getRow(i);
				if(!str.equals(""))
					txtrtTable.append("\n" + str);
			}
			add(new JScrollPane(txtrtTable));
			
			int num = simRouter.interfaceCount;
			for (int i = 1; i <=num; i++) {
				if (simRouter.interfaces[i]!=null && simRouter.interfaces[i].isConfigured) {
					long Id = simRouter.interfaces[i].interfaceId;
					boolean status = simRouter.interfaces[i].getPortStatus();
					jbox = new JCheckBox("" + Id, status);
					jbox.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {

							for (int x = 0; x < chkList.size(); x++) {

								if (e.getSource() == chkList.get(x)) {
									System.out.println("" + idList.get(x)+"status"+ chkList.get(x).isSelected());
									int id=idList.get(x).intValue();
									simRouter.rProto.notifyPortStatusChange(id,chkList.get(x).isSelected());
									
								}
							}

						}
					});
					chkList.add(jbox);
					idList.add(Id);
					add(jbox);
				}

			}
			

		}

	}
}
