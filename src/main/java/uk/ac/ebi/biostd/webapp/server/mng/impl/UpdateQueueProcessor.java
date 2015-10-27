package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.in.pageml.PageMLElements;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class UpdateQueueProcessor implements Runnable
{
 private static Logger log = null;
 
 
 private BlockingQueue<String> queue;
 private Thread myThread;
 
 private boolean shutdown=false;
 
 public UpdateQueueProcessor()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
  
  this.queue = new ArrayBlockingQueue<String>(100);

  myThread = new Thread(this,"UpdateQueueProcessor");
 }

 public void start()
 {
  myThread.start();
 }
 
 public void shutdown()
 {
  shutdown = true;
  
  myThread.interrupt();
 }

 public void put( String msg ) throws InterruptedException
 {
  queue.put(msg);
 }
 
 @Override
 public void run()
 {
  int scanPeriod = BackendConfig.getUpdateScanPeriod();
  
  while( ! shutdown )
  {
   if( queue.size() == 0 )
   {
    long start = System.currentTimeMillis();
    
    long sleepTime = scanPeriod;
    while( true )
    {
     try
     {
      Thread.sleep(sleepTime);
      break;
     }
     catch( InterruptedException ie )
     {
      if( shutdown )
       break;
      
      sleepTime = scanPeriod - ( System.currentTimeMillis() - start );
      
      if( sleepTime <= 0  )
       break;
     }
     catch(Exception e)
     {
      e.printStackTrace();
      break;
     }
    }
   }

   while( queue.size() > 0 )
   {
    String fName = String.valueOf(System.currentTimeMillis()/1000)+"-"+BackendConfig.getSeqNumber()+".xml";
    
    File f = BackendConfig.getSubmissionUpdatePath().resolve(fName).toFile();
    
    try
    {
     sinkQueue(f);
    }
    catch(IOException e)
    {
     f.delete();
     e.printStackTrace();
     continue;
    }
    
    if( BackendConfig.getUpdateListenerURLPrefix() != null )
    {
     try
     {
      URL lUrl = new URL( BackendConfig.getUpdateListenerURLPrefix()+fName+BackendConfig.getUpdateListenerURLPostfix() );
      
      HttpURLConnection conn = (HttpURLConnection)lUrl.openConnection();
      conn.setConnectTimeout(5000);
      
      if( conn.getResponseCode() != HttpURLConnection.HTTP_OK )
       log.error("Update listener '"+lUrl+"' returned invalid responce code: "+conn.getResponseCode());
      
     }
     catch(Exception e)
     {
      log.error("Can't connect update listener: "+e.getMessage());
     }
    }
   }
  }
  

 }

 private void sinkQueue( File f ) throws IOException
 {
  int outCap = BackendConfig.getMaxUpdatesPerFile();
  
  try( Writer out = new FileWriter( f ) )
  {
   out.append("<").append(PageMLElements.ROOT.getElementName()).append(">\n");
   out.append("<").append(PageMLElements.SUBMISSIONS.getElementName()).append(">\n");
   
   String msg = null;
   
   while( outCap > 0 && (msg=queue.poll()) != null )
   {
    outCap--;

    out.append(msg);
   }
   
   out.append("</").append(PageMLElements.SUBMISSIONS.getElementName()).append(">\n");
   out.append("</").append(PageMLElements.ROOT.getElementName()).append(">\n");
  }

 }

}
