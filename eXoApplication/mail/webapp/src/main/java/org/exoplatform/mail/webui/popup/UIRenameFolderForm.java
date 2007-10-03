/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.mail.webui.popup;

import org.exoplatform.mail.MailUtils;
import org.exoplatform.mail.service.Folder;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.webui.UIFolderContainer;
import org.exoplatform.mail.webui.UIMailPortlet;
import org.exoplatform.mail.webui.UISelectAccount;
import org.exoplatform.mail.service.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormStringInput;


/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIRenameFolderForm.SaveActionListener.class), 
      @EventConfig(listeners = UIRenameFolderForm.CancelActionListener.class)
    }
)
public class UIRenameFolderForm extends UIForm implements UIPopupComponent {
  final public static String CUR_FOLDER_NAME = "curFolderName" ;
  final public static String NEW_FOLDER_NAME = "newFolderName" ;
  private String folderId;
  public UIRenameFolderForm() {    
    addUIFormInput(new UIFormInputInfo(CUR_FOLDER_NAME, CUR_FOLDER_NAME, null)) ;
    addUIFormInput(new UIFormStringInput(NEW_FOLDER_NAME, NEW_FOLDER_NAME, null)) ;
  }

  public String getFolderId() throws Exception { return folderId; }
  public void setFolderId(String folderId) throws Exception {
    this.folderId = folderId;
    MailService mailSrv = getApplicationComponent(MailService.class);
    String username = MailUtils.getCurrentUser();
    String accountId = getAncestorOfType(UIMailPortlet.class).findFirstComponentOfType(UISelectAccount.class).getSelectedValue();
    Folder folder = mailSrv.getFolder(username, accountId, folderId);
    getUIFormInputInfo(CUR_FOLDER_NAME).setValue(folder.getName());    
  }

  static  public class SaveActionListener extends EventListener<UIRenameFolderForm> {
    public void execute(Event<UIRenameFolderForm> event) throws Exception {
      UIRenameFolderForm uiForm = event.getSource() ;
      MailService mailService = uiForm.getApplicationComponent(MailService.class) ;
      UIMailPortlet uiMailPortlet = uiForm.getAncestorOfType(UIMailPortlet.class);
      String username = uiMailPortlet.getCurrentUser() ;
      String accountId =  uiMailPortlet.findFirstComponentOfType(UISelectAccount.class).getSelectedValue() ;
      String folderId = uiForm.getFolderId();
      String newFolderName = uiForm.getUIStringInput(NEW_FOLDER_NAME).getValue() ;
      
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      UIFolderContainer uiFolderContainer = uiMailPortlet.findFirstComponentOfType(UIFolderContainer.class) ;

      if(Utils.isEmptyField(newFolderName)) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.name-required", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      
      try {
        String newFolderId = accountId + "UserFolder" + newFolderName;
        if (mailService.getFolder(username, accountId, newFolderId) == null) {
          Folder folder =  mailService.getFolder(username, accountId, folderId);
          folder.setLabel(newFolderName) ;
          folder.setName(newFolderName) ;
          mailService.saveUserFolder(username, accountId, folder) ;
        } else {
          uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.folder-exist", new Object[]{newFolderName})) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      } catch (Exception e){
        uiApp.addMessage(new ApplicationMessage("UIRenameFolderForm.msg.error-rename-folder", null)) ;
        e.printStackTrace() ;
      }
      uiForm.getAncestorOfType(UIPopupAction.class).deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupAction.class)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFolderContainer) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      
    }
  }
  static  public class CancelActionListener extends EventListener<UIRenameFolderForm> {
    public void execute(Event<UIRenameFolderForm> event) throws Exception {
      UIRenameFolderForm uiForm = event.getSource() ;
      uiForm.getAncestorOfType(UIPopupAction.class).deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupAction.class)) ;
    }
  }
  
  public void activate() throws Exception { }
  public void deActivate() throws Exception { }
}
