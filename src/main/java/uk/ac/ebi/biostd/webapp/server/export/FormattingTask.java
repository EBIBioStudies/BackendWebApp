package uk.ac.ebi.biostd.webapp.server.export;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import uk.ac.ebi.biostd.out.Formatter;

public class FormattingTask
{
 private final Formatter          formatter;
 private final BlockingQueue<Object> outQueue;
 
 private final AtomicLong maxCount;
 
 public FormattingTask(Formatter formatter, BlockingQueue<Object> queue, long limit)
 {
  super();
  this.formatter = formatter;
  
  outQueue = queue;
  
  if( limit > 0 )
   maxCount = new AtomicLong(limit);
  else
   maxCount = null;
 }

 
 public boolean confirmOutput()
 {
  if( maxCount == null )
   return true;
  
  long cnt = maxCount.decrementAndGet();
  
  return cnt >= 0;
 }
 
 public Formatter getFormatter()
 {
  return formatter;
 }

 public BlockingQueue<Object> getOutQueue()
 {
  return outQueue;
 }

}
