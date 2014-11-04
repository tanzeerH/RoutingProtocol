import org.w3c.dom.CDATASection;


public class MainClass {
	
	public static void main(String[] args) {
		ConnectionDaemon cDaemon=new ConnectionDaemon();
		GUIMain guiMain=new GUIMain();
		guiMain.setVisible(true);
		guiMain.setSize(500,500);
		cDaemon.setGuiMain(guiMain);
		try {
			cDaemon.join();
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
	
	}

}
