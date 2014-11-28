package uk.ac.ebi.biostd.webapp.client.ui.admin;

import org.moxieapps.gwt.uploader.client.File;
import org.moxieapps.gwt.uploader.client.Uploader;
import org.moxieapps.gwt.uploader.client.events.FileDialogCompleteEvent;
import org.moxieapps.gwt.uploader.client.events.FileDialogCompleteHandler;
import org.moxieapps.gwt.uploader.client.events.FileDialogStartEvent;
import org.moxieapps.gwt.uploader.client.events.FileDialogStartHandler;
import org.moxieapps.gwt.uploader.client.events.FileQueueErrorEvent;
import org.moxieapps.gwt.uploader.client.events.FileQueueErrorHandler;
import org.moxieapps.gwt.uploader.client.events.FileQueuedEvent;
import org.moxieapps.gwt.uploader.client.events.FileQueuedHandler;
import org.moxieapps.gwt.uploader.client.events.UploadCompleteEvent;
import org.moxieapps.gwt.uploader.client.events.UploadCompleteHandler;
import org.moxieapps.gwt.uploader.client.events.UploadErrorEvent;
import org.moxieapps.gwt.uploader.client.events.UploadErrorHandler;
import org.moxieapps.gwt.uploader.client.events.UploadProgressEvent;
import org.moxieapps.gwt.uploader.client.events.UploadProgressHandler;
import org.moxieapps.gwt.uploader.client.events.UploadSuccessEvent;
import org.moxieapps.gwt.uploader.client.events.UploadSuccessHandler;

import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.webapp.client.ClientConfig;
import uk.ac.ebi.biostd.webapp.client.ui.log.LogTree3;
import uk.ac.ebi.biostd.webapp.client.ui.log.ROJSLogNode;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Progressbar;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;

public class SubmitPanel extends HLayout
{
 private Layout resPanel;
 
 public SubmitPanel()
 {
  VLayout vl = new VLayout();
  
  vl.setHeight100();
  vl.setWidth("50%");
  vl.setDefaultLayoutAlign(Alignment.CENTER);
  
  
  final ProgressPanel progressPanel = new ProgressPanel();
  progressPanel.setAutoHeight();
  
  final Uploader uploader = new Uploader();
  uploader
  .setUploadURL("upload")
  .setButtonText("Upload File")
  .setButtonWidth(133)
  .setButtonHeight(22)
  .setButtonTextStyle("border: 1px solid black")
  .setFileSizeLimit("50 MB")
  .setButtonCursor(Uploader.Cursor.HAND)
  .setButtonAction(Uploader.ButtonAction.SELECT_FILE)
  .setFileQueuedHandler(new FileQueuedHandler()
    {
     @Override
     public boolean onFileQueued(final FileQueuedEvent fileQueuedEvent)
     {

      progressPanel.setFileInfo(fileQueuedEvent.getFile());

      return true;
     }
    })
   .setUploadProgressHandler(new UploadProgressHandler()
    {
     @Override
     public boolean onUploadProgress(UploadProgressEvent uploadProgressEvent)
     {

      progressPanel.updateProgress((int) (uploadProgressEvent.getBytesComplete() * 100 / uploadProgressEvent.getBytesTotal()));

      return true;
     }
    })
   .setUploadCompleteHandler(new UploadCompleteHandler()
    {
     @Override
     public boolean onUploadComplete(UploadCompleteEvent uploadCompleteEvent)
     {

      return true;
     }
    })
   .setUploadSuccessHandler(new UploadSuccessHandler()
    {
     @Override
     public boolean onUploadSuccess(UploadSuccessEvent uploadSuccessEvent)
     {
      progressPanel.successState();
      
      LogNode rLn = ROJSLogNode.convert(uploadSuccessEvent.getServerData());
      
      final LogTree3 lgTree = new LogTree3(rLn);
      
      for( Canvas cv : resPanel.getMembers() )
      {
       cv.removeFromParent();
       cv.destroy();
      }
      
      resPanel.addMember(lgTree);
      
      return true;
     }
    })
   .setFileDialogStartHandler(new FileDialogStartHandler()
    {
     @Override
     public boolean onFileDialogStartEvent(FileDialogStartEvent fileDialogStartEvent)
     {
      progressPanel.resetState();
      return true;
     }
    })
   .setFileDialogCompleteHandler(new FileDialogCompleteHandler()
    {
     @Override
     public boolean onFileDialogComplete(FileDialogCompleteEvent fileDialogCompleteEvent)
     {

      progressPanel.initState();
      uploader.startUpload();

      return true;
     }
    })
   .setFileQueueErrorHandler(new FileQueueErrorHandler()
    {
     @Override
     public boolean onFileQueueError(FileQueueErrorEvent fileQueueErrorEvent)
     {
      progressPanel.errorState("Upload of file " + fileQueueErrorEvent.getFile().getName() + " failed due to ["
        + fileQueueErrorEvent.getErrorCode().toString() + "]: " + fileQueueErrorEvent.getMessage());
      return true;
     }
    })
   .setUploadErrorHandler(new UploadErrorHandler()
    {
     @Override
     public boolean onUploadError(UploadErrorEvent uploadErrorEvent)
     {
      if( uploadErrorEvent.getMessage().endsWith(" 403") )
      {
       progressPanel.errorState("User not logged in");
       
       ClientConfig.getLoginManager().askForLogin();
       
       return true;
      }

      progressPanel.errorState("Upload of file " + uploadErrorEvent.getFile().getName() + " failed due to [" + uploadErrorEvent.getErrorCode().toString()
        + "]: " + uploadErrorEvent.getMessage());
      return true;
     }
    });
  
  uploader.setHeight("50px");
  vl.addMember(uploader);
  vl.addMember(progressPanel);

  
  resPanel = new VLayout();
  resPanel.setHeight100();
  resPanel.setWidth("50%");
  resPanel.setBorder("1px solid black");

  addMember(vl);
  addMember(resPanel);
  
 }
}

class ProgressPanel extends VLayout implements ClickHandler
{
 static interface CancelHandler
 {
  void onCancel();
 }
 
 private HLayout progBarPanel;
 private Progressbar progressBar;
 private IButton cancelButton;
 private Label fileLabel;
 private Label msgLabel;
 
 private CancelHandler cancelHndlr;
 
 public ProgressPanel()
 {
  fileLabel = new Label();
  fileLabel.setHeight(30);
  addMember(fileLabel);

  progBarPanel = new HLayout();
  addMember(progBarPanel);
  
  msgLabel = new Label();
  msgLabel.setHeight(30);
  addMember(msgLabel);
 }
 
 public void setFileInfo(File file)
 {
  fileLabel.setContents("File: "+file.getName()+" "+(file.getSize()/1024+1)+"K");
 }

 public void resetState()
 {
  if( progressBar != null )
  {
   progBarPanel.removeMember(progressBar);
   progressBar = null;
  }
  
  if( cancelButton != null )
  {
   progBarPanel.removeMember(cancelButton);
   cancelButton = null;
  }
  
  msgLabel.setContents("");
  fileLabel.setContents("");
 }
 
 public void initState()
 {
  if( progressBar == null )
  {
   progressBar = new Progressbar();
   progressBar.setHeight(26);
   progressBar.setWidth(300);
   progBarPanel.addMember(progressBar);
  }
  
  progressBar.setPercentDone(0);
  
  if( cancelButton == null )
   progBarPanel.addMember(cancelButton = new IButton("Cancel", this));
 }

 public void updateProgress( int prg )
 {
  if( progressBar != null )
   progressBar.setPercentDone(prg);
 }
 
 public void successState()
 {
  if( progressBar != null )
   progressBar.setPercentDone(100);
  
  if( cancelButton != null )
  {
   progBarPanel.removeMember(cancelButton);
   cancelButton = null;
  }

  msgLabel.setContents("Upload OK");
 }
 
 public void errorState(String msg)
 {
  if( progressBar != null )
   progressBar.setPercentDone(0);
  
  if( cancelButton != null )
  {
   progBarPanel.removeMember(cancelButton);
   cancelButton = null;
  }

  msgLabel.setContents("Upload Error: "+msg);

 }
 
 @Override
 public void onClick(ClickEvent event)
 {
  if( cancelHndlr != null )
   cancelHndlr.onCancel();  
 }

 public void setCancelHandler(CancelHandler cancelHndlr)
 {
  this.cancelHndlr = cancelHndlr;
 }
}