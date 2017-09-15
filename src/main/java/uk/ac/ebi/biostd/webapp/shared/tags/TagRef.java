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

package uk.ac.ebi.biostd.webapp.shared.tags;

import java.io.Serializable;

public class TagRef implements Serializable, Comparable<TagRef> {

    private static final long serialVersionUID = 1L;

    private String classiferName;
    private String tagName;
    private String tagValue;

    public TagRef() {
    }

    public TagRef(String classiferName, String tagName) {
        this.classiferName = classiferName;
        this.tagName = tagName;
    }

    public String getClassiferName() {
        return classiferName;
    }

    public void setClassiferName(String classiferName) {
        this.classiferName = classiferName;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }

    @Override
    public int compareTo(TagRef o) {
        int dif = getClassiferName().compareTo(o.getClassiferName());

        if (dif != 0) {
            return dif;
        }

        return getTagName().compareTo(o.getTagName());
    }
}
