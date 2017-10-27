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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.lucene.queryparser.classic.ParseException;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.SubmissionReport;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.biostd.webapp.shared.tags.TagRef;

public interface SubmissionManager {

    Collection<Submission> getSubmissionsByOwner(User u, int offset, int limit);

    Submission getSubmissionsByAccession(String acc);

    Collection<Submission> searchSubmissions(User user, SubmissionSearchRequest ssr) throws ParseException;

    SubmissionReport createSubmission(byte[] data, DataFormat fmt, String charset, Operation op, User usr,
            boolean validateOnly, boolean ignoreAbsFiles);

    LogNode updateSubmissionMeta(String sbmAcc, Collection<TagRef> tags, Set<String> access, long rTime, User user);

    LogNode deleteSubmissionByAccession(String acc, boolean toHistory, User usr);

    LogNode tranklucateSubmissionById(int id, User user);

    LogNode tranklucateSubmissionByAccession(String sbmAcc, User user);

    LogNode tranklucateSubmissionByAccessionPattern(String accPfx, User usr);

    LogNode changeOwnerByAccession(String sbmAcc, String owner, User usr);

    LogNode changeOwnerByAccessionPattern(String sbmAcc, String owner, User usr);

    List<Submission> getHostSubmissionsByType(String type, User user);

    void shutdown();

    enum Operation {
        CREATE,
        CREATEUPDATE,
        UPDATE,
        OVERRIDE,
        CREATEOVERRIDE,
        DELETE,
        REMOVE,
        TRANKLUCATE,
        SETMETA,
        CHOWN
    }
}
