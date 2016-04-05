package uk.ac.ebi.biostd.webapp.server.mng.exception;

public class UserAlreadyActiveException extends UserMngException
{

 public UserAlreadyActiveException()
 {
 }
 
 public UserAlreadyActiveException(String msg)
 {
  super( msg );
 }

 
 public UserAlreadyActiveException(String msg, Throwable cause)
 {
  super( msg, cause );
 }

}