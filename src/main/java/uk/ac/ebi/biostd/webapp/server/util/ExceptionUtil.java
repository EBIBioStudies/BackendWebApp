package uk.ac.ebi.biostd.webapp.server.util;

public class ExceptionUtil
{

 public static Throwable unroll(Throwable e)
 {
  Throwable cause = null;
  Throwable result = e;

  while(null != (cause = result.getCause()) && (result != cause))
  {
   result = cause;
  }
  return result;
 }

}
