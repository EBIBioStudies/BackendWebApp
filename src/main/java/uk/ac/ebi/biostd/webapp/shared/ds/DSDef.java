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

import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.RestDataSource;
import java.util.ArrayList;
import java.util.List;

public class DSDef {

    private DSField keyField;
    private List<DSField> fields = new ArrayList<DSField>(10);


    public void addField(DSField fld) {
        fields.add(fld);

        if (fld.isPrimaryKey()) {
            keyField = fld;
        }
    }

    public DSField getKeyField() {
        return keyField;
    }

    public List<DSField> getFields() {
        return fields;
    }

    public void setFields(List<DSField> fields) {
        this.fields = fields;

        for (DSField dsf : fields) {
            if (dsf.isPrimaryKey()) {
                keyField = dsf;
                break;
            }
        }
    }

    public DataSource createDataSource() {
        RestDataSource ds = new RestDataSource();

        ds.setAutoCacheAllData(false);
        ds.setCacheAllData(false);

        DataSourceField fArr[] = new DataSourceField[fields.size()];

        int i = 0;
        for (DSField f : fields) {
            DataSourceField dsf = null;

            if (f.getWidth() > 0) {
                dsf = new DataSourceField(f.getFieldId(), f.getType(), f.getFieldTitle(), f.getWidth());
            } else {
                dsf = new DataSourceField(f.getFieldId(), f.getType(), f.getFieldTitle());
            }

            dsf.setPrimaryKey(f.isPrimaryKey());
            dsf.setCanEdit(f.isEditable());
            dsf.setHidden(f.isHidden());

            fArr[i++] = dsf;
        }

        ds.setFields(fArr);

        return ds;
    }
}
