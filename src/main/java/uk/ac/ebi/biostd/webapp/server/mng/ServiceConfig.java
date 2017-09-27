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

package uk.ac.ebi.biostd.webapp.server.mng;


public class ServiceConfig {

    public static final String SessionDir = "sessions";

    public static final String DatabaseParameter = "database";
    public static final String WorkdirParameter = "workdir";

    private String serviceName;


    private String databaseProfile;
    private String workDirectory;


    public ServiceConfig(String svcName) {
        serviceName = svcName;
    }

    public String getServiceName() {
        return serviceName;
    }


    public boolean readParameter(String param, String val) throws ServiceConfigException {
        if (DatabaseParameter.equals(param)) {
            databaseProfile = val;
            return true;
        }

        if (WorkdirParameter.equals(param)) {
            workDirectory = val;
            return true;
        }
        return false;
    }

    public String getDatabaseProfile() {
        return databaseProfile;
    }


    public String getWorkDirectory() {
        return workDirectory;
    }

}
