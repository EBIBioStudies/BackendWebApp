package uk.ac.ebi.biostd.webapp.server.config;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biostd.webapp.server.export.TaskConfig;
import uk.ac.ebi.biostd.webapp.server.export.TaskInfo;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceManager;
import uk.ac.ebi.biostd.webapp.server.util.Resource;

public class ConfigBean
{
 private String               dataMountPath;
 private String               recapchaPublicKey;
 private String               recapchaPrivateKey;

 private ServiceManager       defaultServiceManager;
 private EntityManagerFactory emf;
 private TaskInfo             expTaskInfo;

 private boolean              createFileStructure        = false;

 private Path                 baseDirectory;
 private Path                 workDirectory;

 private Path                 userGroupDropboxPath;
 
 private Path                 userGroupIndexPath;
 private Path                 usersIndexPath;
 private Path                 groupsIndexPath;
 
 private Path                 submissionsPath;
 private Path                 submissionsHistoryPath;
 private Path                 submissionsTransactionPath;
 private Path                 submissionUpdatePath;
 
 private Path                 publicFTPPath;

 private String               updateListenerURLPfx;
 private String               updateListenerURLSfx;

 private String               activationEmailSubject;
 private String               passResetEmailSubject;
 private String               subscriptionEmailSubject;

 private Resource             activationEmailPlainTextFile;
 private Resource             activationEmailHtmlFile;
 
 private Resource             passResetEmailPlainTextFile;
 private Resource             passResetEmailHtmlFile;

 private Resource             subscriptionEmailHtmlFile;
 private Resource             subscriptionEmailPlainTextFile;

 private String               defaultSubmissionAccPrefix = null;
 private String               defaultSubmissionAccSuffix = null;

 private int                  updateWaitPeriod           = 5;
 private int                  maxUpdatesPerFile          = 50;

 private boolean              fileLinkAllowed            = true;

 private boolean              publicDropboxes            = false;

 private boolean              enableUnsafeRequests       = true;
 private boolean              mandatoryAccountActivation = true;

 private boolean              searchEnabled              = false;

 private long                 activationTimeout;
 private long                 passResetTimeout;

 private Map<String, Object> databaseConfig;
 private Map<String, Object> emailConfig;
 private TaskConfig taskConfig;
 private Timer timer;
 private String uiURL;


 public String getDataMountPath()
 {
  return dataMountPath;
 }

 public void setDataMountPath(String dataMountPath)
 {
  this.dataMountPath = dataMountPath;
 }
 

 public String getRecapchaPublicKey()
 {
  return recapchaPublicKey;
 }

 public void setRecapchaPublicKey(String key)
 {
  this.recapchaPublicKey = key;
 }
 
 public String getRecapchaPrivateKey()
 {
  return recapchaPrivateKey;
 }

 public void setRecapchaPrivateKey(String recapchaPrivateKey)
 {
  this.recapchaPrivateKey = recapchaPrivateKey;
 }


 public ServiceManager getDefaultServiceManager()
 {
  return defaultServiceManager;
 }

 public void setDefaultServiceManager(ServiceManager defaultServiceManager)
 {
  this.defaultServiceManager = defaultServiceManager;
 }

 public EntityManagerFactory getEmf()
 {
  return emf;
 }

 public void setEmf(EntityManagerFactory emf)
 {
  this.emf = emf;
 }

 public TaskInfo getExpTaskInfo()
 {
  return expTaskInfo;
 }

 public void setExpTaskInfo(TaskInfo expTaskInfo)
 {
  this.expTaskInfo = expTaskInfo;
 }

 public boolean isCreateFileStructure()
 {
  return createFileStructure;
 }

 public void setCreateFileStructure(boolean createFileStructure)
 {
  this.createFileStructure = createFileStructure;
 }

 public Path getBaseDirectory()
 {
  return baseDirectory;
 }

 public void setBaseDirectory(Path baseDirectory)
 {
  this.baseDirectory = baseDirectory;
 }

 public Path getWorkDirectory()
 {
  return workDirectory;
 }

 public void setWorkDirectory(Path workDirectory)
 {
  this.workDirectory = workDirectory;
 }

 public Path getUserGroupDropboxPath()
 {
  return userGroupDropboxPath;
 }

 public void setUserGroupDropboxPath(Path userGroupDropboxPath)
 {
  this.userGroupDropboxPath = userGroupDropboxPath;
 }

 public Path getUserGroupIndexPath()
 {
  return userGroupIndexPath;
 }

 public void setUserGroupIndexPath(Path userGroupIndexPath)
 {
  this.userGroupIndexPath = userGroupIndexPath;
 }

 public Path getUsersIndexPath()
 {
  return usersIndexPath;
 }

 public void setUsersIndexPath(Path usersIndexPath)
 {
  this.usersIndexPath = usersIndexPath;
 }

 public Path getGroupsIndexPath()
 {
  return groupsIndexPath;
 }

 public void setGroupsIndexPath(Path groupsIndexPath)
 {
  this.groupsIndexPath = groupsIndexPath;
 }

 public Path getSubmissionsPath()
 {
  return submissionsPath;
 }

 public void setSubmissionsPath(Path submissionsPath)
 {
  this.submissionsPath = submissionsPath;
 }

 public Path getSubmissionsHistoryPath()
 {
  return submissionsHistoryPath;
 }

 public void setSubmissionsHistoryPath(Path submissionsHistoryPath)
 {
  this.submissionsHistoryPath = submissionsHistoryPath;
 }

 public Path getSubmissionsTransactionPath()
 {
  return submissionsTransactionPath;
 }

 public void setSubmissionsTransactionPath(Path submissionsTransactionPath)
 {
  this.submissionsTransactionPath = submissionsTransactionPath;
 }

 public Path getPublicFTPPath()
 {
  return publicFTPPath;
 }

 public void setPublicFTPPath(Path publicFTPPath)
 {
  this.publicFTPPath = publicFTPPath;
 }

 public Path getSubmissionUpdatePath()
 {
  return submissionUpdatePath;
 }

 public void setSubmissionUpdatePath(Path submissionUpdatePath)
 {
  this.submissionUpdatePath = submissionUpdatePath;
 }

 public String getUpdateListenerURLPfx()
 {
  return updateListenerURLPfx;
 }

 public void setUpdateListenerURLPfx(String updateListenerURLPfx)
 {
  this.updateListenerURLPfx = updateListenerURLPfx;
 }

 public String getUpdateListenerURLSfx()
 {
  return updateListenerURLSfx;
 }

 public void setUpdateListenerURLSfx(String updateListenerURLSfx)
 {
  this.updateListenerURLSfx = updateListenerURLSfx;
 }

 public String getActivationEmailSubject()
 {
  return activationEmailSubject;
 }

 public void setActivationEmailSubject(String activationEmailSubject)
 {
  this.activationEmailSubject = activationEmailSubject;
 }

 public String getPassResetEmailSubject()
 {
  return passResetEmailSubject;
 }

 public void setPassResetEmailSubject(String passResetEmailSubject)
 {
  this.passResetEmailSubject = passResetEmailSubject;
 }

 public String getSubscriptionEmailSubject()
 {
  return subscriptionEmailSubject;
 }

 public void setSubscriptionEmailSubject(String subscriptionEmailSubject)
 {
  this.subscriptionEmailSubject = subscriptionEmailSubject;
 }

 public Resource getActivationEmailPlainTextFile()
 {
  return activationEmailPlainTextFile;
 }

 public void setActivationEmailPlainTextFile(Resource activationEmailPlainTextFile)
 {
  this.activationEmailPlainTextFile = activationEmailPlainTextFile;
 }

 public Resource getActivationEmailHtmlFile()
 {
  return activationEmailHtmlFile;
 }

 public void setActivationEmailHtmlFile(Resource activationEmailHtmlFile)
 {
  this.activationEmailHtmlFile = activationEmailHtmlFile;
 }

 public Resource getPassResetEmailPlainTextFile()
 {
  return passResetEmailPlainTextFile;
 }

 public void setPassResetEmailPlainTextFile(Resource passResetEmailPlainTextFile)
 {
  this.passResetEmailPlainTextFile = passResetEmailPlainTextFile;
 }

 public Resource getPassResetEmailHtmlFile()
 {
  return passResetEmailHtmlFile;
 }

 public void setPassResetEmailHtmlFile(Resource passResetEmailHtmlFile)
 {
  this.passResetEmailHtmlFile = passResetEmailHtmlFile;
 }

 public Resource getSubscriptionEmailHtmlFile()
 {
  return subscriptionEmailHtmlFile;
 }

 public void setSubscriptionEmailHtmlFile(Resource subscriptionEmailHtmlFile)
 {
  this.subscriptionEmailHtmlFile = subscriptionEmailHtmlFile;
 }

 public Resource getSubscriptionEmailPlainTextFile()
 {
  return subscriptionEmailPlainTextFile;
 }

 public void setSubscriptionEmailPlainTextFile(Resource subscriptionEmailPlainTextFile)
 {
  this.subscriptionEmailPlainTextFile = subscriptionEmailPlainTextFile;
 }

 public String getDefaultSubmissionAccPrefix()
 {
  return defaultSubmissionAccPrefix;
 }

 public void setDefaultSubmissionAccPrefix(String defaultSubmissionAccPrefix)
 {
  this.defaultSubmissionAccPrefix = defaultSubmissionAccPrefix;
 }

 public String getDefaultSubmissionAccSuffix()
 {
  return defaultSubmissionAccSuffix;
 }

 public void setDefaultSubmissionAccSuffix(String defaultSubmissionAccSuffix)
 {
  this.defaultSubmissionAccSuffix = defaultSubmissionAccSuffix;
 }

 public int getUpdateWaitPeriod()
 {
  return updateWaitPeriod;
 }

 public void setUpdateWaitPeriod(int updateWaitPeriod)
 {
  this.updateWaitPeriod = updateWaitPeriod;
 }

 public int getMaxUpdatesPerFile()
 {
  return maxUpdatesPerFile;
 }

 public void setMaxUpdatesPerFile(int maxUpdatesPerFile)
 {
  this.maxUpdatesPerFile = maxUpdatesPerFile;
 }

 public boolean isFileLinkAllowed()
 {
  return fileLinkAllowed;
 }

 public void setFileLinkAllowed(boolean fileLinkAllowed)
 {
  this.fileLinkAllowed = fileLinkAllowed;
 }

 public boolean isPublicDropboxes()
 {
  return publicDropboxes;
 }

 public void setPublicDropboxes(boolean publicDropboxes)
 {
  this.publicDropboxes = publicDropboxes;
 }

 public boolean isEnableUnsafeRequests()
 {
  return enableUnsafeRequests;
 }

 public void setEnableUnsafeRequests(boolean enableUnsafeRequests)
 {
  this.enableUnsafeRequests = enableUnsafeRequests;
 }

 public boolean isMandatoryAccountActivation()
 {
  return mandatoryAccountActivation;
 }

 public void setMandatoryAccountActivation(boolean mandatoryAccountActivation)
 {
  this.mandatoryAccountActivation = mandatoryAccountActivation;
 }

 public boolean isSearchEnabled()
 {
  return searchEnabled;
 }

 public void setSearchEnabled(boolean searchEnabled)
 {
  this.searchEnabled = searchEnabled;
 }

 public long getActivationTimeout()
 {
  return activationTimeout;
 }

 public void setActivationTimeout(long activationTimeout)
 {
  this.activationTimeout = activationTimeout;
 }

 public long getPassResetTimeout()
 {
  return passResetTimeout;
 }

 public void setPassResetTimeout(long passResetTimeout)
 {
  this.passResetTimeout = passResetTimeout;
 }

 public Map<String, Object> getDatabaseConfig()
 {
  if( databaseConfig == null )
   databaseConfig = new HashMap<String, Object>();
  
  return databaseConfig;
 }

 public void setDatabaseConfig(Map<String, Object> databaseConfig)
 {
  this.databaseConfig = databaseConfig;
 }

 public TaskConfig getTaskConfig()
 {
  return taskConfig;
 }

 public void setTaskConfig(TaskConfig taskConfig)
 {
  this.taskConfig = taskConfig;
 }

 public Map<String, Object> getEmailConfig()
 {
  if( emailConfig == null )
   emailConfig = new HashMap<String, Object>();

  return emailConfig;
 }

 public void setEmailConfig(Map<String, Object> emailConfig)
 {
  this.emailConfig = emailConfig;
 }

 public Timer getTimer()
 {
  return timer;
 }

 public void setTimer(Timer timer)
 {
  this.timer = timer;
 }

 public void setUIURL(String val)
 {
  uiURL=val;
 }
 
 public String getUIURL()
 {
  return uiURL;
 }
}
