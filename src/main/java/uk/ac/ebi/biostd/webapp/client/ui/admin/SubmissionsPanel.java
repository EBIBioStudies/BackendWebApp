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

package uk.ac.ebi.biostd.webapp.client.ui.admin;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;

public class SubmissionsPanel extends HLayout {

    private Layout resPanel;

    public SubmissionsPanel() {
        VLayout vl = new VLayout();

        vl.setHeight100();
        vl.setWidth("50%");
        vl.setDefaultLayoutAlign(Alignment.CENTER);

        resPanel = new VLayout();
        resPanel.setHeight100();
        resPanel.setWidth("50%");
        resPanel.setBorder("1px solid black");

        addMember(vl);
        addMember(resPanel);

    }
}

