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
package org.exoplatform.contact.webui.popup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.contact.ContactUtils;
import org.exoplatform.contact.service.Contact;
import org.exoplatform.contact.service.ContactService;
import org.exoplatform.contact.service.impl.JCRDataStorage;
import org.exoplatform.contact.webui.UIContactPortlet;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
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
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormInputWithActions.ActionData;

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
      @EventConfig(listeners = UISharedContactsForm.SaveActionListener.class),    
      @EventConfig(listeners = UISharedContactsForm.SelectPermissionActionListener.class, phase = Phase.DECODE),  
      @EventConfig(listeners = UISharedContactsForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UISharedContactsForm extends UIForm implements UIPopupComponent, UISelector {
  final static public String FIELD_CONTACT = "contactName".intern() ;  
  final static public String FIELD_USER = "user".intern() ;
  final static public String FIELD_GROUP = "group".intern() ;  
  final static public String FIELD_EDIT_PERMISSION = "canEdit".intern() ;
  private Map<String, String> permissionUser_ = new LinkedHashMap<String, String>() ;
  private Map<String, String> permissionGroup_ = new LinkedHashMap<String, String>() ;
  private Map<String, Contact> sharedContacts = new LinkedHashMap<String, Contact>() ;
  public UISharedContactsForm() { }
  
  public void init(Map<String, Contact> contacts) throws Exception {
    UIFormInputWithActions inputset = new UIFormInputWithActions("UIInputUserSelect") ;
    sharedContacts = contacts ; 
    StringBuffer buffer = new StringBuffer() ;
    for (Contact contact : contacts.values()) {
      if (buffer.length() > 0) buffer.append(", ") ;
      buffer.append(ContactUtils.encodeHTML(contact.getFullName())) ;
    }
    UIFormInputInfo inputInfo = new UIFormInputInfo(FIELD_CONTACT, FIELD_CONTACT, null) ;
    inputInfo.setValue(buffer.toString()) ;
    inputset.addChild(inputInfo) ;
    inputset.addUIFormInput(new UIFormStringInput(FIELD_USER, FIELD_USER, null)) ;
    List<ActionData> actionUser = new ArrayList<ActionData>() ;
    actionUser = new ArrayList<ActionData>() ;
    ActionData selectUserAction = new ActionData() ;
    selectUserAction.setActionListener("SelectPermission") ;
    selectUserAction.setActionName("SelectUser") ;    
    selectUserAction.setActionType(ActionData.TYPE_ICON) ;
    selectUserAction.setCssIconClass("SelectUserIcon") ;
    selectUserAction.setActionParameter(UISelectComponent.TYPE_USER) ;
    actionUser.add(selectUserAction) ;
    inputset.setActionField(FIELD_USER, actionUser) ;
    
    UIFormStringInput group = new UIFormStringInput(FIELD_GROUP, FIELD_GROUP, null) ;
    group.setEditable(false) ;
    inputset.addUIFormInput(group) ;
    List<ActionData> actionGroup = new ArrayList<ActionData>() ;
    ActionData selectGroupAction = new ActionData() ;
    selectGroupAction.setActionListener("SelectPermission") ;
    selectGroupAction.setActionName("SelectGroup") ;    
    selectGroupAction.setActionType(ActionData.TYPE_ICON) ;  
    selectGroupAction.setCssIconClass("SelectGroupIcon") ;
    selectGroupAction.setActionParameter(UISelectComponent.TYPE_GROUP) ;
    actionGroup.add(selectGroupAction) ;
    inputset.setActionField(FIELD_GROUP, actionGroup) ;
    
    inputset.addChild(new UIFormCheckBoxInput<Boolean>(FIELD_EDIT_PERMISSION, FIELD_EDIT_PERMISSION, null)) ;
    addChild(inputset) ;    
  }

  public String getLabel(String id) {
    try {
      return super.getLabel(id) ;
    } catch (Exception e) {
      return id ;
    }
  }

  public String[] getActions() { return new String[] { "Save", "Cancel" } ; }
  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public void updateSelect(String selectField, String value) throws Exception {
    UIFormStringInput fieldInput = getUIStringInput(selectField) ;
    Map<String, String> permission ;
    if (selectField.equals(FIELD_USER)) {
      permissionUser_.put(value, value) ;
      permission = permissionUser_ ;
    } else {
      permissionGroup_.put(value, value) ;
      permission = permissionGroup_ ;
    }  
    StringBuilder sb = new StringBuilder() ;
    for(String s : permission.values()) {      
      if(sb != null && sb.length() > 0) sb.append(", ") ;
      sb.append(s) ;
    }    
    fieldInput.setValue(sb.toString()) ;
  } 
  
  static  public class SaveActionListener extends EventListener<UISharedContactsForm> {
    @SuppressWarnings("unchecked")
  public void execute(Event<UISharedContactsForm> event) throws Exception {
      UISharedContactsForm uiForm = event.getSource() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      String names = uiForm.getUIStringInput(FIELD_USER).getValue() ;
      String groups = uiForm.getUIStringInput(FIELD_GROUP).getValue() ;
      List<String> receiverUserByGroup = new ArrayList<String>() ;
      List<String> receiverUser = new ArrayList<String>() ;
      if(ContactUtils.isEmpty(names) && ContactUtils.isEmpty(groups)) {        
        uiApp.addMessage(new ApplicationMessage("UISharedContactsForm.msg.empty-username", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      } 
      if(!ContactUtils.isEmpty(names)) {
        OrganizationService organizationService = 
          (OrganizationService)PortalContainer.getComponent(OrganizationService.class) ;
        try {
          String[] array = names.split(",") ;
          for(String name : array) {
            organizationService.getUserHandler().findUserByName(name.trim()).getFullName();
            receiverUser.add(name.trim() + JCRDataStorage.HYPHEN) ;
          }         
        } catch (NullPointerException e) {
          uiApp.addMessage(new ApplicationMessage("UISharedContactsForm.msg.not-exist-username", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }
      ContactService contactService = ContactUtils.getContactService() ;
      String username = ContactUtils.getCurrentUser() ;  
      Map<String, String> viewMapGroups = new LinkedHashMap<String, String>() ;
      if (!ContactUtils.isEmpty(groups)) {
        String[] arrayGroups = groups.split(",") ; 
        for (String group : arrayGroups) {
          viewMapGroups.put(group.trim(), group.trim()) ;
          OrganizationService organizationService = 
                (OrganizationService)PortalContainer.getComponent(OrganizationService.class) ;
          List<User> users = organizationService.getUserHandler().findUsersByGroup(group.trim()).getAll() ;
          for (User user : users) {
            receiverUserByGroup.add(user.getUserName() + JCRDataStorage.HYPHEN) ;
          }
        }        
      } 
      receiverUser.remove(username + JCRDataStorage.HYPHEN) ;
      receiverUserByGroup.remove(username + JCRDataStorage.HYPHEN) ;
      if (receiverUser.size() > 0 || !ContactUtils.isEmpty(groups)) {
        Map<String, String> viewMapUsers = new LinkedHashMap<String, String>() ;
        for (String user : receiverUser) viewMapUsers.put(user, user) ;
        
        Map<String, String> editMapUsers = new LinkedHashMap<String, String>() ; 
        Map<String, String> editMapGroups = new LinkedHashMap<String, String>() ;
        if (uiForm.getUIFormCheckBoxInput(UISharedForm.FIELD_EDIT_PERMISSION).isChecked()) {
          for (String user : receiverUser) editMapUsers.put(user, user) ;          
          editMapGroups.putAll(viewMapGroups) ;
        }
        for (Contact contact : uiForm.sharedContacts.values()) {
          String[] viewPer = contact.getViewPermissionUsers() ;
          Map<String, String> newViewMapUsers = new LinkedHashMap<String, String>() ;
          newViewMapUsers.putAll(viewMapUsers) ;
          if (viewPer != null)
            for (String view : viewPer) newViewMapUsers.put(view, view) ;
          String[] editPer = contact.getEditPermissionUsers() ;
          Map<String, String> newEditMapUsers = new LinkedHashMap<String, String>() ;
          newEditMapUsers.putAll(editMapUsers) ;
          if (editPer != null)
            for (String edit : editPer) newEditMapUsers.put(edit, edit) ; 
          
          Map<String, String> newViewMapGroups = new LinkedHashMap<String, String>() ;
          newViewMapGroups.putAll(viewMapGroups) ;
          String[] viewPerGroup = contact.getViewPermissionGroups() ;
          if (viewPerGroup != null) 
            for (String view : viewPerGroup) newViewMapGroups.put(view, view) ;
          String[] editPerGroup = contact.getEditPermissionGroups() ;
          Map<String, String> newEditMapGroups = new LinkedHashMap<String, String>() ;
          newEditMapGroups.putAll(editMapGroups) ;
          if (editPerGroup != null)
            for (String edit : editPerGroup) newEditMapGroups.put(edit, edit) ; 
          contact.setViewPermissionUsers(newViewMapUsers.keySet().toArray(new String[] {})) ;
          contact.setEditPermissionUsers(newEditMapUsers.keySet().toArray(new String[] {})) ;
          contact.setViewPermissionGroups(newViewMapGroups.keySet().toArray(new String[] {})) ;
          contact.setEditPermissionGroups(newEditMapGroups.keySet().toArray(new String[] {})) ;
          contactService.saveContact(SessionProviderFactory.createSessionProvider(), username, contact, false) ;
        }
        String[] contactIds = uiForm.sharedContacts.keySet().toArray(new String[]{}) ;
        
        receiverUser.addAll(receiverUserByGroup) ;
        contactService.shareContact(SessionProviderFactory.createSessionProvider(), username, contactIds, receiverUser) ;  
        //contactService.shareContact(SessionProviderFactory.createSessionProvider(), username, contactIds, receiverUserByGroup) ;
        uiApp.addMessage(new ApplicationMessage("UISharedContactsForm.msg.contacts-shared", null)) ;
        UIContactPortlet contactPortlet = uiForm.getAncestorOfType(UIContactPortlet.class) ;
        contactPortlet.cancelAction() ;
      } else if (ContactUtils.isEmpty(groups)){
        uiApp.addMessage(new ApplicationMessage("UISharedContactsForm.msg.invalid-username", null)) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      return ;      
    }
  }
  static  public class SelectPermissionActionListener extends EventListener<UISharedContactsForm> {
    public void execute(Event<UISharedContactsForm> event) throws Exception {
      UISharedContactsForm uiForm = event.getSource() ;
      String permType = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIPopupAction childPopup = uiForm.getAncestorOfType(UIPopupContainer.class).getChild(UIPopupAction.class) ;
      UIGroupSelector uiGroupSelector = childPopup.activate(UIGroupSelector.class, 500) ;
      uiGroupSelector.setType(permType) ;
      uiGroupSelector.setSelectedGroups(null) ;
      
      if (permType.equals(UISelectComponent.TYPE_USER)) {
        // add to fix bug cs 997
        String users = uiForm.getUIStringInput(FIELD_USER).getValue() ;
        uiForm.permissionUser_.clear() ;
        if (!ContactUtils.isEmpty(users)) {
          if (users.indexOf(",") < 0) uiForm.permissionUser_.put(users.trim(), users.trim()) ;
          else {
            for (String user : users.split(",")) uiForm.permissionUser_.put(user.trim(), user.trim()) ;              
          }          
        }
        
        uiGroupSelector.setId("UIUserSelector") ;
        uiGroupSelector.setComponent(uiForm, new String[]{UISharedForm.FIELD_USER}) ;
      } else {
        uiGroupSelector.setId("UIGroupSelector") ;
        uiGroupSelector.setComponent(uiForm, new String[]{UISharedForm.FIELD_GROUP}) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(childPopup) ;  
    }
  }
  
  static  public class CancelActionListener extends EventListener<UISharedContactsForm> {
    public void execute(Event<UISharedContactsForm> event) throws Exception {
      UISharedContactsForm uiForm = event.getSource() ;
      UIContactPortlet contactPortlet = uiForm.getAncestorOfType(UIContactPortlet.class) ;
      contactPortlet.cancelAction() ;
    }
  }

}
