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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.mail.Colors;
import org.exoplatform.mail.MailUtils;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.Message;
import org.exoplatform.mail.service.Tag;
import org.exoplatform.mail.webui.UIMailPortlet;
import org.exoplatform.mail.webui.UIMessageArea;
import org.exoplatform.mail.webui.UIMessageList;
import org.exoplatform.mail.webui.UISelectAccount;
import org.exoplatform.mail.webui.UITagContainer;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
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
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UITagForm.AddActionListener.class), 
      @EventConfig(listeners = UITagForm.RemoveActionListener.class), 
      @EventConfig(listeners = UITagForm.CancelActionListener.class, phase = Phase.DECODE)
    }  
)
public class UITagForm extends UIForm implements UIPopupComponent{
  public static final String SELECT_AVAIABLE_TAG = "Tag Name";
  public static final String TAG_COLOR = "Choose Color" ;
  public static final String TAG_MESSAGE = "TagMessage";
  
  private Map<String, Message> messageMap = new HashMap<String, Message>() ;
  private Map<String, Tag> tagMap = new HashMap<String, Tag>();
  
  public UITagForm() { }
  
  public void setTagList(List<Tag> tagList) throws Exception {
    tagMap.clear();   
    addUIFormInput(new UIFormStringInput(SELECT_AVAIABLE_TAG, SELECT_AVAIABLE_TAG, null));
    addUIFormInput(new UIFormColorPicker(TAG_COLOR, TAG_COLOR, Colors.COLORS)) ;
    for(Tag tag : tagList) {
      UIFormCheckBoxInput<Boolean> uiCheckBox = new UIFormCheckBoxInput<Boolean>(tag.getName(), tag.getName(), null);
      addUIFormInput(uiCheckBox) ;
      tagMap.put(tag.getName(), tag);
    }
  }
  
  public String[] getActions() { return new String[] {"Add", "Remove", "Cancel"}; }
  
  public List<Tag> getTagList() {
    return new ArrayList<Tag>(tagMap.values());
  }
  
  public void setMessageList(List<Message> messageList) throws Exception {
    messageMap.clear();
    UIMailPortlet uiPortlet = getAncestorOfType(UIMailPortlet.class);
    String username = uiPortlet.getCurrentUser() ;
    String accountId = uiPortlet.findFirstComponentOfType(UISelectAccount.class).getSelectedValue();
    MailService mailSrv = getApplicationComponent(MailService.class);
    for(Message msg : messageList) {
      String mesSub = "Sub : " + ((msg.getSubject().length() >= 30) ? (msg.getSubject().substring(0, 30) + "...") : msg.getSubject());
      UIFormInputInfo uiTags = new UIFormInputInfo(TAG_MESSAGE, TAG_MESSAGE, null);
      String tags = "";
      if (msg.getTags() != null && msg.getTags().length > 0) {
        for (int i = 0; i < msg.getTags().length; i++) {
          if (i > 0) tags += ", ";
          Tag tag = mailSrv.getTag(SessionProviderFactory.createSystemProvider(), username, accountId, msg.getTags()[i]);
          tags += "[" + tag.getName() + "]";
        }
      } else tags = "No tag";
      
      uiTags.setName(mesSub);
      uiTags.setValue(tags);
      addUIFormInput(uiTags) ;
      messageMap.put(msg.getId(), msg);
    }
  }
  
  public List<Message> getMessageList() {
    return new ArrayList<Message>(messageMap.values());
  }
  
  public List<Tag> getCheckedTags() throws Exception {
    List<Tag> tagList = new ArrayList<Tag>();
    for (Tag tag : getTagList()) {
      UIFormCheckBoxInput<Boolean> checkbox = getChildById(tag.getName());
      if (checkbox != null && checkbox.isChecked()) {
        tagList.add(tag);
      } 
    }
    return tagList;
  }
  
  public String getLabel(String id) { return id ;}
  public String getSelectedColor() {
    return getChild(UIFormColorPicker.class).getValue() ;
  }
  public void setSelectedColor(String value) {
    getChild(UIFormColorPicker.class).setValue(value) ;
  }
  public void activate() throws Exception {}
  public void deActivate() throws Exception {}
  
  static  public class AddActionListener extends EventListener<UITagForm> {
    public void execute(Event<UITagForm> event) throws Exception {
      UITagForm uiTagForm = event.getSource(); 
      String newTagName = uiTagForm.getUIStringInput(SELECT_AVAIABLE_TAG).getValue();
      String tagColor = uiTagForm.getSelectedColor();
      UIMailPortlet uiPortlet = uiTagForm.getAncestorOfType(UIMailPortlet.class);
      UITagContainer uiTagContainer = uiPortlet.findFirstComponentOfType(UITagContainer.class);
      String username = uiPortlet.getCurrentUser() ;
      String accountId = uiPortlet.findFirstComponentOfType(UISelectAccount.class).getSelectedValue();
      MailService mailSrv = uiTagForm.getApplicationComponent(MailService.class);
      List<Tag> tagList = new ArrayList<Tag>();

      if (newTagName != null && newTagName != "") {
        boolean isExist = false;
        for (Tag tag: mailSrv.getTags(SessionProviderFactory.createSystemProvider(), username, accountId)) {
          if (tag.getName().equals(newTagName)) { 
            isExist = true;
            tagList.add(tag);
          }
        }
        if (!isExist) {
          Tag newTag = new Tag();
          newTag.setName(newTagName);
          newTag.setColor(tagColor);
          newTag.setDescription("Tag's description");
          tagList.add(newTag);
        } else {
          UIApplication uiApp = uiTagForm.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UITagForm.msg.tag-already-exists", null, ApplicationMessage.INFO)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return;
        }
      } else {
//        UIApplication uiApp = uiTagForm.getAncestorOfType(UIApplication.class) ;
//        uiApp.addMessage(new ApplicationMessage("UITagForm.msg.name-tag-no-value", null)) ;
//        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
//        return ;
      }
      
      tagList.addAll(uiTagForm.getCheckedTags());
      mailSrv.addTag(SessionProviderFactory.createSystemProvider(), username, accountId, uiTagForm.getMessageList(), tagList);
      
      UIMessageList uiMessageList = uiPortlet.findFirstComponentOfType(UIMessageList.class);
      List<String> tagIdList = new ArrayList<String>() ;
      for (Tag tag : tagList) tagIdList.add(tag.getId()) ;
      for (Message msg : uiTagForm.getMessageList()) {
        if (msg.getTags() != null && msg.getTags().length > 0) {
          for (int i=0 ; i < msg.getTags().length; i++) {
            tagIdList.add(msg.getTags()[i]) ;
          }
        }
        msg.setTags(tagIdList.toArray(new String[]{})) ;
        uiMessageList.messageList_.put(msg.getId(), msg) ;
      }
      uiPortlet.cancelAction() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMessageList.getAncestorOfType(UIMessageArea.class)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTagContainer) ;
    }
  }
  
  static  public class RemoveActionListener extends EventListener<UITagForm> {
    public void execute(Event<UITagForm> event) throws Exception {
      UITagForm uiTagForm = event.getSource(); 
      UIMailPortlet uiPortlet = uiTagForm.getAncestorOfType(UIMailPortlet.class);
      String username = MailUtils.getCurrentUser();
      String accountId = uiPortlet.findFirstComponentOfType(UISelectAccount.class).getSelectedValue();
      MailService mailSrv = uiTagForm.getApplicationComponent(MailService.class);
      List<String> tagList = new ArrayList<String>();
      for (Tag tag : uiTagForm.getCheckedTags()) tagList.add(tag.getId());
      
      mailSrv.removeMessageTag(SessionProviderFactory.createSystemProvider(), username, accountId, uiTagForm.getMessageList(), tagList);
      UIMessageList uiMessageList = uiPortlet.findFirstComponentOfType(UIMessageList.class);
      for (Message msg : uiTagForm.getMessageList()) {
        if (msg.getTags() != null && msg.getTags().length > 0) {
          for (int i=0 ; i < msg.getTags().length; i++) {
            tagList.remove(msg.getTags()[i]) ;
          }
        }
        msg.setTags(tagList.toArray(new String[]{})) ;
        uiMessageList.messageList_.put(msg.getId(), msg) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMessageList.getAncestorOfType(UIMessageArea.class)) ;
      uiPortlet.cancelAction() ;
    }
  }
  
  static  public class CancelActionListener extends EventListener<UITagForm> {
    public void execute(Event<UITagForm> event) throws Exception {
      UITagForm uiForm = event.getSource() ;
      UIMailPortlet uiPortlet = uiForm.getAncestorOfType(UIMailPortlet.class);
      uiPortlet.cancelAction();
    }
  }
   
}
