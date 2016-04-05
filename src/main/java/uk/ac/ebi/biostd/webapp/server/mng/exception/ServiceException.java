package uk.ac.ebi.biostd.webapp.server.mng.exception;

public class ServiceException extends Exception
{

 private static final long serialVersionUID = 1L;

 public ServiceException()
 {
  super();
 }

 public ServiceException(String arg0)
 {
  super(arg0);
 }
 
 public ServiceException( String msg, Throwable t )
 {
  super( msg, t );
 }
}
