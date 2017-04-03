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

import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

public class AdminPanel extends TabSet
{

 public AdminPanel()
 {
  final TabSet mainTs = this;
  
  mainTs.setWidth100();
  mainTs.setHeight100();
  
  Tab submissionsTab = new Tab("Submissions");
  
  submissionsTab.setPane( new SubmissionsPanel() );
  
  Tab submTb = new Tab("Submit");
  
  submTb.setPane(new SubmitPanel());
  
  mainTs.addTab(submissionsTab);
  mainTs.addTab(submTb);
 }
 
}
