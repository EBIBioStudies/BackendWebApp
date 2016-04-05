package uk.ac.ebi.biostd.webapp.server.mng.exception;

public class UserNotActiveException extends UserMngException
{

 public UserNotActiveException()
 {
  super("User not active");
 }
 
 public UserNotActiveException(String msg)
 {
  super( msg );
 }

 
 public UserNotActiveException(String msg, Throwable cause)
 {
  super( msg, cause );
 }

}