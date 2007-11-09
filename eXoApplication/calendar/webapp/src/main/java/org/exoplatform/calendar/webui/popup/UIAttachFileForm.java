/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.calendar.service.Attachment;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormUploadInput;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * Aug 24, 2007  
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIAttachFileForm.SaveActionListener.class), 
      @EventConfig(listeners = UIAttachFileForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)

public class UIAttachFileForm extends UIForm implements UIPopupComponent {

  final static public String FIELD_UPLOAD = "upload" ;  
  private int maxField = 5 ;

  public UIAttachFileForm() throws Exception {
    setMultiPart(true) ;
    int i = 0 ;
    while(i++ < maxField) {
      UIFormUploadInput uiInput = new UIFormUploadInput(FIELD_UPLOAD + String.valueOf(i), FIELD_UPLOAD + String.valueOf(i)) ;
      addUIFormInput(uiInput) ;
    }
  }


  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  static  public class SaveActionListener extends EventListener<UIAttachFileForm> {
    public void execute(Event<UIAttachFileForm> event) throws Exception {
      UIAttachFileForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      List<Attachment> files = new ArrayList<Attachment>() ;
      int i = 0 ;
      while(i++ < uiForm.maxField) {
        UIFormUploadInput input = (UIFormUploadInput)uiForm.getUIInput(FIELD_UPLOAD + String.valueOf(i));
        UploadResource uploadResource = input.getUploadResource() ;
        if(uploadResource != null) {
          Attachment attachfile = new Attachment() ;
          attachfile.setName(uploadResource.getFileName()) ;
          attachfile.setInputStream(input.getUploadDataAsStream()) ;
          attachfile.setMimeType(uploadResource.getMimeType()) ;
          attachfile.setSize((long)uploadResource.getUploadedSize());
          files.add(attachfile) ;
        }
      }
      if(files.isEmpty()){
        uiApp.addMessage(new ApplicationMessage("UIAttachFileForm.msg.fileName-error", null, 
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      } else {
        UIPopupContainer uiPopupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
        UIEventForm uiEventForm = uiPopupContainer.getChild(UIEventForm.class) ;
        UITaskForm uiTaskForm = uiPopupContainer.getChild(UITaskForm.class) ;
        if(uiEventForm != null) {
          UIEventDetailTab uiEventDetailTab = uiEventForm.getChild(UIEventDetailTab.class) ;
          for(Attachment file :  files){
            uiEventDetailTab.addToUploadFileList(file) ;
            //uiEventDetailTab.setAttachments(files) ;
          }
          uiEventDetailTab.refreshUploadFileList() ;
        } else if(uiTaskForm != null) {
          UITaskDetailTab uiTaskDetailTab = uiTaskForm.getChild(UITaskDetailTab.class) ;
          for(Attachment file :  files){
            uiTaskDetailTab.addToUploadFileList(file) ;  
            //uiTaskDetailTab.setAttachments(files) ;
          }
          uiTaskDetailTab.refreshUploadFileList() ;
        }
        UIPopupAction uiPopupAction = uiPopupContainer.getChild(UIPopupAction.class) ;
        /*UploadService uploadService = uiForm.getApplicationComponent(UploadService.class) ;
        uploadService.removeUpload(input.getUploadId()) ;*/
        uiPopupAction.deActivate() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction.getAncestorOfType(UIPopupAction.class)) ;
      }
      /*if(uploadResource == null) {
        uiApp.addMessage(new ApplicationMessage("UIAttachFileForm.msg.fileName-error", null, 
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      String fileName = uploadResource.getFileName() ;
      if(fileName == null || fileName.equals("")) {
        uiApp.addMessage(new ApplicationMessage("UIAttachFileForm.msg.fileName-error", null, 
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }*/
      /*UIPopupContainer uiPopupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
      UIEventForm uiEventForm = uiPopupContainer.getChild(UIEventForm.class) ;
      UITaskForm uiTaskForm = uiPopupContainer.getChild(UITaskForm.class) ;
      UIPopupAction uiPopupAction = uiPopupContainer.getChild(UIPopupAction.class) ;
      try {
        Attachment attachfile = new Attachment() ;
        attachfile.setName(uploadResource.getFileName()) ;
        attachfile.setInputStream(input.getUploadDataAsStream()) ;
        attachfile.setMimeType(uploadResource.getMimeType()) ;
        attachfile.setSize((long)uploadResource.getUploadedSize());
        if(uiEventForm != null) {
          UIEventDetailTab uiEventDetailTab = uiEventForm.getChild(UIEventDetailTab.class) ;
          uiEventDetailTab.addToUploadFileList(attachfile) ;
          uiEventDetailTab.refreshUploadFileList() ;
        } else if(uiTaskForm != null) {
          UITaskDetailTab uiTaskDetailTab = uiTaskForm.getChild(UITaskDetailTab.class) ;
          uiTaskDetailTab.addToUploadFileList(attachfile) ;
          uiTaskDetailTab.refreshUploadFileList() ;
        }
        UploadService uploadService = uiForm.getApplicationComponent(UploadService.class) ;
        uploadService.removeUpload(input.getUploadId()) ;
        uiPopupAction.deActivate() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction.getAncestorOfType(UIPopupAction.class)) ;
      } catch(Exception e) {
        uiApp.addMessage(new ApplicationMessage("UIAttachFileForm.msg.upload-error", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        e.printStackTrace() ;
        return ;
      }*/
    }
  }

  static  public class CancelActionListener extends EventListener<UIAttachFileForm> {
    public void execute(Event<UIAttachFileForm> event) throws Exception {
      UIAttachFileForm uiFileForm = event.getSource() ;
      UIPopupAction uiPopupAction = uiFileForm.getAncestorOfType(UIPopupAction.class) ;
      uiPopupAction.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction.getAncestorOfType(UIPopupAction.class)) ;
    }
  }
}
