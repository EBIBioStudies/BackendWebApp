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

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;

public interface AccessionManager {

    String getNextAccNo(String prefix, String suffix, User usr) throws SecurityException, ServiceException;

    long incrementIdGen(String prefix, String suffix, int num, User usr) throws SecurityException, ServiceException;
}
