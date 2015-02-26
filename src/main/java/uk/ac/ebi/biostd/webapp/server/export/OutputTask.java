package uk.ac.ebi.biostd.webapp.server.export;

import java.util.concurrent.BlockingQueue;

import uk.ac.ebi.biostd.webapp.server.export.ControlMessage.Type;


public class OutputTask implements Runnable
{
 private final Appendable out;
 private final BlockingQueue<Object> inQueue;
 private final BlockingQueue<ControlMessage> controlQueue;
 private final String name;
 
 public OutputTask( String name, Appendable out, BlockingQueue<Object> inQueue, BlockingQueue<ControlMessage> controlQueue)
 {
  this.out = out;
  this.inQueue = inQueue;
  this.controlQueue = controlQueue;
  this.name=name;
 }
 
 
 @Override
 public void run()
 {
  
  Thread.currentThread().setName(name);
  
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
   {
    putIntoQueue(new ControlMessage(Type.OUTPUT_FINISH, this));
    return;
   }
   
   try
   {
    out.append(str);
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
}
