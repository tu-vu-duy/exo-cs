/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.calendar.webui.popup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.webui.UICalendarPortlet;
import org.exoplatform.calendar.webui.UICalendars;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormCheckBoxInput;

/**
 * Created by The eXo Platform SAS
 * Author : Ha Mai
 *          ha.mai@exoplatform.com
 * Feb 27, 2008  
 */
@ComponentConfig (
    lifecycle = UIContainerLifecycle.class, 
    events = {
      @EventConfig(listeners = UIAddEditPermission.EditActionListener.class),
      @EventConfig(listeners = UIAddEditPermission.DeleteActionListener.class, confirm = "UIEventCategoryManager.msg.confirm-delete")
    }
)

public class UIAddEditPermission extends UIContainer implements UIPopupComponent {
  public static String[]  BEAN_FIELD = {"viewPermission","editPermission"} ;
  private static String[] ACTION = {"Edit", "Delete"} ;

  private String calendarId_ = null ;

  public UIAddEditPermission() throws Exception {
    this.setName("UIAddEditPermission");
    UIGrid permissionList = addChild(UIGrid.class, null, "PermissionList") ;
    permissionList.configure("viewPermission", BEAN_FIELD, ACTION);
    permissionList.getUIPageIterator().setId("PermissionListIterator") ;
    addChild(UISharedForm.class, null, null) ;
  }
  public void activate() throws Exception {
    // TODO Auto-generated method stub

  }
  public void deActivate() throws Exception {
    // TODO Auto-generated method stub

  }

  public void init(String username, Calendar cal, boolean isAddNew) throws Exception{
    UISharedForm shareForm = getChild(UISharedForm.class) ;
    shareForm.init(username, cal, isAddNew) ; 
    updateGrid(cal) ;
    calendarId_ = cal.getId() ;
  }

  public void updateGrid(Calendar cal) throws Exception {
    List<data> dataRow = new ArrayList<data>() ;
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    ResourceBundle res = context.getApplicationResourceBundle() ;   
    if(cal.getViewPermission() != null) {
      for(String username : cal.getViewPermission() ) {
        boolean canEdit = cal.getEditPermission()!= null && Arrays.asList(cal.getEditPermission()).contains(username) ;
        String label = getId() + ".label." + String.valueOf(canEdit);  
        String editPerm = String.valueOf(canEdit);  
        try {
          editPerm = res.getString(label);
        } catch (Exception e) {
          System.out.println("Can not find " + label);
        }
        dataRow.add(new data(username,  editPerm)) ;
      }
    }
    UIGrid permissionList = getChild(UIGrid.class) ;
    ObjectPageList objPageList = new ObjectPageList(dataRow, 10) ;
    permissionList.getUIPageIterator().setPageList(objPageList) ;   
  }

  static public class EditActionListener extends EventListener<UIAddEditPermission> {
    public void execute(Event<UIAddEditPermission> event) throws Exception {
      UIAddEditPermission addEdit = event.getSource();
      UISharedForm shareForm = addEdit.getChild(UISharedForm.class);
      String resiceUser = event.getRequestContext().getRequestParameter(OBJECTID);
      UIFormCheckBoxInput checkBox = shareForm.getUIFormCheckBoxInput(UISharedForm.FIELD_EDIT) ;
      CalendarService calService = CalendarUtils.getCalendarService() ;
      String username = CalendarUtils.getCurrentUser() ;
      shareForm.setSharedUser(resiceUser) ;
      Calendar cal = calService.getUserCalendar(SessionProviderFactory.createSessionProvider(), username, addEdit.calendarId_) ;
      checkBox.setChecked((cal.getEditPermission() != null) && Arrays.asList(cal.getEditPermission()).contains(resiceUser)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(shareForm) ;
    }
  }
  static public class DeleteActionListener extends EventListener<UIAddEditPermission> {
    public void execute(Event<UIAddEditPermission> event) throws Exception {
      UIAddEditPermission addEdit = event.getSource();
      String resiceUser = event.getRequestContext().getRequestParameter(OBJECTID);
      CalendarService calService = CalendarUtils.getCalendarService() ;
      String username = CalendarUtils.getCurrentUser() ;
      Calendar cal = calService.getUserCalendar(SessionProviderFactory.createSessionProvider(), username, addEdit.calendarId_) ;
      if(cal.getViewPermission() != null) {
        List<String> newPerms = new ArrayList<String>() ;
        for(String s : cal.getViewPermission()) {
          if(!s.equals(resiceUser)) {
            newPerms.add(s) ;
          }
        }
        cal.setViewPermission(newPerms.toArray(new String[newPerms.size()])) ;
      }
      if(cal.getEditPermission() != null) {
        List<String> newPerms = new ArrayList<String>() ;
        for(String s : cal.getEditPermission()) {
          if(!s.equals(resiceUser)) {
            newPerms.add(s) ;
          }
        }
        cal.setEditPermission(newPerms.toArray(new String[newPerms.size()])) ;
      }
      calService.removeSharedCalendar(SessionProviderFactory.createSystemProvider(), resiceUser, addEdit.calendarId_) ;
      calService.saveUserCalendar(SessionProviderFactory.createSessionProvider(), username, cal, false) ;
      addEdit.updateGrid(cal);
      event.getRequestContext().addUIComponentToUpdateByAjax(addEdit) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(addEdit.getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UICalendars.class)) ;
    }
  }
  public class data {
    String viewPermission = null ;
    String editPermission = null ;

    public  String getViewPermission() {return viewPermission ;}
    public  String getEditPermission() {return editPermission ;}

    public data(String username, boolean canEdit) {
      viewPermission = username ;
      editPermission = String.valueOf(canEdit) ;
    }
    public data(String username, String canEdit) {
      viewPermission = username ;
      editPermission = canEdit ;
    }
  }

}
