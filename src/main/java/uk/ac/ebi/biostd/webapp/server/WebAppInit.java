package uk.ac.ebi.biostd.webapp.server;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Persistence;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.webresources.StandardRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceFactory;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceInitExceprion;
import uk.ac.ebi.biostd.webapp.server.util.ParamPool;
import uk.ac.ebi.biostd.webapp.server.util.ResourceBundleParamPool;
import uk.ac.ebi.biostd.webapp.server.util.ServletContextParamPool;
import uk.ac.ebi.biostd.webapp.server.webdav.ContextWrapper;
import uk.ac.ebi.biostd.webapp.server.webdav.WebdavServlet;

/**
 * Application Lifecycle Listener implementation class WebAppInit
 *
 */

public class WebAppInit implements ServletContextListener 
{
 public static final String DefaultName = "_default_";

 static final String DBParamPrefix = "db";
 static final String ServiceParamPrefix = "biostd";

 
 private Logger log = null;

 private static final long dayInMills = TimeUnit.DAYS.toMillis(1);

 public WebAppInit()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
  
//  java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.WARNING);
//  java.util.logging.Logger.getLogger("com.mchange").setLevel(Level.WARNING);

 }

 /**
  * @see ServletContextListener#contextDestroyed(ServletContextEvent)
  */
 @Override
 public void contextDestroyed(ServletContextEvent arg0)
 {
 }

 
 private void readConfig( ParamPool config ) throws ServiceInitExceprion
 {

  //  java.util.logging.Logger.getLogger("org.hibernate.SQL").setLevel(Level.FINEST);

  log.info("Initializing BioStudies web app");

  Map<String, Map<String, Object>> profMap = new HashMap<>();

  Matcher dbMtch = Pattern.compile("^" + DBParamPrefix + "(\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");
  Matcher serviceMtch = Pattern.compile("^" + ServiceParamPrefix + "(\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");

  boolean confOk = true;

  Enumeration<String> pNames = config.getNames();

  while(pNames.hasMoreElements())
  {
   String key = pNames.nextElement();
   String val = config.getParameter(key);

   dbMtch.reset(key);

   if(dbMtch.matches())
   {

    String profile = null;
    String param = null;

    if(dbMtch.groupCount() == 3)
    {
     profile = dbMtch.group(2);
     param = dbMtch.group(3);
    }
    else
     param = dbMtch.group(dbMtch.groupCount());

    if( profile == null )
     profile = DefaultName;
    
    Map<String, Object> cm = profMap.get(profile);

    if(cm == null)
     profMap.put(profile, cm = new TreeMap<>());

    cm.put(param, val);
   }

   else
   {
    serviceMtch.reset(key);

    if(serviceMtch.matches())
    {

     String taskName = null;
     String param = null;

     if(serviceMtch.groupCount() == 3)
     {
      taskName = serviceMtch.group(2);
      param = serviceMtch.group(3);
     }
     else if( serviceMtch.groupCount() == 2 )
      param = serviceMtch.group(2);
     else
      log.warn("Invalid parameter {} will be ignored.", key);

     try
     {
      if(!BackendConfig.readParameter(param, val))
       log.warn("Unknown configuration parameter: " + key + " will be ignored");
     }
     catch(ServiceConfigException e)
     {
      log.error("Invalid parameter value: " + key + "=" + val);
      confOk = false;
     }
    }
   }
  }

  if(!confOk)
  {
   throw new RuntimeException("BioStudies webapp initialization failed");
  }

  for(Map.Entry<String, Map<String, Object>> me : profMap.entrySet())
  {
   if(me.getKey() == null)
    continue;

   BackendConfig.setEntityManagerFactory( Persistence.createEntityManagerFactory("BioStdCoreModel", me.getValue()));
  }
  
  BackendConfig.setServiceManager( ServiceFactory.createService( ) );
  
 }
 
 
 @Override
 public void contextInitialized(ServletContextEvent ctxEv)
 {
  ServletContext ctx = ctxEv.getServletContext();
  
  ParamPool config = null;

  ResourceBundle rb = null;

  try
  {
   rb = ResourceBundle.getBundle("testconfig");
  }
  catch(MissingResourceException ex)
  {
  }

  if(rb != null)
   config = new ResourceBundleParamPool(rb);
  else
   config = new ServletContextParamPool(ctx);
  
  
  readConfig(config);
  
  WebResourceRoot resRoot = null;
  
  try
  {
   resRoot = (WebResourceRoot)ctx.getAttribute("org.apache.catalina.resources");
  }
  catch(Throwable e)
  {
   throw new RuntimeException("Can't find WebResourceRoot. Not Tomcat 8?");
  }
  
  String dataDir = BackendConfig.getDataDirectory();
  String dataMount = BackendConfig.getDataMountPath();
  
  if( dataDir == null )
   throw new RuntimeException(BackendConfig.DataDirParameter+" parameter is not set");

  if( dataMount == null )
   throw new RuntimeException(BackendConfig.DataMountPathParameter+" parameter is not set");
  
  resRoot.createWebResourceSet(ResourceSetType.POST, dataMount, dataDir, null, "/");
  
  StandardRoot davRoot = new StandardRoot( new ContextWrapper(resRoot.getContext(), dataDir) );
  davRoot.setCachingAllowed(false);
  
  try
  {
   davRoot.start();
  }
  catch(LifecycleException e)
  {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }
  
  ctx.setAttribute("davRoot", davRoot);
  
  ServletRegistration.Dynamic dn = ctx.addServlet("WebDAV",WebdavServlet.class);

  dn.setAsyncSupported(true);
  dn.setInitParameter(BackendConfig.DataMountPathParameter, dataMount);
  dn.setInitParameter("listings", "true");
  dn.setInitParameter("readonly", "false");
  dn.addMapping(dataMount.endsWith("/")?dataMount+'*':dataMount+"/*");
  
 }
	
}
