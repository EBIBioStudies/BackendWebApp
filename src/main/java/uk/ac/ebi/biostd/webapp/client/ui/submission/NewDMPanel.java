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

package uk.ac.ebi.biostd.webapp.client.ui.submission;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

public class NewDMPanel extends CaptionPanel {

    private final TextArea dsc;
    private final FileUpload upload;
    private final Hidden statusInput = new Hidden();
    private final RemoveListener remListener;
    private int order;

    NewDMPanel(int n, RemoveListener rml) {
        remListener = rml;
        order = n;

        //setWidth("*");
        setWidth("auto");
        setCaptionText("Data Module: " + n);

        FlexTable layout = new FlexTable();
        layout.setWidth("100%");
        FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

        layout.setWidget(0, 0, new Label("Description:"));

        dsc = new TextArea();
        dsc.setName(SubmissionConstants.MODULE_DESCRIPTION + n);
        dsc.setWidth("97%");

        cellFormatter.setColSpan(1, 0, 2);
        layout.setWidget(1, 0, dsc);

        cellFormatter.setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_TOP);
        cellFormatter.setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_RIGHT);

        HTML clsBt = new HTML("<img src='admin_images/icons/delete.png'>");
        clsBt.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent arg0) {
                removeFromParent();

                if (remListener != null) {
                    remListener.removed(NewDMPanel.this);
                }
            }
        });

        layout.setWidget(0, 1, clsBt);

        cellFormatter.setColSpan(2, 0, 2);

        upload = new FileUpload();
        upload.setName(SubmissionConstants.MODULE_FILE + n);
        upload.getElement().setAttribute("size", "80");
        layout.setWidget(2, 0, upload);

        statusInput.setName(SubmissionConstants.MODULE_STATUS + n);
        statusInput.setValue(SubmissionStatus.NEW.name());

        layout.setWidget(4, 0, statusInput);

        add(layout);
    }

    public String getDescription() {
        return dsc.getText();
    }

    public String getFile() {
        return upload.getFilename();
    }

    public void setOrder(int ndm) {
        order = ndm;
        setCaptionText("Data Module: " + order);
        dsc.setName(SubmissionConstants.MODULE_DESCRIPTION + ndm);
        upload.setName(SubmissionConstants.MODULE_FILE + ndm);
        statusInput.setName(SubmissionConstants.MODULE_STATUS + ndm);
    }

    public interface RemoveListener {

        void removed(Widget w);
    }
}

