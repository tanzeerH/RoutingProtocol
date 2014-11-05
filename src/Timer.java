
public class Timer extends Thread {
	
	public Timer()
	{
		start();
	}
	@Override
	public void run() {
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.run();
	}

}
