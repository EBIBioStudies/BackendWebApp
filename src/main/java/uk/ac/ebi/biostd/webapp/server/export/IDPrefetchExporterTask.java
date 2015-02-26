package uk.ac.ebi.biostd.webapp.server.export;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.export.ControlMessage.Type;

public class IDPrefetchExporterTask implements MTExportTask
{
 /**
  * 
  */
 public static final int MaxErrorRecoverAttempts=3;
 
// private final SliceManager sliceMngr;
 
 private final List<FormattingTask> tasks;
 private final ExporterStat stat;
 private final BlockingQueue<ControlMessage> controlQueue;
 private final AtomicBoolean stopFlag;

 private final int maxObjsPerThrSoft;
 private final QueryManager sbmQM;

 
 private final Logger log = LoggerFactory.getLogger(IDPrefetchExporterTask.class);

 private int genNo=0;
 private int laneNo;
 private Thread procThread;
 
 private int objectCount = 0;

 public IDPrefetchExporterTask( QueryManager qMgr, List<FormattingTask> tasks,
   ExporterStat stat, BlockingQueue<ControlMessage> controlQueue, AtomicBoolean stf, MTTaskConfig tCfg )
 {
  
  
  sbmQM = qMgr;
  
  this.tasks = tasks;
  this.stat = stat;
  this.controlQueue = controlQueue;

  maxObjsPerThrSoft = tCfg.getItemsPerThreadLimit();
  
  stopFlag = stf;
  
 }

 @Override
 public Thread getProcessingThread()
 {
  return procThread;
 }

 public int getLaneNo()
 {
  return laneNo;
 }

 public void setLaneNo(int laneNo)
 {
  this.laneNo = laneNo;
 }
 
 
 
 private boolean formatSubmission( Submission sbm, StringBuilder sb ) throws IOException
 {
  boolean needMoreData = false;
  
  for(FormattingTask ft : tasks)
  {
   if(!ft.confirmOutput())
    continue;

   needMoreData = true;
   
   if(ft.getOutQueue() == null)
    continue;
   
   int restart = 0;

   while(true)
   {
    try
    {
     sb.setLength(0);
     ft.getFormatter().format(sbm, sb);
     break;
    }
    catch(Exception e)
    {
     restart++;

     stat.incRecoverAttempt();

     if(restart > MaxErrorRecoverAttempts)
      throw e;
    }

   }
   
   putIntoQueue(ft.getOutQueue(), sb.toString());
  }
  
  return needMoreData;
 }
 
 private boolean checkStopFlag()
 {
  if(stopFlag.get())
  {
   log.debug("({}) Stop flag set. Sending FINISH message", Thread.currentThread().getName());
   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
   return true;
  }
  
  return false;
 }
 
 private boolean checkTTLexpired()
 {
  if(maxObjsPerThrSoft > 0 && objectCount > maxObjsPerThrSoft - sbmQM.getChunkSize() )
  {
   log.debug("({}) Thread TTL expired. Processed {} objects. Sending TTL message", Thread.currentThread().getName(), objectCount);

   sbmQM.close();

   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_TTL, this));
   return true;
  }
  
  return false;
 }
 
 @Override
 public void run()
 {
  procThread = Thread.currentThread();

  procThread.setName(procThread.getName() + "-ExporterTask-gen" + (++genNo) + "-lane" + laneNo);


  objectCount = 0;
  
  try
  {
   StringBuilder sb = new StringBuilder();

   boolean needMoreData = true;
   
   boolean needGroupLoop = true;
   
   mainLoop: while(needGroupLoop)
   {
    if(checkStopFlag()  || checkTTLexpired() )
     return;

    Collection<Submission> sbms = null;

    sbms = sbmQM.getSubmissions();

    if(sbms.size() == 0)
    {
     log.debug("({}) No more submissions to process", Thread.currentThread().getName());
     needGroupLoop = false;
    }

    for(Submission sbm : sbms)
    {
     if( checkStopFlag() )
      return;
      
     objectCount++;

     stat.incSubmissionCount();

     needMoreData = formatSubmission(sbm, sb);

     if(!needMoreData)
      break mainLoop;

    }

    if(checkStopFlag() || checkTTLexpired() )
     return;

   }

   
   if(!needMoreData)
    log.debug("({}) Output tasks don't need more data.", Thread.currentThread().getName());

   stat.addRecoverAttempt( sbmQM.getRecovers() );
   
   log.debug("({}) Thread terminating. Sending FINISH message", Thread.currentThread().getName());
   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));

  }
  catch(Throwable e)
  {
   e.printStackTrace();

   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_ERROR, this, e));
  }
  finally
  {
   sbmQM.close();
  }

 }
  
 public <T> void  putIntoQueue( BlockingQueue<T> queue, T o )
 {

  while(true)
  {
   try
   {
    queue.put(o);
   }
   catch(InterruptedException e)
   {
    if( stopFlag.get() )
     return;
    
    continue;
   }
   
   return;
  }

 }
}