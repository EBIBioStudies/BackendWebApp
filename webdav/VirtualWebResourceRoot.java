/**

Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author Mikhail Gostev <gostev@gmail.com>

**/

package uk.ac.ebi.biostd.webapp.server.webdav;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
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
import org.apache.catalina.webresources.EmptyResource;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class VirtualWebResourceRoot implements WebResourceRoot
{
 protected static final String[] EMPTY_STRING_ARRAY = new String[0];

 
 public static final String personalDir="Personal";
 
 private WebResourceRoot realRoot;
 private String mountPath;
 private String userDir;
 private String groupDir;

 public VirtualWebResourceRoot( WebResourceRoot rrt, String mountPath  )
 {
  realRoot = rrt;
  
  if( mountPath.endsWith("/") )
   mountPath=mountPath.substring(0,mountPath.length()-1);
  
  userDir = BackendConfig.UsersDir;
  groupDir = BackendConfig.GroupsDir;
  
  if( ! userDir.startsWith("/") )
   userDir = "/"+userDir;

  if( ! groupDir.startsWith("/") )
   groupDir = "/"+groupDir ;
  
  if( ! userDir.endsWith("/") )
   userDir = userDir + "/";

  if( ! groupDir.endsWith("/") )
   groupDir = groupDir + "/";
  
  this.mountPath = mountPath;
 }
 
 private String translatePath( String path )
 {
  User user = ThreadUser.getUser();
  
  if( path.startsWith(personalDir,1) && path.charAt(0) == '/' )
   return mountPath+userDir+user.getId()+path.substring(personalDir.length()+1);
  
  int pos = path.indexOf('/', 1);
  
  String grpName = pos > 0 ? path.substring(1,pos):path.substring(1);
  
  
  if(user.getGroups() != null)
  {
   for(UserGroup ug : user.getGroups())
   {
    if(ug.getName().equals(grpName) && ug.isProject())
     return mountPath+groupDir + ug.getId() + (pos > 0 ? path.substring(pos) : "/");
   }
  }
   
  return null;
 }
 
 @Override
 public WebResource getResource(String path)
 {
  if( path.equals("/") )
   return realRoot.getResource(path);
//   return new RootResource( this );
  
  path = translatePath(path);
  
  if( path == null )
   return new EmptyResource(this, path);
  
  return realRoot.getResource(path);
 }


 @Override
 public String[] list(String path)
 {
  if( path.equals("/") )
  {
   ArrayList<String> rDir = new ArrayList<String>(5);
   
   rDir.add(personalDir);

   User user = ThreadUser.getUser();
   
   if( user.getGroups() != null )
   {
    for( UserGroup ug : user.getGroups() )
    {
     if( ug.isProject() )
      rDir.add(ug.getName());
    }
   }
   
   return rDir.toArray( new String[ rDir.size() ] );
  }

  path = translatePath(path);
  
  if( path == null )
   return EMPTY_STRING_ARRAY;

  return realRoot.list(path);
 }
 
 @Override
 public boolean mkdir(String path)
 {
  path = translatePath(path);
  
  if( path == null )
   return false;
  
  return realRoot.mkdir(path);
 }

 @Override
 public boolean write(String path, InputStream is, boolean overwrite)
 {
  path = translatePath(path);
  
  if( path == null )
   return false;

  return realRoot.write(path, is, overwrite);
 }
 
 @Override
 public WebResource[] getResources(String path)
 {
  path = translatePath(path);
  
  if( path == null )
   return null;

  return realRoot.getResources(path);
 }

 @Override
 public WebResource getClassLoaderResource(String path)
 {
  path = translatePath(path);
  
  if( path == null )
   return null;

  return realRoot.getClassLoaderResource(path);
 }

 @Override
 public WebResource[] getClassLoaderResources(String path)
 {
  path = translatePath(path);
  
  if( path == null )
   return null;

  return realRoot.getClassLoaderResources(path);
 }


 @Override
 public Set<String> listWebAppPaths(String path)
 {
  path = translatePath(path);
  
  if( path == null )
   return null;

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
  path = translatePath(path);
  
  if( path == null )
   return null;

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
