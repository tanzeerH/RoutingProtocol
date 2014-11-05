


public class MyTimer extends Thread
{
    RoutingProtocol rP;		
    boolean running;		
    int duration;

    int type; //Two types: invalidate timer and update timer
    int interFaceId;

    public MyTimer(RoutingProtocol r, int t)
    {			
        rP=r;			
        running=false;
        type=t;
        interFaceId=-1;
    }
    public MyTimer(RoutingProtocol r,int t,int id)
    {
        rP=r;			
        running=false;
        type=t;
        interFaceId=id;
    }
    public void startTimer(int timeout_duration)//timeout_duration is second 
    {
        running=true;			
        duration=timeout_duration*1000;
        start();                    			
    }
    public void stopTimer() 
    {          
        //try{timer.cancel();}catch(Exception e){e.printStackTrace();} 
        running=false;
        this.interrupt();
        System.out.println("From Routing Protocol : Timer Class : Stopping Timer: "+interFaceId+"\n");
    }		
    public boolean isRunning()
    {
        return running;
    }	
    public void run() 
    {
        System.out.println("From Routing Protocol : Timer Class : Scheduling Timer: "+interFaceId+"\n");
        try
        {
            //Start Timer
            Thread.sleep(duration); //duration in milliseconds								
            //Timer Expired
            running=false;				
            rP.handleTimerEvent(type,interFaceId);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }			



    
}
