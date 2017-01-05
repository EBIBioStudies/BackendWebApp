package uk.ac.ebi.biostd.webapp.server.export;

import java.util.concurrent.BlockingQueue;

import uk.ac.ebi.biostd.webapp.server.export.ControlMessage.Type;


public class OutputTask implements Runnable
{
 private final Appendable out;
 private final BlockingQueue<Object> inQueue;
 private final BlockingQueue<ControlMessage> controlQueue;
 private final String name;
 
 private int outCount=0;
 
 public OutputTask( String name, Appendable out, BlockingQueue<Object> inQueue, BlockingQueue<ControlMessage> controlQueue)
 {
  this.out = out;
  this.inQueue = inQueue;
  this.controlQueue = controlQueue;
  this.name=name;
 }
 

 public String getName()
 {
  return name;
 }
 
 @Override
 public void run()
 {
  
  Thread.currentThread().setName(name);
  
  boolean terminate=false;
  
  while( true )
  {
   Object o = null;
   
   while( true )
   {
    try
    {
     o = inQueue.take();
     break;
    }
    catch(InterruptedException e)
    {
    }
   }
   
   String str = o.toString();
   
   if( str == null )
    terminate = true;
   
   if( terminate && inQueue.size() == 0  )
   {
    putIntoQueue(new ControlMessage(Type.OUTPUT_FINISH, this));
    return;
   }
   
   try
   {
    out.append(str);
    outCount++;
   }
   catch(Exception e)
   {
    e.printStackTrace();
    
    putIntoQueue(new ControlMessage( Type.OUTPUT_ERROR, this,e ));
    return;
   }
   
  }
  
 }

 public BlockingQueue<Object> getIncomingQueue()
 {
  return inQueue;
 }
 
 void  putIntoQueue( ControlMessage o )
 {

  while(true)
  {
   try
   {
    controlQueue.put(o);
    return;
   }
   catch(InterruptedException e)
   {
   }
  }

 }


 public int getOutCount()
 {
  return outCount;
 }

}
