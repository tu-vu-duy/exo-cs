/**
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
 **/
package org.exoplatform.calendar.webui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PathNotFoundException;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.CalendarSetting;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.calendar.service.impl.NewUserListener;
import org.exoplatform.calendar.webui.popup.UIAddEditPermission;
import org.exoplatform.calendar.webui.popup.UICalendarCategoryForm;
import org.exoplatform.calendar.webui.popup.UICalendarCategoryManager;
import org.exoplatform.calendar.webui.popup.UICalendarForm;
import org.exoplatform.calendar.webui.popup.UICalendarSettingForm;
import org.exoplatform.calendar.webui.popup.UIEventCategoryManager;
import org.exoplatform.calendar.webui.popup.UIExportForm;
import org.exoplatform.calendar.webui.popup.UIImportForm;
import org.exoplatform.calendar.webui.popup.UIPopupAction;
import org.exoplatform.calendar.webui.popup.UIPopupContainer;
import org.exoplatform.calendar.webui.popup.UIQuickAddEvent;
import org.exoplatform.calendar.webui.popup.UIRemoteCalendar;
import org.exoplatform.calendar.webui.popup.UISubscribeForm;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.ext.UIFormColorPicker.Colors;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template =  "app:/templates/calendar/webui/UICalendars.gtmpl",
                 events = {
                   @EventConfig(listeners = UICalendars.AddCalendarActionListener.class),
                   @EventConfig(listeners = UICalendars.AddEventCategoryActionListener.class),
                   @EventConfig(listeners = UICalendars.EditGroupActionListener.class),
                   @EventConfig(phase=Phase.DECODE, listeners = UICalendars.DeleteGroupActionListener.class, confirm="UICalendars.msg.confirm-delete-group"),
                   @EventConfig(listeners = UICalendars.ExportCalendarActionListener.class), 
                   @EventConfig(listeners = UICalendars.ExportCalendarsActionListener.class), 
                   @EventConfig(listeners = UICalendars.ImportCalendarActionListener.class),
                   @EventConfig(listeners = UICalendars.AddEventActionListener.class),
                   @EventConfig(listeners = UICalendars.AddTaskActionListener.class),
                   @EventConfig(listeners = UICalendars.EditCalendarActionListener.class),
                   @EventConfig(phase=Phase.DECODE, listeners = UICalendars.RemoveCalendarActionListener.class, confirm="UICalendars.msg.confirm-delete-calendar"),
                   @EventConfig(phase=Phase.DECODE, listeners = UICalendars.RemoveSharedCalendarActionListener.class, confirm="UICalendars.msg.confirm-delete-sharedCalendar"),
                   @EventConfig(listeners = UICalendars.AddCalendarCategoryActionListener.class),
                   @EventConfig(listeners = UICalendars.ShareCalendarActionListener.class),
                   @EventConfig(listeners = UICalendars.ChangeColorActionListener.class),
                   @EventConfig(listeners = UICalendars.TickActionListener.class),
                   @EventConfig(listeners = UICalendars.CalendarSettingActionListener.class),
                   @EventConfig(listeners = UICalendars.RemoteCalendarActionListener.class),
                   @EventConfig(listeners = UICalendars.RefreshRemoteCalendarActionListener.class)
                 }
)

public class UICalendars extends UIForm  {
  private Log log = ExoLogger.getLogger(this.getClass());
  public static String CALENDARID = "calendarid".intern() ;
  public static String CALTYPE = "calType".intern() ;
  public static String CALNAME = "calName".intern() ;
  public static String CALCOLOR = "calColor".intern() ;
//  public static String CURRENTTIME = "ct".intern() ;
//  public static String TIMEZONE = "tz".intern() ;

  private boolean isShowTaskList_ = false ;
  private String[] publicCalendarIds = {} ;
  private LinkedHashMap<String, String> colorMap_ = new LinkedHashMap<String, String>() ;

  public UICalendars() throws Exception {

  } 
  public String getLabel(String key) {
    try {
      return super.getLabel(key) ;
    } catch (Exception e) {
      return key ;
    }
  }
  public String[] getPublicCalendarIds(){ return publicCalendarIds ; }
  public void setShowTaskList(boolean isShowTaskList) {
    this.isShowTaskList_ = isShowTaskList;
  }

  public boolean isShowTaskList() {
    return isShowTaskList_;
  }
  public java.util.Calendar getCurrentMiniBeginDate() {
    UIMiniCalendar miniCal = getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UIMiniCalendar.class) ;
    java.util.Calendar temCal = miniCal.getInstanceTempCalendar() ;
    temCal.setTime(miniCal.getCurrentDate()) ;
    return miniCal.getBeginDay(temCal) ;
  }
  public java.util.Calendar getCurrentMiniEndDate() {
    UIMiniCalendar miniCal = getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UIMiniCalendar.class) ;
    java.util.Calendar temCal = miniCal.getInstanceTempCalendar() ;
    temCal.setTime(miniCal.getCurrentDate()) ;
    return miniCal.getEndDay(temCal) ;
  }
  public String[] getTaskStatus() {
    return CalendarEvent.TASK_STATUS ;
  }
  public List<CalendarEvent> getAllTask(java.util.Calendar formDate,java.util.Calendar toDate, String taskStatus) throws Exception {
    List<CalendarEvent> list = new ArrayList<CalendarEvent>() ;
    EventQuery eq = new EventQuery() ;
    if(!CalendarUtils.isEmpty(taskStatus)) eq.setState(taskStatus) ;
    eq.setEventType(CalendarEvent.TYPE_TASK) ;
    eq.setFromDate(formDate) ;
    eq.setToDate(toDate) ;
    list = CalendarUtils.getCalendarService().getEvents(CalendarUtils.getCurrentUser(), eq, null) ;
    return list ;
  }

  public void checkAll() {
    if (this.getAncestorOfType(UICalendarPortlet.class).isInSpace()) {
      // As just one calendar is set to checked, this function is broken if the portlet is in Social Space.
      return;
    }
    for(UIComponent cpm : getChildren())
      getUIFormCheckBoxInput(cpm.getId()).setChecked(true) ; 
  }

  public List<String> getCheckedCalendars() {
    List<String> list = new ArrayList<String>();
    for(UIComponent cpm : getChildren())
      if (cpm instanceof UIFormCheckBoxInput) {
        UIFormCheckBoxInput checkbox = (UIFormCheckBoxInput) cpm;
        if (checkbox.isChecked()) list.add(cpm.getId());
      }    
    return list ;
  }
  
  public EventQuery getEventQuery(EventQuery eventQuery) throws Exception {
    List<String> checkedCals = getCheckedCalendars() ;  
    List<String> calendarIds = new ArrayList<String>() ; 
    for (GroupCalendarData groupCalendarData : getPrivateCalendars())
      for (org.exoplatform.calendar.service.Calendar cal : groupCalendarData.getCalendars())
        if (checkedCals.contains(cal.getId())) calendarIds.add(cal.getId());
    for (GroupCalendarData calendarData : getPublicCalendars())
      for (org.exoplatform.calendar.service.Calendar  calendar : calendarData.getCalendars())
        if (checkedCals.contains(calendar.getId())) calendarIds.add(calendar.getId());
    GroupCalendarData sharedCalendars = getSharedCalendars();
    if (sharedCalendars != null) {
      for (org.exoplatform.calendar.service.Calendar cal : sharedCalendars.getCalendars()) {
        if (checkedCals.contains(cal.getId())) {
          calendarIds.add(cal.getId());
        }
      }
    }
    if (calendarIds.size() > 0)
      eventQuery.setCalendarId(calendarIds.toArray(new String[] {}));
    else {
      eventQuery.setCalendarId(new String[] {"null"});
    }
    eventQuery.setOrderBy(new String[] {Utils.EXO_SUMMARY});
    return eventQuery;
  }
  
  public List<GroupCalendarData> getPrivateCalendars() throws Exception{
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    UICalendarPortlet calendarPortlet = this.getAncestorOfType(UICalendarPortlet.class);
    String username = CalendarUtils.getCurrentUser() ;
    boolean dontShowAll = false;
    List<GroupCalendarData> groupCalendars = calendarService.getCalendarCategories(username, dontShowAll) ;
    for(GroupCalendarData group : groupCalendars) {
      if (group.getId().equals(NewUserListener.DEFAULT_CALENDAR_CATEGORYID) && group.getName().equals(NewUserListener.DEFAULT_CALENDAR_CATEGORYNAME)) {
        String newName = CalendarUtils.getResourceBundle("UICalendars.label." + group.getId());
        group.setName(newName);
      }      
      List<Calendar> calendars = group.getCalendars() ;
      if(calendars != null) {
        for(Calendar calendar : calendars) {
          if (calendar.getId().equals(Utils.getDefaultCalendarId(username)) && calendar.getName().equals(NewUserListener.DEFAULT_CALENDAR_NAME)) {
            String newName = CalendarUtils.getResourceBundle("UICalendars.label." + NewUserListener.DEFAULT_CALENDAR_ID);
            calendar.setName(newName);
          }
          colorMap_.put(Calendar.TYPE_PRIVATE + CalendarUtils.COLON + calendar.getId(), calendar.getCalendarColor()) ;
          UIFormCheckBoxInput checkbox = getUIFormCheckBoxInput(calendar.getId());
          if (checkbox == null) {
            checkbox = new UIFormCheckBoxInput<Boolean>(calendar.getId(), calendar.getId(), false);
            checkbox.setChecked(isCalendarOfSpace(calendar.getId()));
            addUIFormInput(checkbox);
          } else {
            checkbox.setChecked(isCalendarOfSpace(calendar.getId()));
          }
        }
      }
    }
    return groupCalendars;
  }

  /**
   * 
   * @param calendarId
   * @return true if the calendar is made by Social Space
   * else return false.
   */
  protected boolean isCalendarOfSpace(String calendarId) {
    String spaceId = UICalendarPortlet.getSpaceId();
    return (spaceId == null || (spaceId != null && calendarId.contains(spaceId)));
  }
  
  public List<GroupCalendarData> getPublicCalendars() throws Exception{
    String username = CalendarUtils.getCurrentUser() ;
    String[] groups = CalendarUtils.getUserGroups(username) ;
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    List<GroupCalendarData> groupCalendars = calendarService.getGroupCalendars(groups, false, username) ;
    Map<String, String> map = new HashMap<String, String> () ;
    for(GroupCalendarData group : groupCalendars) {
      List<Calendar> calendars = group.getCalendars() ;
      for(Calendar calendar : calendars) {
        map.put(calendar.getId(), calendar.getId()) ;
        colorMap_.put(Calendar.TYPE_PUBLIC + CalendarUtils.COLON + calendar.getId(), calendar.getCalendarColor()) ;
        UIFormCheckBoxInput checkbox = getUIFormCheckBoxInput(calendar.getId());
        if (checkbox == null) {
          checkbox = new UIFormCheckBoxInput<Boolean>(calendar.getId(), calendar.getId(), false);
          checkbox.setChecked(isCalendarOfSpace(calendar.getId()));
          addUIFormInput(checkbox);
        } else {
          checkbox.setChecked(isCalendarOfSpace(calendar.getId()));
        }
      }
    }
    publicCalendarIds = map.values().toArray(new String[]{}) ;
    return groupCalendars ;
  }

  public GroupCalendarData getSharedCalendars() throws Exception{
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    GroupCalendarData groupCalendars = calendarService.getSharedCalendars(CalendarUtils.getCurrentUser(), false) ;
    CalendarSetting setting = calendarService.getCalendarSetting(CalendarUtils.getCurrentUser()) ;
    Map<String, String> map = new HashMap<String, String>() ;
    for(String key : setting.getSharedCalendarsColors()) {
      map.put(key.split(CalendarUtils.COLON)[0], key.split(CalendarUtils.COLON)[1]) ;
    }
    if(groupCalendars != null) {
      List<Calendar> calendars = groupCalendars.getCalendars() ;
      for(Calendar calendar : calendars) {
        if (calendar.getId().equals(Utils.getDefaultCalendarId(calendar.getCalendarOwner())) && calendar.getName().equals(NewUserListener.DEFAULT_CALENDAR_NAME)) {
          String newName = CalendarUtils.getResourceBundle("UICalendars.label." + NewUserListener.DEFAULT_CALENDAR_ID);
          calendar.setName(newName);
        }
        String color = map.get(calendar.getId()) ;
        if(color == null) color = calendar.getCalendarColor() ;
        colorMap_.put(Calendar.TYPE_SHARED + CalendarUtils.COLON + calendar.getId(), color) ;
        UIFormCheckBoxInput checkbox = getUIFormCheckBoxInput(calendar.getId());
        if (checkbox == null) {
          checkbox = new UIFormCheckBoxInput<Boolean>(calendar.getId(), calendar.getId(), false);
          checkbox.setChecked(isCalendarOfSpace(calendar.getId()));
          addUIFormInput(checkbox);
        } else {
          checkbox.setChecked(isCalendarOfSpace(calendar.getId()));
        }
      }
    }
    return groupCalendars ;
  }

  public LinkedHashMap<String, String> getColorMap() {
    return colorMap_;
  }
  public String[] getColors() {
    return Colors.COLORNAMES ;
  }

  private boolean canAddTaskAndEvent(UICalendars uiComponent, String calendarId, String calType) throws Exception {
    CalendarService calService = CalendarUtils.getCalendarService() ;
    Calendar calendar = null;
    String currentUser = CalendarUtils.getCurrentUser() ;
    
    
    if(calType.equals(CalendarUtils.SHARED_TYPE)) {
      calendar = calService.getSharedCalendars(currentUser, true).getCalendarById(calendarId) ;
      return CalendarUtils.canEdit(null, Utils.getEditPerUsers(calendar), currentUser) ;
    } else if(calType.equals(CalendarUtils.PUBLIC_TYPE)) {
      calendar = calService.getGroupCalendar(calendarId) ;
      // cs-4429: fix for group calendar permissions
      return CalendarUtils.canEdit(uiComponent.getApplicationComponent(OrganizationService.class), calendar.getEditPermission(), currentUser) ;
    }  
    return false ;
  }
  public boolean canEdit(String[] savePerms) throws Exception{
    return CalendarUtils.canEdit(CalendarUtils.getOrganizationService(), savePerms, CalendarUtils.getCurrentUser()) ;
  }
  
  public boolean isRemoteCalendar(String calendarId) throws Exception {
    String username = CalendarUtils.getCurrentUser();
    CalendarService calService = CalendarUtils.getCalendarService();
    return calService.isRemoteCalendar(username, calendarId);    
  }
  
  private void updateView(UICalendars uiComponent, Event<UICalendars> event) throws Exception {
    UICalendarPortlet uiPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
    UICalendarViewContainer uiViewContainer = uiPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
    if(uiViewContainer.getRenderedChild()  instanceof UIListContainer) {
      UIListContainer list = (UIListContainer)uiViewContainer.getRenderedChild() ;
      UIListView uiListView = list.getChild(UIListView.class) ;
      if(uiListView.isDisplaySearchResult()) {
        uiListView.setDisplaySearchResult(false) ;
        uiListView.setCategoryId(null) ;
        uiListView.refresh() ;
        uiListView.setLastViewId(null) ;
        UISearchForm uiSearchForm = uiPortlet.findFirstComponentOfType(UISearchForm.class) ;
        uiSearchForm.reset() ;
        UIActionBar uiActionBar = uiPortlet.findFirstComponentOfType(UIActionBar.class) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchForm) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiActionBar) ;
      }
    }  
  }
  
  static  public class AddCalendarActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      String categoryId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.deActivate() ;
      UIPopupContainer uiPopupContainer = popupAction.activate(UIPopupContainer.class, 600) ;
      uiPopupContainer.setId(UIPopupContainer.UICALENDARPOPUP) ;
      UICalendarForm calendarForm = uiPopupContainer.addChild(UICalendarForm.class, null, null) ;
      calendarForm.setTimeZone(uiCalendarPortlet.getCalendarSetting().getTimeZone());
      calendarForm.setLocale(uiCalendarPortlet.getCalendarSetting().getLocation()) ;
      calendarForm.setSelectedGroup(categoryId) ;
      calendarForm.groupCalId_ = categoryId ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  static  public class AddEventCategoryActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiCalendars = event.getSource() ;
      UICalendarPortlet calendarPortlet = uiCalendars.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = calendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.deActivate() ;
      try {
        popupAction.activate(UIEventCategoryManager.class, 470) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
      } catch (PathNotFoundException e) {
        UIApplication uiApp = uiCalendars.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(calendarPortlet) ;
        return;
      }
    }
  }
  static  public class EditGroupActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.deActivate() ;
      UICalendarCategoryManager uiManager = popupAction.activate(UICalendarCategoryManager.class, 470) ;
      UICalendarCategoryForm uiForm = uiManager.getChild(UICalendarCategoryForm.class) ;
      String categoryId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiForm.init(categoryId) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  static  public class DeleteGroupActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      String calendarCategoryId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UICalendarPortlet uiPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      uiPortlet.cancelAction() ;
      CalendarService calService = uiComponent.getApplicationComponent(CalendarService.class) ;
      String username = CalendarUtils.getCurrentUser() ;
      calService.removeCalendarCategory(username, calendarCategoryId) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiComponent) ; 
      UICalendarWorkingContainer uiWorkingContainer = uiPortlet.findFirstComponentOfType(UICalendarWorkingContainer.class) ;
      UICalendarViewContainer uiViewContainer = uiPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      uiComponent.updateView(uiComponent, event);
      uiViewContainer.refresh() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  }

  static  public class AddEventActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      CalendarService calService = CalendarUtils.getCalendarService() ;
      String currentUser = CalendarUtils.getCurrentUser() ;
      UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
      try {
        String calendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;
        String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
        Calendar calendar = CalendarUtils.getCalendar(calType, calendarId);
        if(calendar == null) {
          uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiCalendarPortlet) ;
        } else {
          // check if calendar is remote
          if(calService.isRemoteCalendar(currentUser, calendarId)) {
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.cant-add-event-on-remote-calendar", null, ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return;
          }        
          
          if(!CalendarUtils.PRIVATE_TYPE.equals(calType) && !uiComponent.canAddTaskAndEvent(uiComponent, calendarId, calType)) {
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-permission-to-edit", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return;
          }

          List<EventCategory> eventCategories = calService.getEventCategories(CalendarUtils.getCurrentUser()) ;
          if(eventCategories.isEmpty()) {
            uiApp.addMessage(new ApplicationMessage("UICalendarView.msg.event-category-list-empty", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }  
         
          String clientTime = CalendarUtils.getCurrentTime(uiComponent) ;
          //String clientTime = event.getRequestContext().getRequestParameter(CURRENTTIME) ;
          String categoryId = event.getRequestContext().getRequestParameter("categoryId") ;
          UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
          popupAction.deActivate() ;
          UIQuickAddEvent uiQuickAddEvent = popupAction.activate(UIQuickAddEvent.class, 600) ;
          uiQuickAddEvent.setEvent(true) ;  
          uiQuickAddEvent.setId("UIQuickAddEvent") ;
          uiQuickAddEvent.update(calType, null) ;
          uiQuickAddEvent.setSelectedCalendar(calendarId) ;
          uiQuickAddEvent.init(uiCalendarPortlet.getCalendarSetting(), clientTime, null) ;
          if(categoryId != null && categoryId.trim().length() >0 && !categoryId.toLowerCase().equals("null")&& !categoryId.equals("calId")) {
            uiQuickAddEvent.setSelectedCategory(categoryId) ;
          } else {
            uiQuickAddEvent.setSelectedCategory("meeting") ;
          }
          event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
        }
      } catch (PathNotFoundException e) {
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      }
    }
  }
  
  static  public class AddTaskActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      CalendarService calService = CalendarUtils.getCalendarService() ;
      String currentUser = CalendarUtils.getCurrentUser() ;
      try {
        UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
        String calendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;        
        String clientTime = CalendarUtils.getCurrentTime(uiComponent) ;
        //String clientTime = event.getRequestContext().getRequestParameter(CURRENTTIME) ;
        String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
        String categoryId = event.getRequestContext().getRequestParameter("categoryId") ;
        Calendar calendar = CalendarUtils.getCalendar(calType, calendarId);
        if(calendar == null) {
          uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiCalendarPortlet) ;
        } else {
          // check if calendar is remote
          if(calService.isRemoteCalendar(currentUser, calendarId)) {
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.cant-add-event-on-remote-calendar", null, ApplicationMessage.WARNING)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return;
          }        
          
          if(!CalendarUtils.PRIVATE_TYPE.equals(calType) && !uiComponent.canAddTaskAndEvent(uiComponent, calendarId, calType)) {
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-permission-to-edit", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return;
          }
          List<EventCategory> eventCategories = calService.getEventCategories(CalendarUtils.getCurrentUser()) ;
          if(eventCategories.isEmpty()) {
            uiApp.addMessage(new ApplicationMessage("UICalendarView.msg.event-category-list-empty", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }  
          UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
          popupAction.deActivate() ;
          UIQuickAddEvent uiQuickAddTask = popupAction.activate(UIQuickAddEvent.class, 600) ;
          uiQuickAddTask.setEvent(false) ;  
          uiQuickAddTask.setId("UIQuickAddTask") ;
          uiQuickAddTask.init(uiCalendarPortlet.getCalendarSetting(), clientTime, null) ;
          uiQuickAddTask.update(calType, null) ;
          uiQuickAddTask.setSelectedCalendar(calendarId) ;
          if(categoryId != null && categoryId.trim().length() >0 && !categoryId.toLowerCase().equals("null")) {
            uiQuickAddTask.setSelectedCategory(categoryId) ;
          } else {
            uiQuickAddTask.setSelectedCategory("meeting") ;
          }
          event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
        }
      } catch (PathNotFoundException e) {
        UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      }
    }
  }

  static  public class EditCalendarActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      String calendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String username = CalendarUtils.getCurrentUser() ;
      String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.deActivate() ;
      CalendarService calService = CalendarUtils.getCalendarService() ;
      
      if(CalendarUtils.SHARED_TYPE.equalsIgnoreCase(calType)) {
        UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.not-support-edit-share-calendar", null, 1)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiCalendarPortlet) ;
        return ;
      }
      try {       
        Calendar calendar = null ;
        if(CalendarUtils.PRIVATE_TYPE.equals(calType)) 
        { 
          calendar = calService.getUserCalendar(username, calendarId) ;
        } else if (CalendarUtils.PUBLIC_TYPE.equals(calType)) {
          calendar = calService.getGroupCalendar(calendarId) ;
        }
        if(calendar == null)
        {
          UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiCalendarPortlet) ;
        } else  
        {
          // check if remote calendar
          if (calService.isRemoteCalendar(username, calendarId)) {
            UIRemoteCalendar uiRemoteCalendar = popupAction.activate(UIRemoteCalendar.class, 600);
            uiRemoteCalendar.init(calendar);
            event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
            return;
          }
      
          // cs-4429: fix for group calendar permission
          if((CalendarUtils.SHARED_TYPE.equals(calType) && !uiComponent.canEdit(Utils.getEditPerUsers(calendar))) ||
             (CalendarUtils.PUBLIC_TYPE.equals(calType) && !uiComponent.canEdit(calendar.getEditPermission()))) 
          {
            UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-permission-to-edit", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiCalendarPortlet) ;
            return;
          }
          UIPopupContainer uiPopupContainer = uiCalendarPortlet.createUIComponent(UIPopupContainer.class, null, null) ;
          uiPopupContainer.setId(UIPopupContainer.UICALENDARPOPUP) ;
          UICalendarForm uiCalendarForm = uiPopupContainer.addChild(UICalendarForm.class, null, null) ;
          uiCalendarForm.calType_ = calType ;
          uiCalendarForm.init(calendar, uiCalendarPortlet.getCalendarSetting()) ;
          popupAction.activate(uiPopupContainer, 600, 0, true) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
        }
      }catch (PathNotFoundException e) {
        UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      }
      UICalendarViewContainer uiViewContainer = uiCalendarPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      CalendarSetting setting = calService.getCalendarSetting(username) ;
      uiViewContainer.refresh() ;
      uiCalendarPortlet.setCalendarSetting(setting) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiCalendarPortlet) ;
    }
  }
  static  public class RemoveCalendarActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      String username = CalendarUtils.getCurrentUser() ;
      String calendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
      CalendarService calService = CalendarUtils.getCalendarService() ;
      Calendar calendar = null ;
      try {
        if(calType.equals(CalendarUtils.PRIVATE_TYPE)) {
          calendar = calService.getUserCalendar(username, calendarId) ;
          if(calendar == null) {
            UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          } else {
            Boolean isRemote = calService.isRemoteCalendar(username, calendarId);
            calService.removeUserCalendar(username, calendarId) ; 
            if (isRemote) {
              if (calService.getRemoteCalendarCount(username) == 0) {
                // remove sync job
                calService.stopSynchronizeRemoteCalendarJob(username);
              }
            }
          }
        }else if(calType.equals(CalendarUtils.PUBLIC_TYPE)) {
          calendar = calService.getGroupCalendar(calendarId) ;
          if(calendar == null) {
            UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          } else {
            boolean canEdit = false ;
            OrganizationService oService = uiComponent.getApplicationComponent(OrganizationService.class) ;
            for(GroupCalendarData groupCal : uiComponent.getPublicCalendars()) {
              for(Calendar cal : groupCal.getCalendars()) {
                if(cal.getId().equals(calendarId)) {
                  // cs-4429: fix for group calendar permission
                  canEdit = CalendarUtils.canEdit(oService, (groupCal.getCalendarById(calendarId)).getEditPermission(), username) ;
                  break ;
                }
              }
            }
            if(canEdit) {
              calService.removePublicCalendar(calendarId) ;
            } else {
              UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
              uiApp.addMessage(new ApplicationMessage("UICalendarView.msg.have-no-delete-permission", null)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
              return ;
            }
          }
        }
      } catch (PathNotFoundException e) {
        UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      }
      UICalendarPortlet uiPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      uiPortlet.cancelAction() ;
      UICalendarViewContainer uiViewContainer = uiPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      uiComponent.updateView(uiComponent, event);
      CalendarSetting setting = calService.getCalendarSetting(username) ;
      uiViewContainer.refresh() ;
      uiPortlet.setCalendarSetting(setting) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet) ;
    }
  }
  
  static  public class RemoveSharedCalendarActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      String username = CalendarUtils.getCurrentUser() ;
      String calendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      CalendarService calService = CalendarUtils.getCalendarService() ;
      Calendar calendar = null ;
      try {
        if(calService.getSharedCalendars(username, true) != null)
          calendar = calService.getSharedCalendars(username, true).getCalendarById(calendarId) ;
        if(calendar == null) {
          UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        } else {
          calService.removeSharedCalendar(username, calendarId) ;
        }      
      } catch (PathNotFoundException e) {
        UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      }
      UICalendarPortlet uiPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      uiPortlet.cancelAction() ;
      UICalendarViewContainer uiViewContainer = uiPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      uiComponent.updateView(uiComponent, event);
      CalendarSetting setting = calService.getCalendarSetting(username) ;
      uiViewContainer.refresh() ;
      uiPortlet.setCalendarSetting(setting) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet) ;
    }
  }

  static  public class AddCalendarCategoryActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.deActivate() ;
      popupAction.activate(UICalendarCategoryManager.class, 470) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }

  static  public class ExportCalendarActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      String currentUser = CalendarUtils.getCurrentUser() ;
      CalendarService calService = CalendarUtils.getCalendarService() ;
      String selectedCalendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
      UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
      Calendar calendar = null;
      if(calType.equals(CalendarUtils.PRIVATE_TYPE)) {
        calendar = calService.getUserCalendar(currentUser, selectedCalendarId) ;
      } else if(calType.equals(CalendarUtils.SHARED_TYPE)) {
        GroupCalendarData gCalendarData = calService.getSharedCalendars(currentUser, true) ;
        if(gCalendarData != null) { 
          calendar = gCalendarData.getCalendarById(selectedCalendarId) ;
          if(calendar != null && !CalendarUtils.isEmpty(calendar.getCalendarOwner())){
            if (calendar.getId().equals(Utils.getDefaultCalendarId(currentUser)) && calendar.getName().equals(NewUserListener.DEFAULT_CALENDAR_NAME)) {
              String newName = CalendarUtils.getResourceBundle("UICalendars.label." + NewUserListener.DEFAULT_CALENDAR_ID);
              calendar.setName(newName);
            }
            calendar.setName(calendar.getCalendarOwner() + "-" + calendar.getName()) ;
          }
        }
      } else if(calType.equals(CalendarUtils.PUBLIC_TYPE)) {
        try {
          calendar = calService.getGroupCalendar(selectedCalendarId) ;
        } catch (PathNotFoundException e) {
          uiComponent.log.debug("\n\n calendar has been removed !");
        }
      }  
      if(calendar == null) {
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiCalendarPortlet) ;
      } else {
        boolean canEdit = false ;
        if(calType.equals(CalendarUtils.SHARED_TYPE)) {
          canEdit = CalendarUtils.canEdit(null, Utils.getEditPerUsers(calendar), currentUser) ;
        } else if(calType.equals(CalendarUtils.PUBLIC_TYPE)) {
          // cs-4429: fix for group calendar permission
          canEdit = CalendarUtils.canEdit(CalendarUtils.getOrganizationService(), calendar.getEditPermission(), currentUser) ;
        }
        if(!calType.equals(CalendarUtils.PRIVATE_TYPE) && !canEdit) {
          uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-permission-to-edit", null)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        List<Calendar> list = new ArrayList<Calendar>() ;
        list.add(calendar) ;
        UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
        popupAction.deActivate() ;
        UIExportForm exportForm = popupAction.activate(UIExportForm.class, 500) ;
        exportForm.update(calType, list, selectedCalendarId) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
      }
    }
  }
  static  public class ExportCalendarsActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.deActivate() ;
      String username = CalendarUtils.getCurrentUser() ;
      List<Calendar> list = CalendarUtils.getCalendarService().getUserCalendarsByCategory(username, groupId) ;
      if(list.isEmpty()){
        UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.calendar-require", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      } else {
        UIExportForm exportForm = popupAction.activate(UIExportForm.class, 500) ;
        exportForm.initCheckBox(list, null) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
      }
    }
  }
  static  public class ImportCalendarActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      String selectedCalendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
      boolean showAll = true;
      String user = CalendarUtils.getCurrentUser();
      List<GroupCalendarData> calendarCategories = CalendarUtils.getCalendarService().getCalendarCategories(user, showAll) ;
      if(calendarCategories== null || calendarCategories.isEmpty()) {
        UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICalendarForm.msg.category-empty", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      }  else {
        UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
        UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
        popupAction.deActivate() ;
        UIPopupContainer uiPopupContainer = popupAction.activate(UIPopupContainer.class, 600) ;
        uiPopupContainer.setId(UIPopupContainer.UICALENDARPOPUP) ;
        UIImportForm form = uiPopupContainer.addChild(UIImportForm.class,null,null); 
        form.init(selectedCalendarId, calType) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiComponent.getParent()) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
      }
    }
  }

  static  public class ShareCalendarActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      String selectedCalendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.deActivate() ;
      UIPopupContainer uiPopupContainer = popupAction.activate(UIPopupContainer.class, 500) ;
      uiPopupContainer.setId("UIPermissionSelectPopup") ;
      UIAddEditPermission uiAddNewEditPermission = uiPopupContainer.addChild(UIAddEditPermission.class, null, null);
      CalendarService calService = CalendarUtils.getCalendarService() ;
      String username = CalendarUtils.getCurrentUser() ;
      Calendar cal = calService.getUserCalendar(username, selectedCalendarId) ;
      if (cal.getId().equals(Utils.getDefaultCalendarId(username)) && cal.getName().equals(NewUserListener.DEFAULT_CALENDAR_NAME)) {
        String newName = CalendarUtils.getResourceBundle("UICalendars.label." + NewUserListener.DEFAULT_CALENDAR_ID);
        cal.setName(newName);
      }
      uiAddNewEditPermission.init(null, cal, true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiComponent) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  static  public class ChangeColorActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      uiComponent.getAncestorOfType(UICalendarPortlet.class).cancelAction() ;
      String calendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String color = event.getRequestContext().getRequestParameter(CALCOLOR) ;
      String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
      CalendarService calService = CalendarUtils.getCalendarService() ;
      String username = CalendarUtils.getCurrentUser() ;
      try{
        Calendar calendar = null ;
        if(CalendarUtils.PRIVATE_TYPE.equals(calType)) {
          calendar = calService.getUserCalendar(username, calendarId) ;
          if(calendar == null) {
            UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          } else {
            calendar.setCalendarColor(color) ;
            calService.saveUserCalendar(username, calendar, false) ;
          }
        } else if(CalendarUtils.SHARED_TYPE.equals(calType)){
          if(calService.getSharedCalendars(username, true) != null)
            calendar = calService.getSharedCalendars(username, true).getCalendarById(calendarId) ;
          if(calendar == null) {
            UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          } else {
            calendar.setCalendarColor(color) ;
            calService.saveSharedCalendar(username, calendar) ;
          }
        } else if(CalendarUtils.PUBLIC_TYPE.equals(calType)){
          calendar = calService.getGroupCalendar(calendarId) ;
          if(calendar == null) {
            UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          } else {
            // cs-4429: fix for group calendar permission
            if(!CalendarUtils.canEdit(uiComponent.getApplicationComponent(OrganizationService.class), calendar.getEditPermission(), username)){
              UIApplication uiApp = uiComponent.getAncestorOfType(UIApplication.class) ;
              uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-permission-to-edit", null, ApplicationMessage.WARNING)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
              return ;
            }
            calendar.setCalendarColor(color) ;
            calService.savePublicCalendar(calendar, false, username) ;
          }
        }
      } catch (Exception e) {
        e.printStackTrace() ;
      }
      UICalendarPortlet uiPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      UICalendarViewContainer uiViewContainer = uiPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      CalendarSetting setting = calService.getCalendarSetting(username) ;
      uiViewContainer.refresh() ;
      uiPortlet.setCalendarSetting(setting) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet) ;
    }
  }
  static  public class CalendarSettingActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiComponent = event.getSource() ;
      UICalendarPortlet uiCalendarPortlet = uiComponent.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = uiCalendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.deActivate() ;
      UIPopupContainer uiPopupContainer = popupAction.activate(UIPopupContainer.class, 600) ;
      uiPopupContainer.setId(UIPopupContainer.UICALENDAR_SETTING_POPUP);
      UICalendarSettingForm uiCalendarSettingForm = uiPopupContainer.addChild(UICalendarSettingForm.class, null, null) ;
      CalendarService cservice = CalendarUtils.getCalendarService() ;
      CalendarSetting calendarSetting = uiComponent.getAncestorOfType(UICalendarPortlet.class).getCalendarSetting() ; 
      uiCalendarSettingForm.init(calendarSetting, cservice) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  
  // TODO CS- 3152 only when list view
  static public class TickActionListener extends EventListener<UICalendars> {
    public void execute(Event<UICalendars> event) throws Exception {
      UICalendars uiCalendars = event.getSource() ;
      UICalendarPortlet uiPortlet = uiCalendars.getAncestorOfType(UICalendarPortlet.class) ;
      UICalendarViewContainer uiViewContainer = uiPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      uiViewContainer.refresh();
      UICalendarContainer uiVContainer = uiPortlet.findFirstComponentOfType(UICalendarContainer.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVContainer.findFirstComponentOfType(UIMiniCalendar.class)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;      
    }
  }  
  
  public static class RemoteCalendarActionListener extends EventListener<UICalendars> {

    @Override
    public void execute(Event<UICalendars> event) throws Exception {
        UICalendars uiCalendars = event.getSource();
      UICalendarPortlet uiPortlet = uiCalendars.getAncestorOfType(UICalendarPortlet.class);
      UIPopupAction popupAction = uiPortlet.getChild(UIPopupAction.class);
      popupAction.deActivate();
      UISubscribeForm subscribeForm = popupAction.activate(UISubscribeForm.class, 600);
      subscribeForm.init(CalendarService.ICALENDAR, "");
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  
  public static class RefreshRemoteCalendarActionListener extends EventListener<UICalendars> {

    @Override
    public void execute(Event<UICalendars> event) throws Exception {
        UICalendars uiCalendars = event.getSource();
      UICalendarPortlet uiPortlet = uiCalendars.getAncestorOfType(UICalendarPortlet.class);
      CalendarService calService = CalendarUtils.getCalendarService() ;
      String remoteCalendarId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String username = CalendarUtils.getCurrentUser();
      Calendar calendar = calService.getUserCalendar(username, remoteCalendarId);
      try {    
        calService.refreshRemoteCalendar(username, remoteCalendarId);
        
        // activate SynchronizeRemoteCalendarJob
        // calService.loadSynchronizeRemoteCalendarJob(username);
        
        UICalendarContainer uiVContainer = uiPortlet.findFirstComponentOfType(UICalendarContainer.class) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet) ;      
      }
      catch (Exception e) {
        e.printStackTrace();
        UIApplication uiApp = uiCalendars.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UICalendars.msg.cant-refresh-remote-calendar", new String[] {calendar.getName()}, ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      }
    }
    
  }
  
}
