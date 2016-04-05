package uk.ac.ebi.biostd.webapp.server.mng.exception;

public class UserMngException extends Exception
{

 public UserMngException()
 {
 }
 
 public UserMngException(String msg)
 {
  super( msg );
 }

 
 public UserMngException(String msg, Throwable cause)
 {
  super( msg, cause );
 }

}
