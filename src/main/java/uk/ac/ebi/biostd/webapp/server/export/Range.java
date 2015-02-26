package uk.ac.ebi.biostd.webapp.server.export;

public class Range
{
 long min;
 long max;
 boolean locked;
 
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
  return "["+min+","+max+"]";
 }
}

