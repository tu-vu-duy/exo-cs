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

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.contact.ContactUtils;
import org.exoplatform.contact.service.Contact;
import org.exoplatform.contact.service.ContactAttachment;
import org.exoplatform.contact.service.ContactFilter;
import org.exoplatform.contact.service.ContactGroup;
import org.exoplatform.contact.service.ContactPageList;
import org.exoplatform.contact.service.ContactService;
import org.exoplatform.contact.service.DataPageList;
import org.exoplatform.contact.service.JCRPageList;
import org.exoplatform.contact.service.SharedAddressBook;
import org.exoplatform.contact.service.Tag;
import org.exoplatform.contact.service.impl.JCRDataStorage;
import org.exoplatform.contact.service.impl.NewUserListener;
import org.exoplatform.contact.webui.popup.UIAddEditPermission;
import org.exoplatform.contact.webui.popup.UICategorySelect;
import org.exoplatform.contact.webui.popup.UIComposeForm;
import org.exoplatform.contact.webui.popup.UIContactPreviewForm;
import org.exoplatform.contact.webui.popup.UIMoveContactsForm;
import org.exoplatform.contact.webui.popup.UIPopupComponent;
import org.exoplatform.contact.webui.popup.UISharedContactsForm;
import org.exoplatform.contact.webui.popup.UITagForm;
import org.exoplatform.contact.webui.popup.UIContactForm;
import org.exoplatform.contact.webui.popup.UIPopupAction;
import org.exoplatform.contact.webui.popup.UIPopupContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.download.DownloadResource;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.mail.service.Account;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/templates/contact/webui/UIContacts.gtmpl",
    events = {
        @EventConfig(listeners = UIContacts.EditContactActionListener.class),
        @EventConfig(listeners = UIContacts.SendEmailActionListener.class),
        @EventConfig(listeners = UIContacts.TagActionListener.class),
        @EventConfig(listeners = UIContacts.MoveContactsActionListener.class),
        @EventConfig(listeners = UIContacts.DNDContactsActionListener.class),
        @EventConfig(listeners = UIContacts.DNDContactsToTagActionListener.class),
        @EventConfig(listeners = UIContacts.DeleteContactsActionListener.class
            , confirm = "UIContacts.msg.confirm-delete"),
        @EventConfig(listeners = UIContacts.SelectedContactActionListener.class), 
        @EventConfig(listeners = UIContacts.CopyContactActionListener.class),
        @EventConfig(listeners = UIContacts.ViewDetailsActionListener.class),
        @EventConfig(listeners = UIContacts.SortActionListener.class),
        @EventConfig(listeners = UIContacts.FirstPageActionListener.class),
        @EventConfig(listeners = UIContacts.PreviousPageActionListener.class),
        @EventConfig(listeners = UIContacts.NextPageActionListener.class),
        @EventConfig(listeners = UIContacts.LastPageActionListener.class),
        @EventConfig(listeners = UIContacts.ExportContactActionListener.class),
        @EventConfig(listeners = UIContacts.CancelActionListener.class),
        @EventConfig(listeners = UIContacts.SelectTagActionListener.class),
        @EventConfig(listeners = UIContacts.SharedContactsActionListener.class),
        @EventConfig(listeners = UIContacts.CloseSearchActionListener.class),
        @EventConfig(listeners = UIContacts.PrintActionListener.class), 
//        @EventConfig(listeners = UIContacts.ChatActionListener.class),
        @EventConfig(listeners = UIContacts.RefreshActionListener.class),
        @EventConfig(listeners = UIContacts.PrintDetailsActionListener.class)
    }
)

public class UIContacts extends UIForm implements UIPopupComponent {
  public boolean viewContactsList = true ;
  public boolean viewListBeforePrint = false ;
  private String selectedTag_ = null ;
  private LinkedHashMap<String, Contact> contactMap = new LinkedHashMap<String, Contact> () ;
  private String selectedGroup = null ;
  private String selectedContact = null ;
  private JCRPageList pageList_ = null ;
  private String sortedBy_ = null;
  private boolean isAscending_ = true;
  private String viewQuery_ = null;
  public static String fullName = "fullName".intern() ;
  public static String emailAddress = "emailAddress".intern() ;
  public static String jobTitle = "jobTitle".intern() ;
  private boolean isSearchResult = false ;
  private boolean defaultNameSorted = true ;
  private boolean isPrintForm = false ;
  @SuppressWarnings("unused")
  private boolean isPrintDetail = false ;
  private boolean isSelectSharedContacts = false ;
  private List<Contact> listBeforePrint = new ArrayList<Contact>() ; 
  private String selectedTagBeforeSearch_ = null ;
  private String selectedGroupBeforeSearch = null ;
  private boolean isSelectSharedContactsBeforeSearch = false ;
  private boolean viewListBeforeSearch = true ;
  private String checkedAll = "" ;
  public UIContacts() throws Exception { }

  public String isCheckAll() { return checkedAll ; }
  public void setListBeforePrint(List<Contact> contacts) { listBeforePrint = contacts ; }  
  public String[] getActions() { return new String[] {"Cancel"} ; }
  public void activate() throws Exception { }
  public void deActivate() throws Exception { } 

  public boolean canChat() {
    try {
      java.lang.Class.forName("org.exoplatform.services.xmpp.rest.RESTXMPPService") ;
      return true ;
    } catch (ClassNotFoundException e) {
      return false ;
    } catch (Exception ex) {
      ex.printStackTrace() ;
      return false ;
    }
  }
  
  
  // only called when refresh brower and close search ;
  @SuppressWarnings({ "unchecked", "unused" })
  private void refreshData() throws Exception {
    if (isDisplaySearchResult() || isPrintForm) return ;
    
    // cs-1823
    long currentPage = 1 ;
    if (selectedGroup != null) {      
      if (pageList_ != null) currentPage = pageList_.getCurrentPage() ;
      ContactPageList pageList = null ;
      if (getPrivateGroupMap().containsKey(selectedGroup)) {
        pageList = ContactUtils.getContactService().getContactPageListByGroup(
            SessionProviderFactory.createSessionProvider(), ContactUtils.getCurrentUser(), selectedGroup);
      } else if (ContactUtils.getUserGroups().contains(selectedGroup)) {
        pageList = ContactUtils.getContactService()
            .getPublicContactsByAddressBook(SessionProviderFactory.createSystemProvider(), selectedGroup);
      } else if (getSharedGroupMap().containsKey(selectedGroup)) {
        UIAddressBooks uiAddressBooks = getAncestorOfType(
            UIWorkingContainer.class).findFirstComponentOfType(UIAddressBooks.class) ;       
        pageList = ContactUtils.getContactService().getSharedContactsByAddressBook(SessionProviderFactory
            .createSystemProvider(),ContactUtils.getCurrentUser(), uiAddressBooks.getSharedGroups().get(selectedGroup)); 
      } else {
        selectedGroup = null ;
      }
      pageList.setCurrentPage(currentPage) ;
      setContacts(pageList) ;
    } else if (selectedTag_ != null) {
      if (pageList_ != null) currentPage = pageList_.getCurrentPage() ;
      DataPageList pageList =ContactUtils.getContactService().getContactPageListByTag(
          SessionProviderFactory.createSystemProvider(), ContactUtils.getCurrentUser(), selectedTag_) ;
      if (pageList != null) {
        List<Contact> contacts = new ArrayList<Contact>() ;
        contacts = pageList.getAll() ;
        if (getSortedBy().equals(UIContacts.fullName)) {
          Collections.sort(contacts, new FullNameComparator()) ;
        } else if (getSortedBy().equals(UIContacts.emailAddress)) {
          Collections.sort(contacts, new EmailComparator()) ;
        } else if (getSortedBy().equals(UIContacts.jobTitle)) {
          Collections.sort(contacts, new JobTitleComparator()) ;
        }
        pageList.setList(contacts) ; 
        pageList.setCurrentPage(currentPage) ;
      }
      setContacts(pageList) ;
    } else if (isSelectSharedContacts) {
      setContacts(ContactUtils.getContactService().getSharedContacts( ContactUtils.getCurrentUser())); 
    }
  }
  
  public void setSelectSharedContacts(boolean selected) { isSelectSharedContacts = selected ; }
  public boolean isSelectSharedContacts() { return isSelectSharedContacts ; }
  public boolean havePermissionAdd(Contact contact) throws Exception {
    if (!contact.getContactType().equals(JCRDataStorage.SHARED)) return false ;
    Map<String, SharedAddressBook> sharedGroupMap = getAncestorOfType(
        UIWorkingContainer.class).findFirstComponentOfType(UIAddressBooks.class).getSharedGroups() ;
    String currentUser = ContactUtils.getCurrentUser() ;
    for (String address : contact.getAddressBook()) { 
      SharedAddressBook add = sharedGroupMap.get(address) ;
      if (add != null) {
        if (add.getEditPermissionUsers() != null &&
            Arrays.asList(add.getEditPermissionUsers()).contains(currentUser + JCRDataStorage.HYPHEN)) {
          return true ;
        }
        String[] editPerGroups = add.getEditPermissionGroups() ;
        if (editPerGroups != null)
          for (String editPer : editPerGroups)
            if (ContactUtils.getUserGroups().contains(editPer)) {
              return true ;
            }
        return false ;
      }
    }
    return false ;
  }
  public boolean havePermission(Contact contact) throws Exception {
    if (!contact.getContactType().equals(JCRDataStorage.SHARED)) return true ;
    // contact shared
    String currentUser = ContactUtils.getCurrentUser() ;
    if (contact.getEditPermissionUsers() != null &&
        Arrays.asList(contact.getEditPermissionUsers()).contains(currentUser + JCRDataStorage.HYPHEN)) {
      return true ;
    }
    String[] editPerGroups = contact.getEditPermissionGroups() ;
    if (editPerGroups != null)
      for (String editPer : editPerGroups)
        if (ContactUtils.getUserGroups().contains(editPer)) {
          return true ;
        }    
    Map<String, SharedAddressBook> sharedGroupMap = getAncestorOfType(UIWorkingContainer.class)
        .findFirstComponentOfType(UIAddressBooks.class).getSharedGroups() ;
    for (String address : contact.getAddressBook()) {
      try {
        SharedAddressBook add = sharedGroupMap.get(address) ;
        if (add.getEditPermissionUsers() != null &&
            Arrays.asList(add.getEditPermissionUsers()).contains(currentUser + JCRDataStorage.HYPHEN)) {
          return true ;
        }
        editPerGroups = add.getEditPermissionGroups() ;
        if (editPerGroups != null)
          for (String editPer : editPerGroups)
            if (ContactUtils.getUserGroups().contains(editPer)) {
              return true ;
            }
      } catch (NullPointerException e) { return false ; }
    }
    return false ;    
  }
  
  public boolean isSharedAddress(Contact contact) throws Exception {
  /*  ContactService service = ContactUtils.getContactService() ;
    String username = ContactUtils.getCurrentUser() ;
    */
    if (isSelectSharedContacts) return false ;
    for (String add : contact.getAddressBook()) {
      if (getSharedGroupMap().containsKey(add)) {
        return true ;
        
        //if (selectedGroup != null && add.equals(selectedGroup)) return true ;
        /*if (isSearchResult || selectedTag_ != null) {
          try {
            // should priority non permission first ?
            if (service.getSharedContact(SessionProviderFactory.createSystemProvider()
                , username, contact.getId()) != null) return false ;         
            else return true ;
          } catch (PathNotFoundException e) { return false ; }
          
        }*/
      }
    }
    return false ;
  }
  
  public void setPrintForm(boolean isPrint) { isPrintForm = isPrint ; }
  public boolean isPrintForm() { return isPrintForm ; }
  public void setPrintDetail(boolean isDetail) { isPrintDetail = isDetail ; }
  
  public boolean isDisplaySearchResult() {
  if (!isSearchResult) {
    getAncestorOfType(UIContactPortlet.class).findFirstComponentOfType(UISearchForm.class)
      .getChild(UIFormStringInput.class).setValue(null) ;
  }
  return isSearchResult ;  
  }
  public void setDisplaySearchResult(boolean search) { isSearchResult = search ; }
  public void setViewListBeforePrint(boolean isList) { viewListBeforePrint = isList ; }
  
  public void setAscending(boolean isAsc) { isAscending_ = isAsc ; }
  public boolean isAscending() {return isAscending_ ; }
  public void setSortedBy(String s) { sortedBy_ = s ; }
  public String getSortedBy() { return sortedBy_ ; }
  public String getViewQuery() {return viewQuery_ ; }
  public void setViewQuery(String view) {viewQuery_ = view ;}
  
  public void setContacts(JCRPageList pageList) throws Exception {
    pageList_ = pageList ;
    updateList() ; 
  }
  public JCRPageList getContactPageList() { return pageList_ ; }
  
  public boolean isAscName() { return FullNameComparator.isAsc ; }
  public boolean isAscEmail() { return EmailComparator.isAsc ; }
  public boolean isAscJob() { return JobTitleComparator.isAsc ; }
  public void setDefaultNameSorted(boolean name) { defaultNameSorted = name ; }
  public boolean isNameSorted() { return defaultNameSorted ; }
  
  public void setContact(List<Contact> contacts, boolean isUpdate) throws Exception{
    if (pageList_ != null) pageList_.setContact(contacts, isUpdate) ;
  }
  public void updateList() throws Exception {
    List<String> checkedList = new ArrayList<String>() ;
    for (String contactId : contactMap.keySet()) {
      UIFormCheckBoxInput uiCheckBox = getChildById(contactId) ;
      if(uiCheckBox != null && uiCheckBox.isChecked()) {
        checkedList.add(contactId) ;
      } 
    }
    getChildren().clear() ;
    contactMap.clear();
    UIContactPreview contactPreview = 
      getAncestorOfType(UIContactContainer.class).getChild(UIContactPreview.class) ;    
    if(pageList_ != null) {
      List<Contact> contactList = pageList_.getPage(pageList_.getCurrentPage(),ContactUtils.getCurrentUser()) ;
      if(contactList.size() == 0 && pageList_.getCurrentPage() > 1) {
        contactList = pageList_.getPage(pageList_.getCurrentPage() - 1,ContactUtils.getCurrentUser()) ;
      }
      for(Contact contact : contactList) {
        UIFormCheckBoxInput<Boolean> checkbox = new UIFormCheckBoxInput<Boolean>(contact.getId(),contact.getId(), false) ;
        if(checkedList.contains(contact.getId())) {
          checkbox.setChecked(true) ;
        }
        addUIFormInput(checkbox);
        contactMap.put(contact.getId(), contact) ;
      }
      checkedAll = "checked" ;      
      if (checkedList.size() != contactMap.size()) checkedAll="" ;
      else {
        for (String id : contactMap.keySet())
          if (!checkedList.contains(id)) {
            checkedAll="" ;
            break ;
          }
      }
      Contact[] array = contactMap.values().toArray(new Contact[]{}) ;
      if (array.length > 0) {
        //cs-1823
        if (!ContactUtils.isEmpty(selectedContact) && contactMap.containsKey(selectedContact)) {
          contactPreview.setContact(contactMap.get(selectedContact)) ;
        } else {
          Contact firstContact = array[0] ;
          contactPreview.setContact(firstContact) ;
          selectedContact = firstContact.getId() ;          
        }
      } else contactPreview.setContact(null) ;
    } else contactPreview.setContact(null) ;
  }
  
  public Contact[] getContacts() throws Exception {
    return contactMap.values().toArray(new Contact[]{}) ;
  }
  public LinkedHashMap<String, Contact> getContactMap() { return contactMap ;}
  public void setContactMap(LinkedHashMap<String, Contact> map) { contactMap = map ; }
  
  public void setSelectedContact(String s) { selectedContact = s ; }
  public String getSelectedContact() { return selectedContact ; }
  
  public void setSelectedGroup(String s) { selectedGroup = s ; }
  public String getSelectedGroup() { return selectedGroup ; }
  
  public void setViewContactsList(boolean list) { viewContactsList = list ; }
  public boolean getViewContactsList() {
    if (viewContactsList) {
      getAncestorOfType(UIContactContainer.class).getChild(UIContactPreview.class).setRendered(true) ;
    } else {
      getAncestorOfType(UIContactContainer.class).getChild(UIContactPreview.class).setRendered(false) ;
    }
    return viewContactsList ; 
  }
  
  public List<String> getCheckedContacts() throws Exception {
    List<String> checkedContacts = new ArrayList<String>() ;
    for (String contactId : contactMap.keySet()) {
      UIFormCheckBoxInput uiCheckBox = getChildById(contactId) ;
      if(uiCheckBox != null && uiCheckBox.isChecked()) {
        checkedContacts.add(contactId) ;
      } 
    }
    return checkedContacts ;
  }
  
  // remove
  public DownloadService getDownloadService() {
    return getApplicationComponent(DownloadService.class) ; 
  }
  
  public String getPortalName() {
    PortalContainer pcontainer =  PortalContainer.getInstance() ;
    return pcontainer.getPortalContainerInfo().getContainerName() ;  
  }
  public String getRepository() throws Exception {
    RepositoryService rService = getApplicationComponent(RepositoryService.class) ;    
    return rService.getCurrentRepository().getConfiguration().getName() ;
  }
  /*
  public void setPageList(JCRPageList pageList, long page) throws Exception {
    getChildren().clear();
    pageList_ = pageList ;
    for (Contact contact : pageList.getPage(page, ContactUtils.getCurrentUser())) {
      addUIFormInput(new UIFormCheckBoxInput<Boolean>(contact.getId(),contact.getId(), false)) ;
    }
  }
  */
  public String getSelectedTag() {return selectedTag_ ;}
  public void setSelectedTag(String tagId) {selectedTag_ = tagId ;}
  
  public Map<String, Tag> getTagMap() {
    return getAncestorOfType(UIWorkingContainer.class)
      .findFirstComponentOfType(UITags.class).getTagMap() ;
  }
  
  public Map<String, String> getPrivateGroupMap() {
    return getAncestorOfType(UIWorkingContainer.class)
      .findFirstComponentOfType(UIAddressBooks.class).getPrivateGroupMap() ;
  }
  public Map<String, SharedAddressBook> getSharedGroupMap() throws Exception {
    return getAncestorOfType(UIWorkingContainer.class)
      .findFirstComponentOfType(UIAddressBooks.class).getSharedGroups() ;
  }
  public List<String> getPublicContactGroups() throws Exception {
    return Arrays.asList(ContactUtils.getUserGroups().toArray(new String[] {})) ;
  }  
  public String getDefaultGroup() { return NewUserListener.DEFAULTGROUP ;}

  public String getSelectedTagBeforeSearch_() { return selectedTagBeforeSearch_; }
  public void setSelectedTagBeforeSearch_(String selectedTagBeforeSearch_) {
    this.selectedTagBeforeSearch_ = selectedTagBeforeSearch_;
  }

  public String getSelectedGroupBeforeSearch() { return selectedGroupBeforeSearch; }
  public void setSelectedGroupBeforeSearch(String selectedGroupBeforeSearch) {
    this.selectedGroupBeforeSearch = selectedGroupBeforeSearch;
  }

  public boolean isSelectSharedContactsBeforeSearch() { return isSelectSharedContactsBeforeSearch; }
  public void setSelectSharedContactsBeforeSearch(boolean isSelectSharedContactsBeforeSearch) {
    this.isSelectSharedContactsBeforeSearch = isSelectSharedContactsBeforeSearch;
  }
  
  public boolean isViewListBeforeSearch() { return viewListBeforeSearch; }
  public void setViewListBeforeSearch(boolean viewListBeforeSearch) {
    this.viewListBeforeSearch = viewListBeforeSearch;
  }
  
  static public class EditContactActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      Contact contact = uiContacts.contactMap.get(contactId) ;
      if (contact == null) {
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null, ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
        return ;
      }
      ContactService service = ContactUtils.getContactService() ;
      String username = ContactUtils.getCurrentUser() ;
      if (contact.getContactType().equalsIgnoreCase(JCRDataStorage.PRIVATE)) {
        try {
          contact = service.getContact(SessionProviderFactory.createSessionProvider(), username, contactId) ;
        } catch (NullPointerException e) {
          contact = null ;
        }        
      } else  {// shared
        try {
          contact = service.getSharedContact(SessionProviderFactory.createSessionProvider(), username, contactId) ;          
        } catch  (NullPointerException e) {
          contact = null ;
        }
        if (contact == null) {
          try {
            contact = service.getSharedContactAddressBook(username, contactId) ;            
          } catch (NullPointerException e) { }          
        }
      }
      if (contact == null) {
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
        return ;
      }
     
      
//    avoid cache id of edited old contact
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      UIPopupContainer popupContainer =  popupAction.activate(UIPopupContainer.class, 800) ;
      popupContainer.setId("AddNewContact");
      
      UICategorySelect uiCategorySelect = popupContainer.addChild(UICategorySelect.class, null, null) ;
      UIAddressBooks uiAddressBooks = uiContacts.getAncestorOfType(UIWorkingContainer.class).findFirstComponentOfType(UIAddressBooks.class) ;
      Map<String, String> privateGroups = uiAddressBooks.getPrivateGroupMap() ;
      Map<String, SharedAddressBook> sharedAddress = uiAddressBooks.getSharedGroups() ;
      List<SelectItemOption<String>> categories = new ArrayList<SelectItemOption<String>>() ;
      for (String add : contact.getAddressBook()) {
        if (privateGroups.containsKey(add)) {
          categories.add(new SelectItemOption<String>(ContactUtils.encodeHTML(privateGroups.get(add)), add)) ;  
          continue ;
        } else if (sharedAddress.containsKey(add)) {
          categories.add(new SelectItemOption<String>(ContactUtils.encodeHTML(ContactUtils
              .getDisplayAdddressShared(sharedAddress.get(add).getSharedUserId(), sharedAddress.get(add).getName())), add)) ;
          continue ;
        }
      }
      //cs-1899
      if (categories.size() == 0) {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
        ResourceBundle res = context.getApplicationResourceBundle() ;
        String sharedLabel = "Shared Contact" ;
        try {
          sharedLabel = res.getString("UIContacts.label.sharedContacts");        
        } catch (MissingResourceException e) {      
          e.printStackTrace() ;
        }
        categories.add(new SelectItemOption<String>(sharedLabel, sharedLabel)) ;
      }

      UIFormInputWithActions input = new UIFormInputWithActions(UICategorySelect.INPUT_CATEGORY) ;
      UIFormSelectBox uiSelectBox = new UIFormSelectBox(UICategorySelect.FIELD_CATEGORY, UICategorySelect.FIELD_CATEGORY, categories) ;
      uiSelectBox.setEnable(false) ;
      input.addUIFormInput(uiSelectBox) ;
      uiCategorySelect.addUIFormInput(input) ;
      UIContactForm uiContactForm = popupContainer.addChild(UIContactForm.class, null, null) ;
      uiContactForm.setValues(contact);
      uiContactForm.setNew(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;      
    }
  }
  
  static public class TagActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      List<String> contactIds = new ArrayList<String>();
      if (!ContactUtils.isEmpty(contactId) && !contactId.equals("null")) {
        contactIds.add(contactId) ;
      } else {
        contactIds = uiContacts.getCheckedContacts() ;
        if (contactIds.size() == 0 ) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-toTag", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }    
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      UITagForm uiTagForm = popupAction.activate(UITagForm.class, 600) ;
      List<Contact> contacts = new ArrayList<Contact>() ;      
      for (String id : contactIds) {
        contacts.add(uiContacts.contactMap.get(id)) ;
      }
      uiTagForm.setContacts(contacts) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  
  static public class DNDContactsToTagActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      uiContacts.getAncestorOfType(UIContactPortlet.class).cancelAction() ;
      String tagId = event.getRequestContext().getRequestParameter(OBJECTID);   
      @SuppressWarnings("unused")
      String type = event.getRequestContext().getRequestParameter("contactType");
      List<String> contactIds = new ArrayList<String>();
      @SuppressWarnings("unused")
      UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
      contactIds = uiContacts.getCheckedContacts() ;
      List<String> newContactIds = new ArrayList<String>();
      for (String contactId : contactIds) {
        Contact contact = uiContacts.contactMap.get(contactId) ;
        newContactIds.add(contactId + JCRDataStorage.SPLIT + contact.getContactType()) ;
      }
      try {
        ContactUtils.getContactService().addTag(SessionProviderFactory
            .createSessionProvider(), ContactUtils.getCurrentUser(), newContactIds, tagId);
      } catch (PathNotFoundException e) {
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
        return ;
      }
      
      // comment to fix bug cs- 1475
      // when select shared contacts 
     // if(ContactUtils.isEmpty(uiContacts.selectedGroup) && ContactUtils.isEmpty(uiContacts.selectedTag_)) {
        //List<Contact> contacts = new ArrayList<Contact>() ;
        for (String contactId : contactIds) {
          Contact contact = uiContacts.contactMap.get(contactId) ;
          String[] tags = contact.getTags() ;
          if (tags != null && tags.length > 0) {
            Map<String, String> newTags = new LinkedHashMap<String, String>() ;
            for (String tag : tags) newTags.put(tag, tag) ;
            newTags.put(tagId, tagId) ;
            contact.setTags(newTags.keySet().toArray(new String[] {})) ;
          }
          else {
            contact.setTags(new String[] {tagId}) ;
          }
          //contacts.add(contact) ;
        }
        //uiContacts.setContact(contacts, true) ;
     // }
     // uiContacts.updateList() ; 
        
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts) ;
    }
  }
  
  static public class MoveContactsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      List<String> contactIds = new ArrayList<String>();
      UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
      UIContactPortlet uiContactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIAddressBooks addressBooks = uiContactPortlet.findFirstComponentOfType(UIAddressBooks.class) ;
      
      Map<String, Contact> movedContacts = new HashMap<String, Contact>() ;
      if (!ContactUtils.isEmpty(contactId) && !contactId.equals("null")) {
        contactIds.add(contactId) ;
        movedContacts.put(contactId, uiContacts.contactMap.get(contactId)) ;
      } else {
        contactIds = uiContacts.getCheckedContacts() ;
        if (contactIds.size() == 0) {          
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-toMove", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        for (String id : contactIds) {
          Contact contact = uiContacts.contactMap.get(id) ;         
          if (contact.getContactType().equals(JCRDataStorage.PUBLIC)) {
            uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move", null
                , ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          } else if (contact.getContactType().equals(JCRDataStorage.SHARED)&& uiContacts.isSharedAddress(contact)) {
            if (contact.isOwner()) {
              uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move", null
                  , ApplicationMessage.WARNING)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
              return ;
            } else {
              String groupId = null ;
              for (String add : contact.getAddressBook())
                if (addressBooks.getSharedGroups().containsKey(add)) groupId = add ;                    
              if (groupId != null && !addressBooks.havePermission(groupId)) {
                uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move", null
                    , ApplicationMessage.WARNING)) ;
                event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
                return ;
              }            
            }    
          } else if (contact.getId().equals(ContactUtils.getCurrentUser())) {
            uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move-ownerContact", null
                , ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          movedContacts.put(id, contact) ;
        }
      }  
      
      
      UIPopupAction popupAction = uiContactPortlet.getChild(UIPopupAction.class) ;
      UIMoveContactsForm uiMoveForm = popupAction.activate(UIMoveContactsForm.class, 540) ;
      uiMoveForm.setContacts(movedContacts) ;
      uiMoveForm.setPrivateGroupMap(addressBooks.getPrivateGroupMap()) ;
      uiMoveForm.setSharedGroupMap(addressBooks.getSharedGroups()) ;
      //event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;     
    }
  }
  
  static public class DNDContactsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      uiContacts.getAncestorOfType(UIContactPortlet.class).cancelAction() ;
      String addressBookId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIAddressBooks uiAddressBooks = uiContacts.getAncestorOfType(
          UIWorkingContainer.class).findFirstComponentOfType(UIAddressBooks.class) ; 
      UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
      ContactService contactService = ContactUtils.getContactService() ;
      String username = ContactUtils.getCurrentUser() ;
      SessionProvider sessionProvider = SessionProviderFactory.createSessionProvider() ;

      if (uiAddressBooks.getSharedGroups().containsKey(addressBookId)) {
        ContactGroup group = contactService.getSharedGroup(username, addressBookId) ;
        if (group.getEditPermissionUsers() == null || 
            !Arrays.asList(group.getEditPermissionUsers()).contains(username + JCRDataStorage.HYPHEN)) {
          boolean canEdit = false ;
          String[] editPerGroups = group.getEditPermissionGroups() ;
          if (editPerGroups != null)
            for (String editPer : editPerGroups)
              if (ContactUtils.getUserGroups().contains(editPer)) canEdit = true ;          
          if (canEdit == false) {
            uiApp.addMessage(new ApplicationMessage("UIContacts.msg.non-permission", null
                , ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
            return ;
          }
        }
      }
      String type = event.getRequestContext().getRequestParameter("addressType");
      List<String> contactIds = uiContacts.getCheckedContacts() ;
      List<Contact> contacts = new ArrayList<Contact>();
      List<Contact> sharedContacts = new ArrayList<Contact>();
      Map<String, String> copySharedContacts = new LinkedHashMap<String, String>() ;
      for(String contactId : contactIds) {
        Contact contact = uiContacts.contactMap.get(contactId) ;
        if (contact.getId().equals(ContactUtils.getCurrentUser())){ 
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move-ownerContact", null
              , ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
          return ;          
        } if (contact.getContactType().equals(JCRDataStorage.PUBLIC)) {
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move", null
              , ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
          return ;
        } else if (contact.getContactType().equals(JCRDataStorage.SHARED)&& uiContacts.isSharedAddress(contact)) {
          if (contact.isOwner()) {
            uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move", null
                , ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
            return ;
          } else {
            String groupId = null ;
            for (String add : contact.getAddressBook())
              if (uiAddressBooks.getSharedGroups().containsKey(add)) groupId = add ;                    
            if (groupId != null && !uiAddressBooks.havePermission(groupId)) {
              uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move", null
                  , ApplicationMessage.WARNING)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
              return ;
            }            
          }    
        }
      }
      
//    cs- 1630
      Map<String, String> copyedContacts = uiAddressBooks.getCopyContacts() ;      
      for(String contactId : contactIds) {
        Contact contact = uiContacts.contactMap.get(contactId) ;
        if (!contact.getAddressBook()[0].equals(addressBookId)) copyedContacts.remove(contactId) ;  
        if (contact.getContactType().equals(JCRDataStorage.SHARED)) {
//        check for existing contact
          Contact tempContact = null ;
          if (uiContacts.isSharedAddress(contact)) {
            tempContact = contactService.getSharedContactAddressBook(username, contactId) ;
          } else {
            try {
              tempContact = contactService.getSharedContact(SessionProviderFactory.createSystemProvider(), username, contactId) ;              
            } catch (PathNotFoundException e) { }
          }
          if (tempContact == null) {
            uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-not-existed", null
                                                    , ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
            return ;
          }
          sharedContacts.add(contact) ; 
          copySharedContacts.put(contactId, JCRDataStorage.SHARED) ;
        }
        else {
          contact.setAddressBook(new String[] { addressBookId }) ;
          contacts.add(contact) ;   
        }  
      }
      List<Contact> pastedContact = new ArrayList<Contact>() ;
      
      if (sharedContacts.size() > 0 ) {
        pastedContact = contactService.pasteContacts(sessionProvider, username, addressBookId, type, copySharedContacts) ;
        for (Contact contact : sharedContacts) {
        
        if (uiContacts.isSharedAddress(contact)) {
          String addressId = null ;
          for (String add : contact.getAddressBook())
            if (uiContacts.getSharedGroupMap().containsKey(add)) addressId = add ;
          /*
          // add to fix bug cs-1509
          if (contact.getAttachment() != null) {
            contact.getAttachment().setInputStream(contact.getAttachment().getInputStream()) ;
          }  */          
          contactService.removeSharedContact(SessionProviderFactory.createSystemProvider(), username, addressId, contact.getId()) ;
        } else {
          //try {
            contactService.removeUserShareContact(
                SessionProviderFactory.createSystemProvider(), contact.getPath(), contact.getId(), username) ;              
          /*} catch (PathNotFoundException e) { 
            uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-not-existed", null, 
                ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ; 
          }*/
        }
        contact.setAddressBook(new String[] { addressBookId }) ;
       }      
      }
      if (contacts.size() > 0) {
        try {
          contactService.moveContacts(sessionProvider, username, contacts, type);           
        } catch (PathNotFoundException e) {
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null, 
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
          return ;          
        }
      }

      // update addressbook when search
      if (uiContacts.isSearchResult) {
        for (String contactId : contactIds) {
          Contact contact = uiContacts.contactMap.get(contactId) ;
          contact.setContactType(type) ;
          contact.setViewPermissionUsers(null) ;
          contact.setViewPermissionGroups(null) ;          
        }
        //cs-2157 
        if (pastedContact.size() > 0) {
          uiContacts.setContact(sharedContacts, false) ;
          uiContacts.pageList_.getAll().addAll(pastedContact) ;
        }        
      } else if (uiContacts.isSelectSharedContacts  && !ContactUtils.isEmpty(addressBookId)) { //select shared contacts        
        if (contacts.size() > 0) uiContacts.setContact(contacts, false) ;
        if (sharedContacts.size() > 0) uiContacts.setContact(sharedContacts, false) ;
      }
      uiContacts.updateList() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddressBooks) ;
    }
  }
  
  static public class CopyContactActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      List<String> contactIds = new ArrayList<String>() ; 
      if (!ContactUtils.isEmpty(contactId)) {
        contactIds.add(contactId) ;
      } else {
        contactIds =  uiContacts.getCheckedContacts() ;
        if (contactIds.size() == 0) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-toCopy", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;        
        }
      }
      UIAddressBooks uiAddressBooks = uiContacts.getAncestorOfType(
          UIWorkingContainer.class).findFirstComponentOfType(UIAddressBooks.class) ;     
      uiAddressBooks.setCopyAddress(null) ;
      Map<String, String> copyContacts = new LinkedHashMap<String, String>();
      for (String id : contactIds) {
        Contact contact = uiContacts.contactMap.get(id) ;
        if (contact == null) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null, ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
          return ; 
        }
        copyContacts.put(id, contact.getContactType()) ;
      }
      uiAddressBooks.setCopyContacts(copyContacts) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddressBooks) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
    }
  }
  static public class DeleteContactsActionListener extends EventListener<UIContacts> {
    @SuppressWarnings("unchecked")
  public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      List<String> contactIds = new ArrayList<String>();
      UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
      UIWorkingContainer uiWorkingContainer = uiContacts.getAncestorOfType(UIWorkingContainer.class) ;
      uiWorkingContainer.getAncestorOfType(UIContactPortlet.class).cancelAction() ;
      UIAddressBooks addressBooks = uiWorkingContainer.findFirstComponentOfType(UIAddressBooks.class) ;
      if (!ContactUtils.isEmpty(contactId) && !contactId.toString().equals("null")) {
        contactIds.add(contactId) ;
      } else {
        contactIds = uiContacts.getCheckedContacts() ;
        if (contactIds.size() == 0) {        
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-toDelete", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
          return ;
        }
      }
      for (String id : contactIds) {
        Contact contact = uiContacts.contactMap.get(id) ;
        if (contact.getId().equals(ContactUtils.getCurrentUser())) {
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-delete-ownerContact", null
              , ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        } else if (contact.getContactType().equals(JCRDataStorage.PUBLIC)) {
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-delete", null
              , ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        } else if (contact.getContactType().equals(JCRDataStorage.SHARED)&& uiContacts.isSharedAddress(contact)) {
          if (contact.isOwner()) {
            uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-delete", null
                , ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          } else {
            String groupId = null ;
            for (String add : contact.getAddressBook())
              if (addressBooks.getSharedGroups().containsKey(add)) groupId = add ;                    
            if (groupId != null && !addressBooks.havePermission(groupId)) {
              uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-delete", null
                  , ApplicationMessage.WARNING)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
              return ;
            }            
          } 
        }
      }
      
      ContactService contactService = ContactUtils.getContactService() ;
      String username = ContactUtils.getCurrentUser() ;
      List <Contact> removedContacts = new ArrayList<Contact>() ;
//    cs- 1630
      Map<String, String> copyedContacts = addressBooks.getCopyContacts() ;
      
      // remove shared contacts
      for (String id : contactIds) {
        copyedContacts.remove(id) ;
        Contact contact = uiContacts.contactMap.get(id) ;    
        if (contact.getContactType().equals(JCRDataStorage.SHARED)) {
          if (uiContacts.isSharedAddress(contact)) {
            String addressBookId = null ;
            for (String add : contact.getAddressBook())
              if (uiContacts.getSharedGroupMap().containsKey(add)) addressBookId = add ;
            try { 
              contactService.removeSharedContact(
                  SessionProviderFactory.createSystemProvider(), username, addressBookId, id) ; 
            } catch (PathNotFoundException e) { }
          } else {
            try {
              String[] tags = contact.getTags() ;
              if (tags != null && tags.length > 0) {
                Set<String> tagsMap = uiWorkingContainer.findFirstComponentOfType(UITags.class).getTagMap().keySet() ;
                List<String> tagsList = new ArrayList<String>() ;
                tagsList.addAll(Arrays.asList(tags)) ;
                for (String tagId : tags) {
                  if (tagsMap.contains(tagId)) {
                    tagsList.remove(tagId) ;
                    contact.setTags(tagsList.toArray(new String[] {})) ;
                    contactService.saveSharedContact(username, contact) ;
                  }                  
                }
              }              
              contactService.removeUserShareContact(
                  SessionProviderFactory.createSystemProvider(), contact.getPath(), id, username) ;
            } catch (PathNotFoundException e) {
              uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-not-existed", null
                  , ApplicationMessage.WARNING)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
              return ;
            }            
          }
          removedContacts.add(contact) ;          
        }
      }
      if (!uiContacts.isSelectSharedContacts) {
        removedContacts.addAll(contactService.removeContacts(SessionProviderFactory.createSessionProvider(), username, contactIds)) ;          
      }      
      if (ContactUtils.isEmpty(uiContacts.selectedGroup) && ContactUtils.isEmpty(uiContacts.selectedTag_)) {
        uiContacts.setContact(removedContacts, false) ;
      }
      if (uiContacts.getSelectedTag() != null) {
        String tagId = uiWorkingContainer.findFirstComponentOfType(UITags.class).getSelectedTag() ;
          DataPageList pageList = contactService
            .getContactPageListByTag(SessionProviderFactory.createSystemProvider(), username, tagId) ;
          if (pageList != null) {
            List<Contact> contacts = new ArrayList<Contact>() ;
            contacts = pageList.getAll() ;
            if (uiContacts.getSortedBy().equals(UIContacts.fullName)) {
              Collections.sort(contacts, new FullNameComparator()) ;
            } else if (uiContacts.getSortedBy().equals(UIContacts.emailAddress)) {
              Collections.sort(contacts, new EmailComparator()) ;
            } else if (uiContacts.getSortedBy().equals(UIContacts.jobTitle)) {
              Collections.sort(contacts, new JobTitleComparator()) ;
            }  
            pageList.setList(contacts) ;
          }
          uiContacts.setContacts(pageList) ;
      } else {
        uiContacts.updateList() ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  } 
  
  static public class ExportContactActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      uiContacts.getAncestorOfType(UIContactPortlet.class).cancelAction() ;
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);      
      String username = ContactUtils.getCurrentUser() ;
      ContactService contactService = ContactUtils.getContactService() ;
      List<Contact> contacts = new ArrayList<Contact>() ;
      Contact contact = uiContacts.contactMap.get(contactId) ;
      contacts.add(contact) ;
      OutputStream out = contactService.getContactImportExports(contactService.getImportExportType()[0]).exportContact(username, contacts) ;
      String contentType = null;
      contentType = "text/x-vcard";
      ByteArrayInputStream is = new ByteArrayInputStream(out.toString().getBytes()) ;
      DownloadResource dresource = new InputStreamDownloadResource(is, contentType) ;
      DownloadService dservice = (DownloadService)PortalContainer.getInstance().getComponentInstanceOfType(DownloadService.class) ;
      dresource.setDownloadName(contact.getFullName() + ".vcf");
      String downloadLink = dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
      event.getRequestContext().getJavascriptManager()
        .addJavascript("ajaxRedirect('" + downloadLink + "');") ;
      /*
      UIContactPortlet uiContactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class);
      UIPopupAction uiPopupAction = uiContactPortlet.getChild(UIPopupAction.class);
      UIExportForm uiExportForm = uiPopupAction.activate(UIExportForm.class, 500) ;
      uiExportForm.setId("ExportForm");
      Contact contact = uiContacts.contactMap.get(contactId) ;
      List<ContactData> data = new ArrayList<ContactData>() ;
      ContactData contactData = uiExportForm.new ContactData(contact.getId(), contact.getFullName(), contact.getEmailAddress()) ;
      data.add(contactData) ;
      
      Map<String, Contact> contactMap = new HashMap<String, Contact>() ;
      contactMap.put(contact.getId(), contact) ;
      uiExportForm.setContacts(contactMap) ;
      uiExportForm.setContactList(data);
      event.getRequestContext()
        .addUIComponentToUpdateByAjax(uiContactPortlet.findFirstComponentOfType(UIContactContainer.class));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);*/
    }
  }
  
  static public class SelectedContactActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIContactContainer uiContactContainer = uiContacts.getAncestorOfType(UIContactContainer.class);
      Contact oldContact = uiContacts.contactMap.get(contactId) ;
      Contact newContact = null ;
      ContactService service = ContactUtils.getContactService() ;
      String username = ContactUtils.getCurrentUser() ;      
      if (oldContact.getContactType().equals(JCRDataStorage.PRIVATE)) {
        newContact = service.getContact(SessionProviderFactory.createSessionProvider(), username, contactId) ;
      } else if(oldContact.getContactType().equals(JCRDataStorage.SHARED)) {
        newContact = service.getSharedContactAddressBook( username, contactId) ;
        if (newContact == null) newContact = service
          .getSharedContact(SessionProviderFactory.createSystemProvider(), username, contactId) ;
      } else {
        newContact = service.getPublicContact(contactId) ;
      }
      if (newContact == null) {
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null
            , ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        List<Contact> contacts = new ArrayList<Contact>() ;
        contacts.add(oldContact) ;
        uiContacts.setContact(contacts, false) ;
        uiContacts.updateList() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContactContainer) ;
        return ;
      }
      uiContacts.contactMap.put(contactId, newContact) ;
      uiContacts.setSelectedContact(contactId) ;
      UIContactPreview uiContactPreview = uiContactContainer.findFirstComponentOfType(UIContactPreview.class);
      uiContactPreview.setContact(newContact);
      uiContactPreview.setRendered(true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContactContainer);   
    }
  }
  
  static public class ViewDetailsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      // cs-1278
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;      
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      UIPopupContainer uiPopupContainer = popupAction.activate(UIPopupContainer.class, 700) ;
      uiPopupContainer.setId("ContactDetails") ;  
      UIContactPreviewForm uiContactPreviewForm = uiPopupContainer.addChild(UIContactPreviewForm.class, null, null) ; 
      uiContactPreviewForm.setPrintForm(false) ;
      uiContactPreviewForm.setContact(uiContacts.contactMap.get(contactId)) ; 
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  
  static public class PrintDetailsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      uiContacts.isPrintDetail = true ;
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIContactPreviewForm uiPreviewForm = contactPortlet.createUIComponent(UIContactPreviewForm.class, null, null) ;
      uiPreviewForm.setId("ContactDetails") ;
      uiPreviewForm.setPrintForm(true) ;
      uiPreviewForm.setContact(uiContacts.contactMap.get(contactId)) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      popupAction.activate(uiPreviewForm, 700, 0) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  static public class FirstPageActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ; 
      JCRPageList pageList = uiContacts.getContactPageList();
      if (pageList != null) {
        //uiContacts.setPageList(pageList, 1) ;
        pageList.setCurrentPage(1)  ;
        uiContacts.updateList() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      }
    }
  }
  
  static public class PreviousPageActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ; 
      JCRPageList pageList = uiContacts.getContactPageList(); 
      if (pageList != null && pageList.getCurrentPage() > 1){
        pageList.setCurrentPage(pageList.getCurrentPage() - 1);
        uiContacts.updateList() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      }      
    }
  }
  
  static public class NextPageActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ; 
      JCRPageList pageList = uiContacts.getContactPageList() ; 
      if (pageList != null && pageList.getCurrentPage() < pageList.getAvailablePage()){
        //uiContacts.setPageList(pageList, pageList.getCurrentPage() + 1);
        pageList.setCurrentPage(pageList.getCurrentPage() + 1) ;
        uiContacts.updateList() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      }      
    }
  }
  
  static public class LastPageActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ; 
      JCRPageList pageList = uiContacts.getContactPageList(); 
      if (pageList != null) {
        pageList.setCurrentPage(pageList.getAvailablePage());
        uiContacts.updateList() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      }      
    }
  }
  
  static public class SortActionListener extends EventListener<UIContacts> {
    @SuppressWarnings("unchecked")
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String sortedBy = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiContacts.setAscending(!uiContacts.isAscending_);
      uiContacts.setSortedBy(sortedBy);
      uiContacts.setDefaultNameSorted(false) ;
      
      JCRPageList pageList = null ;
      String group = uiContacts.selectedGroup ;
      if (!ContactUtils.isEmpty(group)) {
        ContactFilter filter = new ContactFilter() ;
        filter.setViewQuery(uiContacts.getViewQuery());        
        filter.setAscending(uiContacts.isAscending_);
        filter.setOrderBy(sortedBy);
        filter.setCategories(new String[] { group } ) ;

        String type = null;
        UIAddressBooks addressBooks = uiContacts.getAncestorOfType(
            UIWorkingContainer.class).findFirstComponentOfType(UIAddressBooks.class) ;
        if (addressBooks.getPrivateGroupMap().containsKey(group)) type = JCRDataStorage.PRIVATE ;
        else if (addressBooks.getSharedGroups().containsKey(group)) type = JCRDataStorage.SHARED ;
        else type = JCRDataStorage.PUBLIC ;
        
        //else if (addressBooks.getPublicGroupMap().containsKey(group)) type = JCRDataStorage.PUBLIC ;
        
        if(type != null)
          pageList = ContactUtils.getContactService().getContactPageListByGroup(
            SessionProviderFactory.createSystemProvider(),ContactUtils.getCurrentUser(), filter, type) ;
        
      } else {      //selected group = null ;
          pageList = uiContacts.pageList_ ;
          if (pageList != null) {
            List<Contact> contacts = new ArrayList<Contact>() ;
            contacts = pageList.getAll() ;
            if (uiContacts.getSortedBy().equals(UIContacts.fullName)) {
              FullNameComparator.isAsc = (!FullNameComparator.isAsc) ;
              Collections.sort(contacts, new FullNameComparator()) ;
            } else if (uiContacts.getSortedBy().equals(UIContacts.emailAddress)) {
              EmailComparator.isAsc = (!EmailComparator.isAsc) ;
              Collections.sort(contacts, new EmailComparator()) ;
            } else if (uiContacts.getSortedBy().equals(UIContacts.jobTitle)) {
              JobTitleComparator.isAsc = (!JobTitleComparator.isAsc) ;
              Collections.sort(contacts, new JobTitleComparator()) ;
            }  
            pageList.setList(contacts) ;
          }
      }
      uiContacts.setContacts(pageList) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
    }
  }
  
  static public class FullNameComparator implements Comparator {
    public static boolean isAsc ;
    public int compare(Object o1, Object o2) throws ClassCastException {
      String name1 = ((Contact) o1).getFullName() ;
      String name2 = ((Contact) o2).getFullName() ;
      if (isAsc == true) return name1.compareTo(name2) ;
      else return name2.compareTo(name1) ;
    }
  }
  static public class EmailComparator implements Comparator {
    private static boolean isAsc ;
    public int compare(Object o1, Object o2) throws ClassCastException {
      String email1 = ((Contact) o1).getEmailAddress() ;
      String email2 = ((Contact) o2).getEmailAddress() ;
      if (ContactUtils.isEmpty(email1) || ContactUtils.isEmpty(email2)) return 0 ;
      if (isAsc == true) return email1.compareTo(email2) ;
      else return email2.compareTo(email1) ;
    }
  }
  static public class JobTitleComparator implements Comparator {
    private static boolean isAsc ;
    public int compare(Object o1, Object o2) throws ClassCastException {
      String job1 = ((Contact) o1).getJobTitle() ;
      String job2 = ((Contact) o2).getJobTitle() ;
      if (ContactUtils.isEmpty(job1) || ContactUtils.isEmpty(job2)) return 0 ;
      if (isAsc == true) return job1.compareTo(job2) ;
      else return job2.compareTo(job1) ;
    }
  }
  
  static public class CloseSearchActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      uiContacts.setDisplaySearchResult(false) ;
      uiContacts.setSelectedGroup(uiContacts.selectedGroupBeforeSearch) ;
      uiContacts.setSelectedTag(uiContacts.selectedTagBeforeSearch_) ;
      uiContacts.setSelectSharedContacts(uiContacts.isSelectSharedContactsBeforeSearch) ;
      uiContacts.setViewContactsList(uiContacts.viewListBeforeSearch) ;
      uiContacts.refreshData() ;
      if (ContactUtils.isEmpty(uiContacts.selectedGroup) && ContactUtils.isEmpty(uiContacts.selectedTag_)
          && !uiContacts.isSelectSharedContacts) uiContacts.setContacts(null) ;
      UIWorkingContainer uiWorkingContainer = uiContacts.getAncestorOfType(UIWorkingContainer.class) ;
      uiWorkingContainer.findFirstComponentOfType(UIAddressBooks.class).setSelectedGroup(uiContacts.selectedGroup) ;
      uiWorkingContainer.findFirstComponentOfType(UITags.class).setSelectedTag(uiContacts.selectedTag_) ;
      UISearchForm uiSearchForm = uiContacts.getAncestorOfType(
              UIContactPortlet.class).findFirstComponentOfType(UISearchForm.class) ;
          uiSearchForm .getChild(UIFormStringInput.class).setValue(null) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchForm) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  }
  
  static public class SelectTagActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String tagId = event.getRequestContext().getRequestParameter(OBJECTID) ; 
      UIWorkingContainer uiWorkingContainer = uiContacts.getAncestorOfType(UIWorkingContainer.class) ;
      uiWorkingContainer.findFirstComponentOfType(UIAddressBooks.class).setSelectedGroup(null) ;
      UITags tags = uiWorkingContainer.findFirstComponentOfType(UITags.class) ;
      tags.setSelectedTag(tagId) ;
      uiContacts.setContacts(ContactUtils.getContactService()
        .getContactPageListByTag(SessionProviderFactory.createSystemProvider(), ContactUtils.getCurrentUser(), tagId)) ;
      uiContacts.setSelectedGroup(null) ;
      uiContacts.setSelectedTag(tagId) ;
      uiContacts.setDisplaySearchResult(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  }

  static public class SendEmailActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      String emails = null ;
      
      if (!ContactUtils.isEmpty(objectId)) {
        if (uiContacts.contactMap.containsKey(objectId))
          emails = uiContacts.contactMap.get(objectId).getEmailAddress() ;
        else emails = objectId ;
      } else {
        List<String> contactIds = uiContacts.getCheckedContacts() ;
        if (contactIds.size() < 1) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-toSendMail", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        StringBuffer buffer = new StringBuffer() ;
        for (String id : contactIds) {
          String email = uiContacts.contactMap.get(id).getEmailAddress() ;
          if (!ContactUtils.isEmpty(email)) {
            if (buffer.length() > 0) buffer.append(", " + email) ;
            else buffer.append(email) ;
          }
        }
        emails = buffer.toString() ; 
      }
      if (ContactUtils.isEmpty(emails)) {
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.no-email-found", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;        
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      
      List<Account> acc = ContactUtils.getAccounts() ;
      UIComposeForm uiComposeForm = popupAction.activate(UIComposeForm.class, 850) ;
      uiComposeForm.init(acc, emails) ;
     /* if (acc != null && acc.size() > 1) {
        uiComposeForm.init(acc, emails) ;
      } else {
        uiComposeForm.initPortalMail(emails) ;
      }*/
        /*UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIComposeForm.msg.invalidAcc", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;*/  
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  
  static public class SharedContactsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      Map<String, Contact> mapContacts = new LinkedHashMap<String, Contact>() ;
      for (String contactId : uiContacts.getCheckedContacts()) {
        Contact contact = uiContacts.contactMap.get(contactId) ;        
        String contactType = contact.getContactType() ; 
        if (contactType.equals(JCRDataStorage.PUBLIC) || contactType.equals(JCRDataStorage.SHARED)) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-share", null
              , ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        mapContacts.put(contactId, contact) ;
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());  
      if (!ContactUtils.isEmpty(objectId) || uiContacts.getCheckedContacts().size() == 1) {
        if (ContactUtils.isEmpty(objectId)) objectId = uiContacts.getCheckedContacts().get(0) ; 
        UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
        UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
        UIPopupContainer uiPopupContainer = popupAction.activate(UIPopupContainer.class, 400) ;
        uiPopupContainer.setId("UIPermissionContactForm") ;
        UIAddEditPermission uiAddNewEditPermission = uiPopupContainer.addChild(UIAddEditPermission.class, null, null); 
        //cs-2153 
        Contact contact = ContactUtils.getContactService().getContact
          (SessionProviderFactory.createSessionProvider(), ContactUtils.getCurrentUser(),objectId) ;
        if (contact == null) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null, ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
          return ;
        }
        uiAddNewEditPermission.initContact(contact) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;  
      } else {        
        UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
        UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
        UIPopupContainer uiPopupContainer = popupAction.activate(UIPopupContainer.class, 600) ;
        uiPopupContainer.setId("UIPermissionContactsForm") ;
        UISharedContactsForm uiSharedForm = uiPopupContainer.addChild(UISharedContactsForm.class, null, null) ;   
        uiSharedForm.init(mapContacts) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
      }
        
    }
  }
  
  static public class PrintActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      List<String> contactIds = uiContacts.getCheckedContacts() ;
      if (contactIds.size() < 1) {
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-toPrint", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      LinkedHashMap<String, Contact> contactMap = new LinkedHashMap<String, Contact> () ;
      uiContacts.setListBeforePrint(Arrays.asList(uiContacts.getContacts())) ;
      for (String contactId : contactIds) contactMap.put(contactId, uiContacts.contactMap.get(contactId)) ;
      uiContacts.contactMap = contactMap ;
      uiContacts.viewListBeforePrint = uiContacts.viewContactsList ;
      uiContacts.viewContactsList = false ;
      uiContacts.isPrintForm = true ;
      uiContacts.isPrintDetail = false ;
      uiContacts.getAncestorOfType(UIContactContainer.class).findFirstComponentOfType(UIContactPreview.class).setRendered(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
    }
  }
 /* 
  static  public class ChatActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      
      if (!ContactUtils.isEmpty(contactId)) { 
      } else {
        List<String> contactIds = uiContacts.getCheckedContacts() ;
        if (contactIds.size() < 1) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-toChat", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }
      
      try {
        Class im = java.lang.Class.forName("org.exoplatform.services.xmpp.rest.RESTXMPPService") ;
        
        System.out.println("\n\n 11:" + im.toString() );
        
        for (Constructor c : im.getConstructors()) {
          System.out.println("\n\n 22:" + c.toString());
        }
        
     
      } catch (ClassNotFoundException e) {
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.chatApp-notAvaiable", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;        
      }
      
      
    }
  }
  */
  static  public class CancelActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      uiContacts.isPrintForm = false ;
      uiContacts.viewContactsList = uiContacts.viewListBeforePrint ;
      uiContacts.getChildren().clear() ;
      uiContacts.contactMap.clear();
      UIContactPreview contactPreview = 
        uiContacts.getAncestorOfType(UIContactContainer.class).getChild(UIContactPreview.class) ;    
      if (uiContacts.listBeforePrint != null && uiContacts.listBeforePrint.size() > 0) {
        for(Contact contact : uiContacts.listBeforePrint) {
          UIFormCheckBoxInput<Boolean> checkbox = new UIFormCheckBoxInput<Boolean>(contact.getId(),contact.getId(), false) ;
          uiContacts.addUIFormInput(checkbox);
          uiContacts.contactMap.put(contact.getId(), contact) ; 
        }
        Contact[] array = uiContacts.contactMap.values().toArray(new Contact[]{}) ;
        if (array.length > 0) {
          Contact firstContact = array[0] ;
          contactPreview.setContact(firstContact) ;
          uiContacts.selectedContact = firstContact.getId() ;
        } else contactPreview.setContact(null) ;
      } else contactPreview.setContact(null) ;
      uiContacts.setListBeforePrint(new ArrayList<Contact>()) ;
      //uiContacts.updateList() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
    }
  }
  
  static  public class RefreshActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      uiContacts.refreshData() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
    }
  }
 
}
