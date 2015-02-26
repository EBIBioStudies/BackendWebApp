package uk.ac.ebi.biostd.webapp.server;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManagerFactory;
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
import uk.ac.ebi.biostd.webapp.server.export.ExportTask;
import uk.ac.ebi.biostd.webapp.server.export.OutputModule;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfig;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfigException;
import uk.ac.ebi.biostd.webapp.server.export.TaskInfo;
import uk.ac.ebi.biostd.webapp.server.export.TaskInitError;
import uk.ac.ebi.biostd.webapp.server.export.ebeye.EBEyeOutputModule;
import uk.ac.ebi.biostd.webapp.server.export.xmlout.FormatingOutputModule;
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
// public static final String DefaultName = "_default_";

 static final String DBParamPrefix = "db.";
 static final String ServiceParamPrefix = "biostd.";
 static final String TaskParamPrefix = "task.";
 static final String OutputParamPrefix = "output";

 static final String OutputTypeParameter = "type";
 static final String XMLDumpType = "xml";
 static final String EBEyeType = "ebeye";
 
 
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
  if( BackendConfig.getServiceManager().getSessionManager() != null )
   BackendConfig.getServiceManager().getSessionManager().shutdown();
  
  if( BackendConfig.getEntityManagerFactory() != null )
   BackendConfig.getEntityManagerFactory().close();
 }

 
 private void readConfig( ParamPool config ) throws ServiceInitExceprion
 {

  //  java.util.logging.Logger.getLogger("org.hibernate.SQL").setLevel(Level.FINEST);

  log.info("Initializing BioStudies web app");

  Map<String, Object> dbConfig = new HashMap<String, Object>();
  TaskConfig taskConfig = null;
  
//  Map<String, Map<String, Object>> profMap = new HashMap<>();
//  Map<String, TaskConfig> tasksMap = new HashMap<>();


//  Matcher serviceMtch = Pattern.compile("^" + ServiceParamPrefix + "(\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");
//  Matcher dbMtch = Pattern.compile("^" + DBParamPrefix + "(\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");
//  Matcher taskMtch = Pattern.compile("^"+TaskParamPrefix+"(\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");
  Matcher outMtch = Pattern.compile("^"+OutputParamPrefix+"(?:\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");

  boolean confOk = true;

  Enumeration<String> pNames = config.getNames();

  while(pNames.hasMoreElements())
  {
   String key = pNames.nextElement();
   String val = config.getParameter(key);

   if(key.startsWith(DBParamPrefix))
    dbConfig.put(key.substring(DBParamPrefix.length()), val);
   else if(key.startsWith(ServiceParamPrefix))
   {
    String param = key.substring(ServiceParamPrefix.length());

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
   else if(key.startsWith(TaskParamPrefix))
   {
    if(taskConfig == null)
     taskConfig = new TaskConfig("export");

    String param = key.substring(TaskParamPrefix.length());

    outMtch.reset(param);

    if(outMtch.matches())
    {
     String outName = outMtch.group(1);
     String outParam = outMtch.group(2);

     if(outName == null)
      outName = "_default_";

     taskConfig.addOutputParameter(outName, outParam, val);
    }
    else
    {
     try
     {
      if(!taskConfig.readParameter(param, val))
       log.warn("Unknown configuration parameter: " + key + " will be ignored");
     }
     catch(TaskConfigException e)
     {
      log.error("Invalid parameter value: " + key + "=" + val);
      confOk = false;
     }
    }

   }
   else
    log.warn("Invalid parameter {} will be ignored.", key);
   
   
  }

  if(!confOk)
  {
   throw new RuntimeException("BioStudies webapp initialization failed");
  }

  BackendConfig.setEntityManagerFactory( Persistence.createEntityManagerFactory("BioStdCoreModel", dbConfig));

  BackendConfig.setServiceManager( ServiceFactory.createService( ) );
  
  try
  {
   createTask(taskConfig);
  }
  catch( TaskConfigException e )
  {
   log.error("Configuration error : "+e.getMessage());
   throw new RuntimeException("BioStd webapp initialization failed");
  }

  
  for( TaskInfo tinf : TaskManager.getDefaultInstance().getTasks() )
  {
   if( tinf.getTimeZero() >= 0 )
   {
    if(timer == null)
     timer = new Timer("Timer", true);
    
    timer.scheduleAtFixedRate(tinf, tinf.getTimeZero(), dayInMills);
   
    log.info("Task '"+tinf.getTask().getName()+"' is scheduled to run periodically");
   }
  }
 }
 
 private void createTask(TaskConfig tc) throws TaskConfigException
 {
  Calendar cal = Calendar.getInstance(TimeZone.getDefault());
  long ctime = System.currentTimeMillis();
  

   TaskInfo tinf = new TaskInfo();
   
   cal.setTimeInMillis( ctime );
   
   
   EntityManagerFactory emf = BackendConfig.getEntityManagerFactory();
   
   
//   if( tc.getInvokeHour() >= 0 )
//   {
//    cal.set( Calendar.HOUR_OF_DAY, tc.getInvokeHour() );
//    cal.set( Calendar.MINUTE, tc.getInvokeMin() );
//    
//    long delta = cal.getTimeInMillis() - ctime;
//    
//    long perdInMills = tc.getPeriodHours()*60*60*1000; 
//    
//    if( delta > 0 )
//     tinf.setTimeZero(ctime+(delta/perdInMills)*perdInMills+perdInMills);
//    else
//     tinf.setTimeZero(cal.getTimeInMillis()+(-delta/perdInMills)*perdInMills+perdInMills);
//     
//    
//   } 
    
   if( tc.getInvokeHour() >= 0 )
    tinf.setTimeZero( getAdjustedDelay(tc.getInvokeHour(), tc.getInvokeMin() ) );
   else
    tinf.setTimeZero(-1);
   
   List<OutputModule> mods = new ArrayList<>(tc.getOutputModulesConfig().size() );
   
   for( Map.Entry<String, Map<String,String>> me : tc.getOutputModulesConfig().entrySet() )
   {
    Map<String,String> cfg = me.getValue();
    
    String type = cfg.get(OutputTypeParameter);
    
    if( type == null )
     throw new TaskConfigException("Task '"+tc.getName()+"' output '"+me.getKey()+"': missed 'type' parameter");
    
    if( XMLDumpType.equals(type) )
     mods.add( new FormatingOutputModule(tc.getName()+":"+me.getKey(),cfg));
    else if( EBEyeType.equals(type) )
     mods.add( new EBEyeOutputModule(tc.getName()+":"+me.getKey(),cfg) );
    
   }
   
   try
   {
    ExportTask tsk = new ExportTask(tc.getName(), emf, myEqFact, mods, tc);
    
    tinf.setTask(tsk);
    
    TaskManager.getDefaultInstance().addTask(tinf);
   }
   catch(TaskInitError e)
   {
    log.warn("Task '"+tc.getName()+"': Initialization error: "+e.getMessage() );
   }
   
  
 }
 
 private long getAdjustedDelay( int hour, int min )
 {
  if( hour < 0 )
   return -1;
  
  
  Calendar cr = Calendar.getInstance(TimeZone.getDefault());
  cr.setTimeInMillis(System.currentTimeMillis());
  
  cr.set(Calendar.HOUR_OF_DAY, hour);
  cr.set(Calendar.MINUTE, min);
  
  long delay = cr.getTimeInMillis() - System.currentTimeMillis();
  
  long adjustedDelay = (delay > 0 ? delay : dayInMills + delay);
  
  return adjustedDelay;
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
