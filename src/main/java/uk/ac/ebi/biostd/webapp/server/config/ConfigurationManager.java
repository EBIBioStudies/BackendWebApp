package uk.ac.ebi.biostd.webapp.server.config;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.webapp.server.email.EmailInitException;
import uk.ac.ebi.biostd.webapp.server.email.EmailService;
import uk.ac.ebi.biostd.webapp.server.export.ExportTask;
import uk.ac.ebi.biostd.webapp.server.export.OutputModule;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfig;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfigException;
import uk.ac.ebi.biostd.webapp.server.export.TaskInfo;
import uk.ac.ebi.biostd.webapp.server.export.TaskInitError;
import uk.ac.ebi.biostd.webapp.server.mng.IndexManager;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceFactory;
import uk.ac.ebi.biostd.webapp.server.search.SearchMapper;
import uk.ac.ebi.biostd.webapp.server.util.ExceptionUtil;
import uk.ac.ebi.biostd.webapp.server.util.FileNameUtil;
import uk.ac.ebi.biostd.webapp.server.util.FileResource;
import uk.ac.ebi.biostd.webapp.server.util.JavaResource;
import uk.ac.ebi.biostd.webapp.server.util.MapParamPool;
import uk.ac.ebi.biostd.webapp.server.util.ParamPool;
import uk.ac.ebi.biostd.webapp.server.util.PreferencesParamPool;
import uk.ac.ebi.biostd.webapp.server.util.Resource;
import uk.ac.ebi.biostd.webapp.server.util.ResourceBundleParamPool;

public class ConfigurationManager
{
 static final String ApplicationConfigNode = "BioStdWebApp";
 static final String ConfigFileName =  "config.properties";

 static final String DBParamPrefix = "db.";
 static final String ServiceParamPrefix = "biostd.";
 static final String TaskParamPrefix = "export.";
 static final String EmailParamPrefix = "email.";
 static final String OutputParamPrefix = "output";

 static final String OutputClassParameter = "class";
 

 public static final String             ConfigurationResetParameter         = "resetConfig";
 public static final String             DisablePreferencesConfigParameter   = "disableOnlineConfig";

 
 public static final String             BaseDirParameter                    = "baseDir";
 
 public static final String             CreateFileStructureParameter        = "createFileStructure";
 public static final String             WorkdirParameter                    = "workDir";

 public static final String             EnableUnsafeRequestsParameter       = "enableUnsafeRequests";
 public static final String             UserGroupDirParameter               = "userGroupDir";
 public static final String             UserGroupIndexDirParameter          = "userGroupIndexDir";
 public static final String             PublicDropboxesParameter            = "publicDropboxes";
 public static final String             SubmissionDirParameter              = "submissionDir";
 public static final String             SubmissionTransactionDirParameter   = "submissionTransactionDir";
 public static final String             SubmissionHistoryDirParameter       = "submissionHistoryDir";
 public static final String             AllowFileLinksParameter             = "allowFileLinks";
 public static final String             PublicFTPDirParameter               = "publicFTPDir";
 public static final String             SubmissionUpdateParameter           = "updateDir";
 public static final String             UpdateURLParameter                  = "updateListenerURL";
 public static final String             UpdateURLFilePlaceholder            = "{file}";
 public static final String             UpdateWaitPeriodParameter           = "updateWaitPeriod";
 public static final String             MaxUpdatesPerFileParameter          = "maxUpdatesPerFile";
 
 public static final String             MandatoryAccountActivationParameter = "mandatoryAccountActivation";
 public static final String             ActivationEmailSubjectParameter     = "activationEmailSubject";
 public static final String             ActivationEmailPlainTextParameter   = "activationEmailPlainTextFile";
 public static final String             ActivationEmailHtmlParameter        = "activationEmailHtmlFile";
 public static final String             ActivationTimeoutParameter          = "activationTimeout";
 public static final String             ActivationTimeoutParameterHours     = "activationTimeoutHours";

 public static final String             SubscriptionEmailSubjectParameter   = "subscriptionEmailSubject";
 public static final String             SubscriptionEmailPlainTextParameter = "subscriptionEmailPlainTextFile";
 public static final String             SubscriptionEmailHtmlParameter      = "subscriptionEmailHtmlFile";
 
 public static final String             PassResetTimeoutParameter           = "passwordResetTimeout";
 public static final String             PassResetEmailSubjectParameter      = "passwordResetEmailSubject";
 public static final String             PassResetEmailPlainTextParameter    = "passwordResetEmailPlainTextFile";
 public static final String             PassResetEmailHtmlParameter         = "passwordResetEmailHtmlFile";
 
 public static final String             DefaultSubmissionAccPrefixParameter = "defaultSubmissionAccNoPrefix";
 public static final String             DefaultSubmissionAccSuffixParameter = "defaultSubmissionAccNoSuffix";
 
 public static final String             DataMountPathParameter              = "dataMountPath";
 public static final String             RecapchaPrivateKeyParameter         = "recapcha_private_key";
 
 public static final String             HibernateSearchIndexDirParameter    = "hibernate.search.default.indexBase";
 public static final String             HibernateDBConnectionURLParameter   = "hibernate.connection.url";
 
 private ParamPool contextParamPool;
 
 private static Logger log = LoggerFactory.getLogger(ConfigurationManager.class);

 private static final long dayInMills = TimeUnit.DAYS.toMillis(1);
 private static final long hourInMills = TimeUnit.HOURS.toMillis(1);
 
 public ConfigurationManager( ParamPool ctx )
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
 
  contextParamPool = ctx;
 }

 public void loadConfiguration() throws ConfigurationException
 {
  ConfigBean cfgBean = BackendConfig.createConfig(); 
  
  String dsblVal = contextParamPool.getParameter(DisablePreferencesConfigParameter);
  
  boolean disPref = dsblVal != null && ( "yes".equalsIgnoreCase(dsblVal) || "true".equalsIgnoreCase(dsblVal) || "on".equalsIgnoreCase(dsblVal) || "1".equals(dsblVal) ); 
  
  boolean loaded = false;
  try
  {
   if( ! disPref && Preferences.userRoot().nodeExists(ApplicationConfigNode) )
   {
    Preferences prefs = Preferences.userRoot().node(ApplicationConfigNode);
    
    if( ! checkReset( prefs.get(ConfigurationResetParameter, null), "app preferences" ) )
    {
     if( ! checkReset( contextParamPool.getParameter(ConfigurationResetParameter), "webapp" ) )
      loadDefaults( cfgBean );
     
     readConfiguration(contextParamPool, cfgBean);
    }
    
    readConfiguration(new PreferencesParamPool(prefs), cfgBean);
    
    loaded = false;
   }
  }
  catch(BackingStoreException e1)
  {
   log.warn("Error reading preferences: "+e1.getMessage());
  }
  
  if( ! loaded )
  {
   if( ! checkReset( contextParamPool.getParameter(ConfigurationResetParameter), "webapp" ) )
    loadDefaults( cfgBean );
   
   readConfiguration(contextParamPool, cfgBean);
  }
  
  if( cfgBean.getBaseDirectory() != null )
  {
   if( ! cfgBean.getBaseDirectory().isAbsolute() )
    throw new ConfigurationException(BaseDirParameter+" sould be absolute");
   
   ResourceBundle rb = null;
   
   Path cfgFile = cfgBean.getBaseDirectory().resolve(ConfigFileName);
   
   if( Files.exists(cfgFile) )
   {
    try( Reader fr = new FileReader(cfgFile.toFile()) )
    {
     rb = new PropertyResourceBundle(fr);
    }
    catch(Exception e)
    {
     log.error("Can't read config file: "+cfgFile);
     throw new ConfigurationException("Can't read config file: "+cfgFile);
    }
    
    ParamPool rbpp = new ResourceBundleParamPool(rb);
    
    if( checkReset( rbpp.getParameter(ConfigurationResetParameter), "config file" ) )
     cfgBean = BackendConfig.createConfig(); 
    
    readConfiguration( rbpp, cfgBean);
   }
  }

  if( cfgBean.getUserGroupIndexPath() != null )
  {
   cfgBean.setUsersIndexPath( cfgBean.getUserGroupIndexPath().resolve(BackendConfig.UsersDir) );
   cfgBean.setGroupsIndexPath( cfgBean.getUserGroupIndexPath().resolve(BackendConfig.GroupsDir) );
  }
  
  Path baseP = cfgBean.getBaseDirectory();
  
  if( baseP != null && ! baseP.isAbsolute() )
   throw new ConfigurationException(BaseDirParameter+" parameter should be absolute path");

  cfgBean.setUserGroupDropboxPath(adjustPath(cfgBean.getUserGroupDropboxPath(), baseP));

  cfgBean.setUserGroupIndexPath(adjustPath(cfgBean.getUserGroupIndexPath(), baseP));
  cfgBean.setUsersIndexPath(adjustPath(cfgBean.getUsersIndexPath(), baseP));
  cfgBean.setGroupsIndexPath(adjustPath(cfgBean.getGroupsIndexPath(), baseP));

  cfgBean.setWorkDirectory(adjustPath(cfgBean.getWorkDirectory(), baseP));
  cfgBean.setSubmissionsPath(adjustPath(cfgBean.getSubmissionsPath(), baseP));
  cfgBean.setSubmissionsHistoryPath(adjustPath(cfgBean.getSubmissionsHistoryPath(), baseP));
  cfgBean.setSubmissionsTransactionPath(adjustPath(cfgBean.getSubmissionsTransactionPath(), baseP));
  cfgBean.setSubmissionUpdatePath(adjustPath(cfgBean.getSubmissionUpdatePath(), baseP));

  cfgBean.setPublicFTPPath(adjustPath(cfgBean.getPublicFTPPath(), baseP));

  adjustResource(cfgBean.getActivationEmailHtmlFile(), baseP);
  adjustResource(cfgBean.getActivationEmailPlainTextFile(), baseP);

  adjustResource(cfgBean.getPassResetEmailHtmlFile(), baseP);
  adjustResource(cfgBean.getPassResetEmailPlainTextFile(), baseP);

  adjustResource(cfgBean.getSubscriptionEmailHtmlFile(), baseP);
  adjustResource(cfgBean.getSubscriptionEmailPlainTextFile(), baseP);

  adjustSearchIndexPath(cfgBean, baseP);
  adjustH2DBPath(cfgBean, baseP);

  validateConfiguration(cfgBean);

  stopServices();
  
  ConfigBean oldConfig = BackendConfig.getConfig();

  BackendConfig.setConfig(cfgBean);
  
  startServices();

 }
 

 
 public void startServices()
 {
  Path idxPath = null;
  boolean rebuildIndex = false;
  
  Map<String, Object> dbConfig = BackendConfig.getDatabaseConfig();
  
  Object indexBaseParam = dbConfig.get(HibernateSearchIndexDirParameter);
  
  if( indexBaseParam != null )
  {
   idxPath = FileSystems.getDefault().getPath(indexBaseParam.toString());
   

   rebuildIndex = ! Files.exists(idxPath);
   
   dbConfig.put(Environment.MODEL_MAPPING, SearchMapper.makeMapping() );
   
   BackendConfig.setSearchEnabled( true );
  }
  
  BackendConfig.setEntityManagerFactory( Persistence.createEntityManagerFactory("BioStdCoreModel", dbConfig) );

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
   
   IndexManager.rebuildIndex( BackendConfig.getEntityManagerFactory() );
 }
  
  BackendConfig.setServiceManager( ServiceFactory.createService( ) );
  
  
  try
  {
   BackendConfig.getServiceManager().setEmailService( new EmailService(new MapParamPool(BackendConfig.getEmailConfig()), EmailParamPrefix) );
  }
  catch(EmailInitException e)
  {
   log.error("Can't initialize email service: "+e.getMessage());
  }

  
  Timer timer = new Timer("Shared system timer", true);
  
  BackendConfig.setTimer(timer);
  
  long now = System.currentTimeMillis();
  
  timer.scheduleAtFixedRate( new TimerTask()
  {
   @Override
   public void run()
   {
    BackendConfig.getServiceManager().getReleaseManager().doHourlyCheck();
    BackendConfig.getServiceManager().getSecurityManager().removeExpiredUsers();
   }
  }, hourInMills-(now % hourInMills) , hourInMills);
  
  TaskInfo tinf = null;
  
  try
  {
   tinf = createTask(BackendConfig.getTaskConfig());
   
   BackendConfig.setExportTask(tinf);
  }
  catch( TaskConfigException e )
  {
   log.error("Configuration error : "+e.getMessage());
   throw new RuntimeException("BioStd webapp initialization failed",e);
  }

  if(tinf.getTimeZero() >= 0)
  {
   timer.scheduleAtFixedRate(tinf, tinf.getTimeZero(), tinf.getPeriod()*60*1000);

   log.info("Task '" + tinf.getTask().getName() + "' is scheduled to run periodically ("+tinf.getPeriod()+"m)");
  }
 }

 public void stopServices()
 {
  
  EntityManagerFactory emf = BackendConfig.getEntityManagerFactory();
  
  if( BackendConfig.getServiceManager().getSessionManager() != null )
   BackendConfig.getServiceManager().getSessionManager().shutdown();
  
  if( BackendConfig.getServiceManager().getSubmissionManager() != null )
   BackendConfig.getServiceManager().getSubmissionManager().shutdown();

  if( BackendConfig.getExportTask() != null )
   BackendConfig.getExportTask().getTask().interrupt();
  
  
  if( BackendConfig.getTimer() != null )
   BackendConfig.getTimer().cancel();
  
  if( emf != null )
   emf.close();
  
 }

 
 
 public Map<String, String> getPreferences()
 {
  Preferences prefs = Preferences.userRoot().node(ApplicationConfigNode);
  
  Map<String, String> map  = new HashMap<String, String>();
  
  try
  {
   for( String key : prefs.keys() )
    map.put(key, prefs.get(key, null));
  }
  catch(BackingStoreException e)
  {
   log.error("Can't read preferences: "+e.getMessage());
   e.printStackTrace();
  }
  
  return map;
 }
 
 public void setPreferences( Map<String, String> pfs )
 {
  try
  {
   Preferences prefs = Preferences.userRoot().node(ApplicationConfigNode);
   
   prefs.clear();
   
   for( Map.Entry<String, String> me : pfs.entrySet() )
    prefs.put(me.getKey(), me.getValue());
   
   prefs.flush();
  }
  catch(BackingStoreException e)
  {
   log.error("Can't write preferences: "+e.getMessage());
   e.printStackTrace();
  }

 }
 
 private static void adjustH2DBPath(ConfigBean cfgBean, Path baseP) throws ConfigurationException
 {
  String dbURLStr = cfgBean.getDatabaseConfig().get(HibernateDBConnectionURLParameter).toString();
  
  if( dbURLStr == null )
   return;
  
  int p1 = dbURLStr.indexOf(':');
  
  if( p1 < 0 )
   return;
  
  int p2 = dbURLStr.indexOf(':',p1+1);
  
  if( p2 < 0 )
   return;
  
  if( !"h2".equalsIgnoreCase( dbURLStr.substring(p1+1,p2) ) )
   return;
  
  int p3 = dbURLStr.indexOf(':',p2+1);
  
  
  String proto = null;
  String pathStr = null;
  
  if( p3 >= 0 )
  {
   proto = dbURLStr.substring(p2+1,p3);
   pathStr = dbURLStr.substring(p3+1);
  }
  else
   pathStr = dbURLStr.substring(p2+1);
  
  if( proto != null && ( "tcp".equalsIgnoreCase(proto) || "mem".equalsIgnoreCase(proto) || "zip".equalsIgnoreCase(proto) || "ssl".equalsIgnoreCase(proto) ) )
   return;
  
  if( ! "file".equalsIgnoreCase(proto) )
   pathStr = dbURLStr.substring(p2+1);
   
  Path path = Paths.get(pathStr);
  
  if( path.isAbsolute() )
   return;
  
  if( baseP == null || ! baseP.isAbsolute() )
   throw new ConfigurationException("Can't resolve database relative path '"+path.toString()+"' "+BaseDirParameter+" is not set or not absolute");

  path = baseP.resolve(path);
  
  cfgBean.getDatabaseConfig().put(HibernateDBConnectionURLParameter,"jdbc:h2:file:"+FileNameUtil.toUnixPath(path));
 }

 private void adjustSearchIndexPath(ConfigBean cfgBean, Path baseP) throws ConfigurationException
 {
  String iPathStr = cfgBean.getDatabaseConfig().get(HibernateSearchIndexDirParameter).toString();
  
  if( iPathStr == null )
   return;
  
  Path iPath = Paths.get(iPathStr);
  
  if( iPath.isAbsolute() )
   return;
  
  if( baseP == null || ! baseP.isAbsolute() )
   throw new ConfigurationException("Can't resolve index relative path '"+iPath.toString()+"' "+BaseDirParameter+" is not set or not absolute");

  iPath = baseP.resolve(iPath);
  
  cfgBean.getDatabaseConfig().put(HibernateSearchIndexDirParameter, FileNameUtil.toUnixPath(iPath) );
 }

 private void adjustResource(Resource res, Path baseP) throws ConfigurationException
 {
  if( res instanceof FileResource )
  {
   FileResource fres = (FileResource)res;
   Path rp = fres.getPath();
   
   if( rp.isAbsolute() )
    return;
  
   if( baseP == null || ! baseP.isAbsolute() )
    throw new ConfigurationException("Can't resolve resource relative path '"+rp.toString()+"' "+BaseDirParameter+" is not set or not absolute");
   
   fres.setPath(baseP.resolve(rp));
  }
 }

 private Path adjustPath( Path pth, Path basePath ) throws ConfigurationException
 {
  if( pth == null || pth.isAbsolute() )
   return pth;
  
  if( basePath == null || ! basePath.isAbsolute() )
   throw new ConfigurationException("Can't resolve relative path'"+pth.toString()+"' "+BaseDirParameter+" is not set or not absolute");
  
  return basePath.resolve(pth);
 }

 boolean checkReset( String rst, String context ) throws ConfigurationException
 {
  if( rst == null )
   return false;
  
  if(  "true".equalsIgnoreCase(rst) || "yes".equalsIgnoreCase(rst) || "1".equals(rst) )
   return true;
  
  if( !( "false".equalsIgnoreCase(rst) || "no".equalsIgnoreCase(rst) || "0".equals(rst) )  )
   throw new ConfigurationException("Invalid parameter value "+ConfigurationResetParameter+"="+rst+" within "+context+" context");
  
  return false;
 }

 private void loadDefaults(ConfigBean cfgBean)
 {
  Map<String, Object> dbConf = cfgBean.getDatabaseConfig();
  
  dbConf.put("hibernate.connection.driver_class","org.h2.Driver");
  dbConf.put("hibernate.connection.username","");
  dbConf.put("hibernate.connection.password","");
  dbConf.put("hibernate.cache.use_query_cache","false");
  dbConf.put("hibernate.ejb.discard_pc_on_close","true");
  dbConf.put(HibernateDBConnectionURLParameter,"jdbc:h2:db");
  dbConf.put("hibernate.dialect","org.hibernate.dialect.H2Dialect");
  dbConf.put("hibernate.hbm2ddl.auto","update");
  dbConf.put("hibernate.c3p0.max_size","30");
  dbConf.put("hibernate.c3p0.min_size","0");
  dbConf.put("hibernate.c3p0.timeout","5000");
  dbConf.put("hibernate.c3p0.max_statements","0");
  dbConf.put("hibernate.c3p0.idle_test_period","300");
  dbConf.put("hibernate.c3p0.acquire_increment","2");
  dbConf.put("hibernate.c3p0.unreturnedConnectionTimeout","18000");
  dbConf.put(HibernateSearchIndexDirParameter,"index");
  dbConf.put("hibernate.search.default.directory_provider","filesystem");
  dbConf.put("hibernate.search.lucene_version","LUCENE_54");

  cfgBean.setCreateFileStructure(true);
  cfgBean.setFileLinkAllowed(true);
  
  cfgBean.setWorkDirectory(Paths.get("work"));
  cfgBean.setSubmissionsPath(Paths.get("submission"));
  cfgBean.setSubmissionsHistoryPath(Paths.get("history"));
  cfgBean.setSubmissionsTransactionPath(Paths.get("transaction"));
  cfgBean.setSubmissionUpdatePath(Paths.get("updates"));
  cfgBean.setUserGroupIndexPath(Paths.get("ug_index"));
  cfgBean.setUserGroupDropboxPath(Paths.get("ug_data"));
  
  cfgBean.setUpdateWaitPeriod(10);
  cfgBean.setMaxUpdatesPerFile(50);
  
  cfgBean.setMandatoryAccountActivation(false);
  
  cfgBean.setDefaultSubmissionAccPrefix("S-");
  
  cfgBean.setActivationEmailSubject("Account activation request");
  cfgBean.setActivationEmailPlainTextFile( new JavaResource("/resources/mail/activationMail.txt"));
  cfgBean.setActivationEmailHtmlFile( new JavaResource("/resources/mail/activationMail.html"));
  
  cfgBean.setPassResetEmailSubject("Password reset request");
  cfgBean.setPassResetEmailPlainTextFile( new JavaResource("/resources/mail/passResetMail.txt"));
  cfgBean.setPassResetEmailHtmlFile( new JavaResource("/resources/mail/passResetMail.html"));

  cfgBean.setSubscriptionEmailSubject("Subscription notification");
  cfgBean.setSubscriptionEmailPlainTextFile( new JavaResource("/resources/mail/subscriptionMail.txt"));
  cfgBean.setSubscriptionEmailHtmlFile( new JavaResource("/resources/mail/subscriptionMail.html"));

  
 }

 public static boolean readConfiguration( ParamPool config, ConfigBean cfgBean ) throws ConfigurationException
 {
//  ConfigBean cfgBean = BackendConfig.createConfig(); 
  
  Map<String, Object> dbConfig = cfgBean.getDatabaseConfig();
  Map<String, Object> emailConfig = new HashMap<String, Object>();
  TaskConfig taskConfig = null;
  
  Matcher outMtch = Pattern.compile("^"+OutputParamPrefix+"(?:\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");

  boolean confOk = true;
  
  Enumeration<String> pNames = config.getNames();

  String baseDir = config.getParameter(ServiceParamPrefix+BaseDirParameter);
  
  if( baseDir != null )
  {
   try
   {
    if(!readParameter(BaseDirParameter, baseDir,cfgBean))
     log.warn("Unknown configuration parameter: " + BaseDirParameter + " will be ignored");
   }
   catch(ServiceConfigException e)
   {
    log.error("Invalid parameter value: " + BaseDirParameter + "=" + baseDir+" "+e.getMessage());
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
     if(!readParameter(param, val, cfgBean))
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
    if( emailConfig == null )
     emailConfig = new HashMap<String, Object>();
    
    dbConfig.put(key.substring(EmailParamPrefix.length()), val);
   }
   else
    log.warn("Invalid parameter {} will be ignored.", key);
   
   
  }

  
  cfgBean.setEmailConfig(emailConfig);
  cfgBean.setTaskConfig(taskConfig);
  
  return confOk;
  
 }
 
 
 public static void validateConfiguration( ConfigBean cfg ) throws ConfigurationException
 {
  if( cfg.isCreateFileStructure() && cfg.getBaseDirectory() != null )
  {
   try
   {
    Files.createDirectories(cfg.getBaseDirectory());
    Files.setPosixFilePermissions(cfg.getBaseDirectory(), BackendConfig.rwxrwx___);
   }
   catch( UnsupportedOperationException e )
   {
    log.warn("Filesystem doesn't support POSIX file permissions. Please check base directory permissions manually");
   }
   catch(IOException e)
   {
    throw new ConfigurationException("Directory access error: "+cfg.getBaseDirectory());
   }
  }
  
  Path dir = cfg.getWorkDirectory();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+WorkdirParameter+" parameter is not set");
   throw new ConfigurationException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir,cfg.isCreateFileStructure()) )
   throw new ConfigurationException("Directory access error: "+dir);
  
  dir = cfg.getUserGroupDropboxPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+UserGroupDirParameter+" parameter is not set");
   throw new ConfigurationException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir,cfg.isCreateFileStructure()) )
   throw new ConfigurationException("Directory access error: "+dir);

  
  dir = cfg.getUserGroupIndexPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+UserGroupIndexDirParameter+" parameter is not set");
   throw new ConfigurationException("Invalid configuration");
  }

  if( ! checkDirectory( cfg.getUsersIndexPath(), cfg.isCreateFileStructure() ) )
   throw new ConfigurationException("Directory access error: "+cfg.getUsersIndexPath() );

  if( ! checkDirectory( cfg.getGroupsIndexPath(), cfg.isCreateFileStructure() ) )
   throw new ConfigurationException("Directory access error: "+cfg.getGroupsIndexPath() );

  
  if( ! checkDirectory( cfg.getSubmissionUpdatePath(), cfg.isCreateFileStructure() ) )
   throw new ConfigurationException("Directory access error: "+cfg.getSubmissionUpdatePath() );
 

  dir = cfg.getSubmissionsPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+SubmissionDirParameter+" parameter is not set");
   throw new ConfigurationException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir,cfg.isCreateFileStructure()) )
   throw new ConfigurationException("Directory access error: "+dir);

  
  dir = cfg.getSubmissionsHistoryPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+SubmissionHistoryDirParameter+" parameter is not set");
   throw new ConfigurationException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir,cfg.isCreateFileStructure()) )
   throw new ConfigurationException("Directory access error: "+dir);

  
  dir = cfg.getSubmissionsTransactionPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+SubmissionTransactionDirParameter+" parameter is not set");
   throw new ConfigurationException("Invalid configuration");
  }

  if( ! checkDirectory(dir,cfg.isCreateFileStructure()) )
   throw new ConfigurationException("Directory access error: "+dir);

//  if( BackendConfig.getServiceManager().getEmailService() == null )
//  {
//   log.error("Email service is not configured");
//   throw new RuntimeException("Invalid configuration");
//  }
  
  if( cfg.isMandatoryAccountActivation() && cfg.getActivationEmailSubject() == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+ActivationEmailSubjectParameter+" parameter is not set");
   throw new ConfigurationException("Invalid configuration");
  }

  if( cfg.isMandatoryAccountActivation() && cfg.getActivationEmailPlainTextFile() == null && cfg.getActivationEmailHtmlFile() == null )
  {
   log.error("At least one of "+ServiceParamPrefix+ActivationEmailPlainTextParameter+" "+
     ServiceParamPrefix+ActivationEmailHtmlParameter+" parameters must be set");
   throw new ConfigurationException("Invalid configuration");
  }

  Resource emailFile = cfg.getActivationEmailPlainTextFile();
  
  if( cfg.isMandatoryAccountActivation() && emailFile != null && ( ! emailFile.isValid() ) )
  {
   log.error(ServiceParamPrefix+ActivationEmailPlainTextParameter+" should point to a regular readable file");
   throw new ConfigurationException("Invalid configuration");
  }
  
  emailFile = cfg.getActivationEmailHtmlFile();
  
  if( cfg.isMandatoryAccountActivation() && emailFile != null && ( ! emailFile.isValid() ) )
  {
   log.error(ServiceParamPrefix+ActivationEmailHtmlParameter+" should point to a regular readable file");
   throw new ConfigurationException("Invalid configuration");
  }
  
  
  if( cfg.getPassResetEmailSubject() == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+PassResetEmailSubjectParameter+" parameter is not set");
   throw new ConfigurationException("Invalid configuration");
  }

  if( cfg.getPassResetEmailPlainTextFile() == null && cfg.getPassResetEmailHtmlFile() == null )
  {
   log.error("At least one of "+ServiceParamPrefix+PassResetEmailPlainTextParameter+" "+
     ServiceParamPrefix+PassResetEmailHtmlParameter+" parameters must be set");
   throw new ConfigurationException("Invalid configuration");
  }  
  
  emailFile = cfg.getPassResetEmailPlainTextFile();
  
  if( emailFile == null ||  ! emailFile.isValid() )
  {
   log.error(ServiceParamPrefix+PassResetEmailPlainTextParameter+" should point to a regular readable file");
   throw new ConfigurationException("Invalid configuration");
  }

  emailFile = cfg.getPassResetEmailHtmlFile();
  
  if( emailFile == null || ! emailFile.isValid() )
  {
   log.error(ServiceParamPrefix+PassResetEmailHtmlParameter+" should point to a regular readable file");
   throw new ConfigurationException("Invalid configuration");
  }

  
  if( cfg.getSubscriptionEmailSubject() != null || cfg.getSubscriptionEmailPlainTextFile() != null || cfg.getSubscriptionEmailHtmlFile() != null )
  {
   if( cfg.getSubscriptionEmailSubject() == null || cfg.getSubscriptionEmailPlainTextFile() == null || cfg.getSubscriptionEmailHtmlFile() == null )
   {
    log.error("To activate tag subscriptions service the following parameters should be set: "
     +ServiceParamPrefix+SubscriptionEmailSubjectParameter
     +", "+ServiceParamPrefix+SubscriptionEmailPlainTextParameter
     +", "+ServiceParamPrefix+SubscriptionEmailHtmlParameter
      );
    throw new ConfigurationException("Invalid configuration");
   }
   
   emailFile = cfg.getSubscriptionEmailPlainTextFile();

   if( emailFile != null && ! emailFile.isValid() )
   {
    log.error(ServiceParamPrefix+SubscriptionEmailPlainTextParameter+" should point to a regular readable file");
    throw new ConfigurationException("Invalid configuration");
   }

   emailFile = cfg.getSubscriptionEmailHtmlFile();
   
   if( emailFile != null && ! emailFile.isValid()  )
   {
    log.error(ServiceParamPrefix+SubscriptionEmailHtmlParameter+" should point to a regular readable file");
    throw new ConfigurationException("Invalid configuration");
   }
  }
  
  
  Path sbmTestDir = cfg.getSubmissionsPath().resolve("~tmp");
  try
  {
   Path trnTestDir = cfg.getSubmissionsTransactionPath().resolve("~tmp");
   
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

   throw new ConfigurationException("Invalid configuration");
  }


 }
 
 
 public static boolean readParameter(String param, String val, ConfigBean cfg) throws ServiceConfigException
 {
  val = val.trim();
  param = param.trim();
  
  if( DefaultSubmissionAccPrefixParameter.equals(param) )
  {
   cfg.setDefaultSubmissionAccPrefix(val);
   
   return true;
  }

  if( DefaultSubmissionAccSuffixParameter.equals(param) )
  {
   cfg.setDefaultSubmissionAccSuffix(val);
   
   return true;
  }

  if( BaseDirParameter.equals(param) )
  {
   cfg.setBaseDirectory(FileSystems.getDefault().getPath(val));
   
   if( ! cfg.getBaseDirectory().isAbsolute() )
    throw new ServiceConfigException(BaseDirParameter+": path should be absolute");
   
   return true;
  }

  if( WorkdirParameter.equals(param) )
  {
   cfg.setWorkDirectory( createPath(WorkdirParameter,val, cfg.getBaseDirectory() ));
   
   return true;
  }
  
 
  if( SubmissionDirParameter.equals(param) )
  {
   cfg.setSubmissionsPath(createPath(SubmissionDirParameter,val, cfg.getBaseDirectory()));

   return true;
  }
  
  if( SubmissionUpdateParameter.equals(param) )
  {
   cfg.setSubmissionUpdatePath(createPath(SubmissionUpdateParameter,val, cfg.getBaseDirectory()));

   return true;
  }
  
  if( UpdateURLParameter.equals(param) )
  {
   int pos = val.indexOf(UpdateURLFilePlaceholder);
   
   if( pos < 0 )
    throw new ServiceConfigException(UpdateURLParameter+" should contain "+UpdateURLFilePlaceholder+" placeholder");
   
   cfg.setUpdateListenerURLPfx(val.substring(0,pos));
   cfg.setUpdateListenerURLSfx(val.substring(pos+UpdateURLFilePlaceholder.length()));

   try
   {
    new URL(cfg.getUpdateListenerURLPfx()+"aaa.txt"+cfg.getUpdateListenerURLSfx());
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(UpdateURLParameter+": invalid URL '"+val+"'");
   }
   
   return true;
  }

  if( UpdateWaitPeriodParameter.equals(param) )
  {
   try
   {
    cfg.setUpdateWaitPeriod(Integer.parseInt(val));
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(UpdateWaitPeriodParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }
  
  if( MaxUpdatesPerFileParameter.equals(param) )
  {
   try
   {
    cfg.setMaxUpdatesPerFile( Integer.parseInt(val) );
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(MaxUpdatesPerFileParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }

  
  if( SubmissionHistoryDirParameter.equals(param) )
  {
   cfg.setSubmissionsHistoryPath( createPath(SubmissionHistoryDirParameter,val, cfg.getBaseDirectory()) );
 
   return true;
  }
  
  if( SubmissionTransactionDirParameter.equals(param) )
  {
   cfg.setSubmissionsTransactionPath( createPath(SubmissionTransactionDirParameter,val, cfg.getBaseDirectory()) );
 
   return true;
  }

  if( PublicFTPDirParameter.equals(param) )
  {
   cfg.setPublicFTPPath( createPath(PublicFTPDirParameter,val, cfg.getBaseDirectory()) );
 
   return true;
  }

  if( UserGroupDirParameter.equals(param) )
  {
   cfg.setUserGroupDropboxPath( createPath(UserGroupDirParameter, val, cfg.getBaseDirectory()) );
   
   return true;
  }
  
  if( PublicDropboxesParameter.equals(param) )
  {
   cfg.setPublicDropboxes( "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val) || "1".equals(val) );
   
   return true;
  }

  
  if( UserGroupIndexDirParameter.equals(param) )
  {
   cfg.setUserGroupIndexPath( createPath(UserGroupIndexDirParameter,val, cfg.getBaseDirectory()) );

   cfg.setUsersIndexPath( cfg.getUserGroupIndexPath().resolve(BackendConfig.UsersDir) );
   cfg.setGroupsIndexPath( cfg.getUserGroupIndexPath().resolve(BackendConfig.GroupsDir) );
   
   return true;
  }

  if( DataMountPathParameter.equals(param) )
  {
   cfg.setDataMountPath(val);
   return true;
  }
  
  if( RecapchaPrivateKeyParameter.equals(param) )
  {
   cfg.setRecapchaPrivateKey(val);
   return true;
  }

  if( AllowFileLinksParameter.equals(param) )
  {
   cfg.setFileLinkAllowed( val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1") );
   return true;
  }
  
  if( EnableUnsafeRequestsParameter.equals(param) )
  {
   cfg.setEnableUnsafeRequests( val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1") );
   return true;
  }


  if( CreateFileStructureParameter.equals(param) )
  {
   cfg.setCreateFileStructure( val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1") );
   return true; 
  }
  
  if( MandatoryAccountActivationParameter.equals(param) )
  {
   cfg.setMandatoryAccountActivation( val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1") );
   return true; 
  }


  if( ActivationEmailSubjectParameter.equals(param) )
  {
   cfg.setActivationEmailSubject(val);
   
   return true;
  }
  
  if( PassResetEmailSubjectParameter.equals(param) )
  {
   cfg.setPassResetEmailSubject(val);
   
   return true;
  }


  
  if( ActivationEmailPlainTextParameter.equals(param) )
  {
   cfg.setActivationEmailPlainTextFile( new FileResource(createPath(ActivationEmailPlainTextParameter,val, cfg.getBaseDirectory())));
   
   return true;
  }

  if( ActivationEmailHtmlParameter.equals(param) )
  {
   cfg.setActivationEmailHtmlFile(new FileResource(createPath(ActivationEmailHtmlParameter,val, cfg.getBaseDirectory()) ));
   
   return true;
  }
  
  
  if( ActivationTimeoutParameter.equals(param) || ActivationTimeoutParameterHours.equals(param) )
  {
   try
   {
    cfg.setActivationTimeout( (long) ( Double.parseDouble(val) * 60 * 60 * 1000L ) ) ;
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(ActivationTimeoutParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }
  
  
  if( SubscriptionEmailSubjectParameter.equals(param) )
  {
   cfg.setSubscriptionEmailSubject(val);
   
   return true;
  }


  if( SubscriptionEmailPlainTextParameter.equals(param) )
  {
   cfg.setSubscriptionEmailPlainTextFile( new FileResource( createPath(SubscriptionEmailPlainTextParameter,val, cfg.getBaseDirectory()) ) );
   
   return true;
  }

  if( SubscriptionEmailHtmlParameter.equals(param) )
  {
   cfg.setSubscriptionEmailHtmlFile( new FileResource( createPath(SubscriptionEmailHtmlParameter,val, cfg.getBaseDirectory()) ) );
   
   return true;
  }

  
  if( PassResetEmailPlainTextParameter.equals(param) )
  {
   cfg.setPassResetEmailPlainTextFile( new FileResource( createPath(PassResetEmailPlainTextParameter,val, cfg.getBaseDirectory())) );
   
   return true;
  }

  if( PassResetEmailHtmlParameter.equals(param) )
  {
   cfg.setPassResetEmailHtmlFile( new FileResource( createPath(PassResetEmailHtmlParameter,val, cfg.getBaseDirectory())) );
   
   return true;
  }
  
  
  if( PassResetTimeoutParameter.equals(param) )
  {
   try
   {
    cfg.setPassResetTimeout( Integer.parseInt(val) * 60 * 60 * 1000L );
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(PassResetTimeoutParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }
  
  return false;

 }
 
 private static Path createPath( String prm, String p, Path baseDir ) throws ServiceConfigException
 {
  Path np = FileSystems.getDefault().getPath(p);
  
  if( np.isAbsolute() )
   return np;
  
  if( baseDir != null )
   return baseDir.resolve(np);

  throw new ServiceConfigException(prm+": path should be either absolute or "+BaseDirParameter+" parameter should be defined before");
 }

 private static boolean checkDirectory(Path file, boolean create)
 {
  if( file == null )
   return false;
  
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
  else if( create )
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

}
