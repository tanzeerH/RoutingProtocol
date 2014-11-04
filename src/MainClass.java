import org.w3c.dom.CDATASection;


public class MainClass {
	
	public static void main(String[] args) {
		
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		
		GUIMain guiMain=new GUIMain();
		guiMain.setVisible(true);
		guiMain.setSize(500,500);
		ConnectionDaemon cDaemon=new ConnectionDaemon(guiMain);
		guiMain.setConDaemon(cDaemon);
		
		try {
			cDaemon.join();
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
	
	}

}
