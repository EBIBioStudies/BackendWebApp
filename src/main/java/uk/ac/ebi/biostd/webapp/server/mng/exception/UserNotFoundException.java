package uk.ac.ebi.biostd.webapp.server.mng.exception;

public class UserNotFoundException extends UserMngException
{

 public UserNotFoundException()
 {
  super("User not found");
 }
 
 public UserNotFoundException(String msg)
 {
  super( msg );
 }

 
 public UserNotFoundException(String msg, Throwable cause)
 {
  super( msg, cause );
 }

}