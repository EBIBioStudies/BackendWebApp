package uk.ac.ebi.biostd.webapp.server;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
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
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.email.EmailInitException;
import uk.ac.ebi.biostd.webapp.server.email.EmailService;
import uk.ac.ebi.biostd.webapp.server.export.ExportTask;
import uk.ac.ebi.biostd.webapp.server.export.OutputModule;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfig;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfigException;
import uk.ac.ebi.biostd.webapp.server.export.TaskInfo;
import uk.ac.ebi.biostd.webapp.server.export.TaskInitError;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceFactory;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceInitExceprion;
import uk.ac.ebi.biostd.webapp.server.search.SearchMapper;
import uk.ac.ebi.biostd.webapp.server.util.ExceptionUtil;
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
 static final String TaskParamPrefix = "export.";
 static final String EmailParamPrefix = "email.";
 static final String OutputParamPrefix = "output";

 static final String OutputClassParameter = "class";
// static final String XMLDumpType = "xml";
// static final String EBEyeType = "ebeye";
 
 
 private Logger log = null;

 private static final long dayInMills = TimeUnit.DAYS.toMillis(1);
 private static final long hourInMills = TimeUnit.HOURS.toMillis(1);

 private Timer timer;
 
 private Map<String, Object> dbConfig = new HashMap<String, Object>();
 private TaskConfig taskConfig = null;

 
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
  
  if( BackendConfig.getServiceManager().getSubmissionManager() != null )
   BackendConfig.getServiceManager().getSubmissionManager().shutdown();

  if( BackendConfig.getExportTask() != null )
   BackendConfig.getExportTask().getTask().interrupt();
  
  if( BackendConfig.getEntityManagerFactory() != null )
   BackendConfig.getEntityManagerFactory().close();
  
  if( timer != null )
   timer.cancel();
 }

 
 private void readConfig( ParamPool config ) throws ServiceInitExceprion
 {

  log.info("Initializing BioStudies web app");

  Matcher outMtch = Pattern.compile("^"+OutputParamPrefix+"(?:\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");

  boolean confOk = true;
  
  Enumeration<String> pNames = config.getNames();

  String baseDir = config.getParameter(ServiceParamPrefix+BackendConfig.BaseDirParameter);
  
  if( baseDir != null )
  {
   try
   {
    if(!BackendConfig.readParameter(BackendConfig.BaseDirParameter, baseDir))
     log.warn("Unknown configuration parameter: " + BackendConfig.BaseDirParameter + " will be ignored");
   }
   catch(ServiceConfigException e)
   {
    log.error("Invalid parameter value: " + BackendConfig.BaseDirParameter + "=" + baseDir+" "+e.getMessage());
    confOk = false;
   }
  }
  
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
     log.error("Invalid parameter value: " + key + "=" + val+" "+e.getMessage());
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
      log.error("Parameter read error: "+e.getMessage());
      confOk = false;
     }
    }

   }
   else if(key.startsWith(EmailParamPrefix))
   {

   }
   else
    log.warn("Invalid parameter {} will be ignored.", key);
   
   
  }

  if(!confOk)
  {
   throw new RuntimeException("BioStudies webapp initialization failed");
  }

 }
 
 private TaskInfo createTask(TaskConfig tc) throws TaskConfigException
 {
  Calendar cal = Calendar.getInstance(TimeZone.getDefault());
  long ctime = System.currentTimeMillis();
  

   TaskInfo tinf = new TaskInfo();
   
   cal.setTimeInMillis( ctime );
   
   
   EntityManagerFactory emf = BackendConfig.getEntityManagerFactory();
   
   if( tc.getInvokeMin() < 0 )
    tinf.setTimeZero( tc.getInvokePeriodMins()*60*1000 );
   else if( tc.getInvokeHour() >= 0 )
    tinf.setTimeZero( getAdjustedDelay(tc.getInvokeHour(), tc.getInvokeMin() ) );
   else
    tinf.setTimeZero(-1);
   
   tinf.setPeriod( tc.getInvokePeriodMins() );
   
   List<OutputModule> mods = new ArrayList<>(tc.getOutputModulesConfig().size() );
   
   for( Map.Entry<String, Map<String,String>> me : tc.getOutputModulesConfig().entrySet() )
   {
    Map<String,String> cfg = me.getValue();
    
    String type = cfg.get(OutputClassParameter);
    
    if( type == null )
     throw new TaskConfigException("Task '"+tc.getName()+"' output '"+me.getKey()+"': missed '"+OutputClassParameter+"' parameter");
    
    Class<?> outtaskCls = null;
    
    OutputModule outMod = null;
    
    try
    {
     outtaskCls = Class.forName(type);
    }
    catch( ClassNotFoundException e )
    {
     throw new TaskConfigException("Task '"+tc.getName()+"' output '"+me.getKey()+"': output module class '"+type+"' not found");
    }
    
    if( ! OutputModule.class.isAssignableFrom(outtaskCls) )
     throw new TaskConfigException("Task '"+tc.getName()+"' output '"+me.getKey()+"': Class '"+outtaskCls+"' doesn't implement OutputModule interface");
    
    Constructor<?> ctor = null;
    
    try
    {
     try
     {
      ctor = outtaskCls.getConstructor(String.class, Map.class);
      outMod = (OutputModule) ctor.newInstance(tc.getName()+":"+me.getKey(),cfg);
     }
     catch(NoSuchMethodException e)
     {
      try
      {
       ctor = outtaskCls.getConstructor(String.class);
       outMod = (OutputModule) ctor.newInstance(tc.getName()+":"+me.getKey());
      }
      catch(NoSuchMethodException e1)
      {
       throw new TaskConfigException("Task '"+tc.getName()+"' output '"+me.getKey()+"': Can't fine appropriate constructor of class '" + outtaskCls + "'");
      }
     }
     catch(SecurityException e)
     {
      throw new TaskConfigException("Task '"+tc.getName()+"' output '"+me.getKey()+"': Can't get constructor of class '" + outtaskCls + "' " + e.getMessage());
     }
    }
    catch(Exception ex)
    {
     throw new TaskConfigException("Task '"+tc.getName()+"' output '"+me.getKey()+"': Can't create instance of class '" + outtaskCls + "' : "+ExceptionUtil.unroll(ex).getMessage());
    }

    mods.add( outMod );
   }
   
   try
   {
    ExportTask tsk = new ExportTask(tc.getName(), emf, mods, tc);
    
    tinf.setTask(tsk);
    
    return tinf;
   }
   catch(TaskInitError e)
   {
    log.warn("Task '"+tc.getName()+"': Initialization error: "+e.getMessage() );
   }
   
  return null;
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
  try
  {
   contextInitializedUnsafe(ctxEv);
  }
  catch(Throwable e)
  {
   e.printStackTrace();
  }
 }
 
 public void contextInitializedUnsafe(ServletContextEvent ctxEv)
 {
  ServletContext ctx = ctxEv.getServletContext();
  
  BackendConfig.init( ctx.getContextPath().hashCode() );
  
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
  
  
  if( BackendConfig.isCreateFileStructure() && BackendConfig.getBaseDirectory() != null )
  {
   try
   {
    Files.createDirectories(BackendConfig.getBaseDirectory());
    Files.setPosixFilePermissions(BackendConfig.getBaseDirectory(), BackendConfig.rwxrwx___);
   }
   catch( UnsupportedOperationException e )
   {
    log.warn("Filesystem doesn't support POSIX file permissions. Please check base directory permissions manually");
   }
   catch(IOException e)
   {
    throw new RuntimeException("Directory access error: "+BackendConfig.getBaseDirectory());
   }
  }
  
  Path dir = BackendConfig.getWorkDirectory();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+BackendConfig.WorkdirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);
  
  dir = BackendConfig.getUserGroupPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+BackendConfig.UserGroupDirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }
  

  
  
  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);

  if( ! checkDirectory( BackendConfig.getUsersPath() ) )
   throw new RuntimeException("Directory access error: "+BackendConfig.getUsersPath() );

  if( ! checkDirectory( BackendConfig.getGroupsPath() ) )
   throw new RuntimeException("Directory access error: "+BackendConfig.getGroupsPath() );

  if( ! checkDirectory( BackendConfig.getSubmissionUpdatePath() ) )
   throw new RuntimeException("Directory access error: "+BackendConfig.getGroupsPath() );
 

  dir = BackendConfig.getSubmissionsPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+BackendConfig.SubmissionDirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);

  
  dir = BackendConfig.getSubmissionsHistoryPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+BackendConfig.SubmissionHistoryDirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);

  
  dir = BackendConfig.getSubmissionsTransactionPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+BackendConfig.SubmissionTransactionDirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }

  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);

//  if( BackendConfig.getServiceManager().getEmailService() == null )
//  {
//   log.error("Email service is not configured");
//   throw new RuntimeException("Invalid configuration");
//  }
  
  if( BackendConfig.isMandatoryAccountActivation() && BackendConfig.getActivationEmailSubject() == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+BackendConfig.ActivationEmailSubjectParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }

  if( BackendConfig.isMandatoryAccountActivation() && BackendConfig.getActivationEmailPlainTextFile() == null && BackendConfig.getActivationEmailHtmlFile() == null )
  {
   log.error("At least one of "+ServiceParamPrefix+BackendConfig.ActivationEmailPlainTextParameter+" "+
     ServiceParamPrefix+BackendConfig.ActivationEmailHtmlParameter+" parameters must be set");
   throw new RuntimeException("Invalid configuration");
  }

  Path emailFile = BackendConfig.getActivationEmailPlainTextFile();
  
  if( BackendConfig.isMandatoryAccountActivation() && emailFile != null && ( ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) ) )
  {
   log.error(ServiceParamPrefix+BackendConfig.ActivationEmailPlainTextParameter+" should point to a regular readable file");
   throw new RuntimeException("Invalid configuration");
  }
  
  emailFile = BackendConfig.getActivationEmailHtmlFile();
  
  if( BackendConfig.isMandatoryAccountActivation() && emailFile != null && ( ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) ) )
  {
   log.error(ServiceParamPrefix+BackendConfig.ActivationEmailHtmlParameter+" should point to a regular readable file");
   throw new RuntimeException("Invalid configuration");
  }
  
  
  if( BackendConfig.getPassResetEmailSubject() == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+BackendConfig.PassResetEmailSubjectParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }

  if( BackendConfig.getPassResetEmailPlainTextFile() == null && BackendConfig.getPassResetEmailHtmlFile() == null )
  {
   log.error("At least one of "+ServiceParamPrefix+BackendConfig.PassResetEmailPlainTextParameter+" "+
     ServiceParamPrefix+BackendConfig.PassResetEmailHtmlParameter+" parameters must be set");
   throw new RuntimeException("Invalid configuration");
  }  
  
  emailFile = BackendConfig.getPassResetEmailPlainTextFile();
  
  if( emailFile == null || ( ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) ) )
  {
   log.error(ServiceParamPrefix+BackendConfig.PassResetEmailPlainTextParameter+" should point to a regular readable file");
   throw new RuntimeException("Invalid configuration");
  }

  emailFile = BackendConfig.getPassResetEmailHtmlFile();
  
  if( emailFile == null || ( ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) ) )
  {
   log.error(ServiceParamPrefix+BackendConfig.PassResetEmailHtmlParameter+" should point to a regular readable file");
   throw new RuntimeException("Invalid configuration");
  }

  
  if( BackendConfig.getSubscriptionEmailSubject() != null || BackendConfig.getSubscriptionEmailPlainTextFile() != null || BackendConfig.getSubscriptionEmailHtmlFile() != null )
  {
   if( BackendConfig.getSubscriptionEmailSubject() == null || BackendConfig.getSubscriptionEmailPlainTextFile() == null || BackendConfig.getSubscriptionEmailHtmlFile() == null )
   {
    log.error("To activate tag subscriptions service the following parameters should be set: "
     +ServiceParamPrefix+BackendConfig.SubscriptionEmailSubjectParameter
     +", "+ServiceParamPrefix+BackendConfig.SubscriptionEmailPlainTextParameter
     +", "+ServiceParamPrefix+BackendConfig.SubscriptionEmailHtmlParameter
      );
    throw new RuntimeException("Invalid configuration");
   }
   
   emailFile = BackendConfig.getSubscriptionEmailPlainTextFile();

   if(  ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) )
   {
    log.error(ServiceParamPrefix+BackendConfig.SubscriptionEmailPlainTextParameter+" should point to a regular readable file");
    throw new RuntimeException("Invalid configuration");
   }

   emailFile = BackendConfig.getSubscriptionEmailHtmlFile();
   
   if(  ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile)  )
   {
    log.error(ServiceParamPrefix+BackendConfig.SubscriptionEmailHtmlParameter+" should point to a regular readable file");
    throw new RuntimeException("Invalid configuration");
   }
  }
  
  
  Path sbmTestDir = BackendConfig.getSubmissionsPath().resolve("~tmp");
  try
  {
   Path trnTestDir = BackendConfig.getSubmissionsTransactionPath().resolve("~tmp");
   
   Files.createDirectory(sbmTestDir);
   Files.deleteIfExists(trnTestDir);
   Files.move(sbmTestDir, trnTestDir);
   Files.delete(trnTestDir);
  }
  catch(IOException e1)
  {
   try
   {
    Files.deleteIfExists(sbmTestDir);
   }
   catch(IOException e)
   {
   }
  
   log.error("Submission transaction directory: test oparation failed: "+e1.getMessage());
   log.error("Submission transaction directory should be on the same physical drive with submissions directory");

   throw new RuntimeException("Invalid configuration");
  }

  
  
  BackendConfig.setDatabaseConfig( dbConfig );
  
  Path idxPath = null;
  boolean rebuildIndex = false;
  
  Object indexBaseParam = dbConfig.get("hibernate.search.default.indexBase");
  if( indexBaseParam != null )
  {
   idxPath = FileSystems.getDefault().getPath(indexBaseParam.toString());
   
   if( ! idxPath.isAbsolute() )
    idxPath = BackendConfig.getBaseDirectory().resolve(idxPath);
   
   rebuildIndex = ! Files.exists(idxPath);
   
   dbConfig.put("hibernate.search.default.indexBase",idxPath.toString());
   dbConfig.put(Environment.MODEL_MAPPING, SearchMapper.makeMapping() );
   
   BackendConfig.setSearchEnabled( true );
  }
  
  BackendConfig.setEntityManagerFactory( Persistence.createEntityManagerFactory("BioStdCoreModel", dbConfig));

  if( rebuildIndex )
  {
   try
   {
    Files.createDirectories(idxPath);
   }
   catch(IOException e)
   {
    log.error("Can't create search index directory '"+idxPath+"' : "+e.getMessage());
    throw new RuntimeException("BioStd webapp initialization failed");
   }
   
   EntityManager entityManager = BackendConfig.getEntityManagerFactory().createEntityManager();
   FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
   try
   {
    log.info("Starting Hibernate indexer");
    fullTextEntityManager.createIndexer().startAndWait();
    log.info("Hibernate indexer: done");
   }
   catch(InterruptedException e)
   {
    log.error("Can't initialize Hibernate search: "+e.getMessage());
    throw new RuntimeException("BioStd webapp initialization failed");
   }
   
   fullTextEntityManager.close();
  }
  
  BackendConfig.setServiceManager( ServiceFactory.createService( ) );
  
  
  try
  {
   BackendConfig.getServiceManager().setEmailService( new EmailService(config, EmailParamPrefix) );
  }
  catch(EmailInitException e)
  {
   log.error("Can't initialize email service: "+e.getMessage());
  }

  
  timer = new Timer("Timer", true);
  
  long now = System.currentTimeMillis();
  
  timer.scheduleAtFixedRate( new TimerTask()
  {
   @Override
   public void run()
   {
    BackendConfig.getServiceManager().getReleaseManager().doHourlyCheck();
   }
  }, hourInMills-(now % hourInMills) , hourInMills);
  
  TaskInfo tinf = null;
  
  try
  {
   tinf = createTask(taskConfig);
   
   BackendConfig.setExportTask(tinf);
  }
  catch( TaskConfigException e )
  {
   log.error("Configuration error : "+e.getMessage());
   throw new RuntimeException("BioStd webapp initialization failed",e);
  }

  if(tinf.getTimeZero() >= 0)
  {
   if(timer == null)
    timer = new Timer("Timer", true);

   timer.scheduleAtFixedRate(tinf, tinf.getTimeZero(), tinf.getPeriod()*60*1000);

   log.info("Task '" + tinf.getTask().getName() + "' is scheduled to run periodically ("+tinf.getPeriod()+"m)");
  }
  

  
  String dataDir = BackendConfig.getUserGroupPath().toString();
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
  
 }

 private boolean checkDirectory(Path file)
 {
  if( Files.exists( file ) )
  {
   if( ! Files.isDirectory( file ) )
   {
    log.error("Path "+file+" is not a directory");
    return false;
   }
   
   if( ! Files.isWritable(file) )
   {
    log.error("Directory "+file+" is not writable");
    return false;
   }
  }
  else if( BackendConfig.isCreateFileStructure() )
  {
   try
   {
    Files.createDirectories( file );
   }
   catch(IOException e)
   {
    log.error("Can't create directory: "+file );
    return false;
   }
  }
  else
   return false;
  
  return true;
 }
	
}
