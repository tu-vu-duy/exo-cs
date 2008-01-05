/*
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
 */
package org.exoplatform.mail.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.mail.MailUtils;
import org.exoplatform.mail.SessionsUtils;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.Message;
import org.exoplatform.mail.webui.UIFolderContainer;
import org.exoplatform.mail.webui.UIMailPortlet;
import org.exoplatform.mail.webui.UIMessageArea;
import org.exoplatform.mail.webui.UIMessageList;
import org.exoplatform.mail.webui.UISelectFolder;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;


/**
 * Created by The eXo Platform SARL
 * Author : HAI NGUYEN
 *          haiexo1002@gmail.com
 * September 14, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/mail/webui/UIMoveMessageForm.gtmpl",
    events = {
      @EventConfig(listeners = UIMoveMessageForm.SaveActionListener.class), 
      @EventConfig(listeners = UIMoveMessageForm.CancelActionListener.class)
    }
)
public class UIMoveMessageForm extends UIForm implements UIPopupComponent {
  final public static String FIELD_NAME = "folderName" ;
  final public static String SELECT_FOLDER = "folder" ;
  public static String folderId="";
  public List<Message> messageList=new ArrayList<Message>();  
  
  public UIMoveMessageForm() throws Exception { 
    addUIFormInput(new UISelectFolder("UISelectFolder"));
  }
  
  public void setMessageList(List<Message> messageList){
    this.messageList= messageList; 
  }
  
  public List<Message> getMessageList(){ return messageList; }
 
  public String getFolderId() throws Exception { return folderId; }
  
  public String getSelectedFolderId(){ return folderId; }
  
  static public class SaveActionListener extends EventListener<UIMoveMessageForm> {
    public void execute(Event<UIMoveMessageForm> event) throws Exception {
      UIMoveMessageForm uiMoveMessageForm = event.getSource() ;
      MailService mailSrv = uiMoveMessageForm.getApplicationComponent(MailService.class) ;
      UIMailPortlet uiPortlet = uiMoveMessageForm.getAncestorOfType(UIMailPortlet.class) ;
      UIMessageList uiMessageList = uiPortlet.findFirstComponentOfType((UIMessageList.class));
      String username = uiPortlet.getCurrentUser() ;
      String accountId =  MailUtils.getAccountId();
      String destFolder = uiMoveMessageForm.getChild(UISelectFolder.class).getSelectedValue();
      for(Message message: uiMoveMessageForm.getMessageList()) {
         mailSrv.moveMessages(SessionsUtils.getSessionProvider(), username, accountId, message.getId(), message.getFolders()[0], destFolder);
      }       
      uiMessageList.updateList(); 
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMoveMessageForm.getAncestorOfType(UIPopupAction.class)) ;     
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet.findFirstComponentOfType(UIFolderContainer.class)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMessageList.getAncestorOfType(UIMessageArea.class));  
      uiPortlet.cancelAction();
    }
  }
   
  static  public class CancelActionListener extends EventListener<UIMoveMessageForm> {
    public void execute(Event<UIMoveMessageForm> event) throws Exception {
      UIMoveMessageForm uiForm = event.getSource() ;
      uiForm.getAncestorOfType(UIPopupAction.class).deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupAction.class)) ;
    }
  }

  public void activate() throws Exception { }
  public void deActivate() throws Exception { }
}
