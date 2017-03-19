package uk.ac.ebi.biostd.webapp.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.config.ConfigurationException;
import uk.ac.ebi.biostd.webapp.server.config.ConfigurationManager;
import uk.ac.ebi.biostd.webapp.server.util.ServletContextParamPool;


/**
 * Application Lifecycle Listener implementation class WebAppInit
 *
 */

public class WebAppInit implements ServletContextListener 
{
// public static final String DefaultName = "_default_";

 static final String ApplicationConfigNode = "BioStdWebApp";
 static final String ApplicationBasePathParameter = "appBasePath";

 static final String DBParamPrefix = "db.";
 static final String ServiceParamPrefix = "biostd.";
 static final String TaskParamPrefix = "export.";
 static final String EmailParamPrefix = "email.";
 static final String OutputParamPrefix = "output";

 static final String OutputClassParameter = "class";

 
 
 private Logger log = null;


 
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
  if( BackendConfig.isConfigValid() )
   BackendConfig.getConfigurationManager().stopServices();
 }

 
 @Override
 public void contextInitialized(ServletContextEvent ctxEv)
 {
  try
  {
   contextInitializedUnsafe(ctxEv);
  }
  catch(Throwable e)
  {
   log.error("Configuration is not ready: "+e.getMessage());
   BackendConfig.setConfigValid(false);
  }
 }
 
 public void contextInitializedUnsafe(ServletContextEvent ctxEv) throws ConfigurationException
 {
  ServletContext ctx = ctxEv.getServletContext();
  
  BackendConfig.setInstanceId( ctx.getContextPath().hashCode() );
  BackendConfig.setConfigurationManager( new ConfigurationManager(new ServletContextParamPool(ctx)) );
  
  BackendConfig.getConfigurationManager().loadConfiguration();
  
  BackendConfig.setConfigValid(true);

  
  BackendConfig.getConfigurationManager().startServices();

  
/*
  
  String dataDir = BackendConfig.getUserGroupDropboxPath().toString();
  String dataMount = BackendConfig.getDataMountPath();
  

  if( dataMount == null )
   throw new RuntimeException(BackendConfig.DataMountPathParameter+" parameter is not set");
  
  resRoot.createWebResourceSet(ResourceSetType.POST, dataMount, dataDir, null, "/");
  
  StandardRoot davRoot = new StandardRoot( new ContextWrapper(resRoot.getContext(), dataDir ) );
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
  
  */
 }

 
	
}
