package uk.ac.ebi.biostd.webapp.server.export;

public class TerminationException extends Exception
{
 private static final long serialVersionUID = 1L;

 public TerminationException()
 {
  super("User termination request");
 }
 
 public TerminationException( String msg )
 {
  super( msg );
 }


}
