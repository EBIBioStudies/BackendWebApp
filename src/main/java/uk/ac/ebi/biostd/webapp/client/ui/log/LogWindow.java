package uk.ac.ebi.biostd.webapp.client.ui.log;



import uk.ac.ebi.biostd.treelog.LogNode;

import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;

public class LogWindow extends Window
{
 public LogWindow( String title, LogNode rLn )
 {
  super();
  
  setWidth(1000);
  setHeight(600);
  centerInPage();
  setCanDragReposition(true);
  setCanDragResize(true);
  setKeepInParentRect(true);
  setTitle(title);
  
  final LogTree lgTree = new LogTree(rLn);
  
  addCloseClickHandler( new CloseClickHandler()
  {
   @Override
   public void onCloseClick(CloseClickEvent event)
   {
    destroy();
   }
  });
 
  addItem( lgTree );

 }
}
