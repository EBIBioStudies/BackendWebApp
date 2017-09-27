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

package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.ebi.biostd.in.pagetab.SubmissionInfo;
import uk.ac.ebi.biostd.model.Submission;

public class AccNoMatcher {

    public static Match match(SubmissionInfo si, Submission host) {
        String pat = Submission.getNodeAccNoPattern(host);

        if (pat == null) {
            return Match.NOINFO;
        }

        Matcher mtch = Pattern.compile(pat).matcher("");

        if (si.getAccNoPrefix() == null && si.getAccNoSuffix() == null) {
            mtch.reset(si.getAccNoOriginal());
        } else {
            mtch.reset(
                    (si.getAccNoPrefix() == null ? "" : si.getAccNoPrefix()) + "000" + (si.getAccNoSuffix() == null ? ""
                            : si.getAccNoSuffix()));
        }

        return mtch.matches() ? Match.YES : Match.NO;
    }

    public enum Match {
        YES,
        NO,
        NOINFO
    }
}
