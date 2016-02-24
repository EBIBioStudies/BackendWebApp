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
