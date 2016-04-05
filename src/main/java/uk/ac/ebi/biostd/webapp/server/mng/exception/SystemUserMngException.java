package uk.ac.ebi.biostd.webapp.server.mng.exception;

public class SystemUserMngException extends UserMngException
{

 public SystemUserMngException()
 {
  super("System error");
 }
 
 public SystemUserMngException(String msg)
 {
  super( msg );
 }

 
 public SystemUserMngException(String msg, Throwable cause)
 {
  super( msg, cause );
 }

}