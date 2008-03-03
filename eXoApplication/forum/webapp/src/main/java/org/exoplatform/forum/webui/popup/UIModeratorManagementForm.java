/***************************************************************************
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
 ***************************************************************************/
package org.exoplatform.forum.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.ForumSessionUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.JCRPageList;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.webui.UIForumPageIterator;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.services.organization.User;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/popup/UIModeratorsManagementForm.gtmpl",
    events = {
      @EventConfig(listeners = UIModeratorManagementForm.ViewProfileActionListener.class), 
      @EventConfig(listeners = UIModeratorManagementForm.EditProfileActionListener.class), 
      @EventConfig(listeners = UIModeratorManagementForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)
public class UIModeratorManagementForm extends UIForm implements UIPopupComponent {
	private ForumService forumService = (ForumService)PortalContainer.getInstance().getComponentInstanceOfType(ForumService.class) ;
	private List<UserProfile> userProfiles = new ArrayList<UserProfile>();
	private JCRPageList pageList ;
	public UIModeratorManagementForm() throws Exception {
		addChild(UIForumPageIterator.class, null, "ForumUserPageIterator") ;
  }
	
  public JCRPageList setPageListUserProfile() throws Exception {
  	this.pageList = this.forumService.getPageListUserProfile(ForumSessionUtils.getSystemProvider()) ;
  	this.pageList.setPageSize(10);
  	this.getChild(UIForumPageIterator.class).updatePageList(this.pageList) ;
  	this.setListProFileUser() ;
  	return this.pageList;
  }
  
  @SuppressWarnings("unused")
  private List<UserProfile> getListProFileUser() throws Exception {
  	return this.userProfiles ;
  }
  
  @SuppressWarnings("unchecked")
  private void setListProFileUser() throws Exception {
  	List<User> listUser = ForumSessionUtils.getAllUser() ;
  	List<UserProfile> listUserProfile = new ArrayList<UserProfile>() ;
  	this.userProfiles = new ArrayList<UserProfile>();
  	long page = this.getChild(UIForumPageIterator.class).getPageSelected() ;
  	listUserProfile = this.pageList.getPage(page) ;
  	for (User user : listUser) {
  		for (UserProfile userProfile : listUserProfile) {
	      if(user.getUserName().equals(userProfile.getUserId())) {
		  		userProfile.setUser(user);
		  		userProfile.setLastLoginDate(user.getLastLoginTime());
		  		this.userProfiles.add(userProfile);
		  		break ;
	      }
      }
    }
  }
  
  private UserProfile getUserProfile(String userId) throws Exception {
  	for (UserProfile userProfile : this.userProfiles) {
	    if(userProfile.getUserId().equals(userId)) return userProfile ;
    }
  	UserProfile userProfile = new UserProfile() ;
  	userProfile.setUserId(userId) ;
  	return userProfile ;
  }
  
  public void activate() throws Exception {}
	public void deActivate() throws Exception {}
  
  static  public class ViewProfileActionListener extends EventListener<UIModeratorManagementForm> {
    public void execute(Event<UIModeratorManagementForm> event) throws Exception {
    	UIModeratorManagementForm uiForm = event.getSource() ;
  		String userId = event.getRequestContext().getRequestParameter(OBJECTID);
  		UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
			UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
			UIViewUserProfile viewUserProfile = popupAction.activate(UIViewUserProfile.class, 670) ;
			viewUserProfile.setUserProfile(uiForm.getUserProfile(userId)) ;
			event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer) ;
    }
  }

  static  public class EditProfileActionListener extends EventListener<UIModeratorManagementForm> {
  	public void execute(Event<UIModeratorManagementForm> event) throws Exception {
  		UIModeratorManagementForm uiForm = event.getSource() ;
  		String userId = event.getRequestContext().getRequestParameter(OBJECTID);
  		UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
			UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
			UIUserProfileForm userProfileForm = popupAction.activate(UIUserProfileForm.class, 670) ;
			userProfileForm.setUserProfile(uiForm.getUserProfile(userId)) ;
			event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer) ;
  	}
  }
  
  static  public class CancelActionListener extends EventListener<UIModeratorManagementForm> {
    public void execute(Event<UIModeratorManagementForm> event) throws Exception {
      UIForumPortlet forumPortlet = event.getSource().getAncestorOfType(UIForumPortlet.class) ;
      forumPortlet.cancelAction() ;
    }
  }
}
