package uk.ac.ebi.biostd.webapp.server.util;

public class FileNameUtil
{
 private static boolean checkCharClass( char ch )
 {
  return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '.' || ch == '-' || ch == '_'|| ch == ' ';
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
    
    sb.append('!');
    sb.append( convBits( ( ch>>12 ) & 0x3F ) );
    sb.append( convBits( ( ch>>6 ) & 0x3F ) );
    sb.append( convBits( ch & 0x3F ) );
   }
  }
  
  if( sb != null )
   return sb.toString();
  
  return src;
 }
 
 private static char convBits( int bits )
 {
  if( bits < 10 )
   return (char) ('0'+bits);

  bits-=10;
  
  if( bits < 26 )
   return (char) ('a'+bits);
 
  bits-=26;

  if( bits < 26 )
   return (char) ('A'+bits);
  
  bits-=26;

  if( bits == 0  )
   return '~';

  return '!';
 }
}
