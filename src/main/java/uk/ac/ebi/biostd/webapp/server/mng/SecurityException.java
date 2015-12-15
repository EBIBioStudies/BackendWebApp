package uk.ac.ebi.biostd.webapp.server.mng;

public class SecurityException extends Exception
{

 private static final long serialVersionUID = 1L;

 public SecurityException()
 {
 }
 
 public SecurityException( String msg )
 {
  super( msg );
 }
 
 public SecurityException( String msg, Throwable cause )
 {
  super(msg,cause);
 }
}
