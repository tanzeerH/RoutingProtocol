import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	GUIMain()
	{
		super("Main Gui");
		uiFrame=this;
		
		setLayout(new FlowLayout());
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
				System.out.println(dId);
				
			}
		});
		uiFrame.add(btn);
	}

}
