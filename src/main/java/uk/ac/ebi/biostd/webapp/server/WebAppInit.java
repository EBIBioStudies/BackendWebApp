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

package uk.ac.ebi.biostd.webapp.server;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.config.ConfigurationException;
import uk.ac.ebi.biostd.webapp.server.config.ConfigurationManager;
import uk.ac.ebi.biostd.webapp.server.util.ServletContextParamPool;


/**
 * Application Lifecycle Listener implementation class WebAppInit
 */

public class WebAppInit implements ServletContextListener {
    private Logger log = null;


    public WebAppInit() {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }
    }

    /**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        if (BackendConfig.isConfigValid()) {
            BackendConfig.getConfigurationManager().stopServices();
        }

        if ("true".equals(BackendConfig.getDatabaseConfig().get(ConfigurationManager.IsEmbeddedH2Parameter))) {

            Connection conn;
            try {
                conn = DriverManager.getConnection(
                        BackendConfig.getDatabaseConfig().get(ConfigurationManager.HibernateDBConnectionURLParameter)
                                .toString(), "", "");

                Statement stat = conn.createStatement();
                stat.execute("SHUTDOWN");
                stat.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        try {
            Driver h2Drv = DriverManager.getDriver("jdbc:h2:xxx");
            if (h2Drv != null) {
                DriverManager.deregisterDriver(h2Drv);
            }
        } catch (SQLException e) {
        }

    }


    @Override
    public void contextInitialized(ServletContextEvent ctxEv) {
        try {
            contextInitializedUnsafe(ctxEv);
        } catch (ConfigurationException ec) {
            log.error("Configuration is not valid: " + ec.getMessage());
            BackendConfig.setConfigValid(false);
        } catch (Throwable e) {
            log.error("Configuration is not valid: " + e.getMessage(), e);
            BackendConfig.setConfigValid(false);
        }
    }

    public void contextInitializedUnsafe(ServletContextEvent ctxEv) throws ConfigurationException {
        ServletContext ctx = ctxEv.getServletContext();
        BackendConfig.setInstanceId(ctx.getContextPath().hashCode());
        BackendConfig.setConfigurationManager(new ConfigurationManager(new ServletContextParamPool(ctx)));
        BackendConfig.getConfigurationManager().loadConfiguration();
        BackendConfig.setConfigValid(true);
    }


}
