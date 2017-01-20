package uk.ac.ebi.biostd.webapp.server.util;

public class LongNumber extends Number
{
 private static final long serialVersionUID = 1L;

 private long val=0;
 
 public void setValue( long v )
 {
  val = v;
 }
 
 @Override
 public double doubleValue()
 {
  return longValue();
 }

 @Override
 public float floatValue()
 {
  return longValue();
 }

 @Override
 public int intValue()
 {
  return (int)longValue();
 }

 @Override
 public long longValue()
 {
  return val;
 }
 
 public void add( long v )
 {
  val+=v;
 }

}
