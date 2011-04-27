/**
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 **/
package org.exoplatform.calendar.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.Attachment;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
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

  private long attSize = 0;

  public UIAttachFileForm() throws Exception {
    setMultiPart(true) ;
    int sizeLimit = CalendarUtils.getLimitUploadSize();
    int i = 0 ;
    while(i++ < maxField) {
      UIFormUploadInput uiInput = new UIFormUploadInput(FIELD_UPLOAD + String.valueOf(i), FIELD_UPLOAD + String.valueOf(i), sizeLimit, true) ;
      addUIFormInput(uiInput) ;
    }
  }


  public long getAttSize() {return attSize ;}
  public void setAttSize(long value) { attSize = value ;}
  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public static void removeUploadTemp(UploadService uservice, String uploadId) {
    try {
      uservice.removeUploadResource(uploadId) ;
    } catch (Exception e) {
      e.printStackTrace() ;
    }
  }

  static  public class SaveActionListener extends EventListener<UIAttachFileForm> {
    public void execute(Event<UIAttachFileForm> event) throws Exception {
      UIAttachFileForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      List<Attachment> files = new ArrayList<Attachment>() ;
      int i = 0 ;
      long size = uiForm.attSize ;
      while(i++ < uiForm.maxField) {
        UIFormUploadInput input = (UIFormUploadInput)uiForm.getUIInput(FIELD_UPLOAD + String.valueOf(i));
        UploadResource uploadResource = input.getUploadResource() ;
        if(uploadResource != null) {
          long fileSize = ((long)uploadResource.getUploadedSize()) ;
          size = size + fileSize ;
          if(size >= 10*1024*1024) {
            uiApp.addMessage(new ApplicationMessage("UIAttachFileForm.msg.total-attachts-size-over10M", null, ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          Attachment attachfile = new Attachment() ;
          attachfile.setName(uploadResource.getFileName()) ;
          attachfile.setInputStream(input.getUploadDataAsStream()) ;
          attachfile.setMimeType(uploadResource.getMimeType()) ;
          attachfile.setSize(fileSize);
          attachfile.setResourceId(uploadResource.getUploadId());
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
          uiEventForm.setSelectedTab(UIEventForm.TAB_EVENTDETAIL) ;
          UIEventDetailTab uiEventDetailTab = uiEventForm.getChild(UIEventDetailTab.class) ;
          for(Attachment file :  files){
            uiEventDetailTab.addToUploadFileList(file) ;
          }
          uiEventDetailTab.refreshUploadFileList() ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiEventDetailTab) ;
        } else if(uiTaskForm != null) {
          uiTaskForm.setSelectedTab(UITaskForm.TAB_TASKDETAIL) ;
          UITaskDetailTab uiTaskDetailTab = uiTaskForm.getChild(UITaskDetailTab.class) ;
          for(Attachment file :  files){
            uiTaskDetailTab.addToUploadFileList(file) ;  
          }
          uiTaskDetailTab.refreshUploadFileList() ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiTaskDetailTab) ;
        }
        UIPopupAction uiPopupAction = uiPopupContainer.getChild(UIPopupAction.class) ;
        uiPopupAction.deActivate() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UIAttachFileForm> {
    public void execute(Event<UIAttachFileForm> event) throws Exception {
      UIAttachFileForm uiFileForm = event.getSource() ;
      int i = 0 ;
      while(i++ < uiFileForm.maxField) {
        UIFormUploadInput input = (UIFormUploadInput)uiFileForm.getUIInput(FIELD_UPLOAD + String.valueOf(i));
        UploadResource uploadResource = input.getUploadResource() ;
        if(uploadResource != null)
        UIAttachFileForm.removeUploadTemp(uiFileForm.getApplicationComponent(UploadService.class), uploadResource.getUploadId()) ;
      }
      UIPopupAction uiPopupAction = uiFileForm.getAncestorOfType(UIPopupAction.class) ;
      uiPopupAction.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
    }
  }
}
