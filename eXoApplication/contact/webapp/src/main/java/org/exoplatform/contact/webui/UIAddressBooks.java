/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.contact.webui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.contact.service.Contact;
import org.exoplatform.contact.service.ContactGroup;
import org.exoplatform.contact.service.ContactService;
import org.exoplatform.contact.service.GroupContactData;
import org.exoplatform.contact.webui.popup.UICategoryForm;
import org.exoplatform.contact.webui.popup.UIPopupAction;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
    template =  "app:/templates/contact/webui/UIAddressBooks.gtmpl", 
    events = {
        @EventConfig(listeners = UIAddressBooks.EditGroupActionListener.class),
        @EventConfig(listeners = UIAddressBooks.DeleteGroupActionListener.class,
            confirm = "UIAddressBooks.msg.confirm-delete"),
        @EventConfig(listeners = UIAddressBooks.SelectGroupActionListener.class),
        @EventConfig(listeners = UIAddressBooks.SelectSharedGroupActionListener.class),
        @EventConfig(listeners = UIAddressBooks.AddressPopupActionListener.class)
    }
)
public class UIAddressBooks extends UIComponent  {
  private boolean addressBookSelected = true ;
  private boolean personalAddressBookSelected = true ;
  private Map<String, List<Contact>> sharedContactMap_  = new HashMap<String, List<Contact>>() ;
  private String selectedGroup_ = "";
  
  public UIAddressBooks() throws Exception { }
  
  public List<ContactGroup> getGroups()throws Exception {
    ContactService contactService = this.getApplicationComponent(ContactService.class);
    String username = Util.getPortalRequestContext().getRemoteUser() ;    
    return contactService.getGroups(username);
  }
  
  public void setSelectedGroup(String groupId) { selectedGroup_ = groupId ; }
  public String getSelectedGroup() { return selectedGroup_ ; }
  
  public void setAddressBookSelected(boolean selected) { addressBookSelected = selected ; }
  public boolean getAddressBookSelected() { return addressBookSelected ; }
  
  public void setPersonalAddressBookSelected(boolean selected) { personalAddressBookSelected = selected ; }
  public boolean getPersonalAddressBookSelected() { return personalAddressBookSelected ; }
  
  public List<Contact> getContactMapValue(String groupId) { return sharedContactMap_.get(groupId) ; }
  public List<GroupContactData> getSharedContactGroups() throws Exception {
    sharedContactMap_.clear() ;
    String username = Util.getPortalRequestContext().getRemoteUser() ;
    OrganizationService organizationService = getApplicationComponent(OrganizationService.class) ;
    ContactService contactService = getApplicationComponent(ContactService.class) ;
    Object[] objGroupIds = organizationService.getGroupHandler().findGroupsOfUser(username).toArray() ;
    String[] groupIds = new String[objGroupIds.length];
    for (int i = 0; i < groupIds.length; i++) {
      groupIds[i] = ((GroupImpl)objGroupIds[i]).getId() ;
    }
    for(GroupContactData GCD : contactService.getSharedContacts(groupIds)) {
      sharedContactMap_.put(GCD.getName(), GCD.getContacts()) ;
    }
    return contactService.getSharedContacts(groupIds);
  }
  
  static  public class EditGroupActionListener extends EventListener<UIAddressBooks> {
    public void execute(Event<UIAddressBooks> event) throws Exception {
      UIAddressBooks uiAddressBook = event.getSource() ;  
      UIContactPortlet contactPortlet = uiAddressBook.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      UICategoryForm uiCategoryForm = popupAction.createUIComponent(UICategoryForm.class, null, "AddNewAddressBook") ;
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID);
      uiCategoryForm.setValues(groupId) ;
      UICategoryForm.isNew_ = false ;
      popupAction.activate(uiCategoryForm, 500, 0, true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  
  static  public class DeleteGroupActionListener extends EventListener<UIAddressBooks> {
    public void execute(Event<UIAddressBooks> event) throws Exception {
      UIAddressBooks uiAddressBook = event.getSource() ;  
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID);
      ContactService contactService = uiAddressBook.getApplicationComponent(ContactService.class);
      String username = Util.getPortalRequestContext().getRemoteUser() ;
      contactService.removeGroup(username, groupId) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddressBook) ;      
    }
  }

  static  public class SelectGroupActionListener extends EventListener<UIAddressBooks> {
    public void execute(Event<UIAddressBooks> event) throws Exception {
      UIAddressBooks uiAddressBook = event.getSource() ;  
      UIWorkingContainer uiWorkingContainer = uiAddressBook.getAncestorOfType(UIWorkingContainer.class) ;
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID) ;    
      ContactService contactService = uiAddressBook.getApplicationComponent(ContactService.class);
      uiAddressBook.setSelectedGroup(groupId) ;
      uiAddressBook.setAddressBookSelected(true) ;
      uiAddressBook.setPersonalAddressBookSelected(true) ;
      String username = Util.getPortalRequestContext().getRemoteUser() ;
      UIContacts uiContacts = uiWorkingContainer.findFirstComponentOfType(UIContacts.class) ;
      uiContacts.setContacts(contactService.getContactsByGroup(username, groupId)) ; 
      UIContactPreview uiContactPreview = uiWorkingContainer.findFirstComponentOfType(UIContactPreview.class);
      uiContactPreview.updateContact() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  }
  
  static  public class SelectSharedGroupActionListener extends EventListener<UIAddressBooks> {
    public void execute(Event<UIAddressBooks> event) throws Exception {
      UIAddressBooks uiAddressBook = event.getSource() ;  
      UIWorkingContainer uiWorkingContainer = uiAddressBook.getAncestorOfType(UIWorkingContainer.class) ;
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID) ;    
      ContactService contactService = uiAddressBook.getApplicationComponent(ContactService.class);
      uiAddressBook.setSelectedGroup(groupId) ;
      uiAddressBook.setAddressBookSelected(true) ;
      uiAddressBook.setPersonalAddressBookSelected(false) ;
      UIContacts uiContacts = uiWorkingContainer.findFirstComponentOfType(UIContacts.class) ; 
      if (contactService.getSharedContacts(new String[] {groupId}) != null && contactService.getSharedContacts(new String[] {groupId}).size() > 0)
        uiContacts.setContacts(contactService.getSharedContacts(new String[] {groupId}).get(0).getContacts()) ;      
      UIContactPreview uiContactPreview = uiWorkingContainer.findFirstComponentOfType(UIContactPreview.class);
      uiContactPreview.updateContact() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  }
  
  public static class AddressPopupActionListener extends EventListener<UIAddressBooks> {
    public void execute(Event<UIAddressBooks> event) throws Exception {
      UIAddressBooks uiAddressBook = event.getSource() ;
      UIContactPortlet uiContactPortlet = uiAddressBook.getAncestorOfType(UIContactPortlet.class) ;       
      String viewType = event.getRequestContext().getRequestParameter(OBJECTID) ;
      System.out.println("\n\n view type :" + viewType + "\n\n");
  
//      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddressBook.getParent()) ;
    }
  }
  
}
