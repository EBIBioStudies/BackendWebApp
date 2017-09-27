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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RowSpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.client.ClientConfig;

public class LoginPanel extends VLayout {

    private StaticTextItem result;

    public LoginPanel() {
        setWidth("1%");
        setHeight("1%");

        final DynamicForm form = new DynamicForm();

        form.setGroupTitle("User login");
        form.setIsGroup(true);
        form.setWidth(300);
        form.setHeight(180);
        form.setNumCols(2);
        form.setColWidths(60, "*");
        //form.setBorder("1px solid blue");
        form.setPadding(5);

        final TextItem subjectItem = new TextItem();
        subjectItem.setName("login");
        subjectItem.setTitle("Login");
        subjectItem.setWidth("*");

        final PasswordItem passItem = new PasswordItem();
        passItem.setName("pass");
        passItem.setTitle("Password");
        passItem.setWidth("*");

        result = new StaticTextItem();
        result.setColSpan(2);
        result.setShowTitle(false);

        ButtonItem chkinBtn = new ButtonItem("signin", "Signin");
        chkinBtn.setColSpan(2);
        chkinBtn.setAlign(Alignment.RIGHT);

        chkinBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                if (subjectItem.getValue() == null || passItem.getValue() == null) {
                    result.setValue("Please enter login and password");
                    return;
                }

                ClientConfig.getService().login(subjectItem.getValue().toString(), passItem.getValue().toString(),
                        new AsyncCallback<User>() {

                            @Override
                            public void onFailure(Throwable arg0) {
                                result.setValue("Server call error: " + arg0.getMessage());
                            }

                            @Override
                            public void onSuccess(User usr) {
                                if (usr == null) {
                                    result.setValue("Login failed. Please try again");
                                } else {
                                    result.setValue("User " + usr.getFullName() + " signed in");
                                    ClientConfig.getLoginManager().userLoggedIn(usr);
                                }
                            }
                        });
            }
        });

        RowSpacerItem sps = new RowSpacerItem();
        sps.setHeight(50);

        form.setFields(subjectItem, passItem, sps, result, chkinBtn);

        addMember(form);
    }

    public void reset() {
        result.setValue("");
    }


}
