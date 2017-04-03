/**

Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author Mikhail Gostev <gostev@gmail.com>

**/

package uk.ac.ebi.biostd.webapp.client.ui.admin;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

public class RootWidget extends HLayout
{
 
 private LoginPanel loginPnl;
 private Canvas appCanvas;
 private VLayout loginPane;
 
 public RootWidget( Canvas apc )
 {

  appCanvas = apc;
  
  loginPane = new VLayout();
  loginPane.setWidth100();
  loginPane.setHeight100();
  
  loginPane.setDefaultLayoutAlign(Alignment.CENTER);

  loginPnl = new LoginPanel();
  loginPnl.setAlign(Alignment.CENTER);
  
  loginPane.addMember(loginPnl);
  
  appCanvas.hide();
  
  addMember(loginPane);
  addMember(appCanvas);
  
  setWidth100();
  setHeight100();
  
 }
 
 public void askForLogin()
 {
  loginPnl.reset();
  appCanvas.hide();
  loginPane.show();
 }

 public void showApplication()
 {
  appCanvas.show();
  loginPane.hide();
 }
 
}
