package uk.ac.ebi.biostd.webapp.server.config;

import java.nio.file.Paths;
import java.util.Map;

import uk.ac.ebi.biostd.webapp.server.util.JavaResource;

public class DefaultConfiguration
{
 static void loadDefaults(ConfigBean cfgBean)
 {
  Map<String, Object> dbConf = cfgBean.getDatabaseConfig();
  
  dbConf.put("hibernate.connection.driver_class","org.h2.Driver");
  dbConf.put("hibernate.connection.username","");
  dbConf.put("hibernate.connection.password","");
  dbConf.put("hibernate.cache.use_query_cache","false");
  dbConf.put("hibernate.ejb.discard_pc_on_close","true");
  dbConf.put(ConfigurationManager.HibernateDBConnectionURLParameter,"jdbc:h2:db/appdb;IFEXISTS=FALSE");
  dbConf.put("hibernate.dialect","org.hibernate.dialect.H2Dialect");
  dbConf.put("hibernate.hbm2ddl.auto","update");
  dbConf.put("hibernate.c3p0.max_size","30");
  dbConf.put("hibernate.c3p0.min_size","0");
  dbConf.put("hibernate.c3p0.timeout","5000");
  dbConf.put("hibernate.c3p0.max_statements","0");
  dbConf.put("hibernate.c3p0.idle_test_period","300");
  dbConf.put("hibernate.c3p0.acquire_increment","2");
  dbConf.put("hibernate.c3p0.unreturnedConnectionTimeout","18000");
  dbConf.put(ConfigurationManager.HibernateSearchIndexDirParameter,"index");
  dbConf.put("hibernate.search.default.directory_provider","filesystem");
  dbConf.put("hibernate.search.lucene_version","LUCENE_54");

  cfgBean.setWebConfigEnabled(true);
  
  cfgBean.setCreateFileStructure(true);
  cfgBean.setFileLinkAllowed(true);
  
  cfgBean.setWorkDirectory(Paths.get("work"));
  cfgBean.setSubmissionsPath(Paths.get("submission"));
  cfgBean.setSubmissionsHistoryPath(Paths.get("history"));
  cfgBean.setSubmissionsTransactionPath(Paths.get("transaction"));
  cfgBean.setSubmissionUpdatePath(Paths.get("updates"));
  cfgBean.setUserGroupIndexPath(Paths.get("ug_index"));
  cfgBean.setUserGroupDropboxPath(Paths.get("ug_data"));
  
  cfgBean.setPublicDropboxes(false);
  
  cfgBean.setEnableUnsafeRequests(false);
  
  cfgBean.setUpdateWaitPeriod(10);
  cfgBean.setMaxUpdatesPerFile(50);
  
  cfgBean.setMandatoryAccountActivation(false);
  
  cfgBean.setDefaultSubmissionAccPrefix("S-");
  
  cfgBean.setActivationEmailSubject("Account activation request");
  cfgBean.setActivationEmailPlainTextFile( new JavaResource("/resources/email/activationMail.txt"));
  cfgBean.setActivationEmailHtmlFile( new JavaResource("/resources/email/activationMail.html"));
  
  cfgBean.setPassResetEmailSubject("Password reset request");
  cfgBean.setPassResetEmailPlainTextFile( new JavaResource("/resources/email/passResetMail.txt"));
  cfgBean.setPassResetEmailHtmlFile( new JavaResource("/resources/email/passResetMail.html"));

  cfgBean.setSubscriptionEmailSubject("Subscription notification");
  cfgBean.setSubscriptionEmailPlainTextFile( new JavaResource("/resources/email/subscriptionMail.txt"));
  cfgBean.setSubscriptionEmailHtmlFile( new JavaResource("/resources/email/subscriptionMail.html"));
  
 }
}
