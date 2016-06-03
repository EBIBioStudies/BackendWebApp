package uk.ac.ebi.biostd.webapp.server.export;

public class TaskConfigException extends Exception
{
 private static final long serialVersionUID = 1L;

 public TaskConfigException()
 {}
 
 public TaskConfigException( String msg )
 {
  super( msg );
 }

 public TaskConfigException( String msg, Throwable cause )
 {
  super( msg, cause );
 }

 
}
