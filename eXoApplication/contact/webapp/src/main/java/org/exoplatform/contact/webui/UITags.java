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
package org.exoplatform.contact.webui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.contact.ContactUtils;
import org.exoplatform.contact.SessionsUtils;
import org.exoplatform.contact.service.Contact;
import org.exoplatform.contact.service.ContactService;
import org.exoplatform.contact.service.DataPageList;
import org.exoplatform.contact.service.Tag;
import org.exoplatform.contact.webui.UIContacts.FullNameComparator;
import org.exoplatform.contact.webui.popup.UIExportForm;
import org.exoplatform.contact.webui.popup.UIEditTagForm;
import org.exoplatform.contact.webui.popup.UIPopupAction;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
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
    template =  "app:/templates/contact/webui/UITags.gtmpl",
    events = {
        @EventConfig(listeners = UITags.SelectTagActionListener.class),
        @EventConfig(listeners = UITags.EditTagActionListener.class),
        @EventConfig(listeners = UITags.ExportAddressActionListener.class),
        @EventConfig(listeners = UITags.DeleteTagActionListener.class,
            confirm = "UITags.msg.confirm-delete")        
    }
)
public class UITags extends UIComponent {
  
  public UITags() throws Exception { }
  private String selectedTag_ = null ;
  private Map<String, Tag> tagMap_ = new HashMap<String, Tag>() ;
  
  public List<Tag> getTags() throws Exception {
    ContactService contactService = ContactUtils.getContactService();
    String username = ContactUtils.getCurrentUser() ;
    List<Tag> tags = contactService.getTags(SessionsUtils.getSessionProvider(), username) ;
    tagMap_.clear() ;
    for(Tag tag : tags) { tagMap_.put(tag.getId(), tag) ; }
    return tags;
  }
  public Map<String, Tag> getTagMap() { return tagMap_ ; }
  
  public boolean canPrint(String tagId) {
    if (ContactUtils.isEmpty(selectedTag_) || ContactUtils.isEmpty(tagId) || !tagId.equals(selectedTag_) 
        || getAncestorOfType(UIWorkingContainer.class).findFirstComponentOfType(UIContacts.class).getViewContactsList()) {
      return false ;
    }
    return true ;
  }
  public void setSelectedTag(String id) { selectedTag_ = id ; }
  public String getSelectedTag() { return selectedTag_ ; }
  
  static  public class SelectTagActionListener extends EventListener<UITags> {
    @SuppressWarnings("unchecked")
    public void execute(Event<UITags> event) throws Exception {
      UITags uiForm = event.getSource() ;
      String tagId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiForm.setSelectedTag(tagId) ;
      UIWorkingContainer uiWorkingContainer = uiForm.getAncestorOfType(UIWorkingContainer.class) ;
      uiWorkingContainer.findFirstComponentOfType(UIAddressBooks.class).setSelectedGroup(null) ;
      UIContacts uiContacts = uiWorkingContainer.findFirstComponentOfType(UIContacts.class) ;
      
      DataPageList pageList =ContactUtils.getContactService().getContactPageListByTag(
          SessionsUtils.getSystemProvider(), ContactUtils.getCurrentUser(), tagId) ;
      if (pageList != null) {
        List<Contact> contacts = new ArrayList<Contact>() ;
        contacts = pageList.getAll() ;
        FullNameComparator.isAsc = true ;
        Collections.sort(contacts, new FullNameComparator()) ;          
        pageList.setList(contacts) ;      
      }
      uiContacts.setSortedBy(UIContacts.fullName) ;
      uiContacts.setContacts(pageList) ;
      uiContacts.setSelectedGroup(null) ;
      uiContacts.setSelectedTag(tagId) ;
      uiContacts.setDisplaySearchResult(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  }
  
  static  public class EditTagActionListener extends EventListener<UITags> {
    public void execute(Event<UITags> event) throws Exception {
      UITags uiForm = event.getSource() ;
      String tagId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIContactPortlet uiContactPortlet = uiForm.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = uiContactPortlet.getChild(UIPopupAction.class) ;
      UIEditTagForm uiEditTagForm = popupAction.activate(UIEditTagForm.class, 500) ;
      uiEditTagForm.setValues(uiForm.tagMap_.get(tagId)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  
  static  public class ExportAddressActionListener extends EventListener<UITags> {
    public void execute(Event<UITags> event) throws Exception {
      UITags uiForm = event.getSource() ;
      String tagId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIContactPortlet uiContactPortlet = uiForm.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = uiContactPortlet.getChild(UIPopupAction.class) ;  
      UIExportForm uiExportForm = popupAction.activate(UIExportForm.class, 500) ;
          uiExportForm.setId("ExportForm");
      uiExportForm.setSelectedTag(uiForm.tagMap_.get(tagId).getName()) ;

      Contact[] contacts = null ;
      contacts = ContactUtils.getContactService().getContactPageListByTag(SessionsUtils
          .getSystemProvider(), ContactUtils.getCurrentUser(), tagId).getAll().toArray(new Contact[] {});
      if (contacts == null || contacts.length == 0) {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UITag.msg.noContactToExport", null,
          ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;  
      }
      uiExportForm.setContacts(contacts) ;
      uiExportForm.updateList();
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }  
  
  static  public class DeleteTagActionListener extends EventListener<UITags> {
    public void execute(Event<UITags> event) throws Exception {
      UITags uiTags = event.getSource() ;
      String tagId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      ContactUtils.getContactService()
        .removeTag(SessionsUtils.getSystemProvider(), ContactUtils.getCurrentUser(), tagId) ;
      UIWorkingContainer uiWorkingContainer = uiTags.getAncestorOfType(UIWorkingContainer.class) ;
      UIContacts uiContacts = uiWorkingContainer.findFirstComponentOfType(UIContacts.class) ;
      if (tagId.equals(uiTags.getSelectedTag())) {
        uiTags.setSelectedTag(null) ;
        uiContacts.setContacts(null) ;
        uiWorkingContainer.findFirstComponentOfType(UIContactPreview.class).setContact(null) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  }
  
}
