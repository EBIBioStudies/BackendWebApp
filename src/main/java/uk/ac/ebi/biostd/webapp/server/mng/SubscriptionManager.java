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
 * @author Andrew Tikhonov <andrew.tikhonov@gmail.com>
 **/

package uk.ac.ebi.biostd.webapp.server.mng;

import java.util.Collection;
import uk.ac.ebi.biostd.authz.AttributeSubscription;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

/**
 * Created by andrew on 04/05/2017.
 */

public interface SubscriptionManager {

    void addAttributeSubscription(String attributeName, String textPattern, User user) throws ServiceException;

    void deleteAttributeSubscription(long subscriptionId) throws ServiceException;

    Collection<AttributeSubscription> listAttributeSubscriptions(User user) throws ServiceException;

    void triggerAttributeEventProcessors() throws ServiceException;

    void triggerTagEventProcessors() throws ServiceException;
}
