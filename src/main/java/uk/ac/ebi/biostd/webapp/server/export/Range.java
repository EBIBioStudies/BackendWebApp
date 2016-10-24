package uk.ac.ebi.biostd.webapp.server.export;

import java.util.Arrays;

public class Range
{
 private long min;
 private long max;
 private boolean locked;
 private long[] ids;
 
 public Range(long min, long max)
 {
  this.min = min;
  this.max = max;
 }

 public long getMin()
 {
  return min;
 }

 public void setMin(long min)
 {
  this.min = min;
 }

 public long getMax()
 {
  return max;
 }

 public void setMax(long max)
 {
  this.max = max;
 }

 public boolean isLocked()
 {
  return locked;
 }

 public void setLocked(boolean locked)
 {
  this.locked = locked;
 }
 
 @Override
 public String toString()
 {
  String str = "["+min+","+max+"]";
  
  if( ids!= null )
   str+=" Ids: "+ids.length;
  
  return str;
 }

 public void setIds(long[] submissionIds, int offset, int end)
 {
  ids = Arrays.copyOfRange(submissionIds, offset, end);
 }

 public long[] getIds()
 {
  return ids;
 }
}

