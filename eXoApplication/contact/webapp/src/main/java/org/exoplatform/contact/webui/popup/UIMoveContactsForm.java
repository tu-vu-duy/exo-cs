/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.contact.webui.popup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.contact.ContactUtils;
import org.exoplatform.contact.SessionsUtils;
import org.exoplatform.contact.webui.UIAddressBooks;
import org.exoplatform.contact.webui.UIContactContainer;
import org.exoplatform.contact.webui.UIContactPortlet;
import org.exoplatform.contact.webui.UIContacts;
import org.exoplatform.contact.webui.UIWorkingContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputWithActions;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/contact/webui/popup/UIMoveContactsForm.gtmpl",
    events = {     
      @EventConfig(listeners = UIMoveContactsForm.CancelActionListener.class),
      @EventConfig(listeners = UIMoveContactsForm.SaveActionListener.class),
      @EventConfig(listeners = UIMoveContactsForm.SelectGroupActionListener.class)
    }
)
public class UIMoveContactsForm extends UIForm implements UIPopupComponent {
  private List<String> contactIds_ ;
  private String groupId_ ;
  private boolean isPersonal_ ;
  public static String[] FIELD_SHAREDCONTACT_BOX = null;
  public static final String INPUT_MOVE_BOX =  "move" ;
  private Map<String, String> privateGroupMap_ = new HashMap<String, String>() ;
  public UIMoveContactsForm() throws Exception { }
  
  public void setContacts(List<String> contactIds) { contactIds_ = contactIds ; }
  public List<String> getContacts() { return contactIds_ ; }
  
  public void setGroup(String groupId) { groupId_ = groupId ; }
  public String getGroup() { return groupId_ ; }
  
  public void activate() throws Exception { }
  public void deActivate() throws Exception { }
  public String[] getActions() { return new String[] {"Save", "Cancel"} ; }
  
  public boolean isPersonalAddressBookSelected() throws Exception { return isPersonal_  ; }
  public void setPersonal(boolean personal) { isPersonal_ = personal ; }
  
  public void addComponent() throws Exception {
    UIFormInputWithActions moveBox = new UIFormInputWithActions(INPUT_MOVE_BOX) ;
    String[] groups = ContactUtils.getUserGroups() ;
    FIELD_SHAREDCONTACT_BOX = new String[groups.length];
    for(int i = 0; i < groups.length; i ++) {
      FIELD_SHAREDCONTACT_BOX[i] = groups[i] ; 
      moveBox.addUIFormInput(new UIFormCheckBoxInput<Boolean>(FIELD_SHAREDCONTACT_BOX[i], FIELD_SHAREDCONTACT_BOX[i], false));
    }
    addUIFormInput(moveBox) ;
  }

  public Map<String, String> getPrivateGroupMap() { return privateGroupMap_ ; }
  public void setPrivateGroupMap(Map<String, String> map) { privateGroupMap_ = map ; }

  static  public class SelectGroupActionListener extends EventListener<UIMoveContactsForm> {
    public void execute(Event<UIMoveContactsForm> event) throws Exception {
      UIMoveContactsForm uiMoveContactForm = event.getSource() ;
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIContactPortlet uiContactPortlet = uiMoveContactForm.getAncestorOfType(UIContactPortlet.class);
      if (!uiMoveContactForm.groupId_.equals(groupId)) {  
        ContactUtils.getContactService().moveContacts(SessionsUtils.getSystemProvider()
            , ContactUtils.getCurrentUser(), uiMoveContactForm.contactIds_, new String[] { groupId }) ;
        uiContactPortlet.findFirstComponentOfType(UIContacts.class).updateList() ;
      }
      uiContactPortlet.cancelAction() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContactPortlet.getChild(UIWorkingContainer.class)) ;
    }
  }

  static  public class SaveActionListener extends EventListener<UIMoveContactsForm> {
    public void execute(Event<UIMoveContactsForm> event) throws Exception {
      UIMoveContactsForm uiMoveContactForm = event.getSource() ;
      UIContactPortlet uiContactPortlet = uiMoveContactForm.getAncestorOfType(UIContactPortlet.class) ;
      StringBuffer sharedGroups = new StringBuffer("");
      for (int i = 0; i < FIELD_SHAREDCONTACT_BOX.length; i ++) {
        if (uiMoveContactForm.getUIFormCheckBoxInput(FIELD_SHAREDCONTACT_BOX[i]).isChecked())
          sharedGroups.append(FIELD_SHAREDCONTACT_BOX[i] + ",");
      }
      if (ContactUtils.isEmpty(sharedGroups.toString())) {
        UIApplication uiApp = uiMoveContactForm.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContactForm.msg.selectSharedGroups-required", null, 
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ; 
      }
      String[] categories = sharedGroups.toString().split(",") ;
      ContactUtils.getContactService()
        .moveContacts(SessionsUtils.getSystemProvider(), ContactUtils.getCurrentUser(), uiMoveContactForm.contactIds_, categories) ;
      UIContactContainer contactContainer = uiContactPortlet.findFirstComponentOfType(UIContactContainer.class) ;
      contactContainer.getChild(UIContacts.class).updateList() ;
      uiContactPortlet.cancelAction() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(contactContainer) ; 
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContactPortlet.findFirstComponentOfType(UIAddressBooks.class)) ;
    }
  }
  
  static  public class CancelActionListener extends EventListener<UIMoveContactsForm> {
    public void execute(Event<UIMoveContactsForm> event) throws Exception {
      UIMoveContactsForm uiForm = event.getSource() ;
      UIContactPortlet contactPortlet = uiForm.getAncestorOfType(UIContactPortlet.class) ;
      contactPortlet.cancelAction() ; 
    }
  }
  
}
