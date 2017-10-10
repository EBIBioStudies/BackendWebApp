/**
 * Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * @author Mikhail Gostev <gostev@gmail.com>
 **/

package uk.ac.ebi.biostd.webapp.server.config;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.EntityManagerFactory;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfig;
import uk.ac.ebi.biostd.webapp.server.export.TaskInfo;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceManager;
import uk.ac.ebi.biostd.webapp.server.util.AccNoUtil;
import uk.ac.ebi.biostd.webapp.server.util.Resource;


public class BackendConfig {

    public static final String SessionDir = "sessions";

    public static Set<PosixFilePermission> rwxrwx___ = PosixFilePermissions.fromString("rwxrwx---");
    public static Set<PosixFilePermission> rwxrwxr_x = PosixFilePermissions.fromString("rwxrwxr-x");
    public static Set<PosixFilePermission> rwxrwxrwx = PosixFilePermissions.fromString("rwxrwxrwx");
    public static Set<PosixFilePermission> rwx__x__x = PosixFilePermissions.fromString("rwx--x--x");

    public static final String ConvertSpell = "*MYTA6OP!*";

    public static final String UserNamePlaceHolderRx = "\\{USERNAME\\}";
    public static final String ActivateKeyPlaceHolderRx = "\\{KEY\\}";
    public static final String ActivateURLPlaceHolderRx = "\\{URL\\}";
    public static final String TextPlaceHolderRx = "\\{TEXT\\}";
    public static final String AccNoPlaceHolderRx = "\\{ACCNO\\}";
    public static final String TitlePlaceHolderRx = "\\{TITLE\\}";
    public static final String SbmTitlePlaceHolderRx = "\\{SBTITLE\\}";
    public static final String MailToPlaceHolderRx = "\\{MAILTO\\}";
    public static final String UIURLPlaceHolderRx = "\\{UIURL\\}";
    public static final String TypePlaceHolderRx = "\\{TYPE\\}";
    public static final String TagsPlaceHolderRx = "\\{TAGS(:[^}]*)?\\}";

    public static final String googleVerifyURL = "https://www.google.com/recaptcha/api/siteverify";
    public static final String googleSecretParam = "secret";
    public static final String googleResponseParam = "response";
    public static final String googleRemoteipParam = "remoteip";
    public static final String googleClientResponseParameter = "recaptcha2-response";
    public static final String googleSuccessField = "success";

    public static final long defaultActivationTimeout = 2 * 24 * 60 * 60 * 1000L;
    public static final long defaultPassResetTimeout = 1 * 24 * 60 * 60 * 1000L;

    public static final long exportLockTimeout = 1 * 60 * 60 * 1000L;
    public static final long exportLockDelay = 10 * 60 * 1000L;

    // public static final String GuestsGroup = "@Guests";
// public static final String EveryoneGroup = "@Everyone";
// public static final String AuthenticatedGroup = "@Authenticated";
    public static final String DefaultSubmissionPrefix = "S-";


    public static final String PublicTag = "Public";

    public static final String SubmissionHistoryPostfix = "#ver";
    public static final String SubmissionFilesDir = "Files";
    public static final String UsersDir = "Users";
    public static final String GroupsDir = "Groups";


    public static boolean EncodeFileNames = false;


    public static final int maxPageTabSize = 5000000;

    private static final String sessionCookieName = "BIOSTDSESS";   //think about security issues on system that
    // ignore file name cases
    private static final String sessionTokenHeader = "X-Session-Token";   //think about security issues on system
    // that ignore file name cases


    // private static long activationTimeout = defaultActivationTimeout;
// private static long passResetTimeout = defaultPassResetTimeout;
    private static long instanceId;
    private static AtomicInteger sequence = new AtomicInteger(1);

    private static ConfigurationManager configurationManager;

    private static ConfigBean conf = new ConfigBean();

    private static boolean configValid = false;


    public static boolean isConfigValid() {
        return configValid;
    }

    public static boolean isWebConfigEnabled() {
        return conf.isWebConfigEnabled();
    }

    public static void setConfigValid(boolean configValid) {
        BackendConfig.configValid = configValid;
    }

    public static long getInstanceId() {
        return instanceId;
    }

    public static void setInstanceId(long iid) {
        instanceId = System.currentTimeMillis() ^ iid;
    }


    public static int getSeqNumber() {
        return sequence.getAndIncrement();
    }

    public static ConfigBean createConfig() {
        return new ConfigBean();
    }

    public static ConfigBean getConfig() {
        return conf;
    }

    public static void setConfig(ConfigBean cfg) {
        conf = cfg;
    }


    public static Path getWorkDirectory() {
        return conf.getWorkDirectory();
    }

    public static void setWorkDirectory(String dir) {
        conf.setWorkDirectory(FileSystems.getDefault().getPath(dir));
    }

    public static ServiceManager getServiceManager() {
        return conf.getDefaultServiceManager();
    }

    public static void setServiceManager(ServiceManager serviceManager) {
        conf.setDefaultServiceManager(serviceManager);
    }


    public static void setEntityManagerFactory(EntityManagerFactory e) {
        conf.setEmf(e);
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return conf.getEmf();
    }


    public static String getDataMountPath() {
        return conf.getDataMountPath();
    }

    public static Path getUserGroupIndexPath() {
        return conf.getUserGroupIndexPath();
    }

    public static Path getUserGroupDropboxPath() {
        return conf.getUserGroupDropboxPath();
    }

    public static Path getUsersIndexPath() {
        return conf.getUsersIndexPath();
    }

    public static Path getGroupIndexPath() {
        return conf.getGroupsIndexPath();
    }

    public static String getUserDropboxRelPath(User user) {
        String udir = user.getSecret() + "-a" + user.getId();

        return udir.substring(0, 2) + "/" + udir.substring(2);
    }

    public static String getGroupDropboxRelPath(UserGroup ug) {
        String udir = ug.getSecret() + "-b" + ug.getId();

        return udir.substring(0, 2) + "/" + udir.substring(2);
    }


    public static Path getUserDirPath(User user) {
        return conf.getUserGroupDropboxPath().resolve(getUserDropboxRelPath(user));
    }

    public static Path getGroupDirPath(UserGroup g) {
        return conf.getUserGroupDropboxPath().resolve(getGroupDropboxRelPath(g));
    }

    public static Path getUserLoginLinkPath(User u) {
        String login = u.getLogin();

        if (login == null || login.length() == 0) {
            return null;
        }

        String firstCh = login.substring(0, 1);

        return getUsersIndexPath().resolve(AccNoUtil.encode(firstCh)).resolve(AccNoUtil.encode(login) + ".login");
    }

    public static Path getUserEmailLinkPath(User u) {
        String email = u.getEmail();

        if (email == null || email.length() == 0) {
            return null;
        }

        String firstCh = email.substring(0, 1);

        return getUsersIndexPath().resolve(AccNoUtil.encode(firstCh)).resolve(AccNoUtil.encode(email) + ".email");
    }

    public static Path getGroupLinkPath(UserGroup u) {
        String name = u.getName();

        if (name == null || name.length() == 0) {
            return null;
        }

        String firstCh = name.substring(0, 1);

        return getGroupIndexPath().resolve(AccNoUtil.encode(firstCh)).resolve(AccNoUtil.encode(name));
    }

    public static Path getSubmissionsPath() {
        return conf.getSubmissionsPath();
    }

    public static String getSubmissionRelativePath(Submission sbm) {
        return AccNoUtil.getPartitionedPath(sbm.getAccNo());
    }

    public static Path getSubmissionPath(Submission sbm) {
        return conf.getSubmissionsPath().resolve(getSubmissionRelativePath(sbm));
    }


    public static Path getSubmissionFilesPath(Submission sbm) {
        return getSubmissionPath(sbm).resolve(SubmissionFilesDir);
    }

    public static Path getSubmissionFilesUGPath(long groupID) {
        return Paths.get(groupID > 0 ? Long.toHexString(groupID) : "u");
    }

    public static Path getSubmissionPublicFTPPath(Submission sbm) {
        return getPublicFTPPath().resolve(getSubmissionRelativePath(sbm));
    }

    public static Path getSubmissionsHistoryPath() {
        return conf.getSubmissionsHistoryPath();
    }

    public static Path getSubmissionHistoryPath(Submission sbm) {
        return conf.getSubmissionsHistoryPath()
                .resolve(getSubmissionRelativePath(sbm) + SubmissionHistoryPostfix + Math.abs(sbm.getVersion()));
    }


    public static String getRecaptchaPublicKey() {
        return conf.getRecaptchaPublicKey();
    }

    public static String getRecaptchaPrivateKey() {
        return conf.getRecaptchaPrivateKey();
    }


    public static boolean isLinkingAllowed() {
        return conf.isFileLinkAllowed();
    }

    public static Path getPublicFTPPath() {
        return conf.getPublicFTPPath();
    }

    public static Path getSubmissionUpdatePath() {
        return conf.getSubmissionUpdatePath();
    }

    public static String getUpdateListenerURLPrefix() {
        return conf.getUpdateListenerURLPfx();
    }

    public static String getUIURL() {
        return conf.getUIURL();
    }

    public static String getUpdateListenerURLPostfix() {
        return conf.getUpdateListenerURLSfx();
    }

    public static void setPublicFTPPath(Path publicFTPPath) {
        conf.setPublicFTPPath(publicFTPPath);
    }

    public static String getDefaultSubmissionAccPrefix() {
        return conf.getDefaultSubmissionAccPrefix();
    }

    public static String getDefaultSubmissionAccSuffix() {
        return conf.getDefaultSubmissionAccSuffix();
    }

    public static int getUpdateWaitPeriod() {
        return conf.getUpdateWaitPeriod();
    }

    public static int getMaxUpdatesPerFile() {
        return conf.getMaxUpdatesPerFile();
    }

    public static String getFrontendUpdateFormat() {
        return conf.getFrontendUpdateFormat();
    }

    public static boolean isCreateFileStructure() {
        return conf.isCreateFileStructure();
    }

    public static Path getBaseDirectory() {
        return conf.getBaseDirectory();
    }

    public static TaskInfo getExportTask() {
        return conf.getExpTaskInfo();
    }

    public static void setExportTask(TaskInfo ti) {
        conf.setExpTaskInfo(ti);
    }

    public static void setDatabaseConfig(Map<String, Object> dbConfig) {
        conf.setDatabaseConfig(new HashMap<>(dbConfig));

    }

    public static Map<String, Object> getDatabaseConfig() {
        return conf.getDatabaseConfig();
    }

    public static String getActivationEmailSubject() {
        return conf.getActivationEmailSubject();
    }

    public static Resource getActivationEmailPlainTextFile() {
        return conf.getActivationEmailPlainTextFile();
    }

    public static Resource getActivationEmailHtmlFile() {
        return conf.getActivationEmailHtmlFile();
    }

    public static boolean isEnableUnsafeRequests() {
        return conf.isEnableUnsafeRequests();
    }

    public static boolean isMandatoryAccountActivation() {
        return conf.isMandatoryAccountActivation();
    }

    public static boolean isPublicDropboxes() {
        return conf.isPublicDropboxes();
    }

    public static boolean isSearchEnabled() {
        return conf.isSearchEnabled();
    }

    public static void setSearchEnabled(boolean searchEnabled) {
        conf.setSearchEnabled(searchEnabled);
    }

    public static long getActivationTimeout() {
        return conf.getActivationTimeout();
    }

    public static long getPasswordResetTimeout() {
        return conf.getPassResetTimeout();
    }

    public static Resource getPassResetEmailHtmlFile() {
        return conf.getPassResetEmailHtmlFile();
    }

    public static Resource getPassResetEmailPlainTextFile() {
        return conf.getPassResetEmailPlainTextFile();
    }

    public static String getPassResetEmailSubject() {
        return conf.getPassResetEmailSubject();
    }


    public static String getSessionCookieName() {
        return sessionCookieName;
    }

    public static String getSessionTokenHeader() {
        return sessionTokenHeader;
    }

    public static Resource getTagSubscriptionEmailHtmlFile() {
        return conf.getTagSubscriptionEmailHtmlFile();
    }

    public static Resource getTagSubscriptionEmailPlainTextFile() {
        return conf.getTagSubscriptionEmailPlainTextFile();
    }

    public static Resource getAttributeSubscriptionEmailHtmlFile() {
        return conf.getAttributeSubscriptionEmailHtmlFile();
    }

    public static Resource getAttributeSubscriptionEmailPlainTextFile() {
        return conf.getAttributeSubscriptionEmailPlainTextFile();
    }

    public static String getSubscriptionEmailSubject() {
        return conf.getSubscriptionEmailSubject();
    }

    public static Path getSubmissionsTransactionPath() {
        return conf.getSubmissionsTransactionPath();
    }

    public static boolean isEncodeFileNames() {
        return EncodeFileNames;
    }

    public static long getExportLockTimeoutMsec() {
        return exportLockTimeout;
    }

    public static long getExportLockDelayMsec() {
        return exportLockDelay;
    }

    public static Map<String, Object> getEmailConfig() {
        return conf.getEmailConfig();
    }

    public static TaskConfig getTaskConfig() {
        return conf.getTaskConfig();
    }

    public static void setEmailConfig(Map<String, Object> emailConfig) {
        conf.setEmailConfig(emailConfig);
    }

    public static void setTaskConfig(TaskConfig taskConfig) {
        conf.setTaskConfig(taskConfig);
    }

    public static ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public static void setConfigurationManager(ConfigurationManager configurationManager) {
        BackendConfig.configurationManager = configurationManager;
    }

    public static Timer getTimer() {
        return conf.getTimer();
    }

    public static void setTimer(Timer timer) {
        conf.setTimer(timer);
    }

    // SSO support
    public static String getSSOPublicCertificatePemURL() {
        return conf.getSSOPublicCertificatePemURL();
    }

    public static String getSSOPublicCertificateDerURL() {
        return conf.getSSOPublicCertificateDerURL();
    }

    public static String getSSOAuthURL() {
        return conf.getSSOAuthURL();
    }

}
