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
