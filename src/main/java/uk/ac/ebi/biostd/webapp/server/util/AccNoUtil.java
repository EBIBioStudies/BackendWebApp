package uk.ac.ebi.biostd.webapp.server.util;

public class AccNoUtil
{

 private static boolean checkCharClass( char ch )
 {
  return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '.' || ch == '-';
 }
 
 public static String encode( String src )
 {
  StringBuilder sb = null;
  
  int len = src.length();
  
  for( int i=0; i < len; i++ )
  {
   char ch = src.charAt(i);
   
   if( checkCharClass(ch) )
   {
    if( sb != null )
     sb.append(ch);
   }
   else
   {
    if( sb == null )
    {
     sb=new StringBuilder( len*4 );
     
     if( i > 0 )
      sb.append(src.substring(0,i) );
    }
    
    sb.append('#').append(Integer.toHexString(ch)).append('#');
   }
  }
  
  if( sb != null )
   return sb.toString();
  
  return src;
 }
 
 public static boolean checkAccNoStr( String src )
 {
  int len = src.length();
  
  for( int i=0; i < len; i++ )
  {
   char ch = src.charAt(i);
   
   if( !checkCharClass(ch) )
    return false;
  }
  
  return true;
 }
 
}
