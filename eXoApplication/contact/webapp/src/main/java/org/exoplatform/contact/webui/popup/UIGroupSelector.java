/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.contact.webui.popup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.UIGroupMembershipSelector;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Aug 29, 2007 11:57:56 AM 
 */
@ComponentConfigs({ 
  @ComponentConfig(
      template = "app:/templates/contact/webui/popup/UIGroupSelector.gtmpl",
      events = {
          @EventConfig(listeners = UIGroupSelector.ChangeNodeActionListener.class),
          @EventConfig(listeners = UIGroupSelector.SelectMembershipActionListener.class),
          @EventConfig(listeners = UIGroupSelector.SelectPathActionListener.class)  
      }  
  ),
  @ComponentConfig(
      type = UITree.class, id = "UITreeGroupSelector",
      template = "system:/groovy/webui/core/UITree.gtmpl",
      events = @EventConfig(listeners = UITree.ChangeNodeActionListener.class)
  ),
  @ComponentConfig(
      type = UIBreadcumbs.class, id = "BreadcumbGroupSelector",
      template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl",
      events = @EventConfig(listeners = UIBreadcumbs.SelectPathActionListener.class)
  )
})

public class UIGroupSelector extends UIGroupMembershipSelector implements UIPopupComponent, UISelectComponent {

  private UIComponent uiComponent ;
  private String type_ = null ;
  private List selectedGroup_ ;
  private String returnFieldName = null ;

  public UIGroupSelector() throws Exception {}

  public UIComponent getReturnComponent() { return uiComponent ; }
  public String getReturnField() { return returnFieldName ; }

  public void setComponent(UIComponent uicomponent, String[] initParams) {
    uiComponent = uicomponent ;
    if(initParams == null || initParams.length < 0) return ;
    for(int i = 0; i < initParams.length; i ++) {
      if(initParams[i].indexOf("returnField") > -1) {
        String[] array = initParams[i].split("=") ;
        returnFieldName = array[1] ;
        break ;
      }
      returnFieldName = initParams[0] ;
    }
  }

  @SuppressWarnings({ "unchecked", "cast" })
  public List getChildGroup() throws Exception {
    List children = new ArrayList() ;    
    OrganizationService service = getApplicationComponent(OrganizationService.class) ;
    for (Object child : service.getGroupHandler().findGroups(this.getCurrentGroup())) {
      children.add((Group)child) ;
    }
    return children ;
  }
  public boolean isSelectGroup() {return TYPE_GROUP.equals(type_);}
  public boolean isSelectUser() {return TYPE_USER.equals(type_);}
  public boolean isSelectMemberShip() {return TYPE_MEMBERSHIP.equals(type_);}
  @SuppressWarnings({ "unchecked", "cast" })
  public List<String> getList() throws Exception {
    List<String> children = new ArrayList<String>() ; 
    OrganizationService service = getApplicationComponent(OrganizationService.class) ;
    if(TYPE_USER.equals(type_)){
      PageList userPageList = service.getUserHandler().findUsersByGroup(this.getCurrentGroup().getId()) ;    
      for(Object child : userPageList.getAll()){
        children.add(((User)child).getUserName()) ;
      }
    } else if(TYPE_MEMBERSHIP.equals(type_)) {
      for(String child :  getListMemberhip()){
        children.add(child) ;
      } 
    } else if(TYPE_GROUP.equals(type_)) {
      Collection  groups = service.getGroupHandler().findGroups(this.getCurrentGroup()) ;    
      for(Object child : groups){
        children.add(((Group)child).getGroupName()) ;
      }
    }
    return children ;
  }
  public void setSelectedGroups(List groups){
    if(groups != null) {
      selectedGroup_ = groups ;
      getChild(UITree.class).setSibbling(selectedGroup_) ;
    }
  }
  public void changeGroup(String groupId) throws Exception {    
    super.changeGroup(groupId) ;  
    if(selectedGroup_ != null) {
      UITree tree = getChild(UITree.class);
      tree.setSibbling(selectedGroup_);
      tree.setChildren(null);
    }
  }

  public void activate() throws Exception {
    // TODO Auto-generated method stub

  }

  public void deActivate() throws Exception {
    // TODO Auto-generated method stub
  }
  public void setType(String type) {
    this.type_ = type;
  }

  public String getType() {
    return type_;
  }

  static  public class SelectMembershipActionListener extends EventListener<UIGroupSelector> {   
    public void execute(Event<UIGroupSelector> event) throws Exception {
      String user = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIGroupSelector uiGroupSelector = event.getSource();
      UIPopupContainer uiPopupContainer = uiGroupSelector.getAncestorOfType(UIPopupContainer.class) ;
      UIPopupAction uiPopup = uiPopupContainer.getChild(UIPopupAction.class) ;
      String returnField = uiGroupSelector.getReturnField() ;
      ((UISelector)uiGroupSelector.getReturnComponent()).updateSelect(returnField, user) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer.getAncestorOfType(UIPopupAction.class)) ;
    }
  }

  static  public class ChangeNodeActionListener extends EventListener<UITree> {   
    public void execute(Event<UITree> event) throws Exception {
      UIGroupSelector uiGroupSelector = event.getSource().getAncestorOfType(UIGroupSelector.class) ;
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID)  ;
      uiGroupSelector.changeGroup(groupId) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiGroupSelector) ;
    }
  }

  static  public class SelectPathActionListener extends EventListener<UIBreadcumbs> {
    public void execute(Event<UIBreadcumbs> event) throws Exception {
      UIBreadcumbs uiBreadcumbs = event.getSource() ;
      UIGroupSelector uiGroupSelector = uiBreadcumbs.getParent() ;
      String objectId =  event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiBreadcumbs.setSelectPath(objectId);     
      String selectGroupId = uiBreadcumbs.getSelectLocalPath().getId() ;
      uiGroupSelector.changeGroup(selectGroupId) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiGroupSelector) ;
    }
  }

}
