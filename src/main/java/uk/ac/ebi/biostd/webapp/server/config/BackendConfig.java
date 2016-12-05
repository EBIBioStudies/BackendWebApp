package uk.ac.ebi.biostd.webapp.server.config;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
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
import uk.ac.ebi.biostd.webapp.server.export.TaskInfo;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceManager;
import uk.ac.ebi.biostd.webapp.server.util.AccNoUtil;


public class BackendConfig
{
 public static Path getSubmissionsTransactionPath()
 {
  return submissionsTransactionPath;
 }


 
 public static final String SessionDir = "sessions";

 public static Set<PosixFilePermission> rwxrwx___ = PosixFilePermissions.fromString("rwxrwx---");
 public static Set<PosixFilePermission> rwxrwxr_x = PosixFilePermissions.fromString("rwxrwxr-x");
 public static Set<PosixFilePermission> rwxrwxrwx = PosixFilePermissions.fromString("rwxrwxrwx");
 public static Set<PosixFilePermission> rwx__x__x = PosixFilePermissions.fromString("rwx--x--x");

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
 
 
// public static final String GuestsGroup = "@Guests";
// public static final String EveryoneGroup = "@Everyone";
// public static final String AuthenticatedGroup = "@Authenticated";

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

 
 
 public static final String DataMountPathParameter = "dataMountPath";
 public static final String RecapchaPrivateKeyParameter = "recapcha_private_key";
 
 public static final int maxPageTabSize=5000000;

 private static final String sessionCookieName = "BIOSTDSESS";   //think about security issues on system that ignore file name cases
 private static final String sessionTokenHeader = "X-Session-Token";   //think about security issues on system that ignore file name cases



 private static String dataMountPath;
 private static String recapchaPrivateKey;

 private static long instanceId;
 private static AtomicInteger sequence;

 private static ServiceManager defaultServiceManager;
 private static EntityManagerFactory emf;
 private static TaskInfo expTaskInfo;

 private static boolean createFileStructure=false;

 private static Path baseDirectory;
 private static Path workDirectory;
 private static Path userGroupDropboxPath;
 private static Path userGroupIndexPath;
 private static Path usersIndexPath;
 private static Path groupsIndexPath;
 private static Path submissionsPath;
 private static Path submissionsHistoryPath;
 private static Path submissionsTransactionPath;
 private static Path publicFTPPath;
 private static Path submissionUpdatePath;
 private static Path ftpRootPath;
 private static Path dropboxPath;

 private static String updateListenerURLPfx;
 private static String updateListenerURLSfx;
 
 private static String activationEmailSubject;
 private static String passResetEmailSubject;
 private static String subscriptionEmailSubject;
 
 private static Path activationEmailPlainTextFile;
 private static Path activationEmailHtmlFile;
 private static Path passResetEmailPlainTextFile;
 private static Path passResetEmailHtmlFile;
 
 private static Path subscriptionEmailHtmlFile;
 private static Path subscriptionEmailPlainTextFile;
 
 private static String defaultSubmissionAccPrefix = null;
 private static String defaultSubmissionAccSuffix = null;
 
 private static int updateWaitPeriod = 5;
 private static int maxUpdatesPerFile = 50;
 
 private static boolean fileLinkAllowed=true;

 private static boolean publicDropboxes = false;
 
 private static boolean enableUnsafeRequests=true;
 private static boolean mandatoryAccountActivation=true;
 
 private static boolean searchEnabled=false;
 
 private static long activationTimeout = defaultActivationTimeout;
 private static long passResetTimeout = defaultPassResetTimeout;

 private static Map<String, Object> databaseConfig;
 
 public static void init( int contextHash )
 {
  instanceId = System.currentTimeMillis() ^ contextHash;
  sequence = new AtomicInteger();
 }
 
 public static long getInstanceId()
 {
  return instanceId;
 }
 
 public static int getSeqNumber()
 {
  return sequence.getAndIncrement();
 }
 
 public static boolean readParameter(String param, String val) throws ServiceConfigException
 {
  val = val.trim();
  param = param.trim();
  
  if( DefaultSubmissionAccPrefixParameter.equals(param) )
  {
   defaultSubmissionAccPrefix=val;
   
   return true;
  }

  if( DefaultSubmissionAccSuffixParameter.equals(param) )
  {
   defaultSubmissionAccSuffix=val;
   
   return true;
  }

  if( BaseDirParameter.equals(param) )
  {
   baseDirectory=FileSystems.getDefault().getPath(val);
   
   if( ! baseDirectory.isAbsolute() )
    throw new ServiceConfigException(BaseDirParameter+": path should be absolute");
   
   return true;
  }

  if( WorkdirParameter.equals(param) )
  {
   workDirectory=createPath(WorkdirParameter,val);
   
   return true;
  }
  

  if( FTPRootPathParameter.equals(param) )
  {
   ftpRootPath=FileSystems.getDefault().getPath(val);
   
   return true;
  }

  
  if( SubmissionDirParameter.equals(param) )
  {
   submissionsPath = createPath(SubmissionDirParameter,val);

   return true;
  }
  
  if( SubmissionUpdateParameter.equals(param) )
  {
   submissionUpdatePath = createPath(SubmissionUpdateParameter,val);

   return true;
  }
  
  if( UpdateURLParameter.equals(param) )
  {
   int pos = val.indexOf(UpdateURLFilePlaceholder);
   
   if( pos < 0 )
    throw new ServiceConfigException(UpdateURLParameter+" should contain "+UpdateURLFilePlaceholder+" placeholder");
   
   updateListenerURLPfx = val.substring(0,pos);
   updateListenerURLSfx = val.substring(pos+UpdateURLFilePlaceholder.length());

   try
   {
    new URL(updateListenerURLPfx+"aaa.txt"+updateListenerURLSfx);
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
    updateWaitPeriod = Integer.parseInt(val);
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
    maxUpdatesPerFile = Integer.parseInt(val);
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(MaxUpdatesPerFileParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }

  
  if( SubmissionHistoryDirParameter.equals(param) )
  {
   submissionsHistoryPath = createPath(SubmissionHistoryDirParameter,val);

 
   return true;
  }
  
  if( SubmissionTransactionDirParameter.equals(param) )
  {
   submissionsTransactionPath = createPath(SubmissionTransactionDirParameter,val);
 
   return true;
  }

  if( PublicFTPDirParameter.equals(param) )
  {
   publicFTPPath = createPath(PublicFTPDirParameter,val);
 
   return true;
  }

  if( UserGroupDirParameter.equals(param) )
  {
   userGroupDropboxPath = createPath(UserGroupDirParameter, val);
   
   return true;
  }
  
  if( PublicDropboxesParameter.equals(param) )
  {
   publicDropboxes= "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val) || "1".equals(val) ;
   
   return true;
  }

  
  if( UserGroupIndexDirParameter.equals(param) )
  {
   userGroupIndexPath = createPath(UserGroupIndexDirParameter,val);

   usersIndexPath = userGroupIndexPath.resolve(UsersDir);
   groupsIndexPath = userGroupIndexPath.resolve(GroupsDir);
   
   return true;
  }

  if( DataMountPathParameter.equals(param) )
  {
   dataMountPath=val;
   return true;
  }
  
  if( RecapchaPrivateKeyParameter.equals(param) )
  {
   recapchaPrivateKey=val;
   return true;
  }

  if( AllowFileLinksParameter.equals(param) )
  {
   fileLinkAllowed = val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1");
   return true;
  }
  
  if( EnableUnsafeRequestsParameter.equals(param) )
  {
   enableUnsafeRequests = val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1");
   return true;
  }


  if( CreateFileStructureParameter.equals(param) )
  {
   createFileStructure = val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1");
   return true; 
  }
  
  if( MandatoryAccountActivationParameter.equals(param) )
  {
   mandatoryAccountActivation = val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1");
   return true; 
  }


  if( ActivationEmailSubjectParameter.equals(param) )
  {
   activationEmailSubject=val;
   
   return true;
  }
  
  if( PassResetEmailSubjectParameter.equals(param) )
  {
   passResetEmailSubject=val;
   
   return true;
  }


  
  if( ActivationEmailPlainTextParameter.equals(param) )
  {
   activationEmailPlainTextFile=createPath(ActivationEmailPlainTextParameter,val);
   
   return true;
  }

  if( ActivationEmailHtmlParameter.equals(param) )
  {
   activationEmailHtmlFile=createPath(ActivationEmailHtmlParameter,val);
   
   return true;
  }
  
  
  if( ActivationTimeoutParameter.equals(param) || ActivationTimeoutParameterHours.equals(param) )
  {
   try
   {
    activationTimeout = (long) ( Double.parseDouble(val) * 60 * 60 * 1000L );
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(ActivationTimeoutParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }
  
  
  if( SubscriptionEmailSubjectParameter.equals(param) )
  {
   subscriptionEmailSubject=val;
   
   return true;
  }


  if( SubscriptionEmailPlainTextParameter.equals(param) )
  {
   subscriptionEmailPlainTextFile=createPath(SubscriptionEmailPlainTextParameter,val);
   
   return true;
  }

  if( SubscriptionEmailHtmlParameter.equals(param) )
  {
   subscriptionEmailHtmlFile=createPath(SubscriptionEmailHtmlParameter,val);
   
   return true;
  }

  
  if( PassResetEmailPlainTextParameter.equals(param) )
  {
   passResetEmailPlainTextFile=createPath(PassResetEmailPlainTextParameter,val);
   
   return true;
  }

  if( PassResetEmailHtmlParameter.equals(param) )
  {
   passResetEmailHtmlFile=createPath(PassResetEmailHtmlParameter,val);
   
   return true;
  }
  
  
  if( PassResetTimeoutParameter.equals(param) )
  {
   try
   {
    passResetTimeout = Integer.parseInt(val) * 60 * 60 * 1000L;
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
  
  if( baseDirectory != null )
   return baseDirectory.resolve(np);

  throw new ServiceConfigException(prm+": path should be either absolute or "+BaseDirParameter+" parameter should be defined before");
 }
 
 
 public static Path getWorkDirectory()
 {
  return workDirectory;
 }
 
 public static void setWorkDirectory( String dir )
 {
  workDirectory=FileSystems.getDefault().getPath(dir);
 }

 public static ServiceManager getServiceManager()
 {
  return defaultServiceManager;
 }
 
 public static void setServiceManager(ServiceManager serviceManager)
 {
  defaultServiceManager = serviceManager;
 }


 public static void setEntityManagerFactory(EntityManagerFactory e)
 {
  emf=e;
 }
 
 public static EntityManagerFactory getEntityManagerFactory()
 {
  return emf;
 }


 public static String getDataMountPath()
 {
  return dataMountPath;
 }

 public static Path getUserGroupIndexPath()
 {
  return userGroupIndexPath;
 }

 public static Path getUserGroupDropboxPath()
 {
  return userGroupDropboxPath;
 }
 
 public static Path getUsersIndexPath()
 {
  return usersIndexPath;
 }

 public static Path getGroupIndexPath()
 {
  return groupsIndexPath;
 }

 public static String getUserDropboxRelPath(User user)
 {
  String udir = user.getSecret()+"-a"+user.getId();
  
  return udir.substring(0,2)+"/"+udir.substring(2); 
 }
 
 public static Path getUserDirPath(User user)
 {
  return userGroupDropboxPath.resolve( getUserDropboxRelPath(user) );
 }

 public static Path getGroupDirPath( UserGroup g )
 {
  String udir = g.getSecret()+"-b"+g.getId();
  
  return userGroupDropboxPath.resolve( udir );
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
 
 public static Path getSubmissionsPath()
 {
  return submissionsPath;
 }

 public static String getSubmissionRelativePath( Submission sbm )
 {
  return AccNoUtil.getPartitionedPath( sbm.getAccNo() );
 }
 
 public static Path getSubmissionPath(Submission sbm)
 {
  return submissionsPath.resolve( getSubmissionRelativePath(sbm) );
 }


 public static Path getSubmissionFilesPath(Submission sbm)
 {
  return getSubmissionPath(sbm).resolve( SubmissionFilesDir );
 }

 public static Path getSubmissionPublicFTPPath(Submission sbm)
 {
  return getPublicFTPPath().resolve( getSubmissionRelativePath(sbm) );
 }

 public static Path getSubmissionsHistoryPath()
 {
  return submissionsHistoryPath;
 }
 
 public static Path getSubmissionHistoryPath(Submission sbm)
 {
  return submissionsHistoryPath.resolve( getSubmissionRelativePath(sbm) + SubmissionHistoryPostfix + Math.abs(sbm.getVersion()) );
 }


 public static String getRecapchaPrivateKey()
 {
  return recapchaPrivateKey;
 }


 public static boolean isLinkingAllowed()
 {
  return fileLinkAllowed;
 }

 public static Path getPublicFTPPath()
 {
  return publicFTPPath;
 }
 
 public static Path getSubmissionUpdatePath()
 {
  return submissionUpdatePath;
 }

 public static String getUpdateListenerURLPrefix()
 {
  return updateListenerURLPfx;
 }
 
 public static String getUpdateListenerURLPostfix()
 {
  return updateListenerURLSfx;
 }

 public static void setPublicFTPPath(Path publicFTPPath)
 {
  BackendConfig.publicFTPPath = publicFTPPath;
 }

 public static String getDefaultSubmissionAccPrefix()
 {
  return defaultSubmissionAccPrefix;
 }

 public static String getDefaultSubmissionAccSuffix()
 {
  return defaultSubmissionAccSuffix;
 }

 public static int getUpdateWaitPeriod()
 {
  return updateWaitPeriod;
 }

 public static int getMaxUpdatesPerFile()
 {
  return maxUpdatesPerFile;
 }

 public static boolean isCreateFileStructure()
 {
  return createFileStructure;
 }

 public static Path getBaseDirectory()
 {
  return baseDirectory;
 }

 public static Path getFtpRootPath()
 {
  return ftpRootPath;
 }

 public static Path getDropboxPath()
 {
  return dropboxPath;
 }


 public static TaskInfo getExportTask()
 {
  return expTaskInfo;
 }
 
 public static void setExportTask( TaskInfo ti )
 {
  expTaskInfo = ti;
 }

 public static void setDatabaseConfig(Map<String, Object> dbConfig)
 {
  databaseConfig = new HashMap<String, Object>(dbConfig);
  
 }
 
 public static Map<String, Object> getDatabaseConfig()
 {
  return databaseConfig;
  
 }
 
 public static String getActivationEmailSubject()
 {
  return activationEmailSubject;
 }

 public static Path getActivationEmailPlainTextFile()
 {
  return activationEmailPlainTextFile;
 }

 public static Path getActivationEmailHtmlFile()
 {
  return activationEmailHtmlFile;
 }

 public static boolean isEnableUnsafeRequests()
 {
  return enableUnsafeRequests;
 }

 public static boolean isMandatoryAccountActivation()
 {
  return mandatoryAccountActivation;
 }

 public static boolean isPublicDropboxes()
 {
  return publicDropboxes;
 }
 
 public static boolean isSearchEnabled()
 {
  return searchEnabled;
 }

 public static void setSearchEnabled(boolean searchEnabled)
 {
  BackendConfig.searchEnabled = searchEnabled;
 }

 public static long getActivationTimeout()
 {
  return activationTimeout;
 }
 
 public static long getPasswordResetTimeout()
 {
  return passResetTimeout;
 }

 public static Path getPassResetEmailHtmlFile()
 {
  return passResetEmailHtmlFile;
 }

 public static Path getPassResetEmailPlainTextFile()
 {
  return passResetEmailPlainTextFile;
 }

 public static String getPassResetEmailSubject()
 {
  return passResetEmailSubject;
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
  return subscriptionEmailHtmlFile;
 }

 public static Path getSubscriptionEmailPlainTextFile()
 {
  return subscriptionEmailPlainTextFile;
 }
 
 public static String getSubscriptionEmailSubject()
 {
  return subscriptionEmailSubject;
 }
}
