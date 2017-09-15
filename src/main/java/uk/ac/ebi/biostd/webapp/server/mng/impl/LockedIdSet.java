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

import java.util.Map;
import uk.ac.ebi.biostd.in.ElementPointer;

public class LockedIdSet {

    private Map<String, ElementPointer> submissionMap;
    private Map<String, ElementPointer> sectionMap;
    private int waitCount = 0;

    public Map<String, ElementPointer> getSubmissionMap() {
        return submissionMap;
    }

    public void setSubmissionMap(Map<String, ElementPointer> submissionMap) {
        this.submissionMap = submissionMap;
    }

    public Map<String, ElementPointer> getSectionMap() {
        return sectionMap;
    }

    public void setSectionMap(Map<String, ElementPointer> sectionMap) {
        this.sectionMap = sectionMap;
    }

    public boolean empty() {
        return (submissionMap == null || submissionMap.size() == 0) && (sectionMap == null || sectionMap.size() == 0);
    }

    public int getWaitCount() {
        return waitCount;
    }

    public void incWaitCount() {
        waitCount++;
    }
}
