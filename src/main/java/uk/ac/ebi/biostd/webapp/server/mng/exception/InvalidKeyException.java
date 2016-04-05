package uk.ac.ebi.biostd.webapp.server.mng.exception;

public class InvalidKeyException extends UserMngException
{

 public InvalidKeyException()
 {
  super("Invalid key");
 }
 
 public InvalidKeyException(String msg)
 {
  super( msg );
 }

 
 public InvalidKeyException(String msg, Throwable cause)
 {
  super( msg, cause );
 }

}