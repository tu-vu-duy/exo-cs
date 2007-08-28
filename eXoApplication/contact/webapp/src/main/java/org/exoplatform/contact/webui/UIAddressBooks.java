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
        @EventConfig(listeners = UIAddressBooks.SelectGroupActionListener.class),
        @EventConfig(listeners = UIAddressBooks.SelectShareGroupActionListener.class)      
    }  
)
public class UIAddressBooks extends UIComponent  {
  private Map<String, List<Contact>> sharedContactMap_  = new HashMap<String, List<Contact>>() ;

  public UIAddressBooks() throws Exception {

  }

  public List<ContactGroup> getGroups()throws Exception {
    ContactService contactService = this.getApplicationComponent(ContactService.class);
    String username = Util.getPortalRequestContext().getRemoteUser() ;    
    return contactService.getGroups(username);
  }
  
  public List<Contact> getContactMapValue(String groupId) {
    return sharedContactMap_.get(groupId) ;
  }
  public List<GroupContactData> getSharedContact() throws Exception {
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

  static  public class SelectGroupActionListener extends EventListener<UIAddressBooks> {
    public void execute(Event<UIAddressBooks> event) throws Exception {
      UIAddressBooks uiForm = event.getSource() ;
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIWorkingContainer uiWorkingContainer = uiForm.getAncestorOfType(UIWorkingContainer.class) ;
      
      UIContacts uiContacts = uiWorkingContainer.findFirstComponentOfType(UIContacts.class) ; 
      uiContacts.setGroupId(groupId) ;
      uiContacts.setIsPersonalContact(true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts) ;
      
      UIContactPreview uiContactPreview = uiWorkingContainer.findFirstComponentOfType(UIContactPreview.class);
      ContactService contactService = uiForm.getApplicationComponent(ContactService.class) ;
      String username = Util.getPortalRequestContext().getRemoteUser() ;
      Contact contact = null ;
      if (contactService.getContactsByGroup(username, groupId).size() > 0) 
        contact = contactService.getContactsByGroup(username, groupId).get(0);
      uiContactPreview.setContact(contact);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContactPreview) ;
    }
  }
  static  public class SelectShareGroupActionListener extends EventListener<UIAddressBooks> {
    public void execute(Event<UIAddressBooks> event) throws Exception {
      UIAddressBooks uiForm = event.getSource() ;
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIWorkingContainer uiWorkingContainer = uiForm.getAncestorOfType(UIWorkingContainer.class) ;
      
      UIContacts uiContacts = uiWorkingContainer.findFirstComponentOfType(UIContacts.class) ; 
      uiContacts.setGroupId(groupId) ;
      uiContacts.setIsPersonalContact(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts) ;
      
      UIContactPreview uiContactPreview = uiWorkingContainer.findFirstComponentOfType(UIContactPreview.class);
      ContactService contactService = uiForm.getApplicationComponent(ContactService.class) ;
      Contact contact = contactService.getSharedContacts(new String[] { groupId }).get(0).getContacts().get(0);
      uiContactPreview.setContact(contact);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContactPreview) ;
    }
  }
}
