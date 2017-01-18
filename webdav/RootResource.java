package uk.ac.ebi.biostd.webapp.server.webdav;

import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.jar.Manifest;

import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.util.ConcurrentDateFormat;

public class RootResource implements WebResource
{

 private WebResourceRoot wrRoot;
 private String mimeType="text/plain";
 
 public RootResource( WebResourceRoot wr )
 {
  wrRoot = wr;
 }
 
 @Override
 public long getLastModified()
 {
  return System.currentTimeMillis();
 }

 @Override
 public String getLastModifiedHttp()
 {
  return ConcurrentDateFormat.formatRfc1123(new Date(getLastModified()));
 }

 @Override
 public boolean exists()
 {
  return true;
 }

 @Override
 public boolean isVirtual()
 {
  return true;
 }

 @Override
 public boolean isDirectory()
 {
  return true;
 }

 @Override
 public boolean isFile()
 {
  return false;
 }

 @Override
 public boolean delete()
 {
  return false;
 }

 @Override
 public String getName()
 {
  return "/";
 }

 @Override
 public long getContentLength()
 {
  return 0;
 }

 @Override
 public String getCanonicalPath()
 {
  return "/";
 }

 @Override
 public boolean canRead()
 {
  return true;
 }

 @Override
 public String getWebappPath()
 {
  return "/";
 }

 @Override
 public String getETag()
 {
  return Long.toString( System.currentTimeMillis() );
 }

 @Override
 public void setMimeType(String mimeType)
 {
  this.mimeType=mimeType;
 }

 @Override
 public String getMimeType()
 {
  return mimeType;
 }

 @Override
 public InputStream getInputStream()
 {
  return null;
 }

 @Override
 public byte[] getContent()
 {
  return null;
 }

 @Override
 public long getCreation()
 {
  return System.currentTimeMillis();
 }

 @Override
 public URL getURL()
 {
  return null;
 }

 @Override
 public WebResourceRoot getWebResourceRoot()
 {
  return wrRoot;
 }

 @Override
 public Certificate[] getCertificates()
 {
  return null;
 }

 @Override
 public Manifest getManifest()
 {
  return null;
 }

 @Override
 public URL getCodeBase()
 {
  return null;
 }

}
