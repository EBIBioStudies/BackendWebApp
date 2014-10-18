package uk.ac.ebi.biostd.webapp.server.webdav;

import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;

public class RootResource implements WebResource
{

 private WebResourceRoot wrRoot;
 
 @Override
 public long getLastModified()
 {
  // TODO Auto-generated method stub
  return 0;
 }

 @Override
 public String getLastModifiedHttp()
 {
  // TODO Auto-generated method stub
  return null;
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
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public long getContentLength()
 {
  // TODO Auto-generated method stub
  return 0;
 }

 @Override
 public String getCanonicalPath()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public boolean canRead()
 {
  // TODO Auto-generated method stub
  return false;
 }

 @Override
 public String getWebappPath()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public String getETag()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public void setMimeType(String mimeType)
 {
  // TODO Auto-generated method stub
  
 }

 @Override
 public String getMimeType()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public InputStream getInputStream()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public byte[] getContent()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public long getCreation()
 {
  // TODO Auto-generated method stub
  return 0;
 }

 @Override
 public URL getURL()
 {
  // TODO Auto-generated method stub
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

}
