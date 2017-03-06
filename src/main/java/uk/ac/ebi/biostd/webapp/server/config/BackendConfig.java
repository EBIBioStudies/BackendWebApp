package uk.ac.ebi.biostd.webapp.server.config;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfig;
import uk.ac.ebi.biostd.webapp.server.export.TaskInfo;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceManager;
import uk.ac.ebi.biostd.webapp.server.util.AccNoUtil;


public class BackendConfig
{
 
 public static final String SessionDir = "sessions";

 public static Set<PosixFilePermission> rwxrwx___ = PosixFilePermissions.fromString("rwxrwx---");
 public static Set<PosixFilePermission> rwxrwxr_x = PosixFilePermissions.fromString("rwxrwxr-x");
 public static Set<PosixFilePermission> rwxrwxrwx = PosixFilePermissions.fromString("rwxrwxrwx");
 public static Set<PosixFilePermission> rwx__x__x = PosixFilePermissions.fromString("rwx--x--x");

 public static final String ConvertSpell = "*MYTA6OP!*";
 
 public static final String UserNamePlaceHolderRx = "\\{USERNAME\\}";
 public static final String ActivateKeyPlaceHolderRx= "\\{KEY\\}";
 public static final String ActivateURLPlaceHolderRx= "\\{URL\\}";
 public static final String TextPlaceHolderRx= "\\{TEXT\\}";
 public static final String AccNoPlaceHolderRx= "\\{ACCNO\\}";
 public static final String TitlePlaceHolderRx= "\\{TITLE\\}";
 public static final String SbmTitlePlaceHolderRx= "\\{SBTITLE\\}";
 public static final String TypePlaceHolderRx= "\\{TYPE\\}";
 public static final String TagsPlaceHolderRx= "\\{TAGS(:[^}]*)?\\}";
 
 public static final String googleVerifyURL = "https://www.google.com/recaptcha/api/siteverify";
 public static final String googleSecretParam = "secret";
 public static final String googleResponseParam = "response";
 public static final String googleRemoteipParam = "remoteip";
 public static final String googleClientResponseParameter="recaptcha2-response";
 public static final String googleSuccessField = "success";
 
 public static final long defaultActivationTimeout = 2*24*60*60*1000L; 
 public static final long defaultPassResetTimeout  = 1*24*60*60*1000L; 
 
 public static final long exportLockTimeout = 1*60*60*1000L;
 public static final long exportLockDelay = 10*60*1000L;
 
// public static final String GuestsGroup = "@Guests";
// public static final String EveryoneGroup = "@Everyone";
// public static final String AuthenticatedGroup = "@Authenticated";
 public static final String             DefaultSubmissionPrefix             = "S-";

 
 public static final String             PublicTag                           = "Public";

 public static final String             SubmissionHistoryPostfix            = "#ver";
 public static final String             SubmissionFilesDir                  = "Files";
 public static final String             UsersDir                            = "Users";
 public static final String             GroupsDir                           = "Groups";

 public static final String             CreateFileStructureParameter        = "createFileStructure";

 public static final String             BaseDirParameter                    = "baseDir";
 
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
 public static final String             FTPRootPathParameter                = "FTPRootPath";
 
 public static final String             MandatoryAccountActivationParameter = "mandatoryAccountActivation";
 public static final String             ActivationEmailSubjectParameter     = "activationEmailSubject";
 public static final String             ActivationEmailPlainTextParameter   = "activationEmailPlainTextFile";
 public static final String             ActivationEmailHtmlParameter        = "activationEmailHtmlFile";
 public static final String             ActivationTimeoutParameter          = "activationTimeout";
 public static final String             ActivationTimeoutParameterHours     = "activationTimeoutHours";

 public static final String             SubscriptionEmailSubjectParameter     = "subscriptionEmailSubject";
 public static final String             SubscriptionEmailPlainTextParameter   = "subscriptionEmailPlainTextFile";
 public static final String             SubscriptionEmailHtmlParameter        = "subscriptionEmailHtmlFile";
 
 public static final String             PassResetTimeoutParameter           = "passwordResetTimeout";
 public static final String             PassResetEmailSubjectParameter      = "passwordResetEmailSubject";
 public static final String             PassResetEmailPlainTextParameter    = "passwordResetEmailPlainTextFile";
 public static final String             PassResetEmailHtmlParameter         = "passwordResetEmailHtmlFile";
 
 public static final String             DefaultSubmissionAccPrefixParameter = "defaultSubmissionAccNoPrefix";
 public static final String             DefaultSubmissionAccSuffixParameter = "defaultSubmissionAccNoSuffix";

 public static boolean EncodeFileNames = false; 
 
 
 public static final String DataMountPathParameter = "dataMountPath";
 public static final String RecapchaPrivateKeyParameter = "recapcha_private_key";
 
 public static final int maxPageTabSize=5000000;

 private static final String sessionCookieName = "BIOSTDSESS";   //think about security issues on system that ignore file name cases
 private static final String sessionTokenHeader = "X-Session-Token";   //think about security issues on system that ignore file name cases


// private static long activationTimeout = defaultActivationTimeout;
// private static long passResetTimeout = defaultPassResetTimeout;

  
 private static ConfigBean conf;
 
 public static void init( int contextHash )
 {
  conf = new ConfigBean();
  
  initConfigBean(conf);
  
  conf.setInstanceId( System.currentTimeMillis() ^ contextHash );
 }
 
 private static void initConfigBean( ConfigBean cfg )
 {
  cfg.setCreateFileStructure(true);
  cfg.setDefaultSubmissionAccPrefix(DefaultSubmissionPrefix);
  cfg.setDropboxPath(Paths.get("dropbox"));
  
  Map<String, Object> dbConf = new HashMap<String, Object>();

  
  dbConf.put("hibernate.connection.driver_class","com.mysql.jdbc.Driver");
  dbConf.put("hibernate.connection.username","");
  dbConf.put("hibernate.connection.password","");
  dbConf.put("hibernate.cache.use_query_cache","false");
  dbConf.put("hibernate.ejb.discard_pc_on_close","true");
  dbConf.put("hibernate.connection.url","jdbc:mysql://mysql-fg-biostudy.ebi.ac.uk:4469/biostd_beta?autoReconnect=true&amp;useUnicode=yes&amp;characterEncoding=UTF-8");
  dbConf.put("hibernate.dialect","org.hibernate.dialect.MySQLDialect");
  dbConf.put("hibernate.hbm2ddl.auto","update");
  dbConf.put("hibernate.c3p0.max_size","30");
  dbConf.put("hibernate.c3p0.min_size","0");
  dbConf.put("hibernate.c3p0.timeout","5000");
  dbConf.put("hibernate.c3p0.max_statements","0");
  dbConf.put("hibernate.c3p0.idle_test_period","300");
  dbConf.put("hibernate.c3p0.acquire_increment","2");
  dbConf.put("hibernate.c3p0.unreturnedConnectionTimeout","1800");
  dbConf.put("hibernate.search.default.indexBase","index");
  dbConf.put("hibernate.search.default.directory_provider","filesystem");
  dbConf.put("hibernate.search.lucene_version","LUCENE_54");
  
  cfg.setDatabaseConfig(dbConf);
  
  cfg.setSequence(new AtomicInteger() );
 }
 
 public static long getInstanceId()
 {
  return conf.getInstanceId();
 }
 
 public static int getSeqNumber()
 {
  return conf.getSequence().getAndIncrement();
 }
 
 public static boolean readParameter(String param, String val) throws ServiceConfigException
 {
  return readParameter(param, val, conf);
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
   cfg.setWorkDirectory(createPath(WorkdirParameter,val));
   
   return true;
  }
  

  if( FTPRootPathParameter.equals(param) )
  {
   cfg.setFtpRootPath(FileSystems.getDefault().getPath(val));
   
   return true;
  }

  
  if( SubmissionDirParameter.equals(param) )
  {
   cfg.setSubmissionsPath(createPath(SubmissionDirParameter,val));

   return true;
  }
  
  if( SubmissionUpdateParameter.equals(param) )
  {
   cfg.setSubmissionUpdatePath(createPath(SubmissionUpdateParameter,val));

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
   cfg.setSubmissionsHistoryPath( createPath(SubmissionHistoryDirParameter,val) );
 
   return true;
  }
  
  if( SubmissionTransactionDirParameter.equals(param) )
  {
   cfg.setSubmissionsTransactionPath( createPath(SubmissionTransactionDirParameter,val) );
 
   return true;
  }

  if( PublicFTPDirParameter.equals(param) )
  {
   cfg.setPublicFTPPath( createPath(PublicFTPDirParameter,val) );
 
   return true;
  }

  if( UserGroupDirParameter.equals(param) )
  {
   cfg.setUserGroupDropboxPath( createPath(UserGroupDirParameter, val) );
   
   return true;
  }
  
  if( PublicDropboxesParameter.equals(param) )
  {
   cfg.setPublicDropboxes( "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val) || "1".equals(val) );
   
   return true;
  }

  
  if( UserGroupIndexDirParameter.equals(param) )
  {
   cfg.setUserGroupIndexPath( createPath(UserGroupIndexDirParameter,val) );

   cfg.setUsersIndexPath( cfg.getUserGroupIndexPath().resolve(UsersDir) );
   cfg.setGroupsIndexPath( cfg.getUserGroupIndexPath().resolve(GroupsDir) );
   
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
   cfg.setActivationEmailPlainTextFile(createPath(ActivationEmailPlainTextParameter,val));
   
   return true;
  }

  if( ActivationEmailHtmlParameter.equals(param) )
  {
   cfg.setActivationEmailHtmlFile(createPath(ActivationEmailHtmlParameter,val) );
   
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
   cfg.setSubscriptionEmailPlainTextFile( createPath(SubscriptionEmailPlainTextParameter,val) );
   
   return true;
  }

  if( SubscriptionEmailHtmlParameter.equals(param) )
  {
   cfg.setSubscriptionEmailHtmlFile( createPath(SubscriptionEmailHtmlParameter,val) );
   
   return true;
  }

  
  if( PassResetEmailPlainTextParameter.equals(param) )
  {
   cfg.setPassResetEmailPlainTextFile( createPath(PassResetEmailPlainTextParameter,val) );
   
   return true;
  }

  if( PassResetEmailHtmlParameter.equals(param) )
  {
   cfg.setPassResetEmailHtmlFile( createPath(PassResetEmailHtmlParameter,val) );
   
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

 private static Path createPath( String prm, String p ) throws ServiceConfigException
 {
  Path np = FileSystems.getDefault().getPath(p);
  
  if( np.isAbsolute() )
   return np;
  
  if( conf.getBaseDirectory() != null )
   return conf.getBaseDirectory().resolve(np);

  throw new ServiceConfigException(prm+": path should be either absolute or "+BaseDirParameter+" parameter should be defined before");
 }
 
 
 public static Path getWorkDirectory()
 {
  return conf.getWorkDirectory();
 }
 
 public static void setWorkDirectory( String dir )
 {
  conf.setWorkDirectory( FileSystems.getDefault().getPath(dir) );
 }

 public static ServiceManager getServiceManager()
 {
  return conf.getDefaultServiceManager();
 }
 
 public static void setServiceManager(ServiceManager serviceManager)
 {
  conf.setDefaultServiceManager( serviceManager );
 }


 public static void setEntityManagerFactory(EntityManagerFactory e)
 {
  conf.setEmf( e );
 }
 
 public static EntityManagerFactory getEntityManagerFactory()
 {
  return conf.getEmf();
 }


 public static String getDataMountPath()
 {
  return conf.getDataMountPath();
 }

 public static Path getUserGroupIndexPath()
 {
  return conf.getUserGroupIndexPath();
 }

 public static Path getUserGroupDropboxPath()
 {
  return conf.getUserGroupDropboxPath();
 }
 
 public static Path getUsersIndexPath()
 {
  return conf.getUsersIndexPath();
 }

 public static Path getGroupIndexPath()
 {
  return conf.getGroupsIndexPath();
 }

 public static String getUserDropboxRelPath(User user)
 {
  String udir = user.getSecret()+"-a"+user.getId();
  
  return udir.substring(0,2)+"/"+udir.substring(2); 
 }
 
 public static String getGroupDropboxRelPath(UserGroup ug)
 {
  String udir = ug.getSecret()+"-b"+ug.getId();
  
  return udir.substring(0,2)+"/"+udir.substring(2); 
 }

 
 public static Path getUserDirPath(User user)
 {
  return conf.getUserGroupDropboxPath().resolve( getUserDropboxRelPath(user) );
 }

 public static Path getGroupDirPath( UserGroup g )
 {
  return conf.getUserGroupDropboxPath().resolve( getGroupDropboxRelPath(g) );
 }

 public static Path getUserLoginLinkPath( User u )
 {
  String login = u.getLogin();
  
  if( login == null || login.length() == 0 )
   return null;
  
  String firstCh = login.substring(0,1);
  
  return getUsersIndexPath().resolve(AccNoUtil.encode(firstCh) ).resolve( AccNoUtil.encode(login)+".login");
 }
 
 public static Path getUserEmailLinkPath( User u )
 {
  String email = u.getEmail();
  
  if( email == null || email.length() == 0 )
   return null;
  
  String firstCh = email.substring(0,1);
  
  return getUsersIndexPath().resolve(AccNoUtil.encode(firstCh) ).resolve( AccNoUtil.encode(email)+".email");
 }
 
 public static Path getGroupLinkPath( UserGroup u )
 {
  String name = u.getName();
  
  if( name == null || name.length() == 0 )
   return null;
  
  String firstCh = name.substring(0,1);
  
  return getGroupIndexPath().resolve(AccNoUtil.encode(firstCh) ).resolve( AccNoUtil.encode(name));
 }
 
 public static Path getSubmissionsPath()
 {
  return conf.getSubmissionsPath();
 }

 public static String getSubmissionRelativePath( Submission sbm )
 {
  return AccNoUtil.getPartitionedPath( sbm.getAccNo() );
 }
 
 public static Path getSubmissionPath(Submission sbm)
 {
  return conf.getSubmissionsPath().resolve( getSubmissionRelativePath(sbm) );
 }


 public static Path getSubmissionFilesPath(Submission sbm)
 {
  return getSubmissionPath(sbm).resolve( SubmissionFilesDir );
 }
 
 public static Path getSubmissionFilesUGPath(long groupID)
 {
  return Paths.get( groupID>0?Long.toHexString(groupID):"u");
 }

 public static Path getSubmissionPublicFTPPath(Submission sbm)
 {
  return getPublicFTPPath().resolve( getSubmissionRelativePath(sbm) );
 }

 public static Path getSubmissionsHistoryPath()
 {
  return conf.getSubmissionsHistoryPath();
 }
 
 public static Path getSubmissionHistoryPath(Submission sbm)
 {
  return conf.getSubmissionsHistoryPath().resolve( getSubmissionRelativePath(sbm) + SubmissionHistoryPostfix + Math.abs(sbm.getVersion()) );
 }


 public static String getRecapchaPrivateKey()
 {
  return conf.getRecapchaPrivateKey();
 }


 public static boolean isLinkingAllowed()
 {
  return conf.isFileLinkAllowed();
 }

 public static Path getPublicFTPPath()
 {
  return conf.getPublicFTPPath();
 }
 
 public static Path getSubmissionUpdatePath()
 {
  return conf.getSubmissionUpdatePath();
 }

 public static String getUpdateListenerURLPrefix()
 {
  return conf.getUpdateListenerURLPfx();
 }
 
 public static String getUpdateListenerURLPostfix()
 {
  return conf.getUpdateListenerURLSfx();
 }

 public static void setPublicFTPPath(Path publicFTPPath)
 {
  conf.setPublicFTPPath( publicFTPPath );
 }

 public static String getDefaultSubmissionAccPrefix()
 {
  return conf.getDefaultSubmissionAccPrefix();
 }

 public static String getDefaultSubmissionAccSuffix()
 {
  return conf.getDefaultSubmissionAccSuffix();
 }

 public static int getUpdateWaitPeriod()
 {
  return conf.getUpdateWaitPeriod();
 }

 public static int getMaxUpdatesPerFile()
 {
  return conf.getMaxUpdatesPerFile();
 }

 public static boolean isCreateFileStructure()
 {
  return conf.isCreateFileStructure();
 }

 public static Path getBaseDirectory()
 {
  return conf.getBaseDirectory();
 }

 public static Path getFtpRootPath()
 {
  return conf.getFtpRootPath();
 }

 public static Path getDropboxPath()
 {
  return conf.getDropboxPath();
 }


 public static TaskInfo getExportTask()
 {
  return conf.getExpTaskInfo();
 }
 
 public static void setExportTask( TaskInfo ti )
 {
  conf.setExpTaskInfo( ti );
 }

 public static void setDatabaseConfig(Map<String, Object> dbConfig)
 {
  conf.setDatabaseConfig( new HashMap<String, Object>(dbConfig) );
  
 }
 
 public static Map<String, Object> getDatabaseConfig()
 {
  return conf.getDatabaseConfig();
 }
 
 public static String getActivationEmailSubject()
 {
  return conf.getActivationEmailSubject();
 }

 public static Path getActivationEmailPlainTextFile()
 {
  return conf.getActivationEmailPlainTextFile();
 }

 public static Path getActivationEmailHtmlFile()
 {
  return conf.getActivationEmailHtmlFile();
 }

 public static boolean isEnableUnsafeRequests()
 {
  return conf.isEnableUnsafeRequests();
 }

 public static boolean isMandatoryAccountActivation()
 {
  return conf.isMandatoryAccountActivation();
 }

 public static boolean isPublicDropboxes()
 {
  return conf.isPublicDropboxes();
 }
 
 public static boolean isSearchEnabled()
 {
  return conf.isSearchEnabled();
 }

 public static void setSearchEnabled(boolean searchEnabled)
 {
  conf.setSearchEnabled( searchEnabled );
 }

 public static long getActivationTimeout()
 {
  return conf.getActivationTimeout();
 }
 
 public static long getPasswordResetTimeout()
 {
  return conf.getPassResetTimeout();
 }

 public static Path getPassResetEmailHtmlFile()
 {
  return conf.getPassResetEmailHtmlFile();
 }

 public static Path getPassResetEmailPlainTextFile()
 {
  return conf.getPassResetEmailPlainTextFile();
 }

 public static String getPassResetEmailSubject()
 {
  return conf.getPassResetEmailSubject();
 }


 public static String getSessionCookieName()
 {
  return sessionCookieName;
 }

 public static String getSessionTokenHeader()
 {
  return sessionTokenHeader;
 }

 public static Path getSubscriptionEmailHtmlFile()
 {
  return conf.getSubscriptionEmailHtmlFile();
 }

 public static Path getSubscriptionEmailPlainTextFile()
 {
  return conf.getSubscriptionEmailPlainTextFile();
 }
 
 public static String getSubscriptionEmailSubject()
 {
  return conf.getSubscriptionEmailSubject();
 }

 public static Path getSubmissionsTransactionPath()
 {
  return conf.getSubmissionsTransactionPath();
 }
 
 public static boolean isEncodeFileNames()
 {
  return EncodeFileNames;
 }

 public static long getExportLockTimeoutMsec()
 {
  return exportLockTimeout;
 }

 public static long getExportLockDelayMsec()
 {
  return exportLockDelay;
 }

 public static Map<String, Object> getEmailConfig()
 {
  return conf.getEmailConfig();
 }
 
 public static TaskConfig getTaskConfig()
 {
  return conf.getTaskConfig();
 }
 
 public static void setEmailConfig(Map<String, Object> emailConfig)
 {
  conf.setEmailConfig(emailConfig);
 }

 public static void setTaskConfig(TaskConfig taskConfig)
 {
  conf.setTaskConfig(taskConfig); 
 }

}
