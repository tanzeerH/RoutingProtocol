import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class GUIRouter  extends JFrame{
	
	private JLabel lblconfig,lblrtable,lblarea;
	private JTextArea txtrtTable,txtHints;
	GUIRouter(String name)
	{
		super(name);
		//btnRouter=new JButton(name);
		lblconfig=new JLabel("Rounter Configaration:       ");
		lblrtable=new JLabel("Routing Table");
		lblarea=new JLabel("Hints....");
		txtrtTable=new JTextArea(5,40);
		txtHints=new JTextArea(5,40);
		
		
		setLayout(new FlowLayout());
		System.out.println("inside gui");
		add(lblrtable);
		add(new JScrollPane(txtrtTable));
		add(lblarea);
		add(new JScrollPane(txtHints));
	}
		
	public void writeRoutingTable(String txt)
	{
		txtrtTable.append(txt+"\n");
	}
	public void updateGUI(String txt)
	{
		txtHints.append(txt+"\n");
	}
	

}
