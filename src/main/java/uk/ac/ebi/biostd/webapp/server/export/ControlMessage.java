package uk.ac.ebi.biostd.webapp.server.export;



public class ControlMessage
{
 public enum Type
 {
  OUTPUT_FINISH,
  OUTPUT_ERROR,
  PROCESS_FINISH,
  PROCESS_TTL,
  PROCESS_ERROR,
  TERMINATE
 }

 private final String threadName;
 private final Type      type;
 private final Object    subject;
 private Throwable exception;

 public ControlMessage(String tName, Type type, Object subject)
 {
  super();
  this.type = type;
  this.subject = subject;
  threadName = tName;
 }

 public ControlMessage(String tName, Type type, Object subject, Throwable exception)
 {
  super();
  this.type = type;
  this.subject = subject;
  this.exception = exception;
  threadName = tName;
 }

 public ControlMessage(Type type, Object subject)
 {
  this(Thread.currentThread().getName(),type,subject);
 }

 public ControlMessage(Type type, Object subject, Throwable exception)
 {
  this(Thread.currentThread().getName(),type,subject,exception);
 }
 
 public Type getType()
 {
  return type;
 }

 public Object getSubject()
 {
  return subject;
 }

 public Throwable getException()
 {
  return exception;
 }

 public String getThreadName()
 {
  return threadName;
 }

}
