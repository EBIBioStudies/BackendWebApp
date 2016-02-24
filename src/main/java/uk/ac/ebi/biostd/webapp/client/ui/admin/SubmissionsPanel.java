package uk.ac.ebi.biostd.webapp.client.ui.admin;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;

public class SubmissionsPanel extends HLayout
{
 private Layout resPanel;
 
 public SubmissionsPanel()
 {
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

