package uk.ac.ebi.biostd.webapp.client;

import com.google.gwt.core.client.EntryPoint;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.RestDataSource;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class BioStdWebApp implements EntryPoint
{

 @Override
 public void onModuleLoad()
 {
  TabSet mainTs = new TabSet();
  
  mainTs.setWidth100();
  mainTs.setHeight100();
  
  Tab submTb = new Tab("Submit");
  
  VLayout vl = new VLayout();
  
  DataSource ds = new RestDataSource();
  
  ds.addField( new DataSourceField("file", FieldType.BINARY, "File") );
  
  ds.setID("ptSubmit");
  ds.setDataFormat(DSDataFormat.JSON);
  ds.setDataURL("upload");
  ds.setDataProtocol(DSProtocol.POSTPARAMS);
  
  final DynamicForm sumbForm = new DynamicForm();
  
  sumbForm.setDataSource(ds);
  
  submTb.setPane(vl);
  
  vl.addMember(sumbForm);
  
  IButton btn = new IButton("Submit");
  
  btn.addClickHandler( new ClickHandler()
  {
   
   @Override
   public void onClick(ClickEvent event)
   {
    sumbForm.saveData();
   }
  });

  vl.addMember(btn);
  
  mainTs.addTab(submTb);
  
  mainTs.draw();
 }
}
