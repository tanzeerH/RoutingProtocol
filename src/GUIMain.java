import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;


public class GUIMain extends JFrame{
	private JButton btn;
	private JLabel jLabel;
	private JFrame uiFrame;
	private int count=0;
	private ArrayList<JButton> btnList=new ArrayList<JButton>();
	private ArrayList<String> btnTexList=new ArrayList<String>();
	private ConnectionDaemon conDaemon;
	GUIMain()
	{
		super("Main Gui");
		uiFrame=this;
		setLayout(new FlowLayout());
		try {
			loadDevices();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public void setConDaemon(ConnectionDaemon con)
	{
		this.conDaemon=con;
	}
	public void setDevicesOnUI(String deviceId)
	{
		System.out.println("Update UI:  " + deviceId);
		btn=new JButton(deviceId);
		jLabel=new JLabel("                                            ");
		btnList.add(btn);
		btnTexList.add(deviceId);
		btn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int sourceId=btnList.indexOf(e.getSource());
				String dId=btnTexList.get(sourceId);
				System.out.println(dId+" clicked");
				if(dId.contains("H"))
					getHostInfoById(dId);
				
			}
		});
		uiFrame.add(btn);
		uiFrame.add(jLabel);
		uiFrame.invalidate();
	}
	private void getHostInfoById(String hId)
	{
		SimHost simHost=null;
		ArrayList<SimHost> hostList=conDaemon.getSimHostList(); 
		for(int i=0;i<hostList.size();i++)
		{
			String str=hostList.get(i).deviceId;
			if(str.equals(hId))
			{
				simHost=hostList.get(i);
				break;
			}
		}
		if(simHost!=null)
		{
			System.out.println(simHost.ipAddr.getString()+" "+ simHost.macAddress);
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
				if(!btnTexList.contains(oneEnd))
				{
					setDevicesOnUI(oneEnd);
				}
				if(!btnTexList.contains(otherEnd))
				{
					setDevicesOnUI(otherEnd);
				}
				
			}
		}
	}
}
