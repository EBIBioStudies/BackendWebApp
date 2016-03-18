package uk.ac.ebi.biostd.webapp.server.mng;

import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.Charsets;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.util.FileUtil;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class AccountActivation
{
 public static class ActivationInfo
 {
  public String email;
  public String key;
  public UUID uuidkey;
 }
 
 public static String createActivationKey( String email, UUID key )
 {
  StringBuilder sb = new StringBuilder(512);
  
  byte[] emailBts = email.getBytes(Charsets.UTF_8);
  
  int xor = (int)( key.getLeastSignificantBits() & 0xFFL );
  
  for( byte b : emailBts )
  {
   String hex = Integer.toHexString( Byte.toUnsignedInt(b) ^ xor );
   
   if( hex.length() == 1 )
    sb.append('0');
   
   sb.append( hex );
  }
   
   
  sb.append('O');
  sb.append(key.toString());
  
  return sb.toString();
 }
 
 public static ActivationInfo decodeActivationKey(String key)
 {
  int pos = key.indexOf('O');
  
  if( pos <= 0 || pos >= key.length()-1)
   return null;
  
  String encEMail = key.substring(0,pos);
  
  int encLen = encEMail.length();
  
  if( encLen % 2 == 1 )
   return null;
  
  ActivationInfo res = new ActivationInfo();
  
  res.key = key.substring(pos+1);
  

  UUID id = null;
  try 
  {
   id = UUID.fromString(res.key);
  }
  catch( Exception e )
  {
   return null;
  }
  
  int xor = (int)( id.getLeastSignificantBits() & 0xFFL );
  res.uuidkey = id;
 
  
  byte [] bytes = new byte[encLen/2];
  
  for( int i=0; i < encLen; i+=2 )
  {
   try 
   {
    bytes[i/2] = (byte)(Integer.parseInt(encEMail.substring(i,i+2), 16) ^ xor);
   }
   catch( Exception e )
   {
    return null;
   }
  }
  
  res.email = new String(bytes,Charsets.UTF_8); 
  
  return res;
 }
 
 public static boolean sendActivationRequest(User u, UUID key, String url)
 {
  Path txtFile = BackendConfig.getActivationEmailPlainTextFile();

  String textBody = null;
  
  Path htmlFile = BackendConfig.getActivationEmailHtmlFile();
  
  String htmlBody = null;
  
  String actKey = AccountActivation.createActivationKey(u.getEmail(), key);
  
  try
  {
   if( txtFile != null )
   {
    textBody = FileUtil.readFile(txtFile.toFile(), Charsets.UTF_8);
    
    if( url != null )
     textBody = textBody.replaceAll(BackendConfig.ActivateURLPlaceHolderRx, url);
    
    textBody = textBody.replaceAll(BackendConfig.ActivateKeyPlaceHolderRx, actKey);
    
    if(u.getFullName() != null )
     textBody = textBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
    else
     textBody = textBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, "");
   }
   
   if( htmlFile != null )
   {
    htmlBody = FileUtil.readFile(htmlFile.toFile(), Charsets.UTF_8);

    if( url != null )
     htmlBody = htmlBody.replaceAll(BackendConfig.ActivateURLPlaceHolderRx, url);
    
    htmlBody = htmlBody.replaceAll(BackendConfig.ActivateKeyPlaceHolderRx, actKey);

    if(u.getFullName() != null )
     htmlBody = htmlBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
    else
     htmlBody = htmlBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, "");

   }
  }
  catch(Exception e)
  {
   e.printStackTrace();
   
   return false;
  }
  
  return BackendConfig.getServiceManager().getEmailService().sendMultipartEmail(u.getEmail(), BackendConfig.getActivationEmailSubject(), textBody, htmlBody);
 }
 
 
 static void testActivationKey(String[] args )
 {
  String email = "ebi@ebi.ac.uk";
  UUID uu = UUID.randomUUID();
  
  System.out.println("Mail: "+email+" UUID: "+uu);
  
  String key = createActivationKey(email, uu);
  
  System.out.println("Key: "+key);
  
  ActivationInfo ainf = decodeActivationKey(key);
  

  if( ainf != null && email.equals(ainf.email) && uu.equals(ainf.uuidkey) )
   System.out.println("OK");
  else
   System.out.println("FAIL");
  
  System.out.println("Mail: "+ainf.email+" UUID: "+ainf.uuidkey+" UUIDstr: "+ainf.key);
 }
}
