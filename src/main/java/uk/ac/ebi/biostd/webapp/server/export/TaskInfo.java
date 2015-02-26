package uk.ac.ebi.biostd.webapp.server.export;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskInfo extends TimerTask
{
 private final Logger log = LoggerFactory.getLogger(TaskInfo.class);
 
 private Timer timer;

 private ExportTask task;
 private long timeZero =-1;
 private int enqueueTimeMoD =-1;

 private int periodMin =-1;

 private boolean enqueued;
 
 public boolean isEnqueued()
 {
  return enqueued;
 }

 public void setEnqueued(boolean enqueued)
 {
  this.enqueued = enqueued;
 }
 
 public long getEnqueueTime()
 {
  return enqueueTimeMoD;
 }
 
 public void setEnqueueTime(int enqueueTime)
 {
  this.enqueueTimeMoD = enqueueTime;
 }
 
 public int getPeriod()
 {
  return periodMin;
 }
 
 public void setPeriod(int period)
 {
  this.periodMin = period;
 }
 
 public ExportTask getTask()
 {
  return task;
 }

 public void setTask(ExportTask task)
 {
  this.task = task;
 }

 public long getTimeZero()
 {
  return timeZero;
 }

 public void setTimeZero(long tz)
 {
  this.timeZero = tz;
 }


 @Override
 public void run()
 {
  log.info("Starting scheduled task: " + task.getName());

  new Thread(new Runnable()
  {

   @Override
   public void run()
   {
    try
    {
     task.export(task.getTaskConfig().getLimit(-1), task.getTaskConfig().getThreads(-1));
    }
    catch(Throwable e)
    {
     log.error("Export error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
     e.printStackTrace();
    }

    log.info("Finishing scheduled task: " + task.getName());

   }
  }, "Task '"+task.getName()+"' export").start();
 }

 public Timer getTimer()
 {
  return timer;
 }

 public void setTimer(Timer timer)
 {
  this.timer = timer;
 }

}
