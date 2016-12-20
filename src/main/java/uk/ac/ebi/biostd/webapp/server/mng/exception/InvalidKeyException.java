package uk.ac.ebi.biostd.webapp.server.mng.exception;

public class InvalidKeyException extends UserMngException
{

 private static final long serialVersionUID = 1L;


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