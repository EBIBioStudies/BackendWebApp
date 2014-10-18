package uk.ac.ebi.biostd.webapp.server.webdav;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.TrackedWebResource;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;

public class VirtualWebResourceRoot implements WebResourceRoot
{
 private WebResourceRoot realRoot;

 public VirtualWebResourceRoot( WebResourceRoot rrt )
 {
  realRoot = rrt;
 }
 
 @Override
 public WebResource getResource(String path)
 {
  return realRoot.getResource(path);
 }


 @Override
 public String[] list(String path)
 {
  return realRoot.list(path);
 }
 
 @Override
 public boolean mkdir(String path)
 {
  return realRoot.mkdir(path);
 }

 @Override
 public boolean write(String path, InputStream is, boolean overwrite)
 {
  return realRoot.write(path, is, overwrite);
 }
 
 @Override
 public WebResource[] getResources(String path)
 {
  return realRoot.getResources(path);
 }

 @Override
 public WebResource getClassLoaderResource(String path)
 {
  return realRoot.getClassLoaderResource(path);
 }

 @Override
 public WebResource[] getClassLoaderResources(String path)
 {
  return realRoot.getClassLoaderResources(path);
 }


 @Override
 public Set<String> listWebAppPaths(String path)
 {
  return realRoot.listWebAppPaths(path);
 }

 @Override
 public void addLifecycleListener(LifecycleListener listener)
 {
  realRoot.addLifecycleListener(listener);
 }

 @Override
 public LifecycleListener[] findLifecycleListeners()
 {
  return realRoot.findLifecycleListeners();
 }

 @Override
 public WebResource[] listResources(String path)
 {
  return realRoot.listResources(path);
 }

 @Override
 public void removeLifecycleListener(LifecycleListener listener)
 {
  realRoot.removeLifecycleListener(listener);
 }

 @Override
 public void init() throws LifecycleException
 {
  realRoot.init();
 }



 @Override
 public void start() throws LifecycleException
 {
  realRoot.start();
 }

 @Override
 public void createWebResourceSet(ResourceSetType type, String webAppMount, URL url, String internalPath)
 {
  realRoot.createWebResourceSet(type, webAppMount, url, internalPath);
 }

 @Override
 public void createWebResourceSet(ResourceSetType type, String webAppMount, String base, String archivePath, String internalPath)
 {
  realRoot.createWebResourceSet(type, webAppMount, base, archivePath, internalPath);
 }

 @Override
 public void stop() throws LifecycleException
 {
  realRoot.stop();
 }

 @Override
 public void addPreResources(WebResourceSet webResourceSet)
 {
  realRoot.addPreResources(webResourceSet);
 }

 @Override
 public WebResourceSet[] getPreResources()
 {
  return realRoot.getPreResources();
 }

 @Override
 public void addJarResources(WebResourceSet webResourceSet)
 {
  realRoot.addJarResources(webResourceSet);
 }

 @Override
 public WebResourceSet[] getJarResources()
 {
  return realRoot.getJarResources();
 }

 @Override
 public void addPostResources(WebResourceSet webResourceSet)
 {
  realRoot.addPostResources(webResourceSet);
 }

 @Override
 public void destroy() throws LifecycleException
 {
  realRoot.destroy();
 }

 @Override
 public WebResourceSet[] getPostResources()
 {
  return realRoot.getPostResources();
 }

 @Override
 public Context getContext()
 {
  return realRoot.getContext();
 }

 @Override
 public void setContext(Context context)
 {
  realRoot.setContext(context);
 }

 @Override
 public void setAllowLinking(boolean allowLinking)
 {
  realRoot.setAllowLinking(allowLinking);
 }

 @Override
 public LifecycleState getState()
 {
  return realRoot.getState();
 }

 @Override
 public String getStateName()
 {
  return realRoot.getStateName();
 }

 @Override
 public boolean getAllowLinking()
 {
  return realRoot.getAllowLinking();
 }

 @Override
 public void setCachingAllowed(boolean cachingAllowed)
 {
  realRoot.setCachingAllowed(cachingAllowed);
 }

 @Override
 public boolean isCachingAllowed()
 {
  return realRoot.isCachingAllowed();
 }

 @Override
 public void setCacheTtl(long ttl)
 {
  realRoot.setCacheTtl(ttl);
 }

 @Override
 public long getCacheTtl()
 {
  return realRoot.getCacheTtl();
 }

 @Override
 public void setCacheMaxSize(long cacheMaxSize)
 {
  realRoot.setCacheMaxSize(cacheMaxSize);
 }

 @Override
 public long getCacheMaxSize()
 {
  return realRoot.getCacheMaxSize();
 }

 @Override
 public void setCacheObjectMaxSize(int cacheObjectMaxSize)
 {
  realRoot.setCacheObjectMaxSize(cacheObjectMaxSize);
 }

 @Override
 public int getCacheObjectMaxSize()
 {
  return realRoot.getCacheObjectMaxSize();
 }

 @Override
 public void setTrackLockedFiles(boolean trackLockedFiles)
 {
  realRoot.setTrackLockedFiles(trackLockedFiles);
 }

 @Override
 public boolean getTrackLockedFiles()
 {
  return realRoot.getTrackLockedFiles();
 }

 @Override
 public void backgroundProcess()
 {
  realRoot.backgroundProcess();
 }

 @Override
 public void registerTrackedResource(TrackedWebResource trackedResource)
 {
  realRoot.registerTrackedResource(trackedResource);
 }

 @Override
 public void deregisterTrackedResource(TrackedWebResource trackedResource)
 {
  realRoot.deregisterTrackedResource(trackedResource);
 }

 @Override
 public List<URL> getBaseUrls()
 {
  return realRoot.getBaseUrls();
 }
 

}
