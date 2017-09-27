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

package uk.ac.ebi.biostd.webapp.client;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.client.ui.admin.RootWidget;
import uk.ac.ebi.biostd.webapp.client.ui.mng.LoginManager;

public class ClientConfig {

    private static BioStdServiceAsync service;
    private static LoginManager loginManager;
    private static RootWidget rootWidget;
    private static User loggedUser;

    public static BioStdServiceAsync getService() {
        return service;
    }

    public static void setService(BioStdServiceAsync svc) {
        service = svc;
    }

    public static LoginManager getLoginManager() {
        return loginManager;
    }

    public static void setLoginManager(LoginManager loginManager) {
        ClientConfig.loginManager = loginManager;
    }

    public static RootWidget getRootWidget() {
        return rootWidget;
    }

    public static void setRootWidget(RootWidget rootWidget) {
        ClientConfig.rootWidget = rootWidget;
    }

    public static User getLoggedUser() {
        return loggedUser;
    }

    public static void setLoggedUser(User loggedUser) {
        ClientConfig.loggedUser = loggedUser;
    }

}
