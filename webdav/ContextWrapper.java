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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletRequest;
import javax.servlet.ServletSecurityElement;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.catalina.AccessLog;
import org.apache.catalina.Authenticator;
import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.ThreadBindingListener;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.NamingResourcesImpl;
import org.apache.juli.logging.Log;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.descriptor.web.ApplicationParameter;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.http.CookieProcessor;

public class ContextWrapper implements Context
{
 private Context context;
 private String docBase;

 
 public ContextWrapper(Context context, String docBase)
 {
  super();
  this.context = context;
  this.docBase = docBase;
 }

 @Override
 public boolean getAllowCasualMultipartParsing()
 {
  return context.getAllowCasualMultipartParsing();
 }

 @Override
 public void setAllowCasualMultipartParsing(boolean allowCasualMultipartParsing)
 {
  context.setAllowCasualMultipartParsing(allowCasualMultipartParsing);
 }

 @Override
 public Object[] getApplicationEventListeners()
 {
  return context.getApplicationEventListeners();
 }

 @Override
 public void setApplicationEventListeners(Object[] listeners)
 {
  context.setApplicationEventListeners(listeners);
 }

 @Override
 public Log getLogger()
 {
  return context.getLogger();
 }

 @Override
 public Object[] getApplicationLifecycleListeners()
 {
  return context.getApplicationLifecycleListeners();
 }

 @Override
 public ObjectName getObjectName()
 {
  return context.getObjectName();
 }

 @Override
 public String getDomain()
 {
  return context.getDomain();
 }

 @Override
 public String getMBeanKeyProperties()
 {
  return context.getMBeanKeyProperties();
 }

 @Override
 public void setApplicationLifecycleListeners(Object[] listeners)
 {
  context.setApplicationLifecycleListeners(listeners);
 }

 @Override
 public Pipeline getPipeline()
 {
  return context.getPipeline();
 }

 @Override
 public Cluster getCluster()
 {
  return context.getCluster();
 }

 @Override
 public String getCharset(Locale locale)
 {
  return context.getCharset(locale);
 }

 @Override
 public URL getConfigFile()
 {
  return context.getConfigFile();
 }

 @Override
 public void setCluster(Cluster cluster)
 {
  context.setCluster(cluster);
 }

 @Override
 public void setConfigFile(URL configFile)
 {
  context.setConfigFile(configFile);
 }

 @Override
 public int getBackgroundProcessorDelay()
 {
  return context.getBackgroundProcessorDelay();
 }

 @Override
 public boolean getConfigured()
 {
  return context.getConfigured();
 }

 @Override
 public void setConfigured(boolean configured)
 {
  context.setConfigured(configured);
 }

 @Override
 public void setBackgroundProcessorDelay(int delay)
 {
  context.setBackgroundProcessorDelay(delay);
 }

 @Override
 public boolean getCookies()
 {
  return context.getCookies();
 }

 @Override
 public void setCookies(boolean cookies)
 {
  context.setCookies(cookies);
 }

 @Override
 public String getSessionCookieName()
 {
  return context.getSessionCookieName();
 }

 @Override
 public String getName()
 {
  return context.getName();
 }

 @Override
 public void setName(String name)
 {
  context.setName(name);
 }

 @Override
 public void setSessionCookieName(String sessionCookieName)
 {
  context.setSessionCookieName(sessionCookieName);
 }

 @Override
 public boolean getUseHttpOnly()
 {
  return context.getUseHttpOnly();
 }

 @Override
 public void addLifecycleListener(LifecycleListener listener)
 {
  context.addLifecycleListener(listener);
 }

 @Override
 public Container getParent()
 {
  return context.getParent();
 }

 @Override
 public void setUseHttpOnly(boolean useHttpOnly)
 {
  context.setUseHttpOnly(useHttpOnly);
 }

 @Override
 public void setParent(Container container)
 {
  context.setParent(container);
 }

 @Override
 public LifecycleListener[] findLifecycleListeners()
 {
  return context.findLifecycleListeners();
 }

 @Override
 public String getSessionCookieDomain()
 {
  return context.getSessionCookieDomain();
 }

 @Override
 public void removeLifecycleListener(LifecycleListener listener)
 {
  context.removeLifecycleListener(listener);
 }

 @Override
 public void setSessionCookieDomain(String sessionCookieDomain)
 {
  context.setSessionCookieDomain(sessionCookieDomain);
 }

 @Override
 public void init() throws LifecycleException
 {
  context.init();
 }

 @Override
 public ClassLoader getParentClassLoader()
 {
  return context.getParentClassLoader();
 }

 @Override
 public String getSessionCookiePath()
 {
  return context.getSessionCookiePath();
 }

 @Override
 public void setParentClassLoader(ClassLoader parent)
 {
  context.setParentClassLoader(parent);
 }

 @Override
 public void setSessionCookiePath(String sessionCookiePath)
 {
  context.setSessionCookiePath(sessionCookiePath);
 }

 @Override
 public void start() throws LifecycleException
 {
  context.start();
 }

 @Override
 public Realm getRealm()
 {
  return context.getRealm();
 }

 @Override
 public boolean getSessionCookiePathUsesTrailingSlash()
 {
  return context.getSessionCookiePathUsesTrailingSlash();
 }

 @Override
 public void setRealm(Realm realm)
 {
  context.setRealm(realm);
 }

 @Override
 public void setSessionCookiePathUsesTrailingSlash(boolean sessionCookiePathUsesTrailingSlash)
 {
  context.setSessionCookiePathUsesTrailingSlash(sessionCookiePathUsesTrailingSlash);
 }

 @Override
 public void backgroundProcess()
 {
  context.backgroundProcess();
 }

 @Override
 public void addChild(Container child)
 {
  context.addChild(child);
 }

 @Override
 public boolean getCrossContext()
 {
  return context.getCrossContext();
 }

 @Override
 public String getAltDDName()
 {
  return context.getAltDDName();
 }

 @Override
 public void setAltDDName(String altDDName)
 {
  context.setAltDDName(altDDName);
 }

 @Override
 public void setCrossContext(boolean crossContext)
 {
  context.setCrossContext(crossContext);
 }

 @Override
 public void stop() throws LifecycleException
 {
  context.stop();
 }

 @Override
 public boolean getDenyUncoveredHttpMethods()
 {
  return context.getDenyUncoveredHttpMethods();
 }

 @Override
 public void setDenyUncoveredHttpMethods(boolean denyUncoveredHttpMethods)
 {
  context.setDenyUncoveredHttpMethods(denyUncoveredHttpMethods);
 }

 @Override
 public void addContainerListener(ContainerListener listener)
 {
  context.addContainerListener(listener);
 }

 @Override
 public void addPropertyChangeListener(PropertyChangeListener listener)
 {
  context.addPropertyChangeListener(listener);
 }

 @Override
 public String getDisplayName()
 {
  return context.getDisplayName();
 }

 @Override
 public void setDisplayName(String displayName)
 {
  context.setDisplayName(displayName);
 }

 @Override
 public Container findChild(String name)
 {
  return context.findChild(name);
 }

 @Override
 public boolean getDistributable()
 {
  return context.getDistributable();
 }

 @Override
 public void setDistributable(boolean distributable)
 {
  context.setDistributable(distributable);
 }

 @Override
 public Container[] findChildren()
 {
  return context.findChildren();
 }

 @Override
 public String getDocBase()
 {
  return docBase;
 }

 @Override
 public ContainerListener[] findContainerListeners()
 {
  return context.findContainerListeners();
 }

 @Override
 public void setDocBase(String docBase)
 {
  this.docBase=docBase;
 }

 @Override
 public void removeChild(Container child)
 {
  context.removeChild(child);
 }

 @Override
 public String getEncodedPath()
 {
  return context.getEncodedPath();
 }

 @Override
 public void removeContainerListener(ContainerListener listener)
 {
  context.removeContainerListener(listener);
 }

 @Override
 public boolean getIgnoreAnnotations()
 {
  return context.getIgnoreAnnotations();
 }

 @Override
 public void destroy() throws LifecycleException
 {
  //context.destroy();
 }

 @Override
 public void setIgnoreAnnotations(boolean ignoreAnnotations)
 {
  context.setIgnoreAnnotations(ignoreAnnotations);
 }

 @Override
 public void removePropertyChangeListener(PropertyChangeListener listener)
 {
  context.removePropertyChangeListener(listener);
 }

 @Override
 public LoginConfig getLoginConfig()
 {
  return context.getLoginConfig();
 }

 @Override
 public void fireContainerEvent(String type, Object data)
 {
  context.fireContainerEvent(type, data);
 }

 @Override
 public void setLoginConfig(LoginConfig config)
 {
  context.setLoginConfig(config);
 }

 @Override
 public LifecycleState getState()
 {
  return context.getState();
 }

 @Override
 public NamingResourcesImpl getNamingResources()
 {
  return context.getNamingResources();
 }

 @Override
 public String getStateName()
 {
  return context.getStateName();
 }

 @Override
 public void logAccess(Request request, Response response, long time, boolean useDefault)
 {
  context.logAccess(request, response, time, useDefault);
 }

 @Override
 public void setNamingResources(NamingResourcesImpl namingResources)
 {
  context.setNamingResources(namingResources);
 }

 @Override
 public String getPath()
 {
  return context.getPath();
 }

 @Override
 public void setPath(String path)
 {
  context.setPath(path);
 }

 @Override
 public String getPublicId()
 {
  return context.getPublicId();
 }

 @Override
 public void setPublicId(String publicId)
 {
  context.setPublicId(publicId);
 }

 @Override
 public AccessLog getAccessLog()
 {
  return context.getAccessLog();
 }

 @Override
 public boolean getReloadable()
 {
  return context.getReloadable();
 }

 @Override
 public int getStartStopThreads()
 {
  return context.getStartStopThreads();
 }

 @Override
 public void setReloadable(boolean reloadable)
 {
  context.setReloadable(reloadable);
 }

 @Override
 public boolean getOverride()
 {
  return context.getOverride();
 }

 @Override
 public void setStartStopThreads(int startStopThreads)
 {
  context.setStartStopThreads(startStopThreads);
 }

 @Override
 public void setOverride(boolean override)
 {
  context.setOverride(override);
 }

 @Override
 public boolean getPrivileged()
 {
  return context.getPrivileged();
 }

 @Override
 public void setPrivileged(boolean privileged)
 {
  context.setPrivileged(privileged);
 }

 @Override
 public File getCatalinaBase()
 {
  return context.getCatalinaBase();
 }

 @Override
 public File getCatalinaHome()
 {
  return context.getCatalinaHome();
 }

 @Override
 public ServletContext getServletContext()
 {
  return context.getServletContext();
 }

 @Override
 public int getSessionTimeout()
 {
  return context.getSessionTimeout();
 }

 @Override
 public void setSessionTimeout(int timeout)
 {
  context.setSessionTimeout(timeout);
 }

 @Override
 public boolean getSwallowAbortedUploads()
 {
  return context.getSwallowAbortedUploads();
 }

 @Override
 public void setSwallowAbortedUploads(boolean swallowAbortedUploads)
 {
  context.setSwallowAbortedUploads(swallowAbortedUploads);
 }

 @Override
 public boolean getSwallowOutput()
 {
  return context.getSwallowOutput();
 }

 @Override
 public void setSwallowOutput(boolean swallowOutput)
 {
  context.setSwallowOutput(swallowOutput);
 }

 @Override
 public String getWrapperClass()
 {
  return context.getWrapperClass();
 }

 @Override
 public void setWrapperClass(String wrapperClass)
 {
  context.setWrapperClass(wrapperClass);
 }

 @Override
 public boolean getXmlNamespaceAware()
 {
  return context.getXmlNamespaceAware();
 }

 @Override
 public void setXmlNamespaceAware(boolean xmlNamespaceAware)
 {
  context.setXmlNamespaceAware(xmlNamespaceAware);
 }

 @Override
 public boolean getXmlValidation()
 {
  return context.getXmlValidation();
 }

 @Override
 public void setXmlValidation(boolean xmlValidation)
 {
  context.setXmlValidation(xmlValidation);
 }

 @Override
 public boolean getXmlBlockExternal()
 {
  return context.getXmlBlockExternal();
 }

 @Override
 public void setXmlBlockExternal(boolean xmlBlockExternal)
 {
  context.setXmlBlockExternal(xmlBlockExternal);
 }

 @Override
 public boolean getTldValidation()
 {
  return context.getTldValidation();
 }

 @Override
 public void setTldValidation(boolean tldValidation)
 {
  context.setTldValidation(tldValidation);
 }

 @Override
 public JarScanner getJarScanner()
 {
  return context.getJarScanner();
 }

 @Override
 public void setJarScanner(JarScanner jarScanner)
 {
  context.setJarScanner(jarScanner);
 }

 @Override
 public Authenticator getAuthenticator()
 {
  return context.getAuthenticator();
 }

 @Override
 public void setLogEffectiveWebXml(boolean logEffectiveWebXml)
 {
  context.setLogEffectiveWebXml(logEffectiveWebXml);
 }

 @Override
 public boolean getLogEffectiveWebXml()
 {
  return context.getLogEffectiveWebXml();
 }

 @Override
 public InstanceManager getInstanceManager()
 {
  return context.getInstanceManager();
 }

 @Override
 public void setInstanceManager(InstanceManager instanceManager)
 {
  context.setInstanceManager(instanceManager);
 }

 @Override
 public void setContainerSciFilter(String containerSciFilter)
 {
  context.setContainerSciFilter(containerSciFilter);
 }

 @Override
 public String getContainerSciFilter()
 {
  return context.getContainerSciFilter();
 }

 @Override
 public void addApplicationListener(String listener)
 {
  context.addApplicationListener(listener);
 }

 @Override
 public void addApplicationParameter(ApplicationParameter parameter)
 {
  context.addApplicationParameter(parameter);
 }

 @Override
 public void addConstraint(SecurityConstraint constraint)
 {
  context.addConstraint(constraint);
 }

 @Override
 public void addErrorPage(ErrorPage errorPage)
 {
  context.addErrorPage(errorPage);
 }

 @Override
 public void addFilterDef(FilterDef filterDef)
 {
  context.addFilterDef(filterDef);
 }

 @Override
 public void addFilterMap(FilterMap filterMap)
 {
  context.addFilterMap(filterMap);
 }

 @Override
 public void addFilterMapBefore(FilterMap filterMap)
 {
  context.addFilterMapBefore(filterMap);
 }

 @Override
 public void addInstanceListener(String listener)
 {
  context.addInstanceListener(listener);
 }

 @Override
 public void addLocaleEncodingMappingParameter(String locale, String encoding)
 {
  context.addLocaleEncodingMappingParameter(locale, encoding);
 }

 @Override
 public void addMimeMapping(String extension, String mimeType)
 {
  context.addMimeMapping(extension, mimeType);
 }

 @Override
 public void addParameter(String name, String value)
 {
  context.addParameter(name, value);
 }

 @Override
 public void addRoleMapping(String role, String link)
 {
  context.addRoleMapping(role, link);
 }

 @Override
 public void addSecurityRole(String role)
 {
  context.addSecurityRole(role);
 }

 @Override
 public void addServletMapping(String pattern, String name)
 {
  context.addServletMapping(pattern, name);
 }

 @Override
 public void addServletMapping(String pattern, String name, boolean jspWildcard)
 {
  context.addServletMapping(pattern, name, jspWildcard);
 }

 @Override
 public void addWatchedResource(String name)
 {
  context.addWatchedResource(name);
 }

 @Override
 public void addWelcomeFile(String name)
 {
  context.addWelcomeFile(name);
 }

 @Override
 public void addWrapperLifecycle(String listener)
 {
  context.addWrapperLifecycle(listener);
 }

 @Override
 public void addWrapperListener(String listener)
 {
  context.addWrapperListener(listener);
 }

 @Override
 public Wrapper createWrapper()
 {
  return context.createWrapper();
 }

 @Override
 public String[] findApplicationListeners()
 {
  return context.findApplicationListeners();
 }

 @Override
 public ApplicationParameter[] findApplicationParameters()
 {
  return context.findApplicationParameters();
 }

 @Override
 public SecurityConstraint[] findConstraints()
 {
  return context.findConstraints();
 }

 @Override
 public ErrorPage findErrorPage(int errorCode)
 {
  return context.findErrorPage(errorCode);
 }

 @Override
 public ErrorPage findErrorPage(String exceptionType)
 {
  return context.findErrorPage(exceptionType);
 }

 @Override
 public ErrorPage[] findErrorPages()
 {
  return context.findErrorPages();
 }

 @Override
 public FilterDef findFilterDef(String filterName)
 {
  return context.findFilterDef(filterName);
 }

 @Override
 public FilterDef[] findFilterDefs()
 {
  return context.findFilterDefs();
 }

 @Override
 public FilterMap[] findFilterMaps()
 {
  return context.findFilterMaps();
 }

 @Override
 public String[] findInstanceListeners()
 {
  return context.findInstanceListeners();
 }

 @Override
 public String findMimeMapping(String extension)
 {
  return context.findMimeMapping(extension);
 }

 @Override
 public String[] findMimeMappings()
 {
  return context.findMimeMappings();
 }

 @Override
 public String findParameter(String name)
 {
  return context.findParameter(name);
 }

 @Override
 public String[] findParameters()
 {
  return context.findParameters();
 }

 @Override
 public String findRoleMapping(String role)
 {
  return context.findRoleMapping(role);
 }

 @Override
 public boolean findSecurityRole(String role)
 {
  return context.findSecurityRole(role);
 }

 @Override
 public String[] findSecurityRoles()
 {
  return context.findSecurityRoles();
 }

 @Override
 public String findServletMapping(String pattern)
 {
  return context.findServletMapping(pattern);
 }

 @Override
 public String[] findServletMappings()
 {
  return context.findServletMappings();
 }

 @Override
 public String findStatusPage(int status)
 {
  return context.findStatusPage(status);
 }

 @Override
 public int[] findStatusPages()
 {
  return context.findStatusPages();
 }

 @Override
 public ThreadBindingListener getThreadBindingListener()
 {
  return context.getThreadBindingListener();
 }

 @Override
 public void setThreadBindingListener(ThreadBindingListener threadBindingListener)
 {
  context.setThreadBindingListener(threadBindingListener);
 }

 @Override
 public String[] findWatchedResources()
 {
  return context.findWatchedResources();
 }

 @Override
 public boolean findWelcomeFile(String name)
 {
  return context.findWelcomeFile(name);
 }

 @Override
 public String[] findWelcomeFiles()
 {
  return context.findWelcomeFiles();
 }

 @Override
 public String[] findWrapperLifecycles()
 {
  return context.findWrapperLifecycles();
 }

 @Override
 public String[] findWrapperListeners()
 {
  return context.findWrapperListeners();
 }

 @Override
 public boolean fireRequestInitEvent(ServletRequest request)
 {
  return context.fireRequestInitEvent(request);
 }

 @Override
 public boolean fireRequestDestroyEvent(ServletRequest request)
 {
  return context.fireRequestDestroyEvent(request);
 }

 @Override
 public void reload()
 {
  context.reload();
 }

 @Override
 public void removeApplicationListener(String listener)
 {
  context.removeApplicationListener(listener);
 }

 @Override
 public void removeApplicationParameter(String name)
 {
  context.removeApplicationParameter(name);
 }

 @Override
 public void removeConstraint(SecurityConstraint constraint)
 {
  context.removeConstraint(constraint);
 }

 @Override
 public void removeErrorPage(ErrorPage errorPage)
 {
  context.removeErrorPage(errorPage);
 }

 @Override
 public void removeFilterDef(FilterDef filterDef)
 {
  context.removeFilterDef(filterDef);
 }

 @Override
 public void removeFilterMap(FilterMap filterMap)
 {
  context.removeFilterMap(filterMap);
 }

 @Override
 public void removeInstanceListener(String listener)
 {
  context.removeInstanceListener(listener);
 }

 @Override
 public void removeMimeMapping(String extension)
 {
  context.removeMimeMapping(extension);
 }

 @Override
 public void removeParameter(String name)
 {
  context.removeParameter(name);
 }

 @Override
 public void removeRoleMapping(String role)
 {
  context.removeRoleMapping(role);
 }

 @Override
 public void removeSecurityRole(String role)
 {
  context.removeSecurityRole(role);
 }

 @Override
 public void removeServletMapping(String pattern)
 {
  context.removeServletMapping(pattern);
 }

 @Override
 public void removeWatchedResource(String name)
 {
  context.removeWatchedResource(name);
 }

 @Override
 public void removeWelcomeFile(String name)
 {
  context.removeWelcomeFile(name);
 }

 @Override
 public void removeWrapperLifecycle(String listener)
 {
  context.removeWrapperLifecycle(listener);
 }

 @Override
 public void removeWrapperListener(String listener)
 {
  context.removeWrapperListener(listener);
 }

 @Override
 public String getRealPath(String path)
 {
  return context.getRealPath(path);
 }

 @Override
 public int getEffectiveMajorVersion()
 {
  return context.getEffectiveMajorVersion();
 }

 @Override
 public void setEffectiveMajorVersion(int major)
 {
  context.setEffectiveMajorVersion(major);
 }

 @Override
 public int getEffectiveMinorVersion()
 {
  return context.getEffectiveMinorVersion();
 }

 @Override
 public void setEffectiveMinorVersion(int minor)
 {
  context.setEffectiveMinorVersion(minor);
 }

 @Override
 public JspConfigDescriptor getJspConfigDescriptor()
 {
  return context.getJspConfigDescriptor();
 }

 @Override
 public void setJspConfigDescriptor(JspConfigDescriptor descriptor)
 {
  context.setJspConfigDescriptor(descriptor);
 }

 @Override
 public void addServletContainerInitializer(ServletContainerInitializer sci, Set<Class< ? >> classes)
 {
  context.addServletContainerInitializer(sci, classes);
 }

 @Override
 public boolean getPaused()
 {
  return context.getPaused();
 }

 @Override
 public boolean isServlet22()
 {
  return context.isServlet22();
 }

 @Override
 public Set<String> addServletSecurity(Dynamic registration, ServletSecurityElement servletSecurityElement)
 {
  return context.addServletSecurity(registration, servletSecurityElement);
 }

 @Override
 public void setResourceOnlyServlets(String resourceOnlyServlets)
 {
  context.setResourceOnlyServlets(resourceOnlyServlets);
 }

 @Override
 public String getResourceOnlyServlets()
 {
  return context.getResourceOnlyServlets();
 }

 @Override
 public boolean isResourceOnlyServlet(String servletName)
 {
  return context.isResourceOnlyServlet(servletName);
 }

 @Override
 public String getBaseName()
 {
  return context.getBaseName();
 }

 @Override
 public void setWebappVersion(String webappVersion)
 {
  context.setWebappVersion(webappVersion);
 }

 @Override
 public String getWebappVersion()
 {
  return context.getWebappVersion();
 }

 @Override
 public void setFireRequestListenersOnForwards(boolean enable)
 {
  context.setFireRequestListenersOnForwards(enable);
 }

 @Override
 public boolean getFireRequestListenersOnForwards()
 {
  return context.getFireRequestListenersOnForwards();
 }

 @Override
 public void setPreemptiveAuthentication(boolean enable)
 {
  context.setPreemptiveAuthentication(enable);
 }

 @Override
 public boolean getPreemptiveAuthentication()
 {
  return context.getPreemptiveAuthentication();
 }

 @Override
 public void setSendRedirectBody(boolean enable)
 {
  context.setSendRedirectBody(enable);
 }

 @Override
 public boolean getSendRedirectBody()
 {
  return context.getSendRedirectBody();
 }

 @Override
 public Loader getLoader()
 {
  return context.getLoader();
 }

 @Override
 public void setLoader(Loader loader)
 {
  context.setLoader(loader);
 }

 @Override
 public WebResourceRoot getResources()
 {
  return context.getResources();
 }

 @Override
 public void setResources(WebResourceRoot resources)
 {
  context.setResources(resources);
 }

 @Override
 public Manager getManager()
 {
  return context.getManager();
 }

 @Override
 public void setManager(Manager manager)
 {
  context.setManager(manager);
 }

 @Override
 public void setAddWebinfClassesResources(boolean addWebinfClassesResources)
 {
  context.setAddWebinfClassesResources(addWebinfClassesResources);
 }

 @Override
 public boolean getAddWebinfClassesResources()
 {
  return context.getAddWebinfClassesResources();
 }

 @Override
 public void addPostConstructMethod(String clazz, String method)
 {
  context.addPostConstructMethod(clazz, method);
 }

 @Override
 public void addPreDestroyMethod(String clazz, String method)
 {
  context.addPreDestroyMethod(clazz, method);
 }

 @Override
 public void removePostConstructMethod(String clazz)
 {
  context.removePostConstructMethod(clazz);
 }

 @Override
 public void removePreDestroyMethod(String clazz)
 {
  context.removePreDestroyMethod(clazz);
 }

 @Override
 public String findPostConstructMethod(String clazz)
 {
  return context.findPostConstructMethod(clazz);
 }

 @Override
 public String findPreDestroyMethod(String clazz)
 {
  return context.findPreDestroyMethod(clazz);
 }

 @Override
 public Map<String, String> findPostConstructMethods()
 {
  return context.findPostConstructMethods();
 }

 @Override
 public Map<String, String> findPreDestroyMethods()
 {
  return context.findPreDestroyMethods();
 }

 @Override
 public ClassLoader bind(boolean usePrivilegedAction, ClassLoader originalClassLoader)
 {
  return context.bind(usePrivilegedAction, originalClassLoader);
 }

 @Override
 public void unbind(boolean usePrivilegedAction, ClassLoader originalClassLoader)
 {
  context.unbind(usePrivilegedAction, originalClassLoader);
 }

 @Override
 public Object getNamingToken()
 {
  return context.getNamingToken();
 }

 @Override
 public CookieProcessor getCookieProcessor()
 {
  return context.getCookieProcessor();
 }

 @Override
 public void setCookieProcessor(CookieProcessor cp)
 {
  context.setCookieProcessor(cp);
 }


}
