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

package uk.ac.ebi.biostd.webapp.shared.ds;

import com.smartgwt.client.types.FieldType;

public class DSField {

    public static final String fieldIdPrefix = "##";
    private String fieldId;
    private String fieldTitle;
    private FieldType type;
    private int width = -1;
    private boolean primaryKey = false;
    private boolean editable = false;
    private boolean hidden = false;

    public DSField(String string, FieldType t, String ttl, int w) {

        if (string.startsWith(fieldIdPrefix)) {
            fieldId = string;
        } else {
            fieldId = fieldIdPrefix + string;
        }

        type = t;
        fieldTitle = ttl;
        width = w;
    }

    public DSField(String string, FieldType text, String string2) {
        this(string, text, string2, -1);
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fId) {
        if (fId.startsWith(fieldIdPrefix)) {
            fieldId = fId;
        } else {
            fieldId = fieldIdPrefix + fId;
        }
    }

    public String getFieldTitle() {
        return fieldTitle;
    }

    public void setFieldTitle(String fieldTitle) {
        this.fieldTitle = fieldTitle;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DSField && ((DSField) o).getFieldId().equals(getFieldId())) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return fieldId.hashCode();
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }


}
