package uk.ac.ebi.biostd.webapp.server.mng.exception;

public class KeyExpiredException extends UserMngException
{

 public KeyExpiredException()
 {
  super("Key expired");
 }
 
 public KeyExpiredException(String msg)
 {
  super( msg );
 }

 
 public KeyExpiredException(String msg, Throwable cause)
 {
  super( msg, cause );
 }

}