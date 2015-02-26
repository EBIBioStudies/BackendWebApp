package uk.ac.ebi.biostd.webapp.server.email;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import uk.ac.ebi.biostd.webapp.server.util.ParamPool;

public class Email
{
 public static final String SMTPHostParam="SMTPHost";
 public static final String recipientParam="to";
 public static final String errorRecipientParam="errorsTo";
 public static final String fromParam="from";
 
 private static Email defaultInstance;

 
 private InternetAddress toAddr; 
 private InternetAddress errorsToAddr; 
 private final InternetAddress fromAddr;
 
 private final Properties properties;
 
 public Email( ParamPool prms, String pfx ) throws EmailInitException
 {
  String str = prms.getParameter(pfx+SMTPHostParam);
  
  if( str == null )
   throw new EmailInitException("Parameter "+pfx+SMTPHostParam+" is not defined");
  
  properties = new Properties();
  
  properties.setProperty("mail.smtp.host", str);
  
  str = prms.getParameter(pfx+fromParam);

  if( str == null )
   throw new EmailInitException("Parameter "+pfx+fromParam+" is not defined");
  
  try
  {
   fromAddr = new InternetAddress( str );
  }
  catch(AddressException e)
  {
   throw new EmailInitException("Invalid 'From' address: "+str);
  }
  
  str = prms.getParameter(pfx+recipientParam);

//  if( str == null )
//   throw new EmailInitException("Parameter "+pfx+recipientParam+" is not defined");
  
  try
  {
   toAddr = new InternetAddress( str );
  }
  catch(AddressException e)
  {
   throw new EmailInitException("Invalid 'To' address: "+str);
  }

  str = prms.getParameter(pfx+errorRecipientParam);

  if( str == null )
   errorsToAddr = toAddr;
  else
  {
   try
   {
    toAddr = new InternetAddress( str );
   }
   catch(AddressException e)
   {
    throw new EmailInitException("Invalid 'To' address for error messages ("+pfx+errorRecipientParam+"): "+str);
   }
  }
  
  if( errorsToAddr == null && toAddr == null )
   throw new EmailInitException("Parameter "+pfx+recipientParam+" or "+pfx+errorRecipientParam+" should be defined");
 
 }
 
 public static Email getDefaultInstance()
 {
  return defaultInstance;
 }

 public static void setDefaultInstance(Email defaultInstance)
 {
  Email.defaultInstance = defaultInstance;
 }
 
 public boolean sendErrorAnnouncement(String subj, String msg, Throwable t)
 {

  if( errorsToAddr == null )
   return false;
  
  Session session = Session.getDefaultInstance(properties);

  try
  {
   MimeMessage message = new MimeMessage(session);

   message.setFrom(fromAddr);
   message.addHeader("X-Priority", "1 (Highest)");
   message.addHeader("X-MSMail-Priority", "High");
   message.addHeader("Importance", "High");


   message.addRecipient(Message.RecipientType.TO, errorsToAddr);

   if( subj == null )
    subj = "BioStd error message";
   
   message.setSubject(subj);

   StringBuffer buf = new StringBuffer();

   buf.append("BioStd has encountered some problem:\n"+msg+"\n");
   
   if( t != null )
   {
   
    StringWriter stkOut = new StringWriter();
   
    t.printStackTrace(new PrintWriter(stkOut) );
    
    buf.append("\n\n"+stkOut.toString());
   }   
   
   
   message.setText(buf.toString());

   Transport.send(message);
  }
  catch(MessagingException mex)
  {
   mex.printStackTrace();
   
   return false;
  }

  return true;
 }

 public boolean sendAnnouncement(String subj, String msg)
 {

  if( toAddr == null )
   return false;
  
  Session session = Session.getDefaultInstance(properties);

  try
  {
   MimeMessage message = new MimeMessage(session);

   message.setFrom(fromAddr);

   message.addRecipient(Message.RecipientType.TO, toAddr);
 
   if( subj == null )
    subj = "BioStd info message";
   
   message.setSubject(subj);

  
   message.setText(msg);

   Transport.send(message);
  }
  catch(MessagingException mex)
  {
   mex.printStackTrace();
   
   return false;
  }

  return true;
 }


}
