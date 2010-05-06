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
package org.exoplatform.calendar.webui.popup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.CalendarSetting;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.calendar.service.impl.NewUserListener;
import org.exoplatform.calendar.webui.UIActionBar;
import org.exoplatform.calendar.webui.UICalendarPortlet;
import org.exoplatform.calendar.webui.UICalendarView;
import org.exoplatform.calendar.webui.UICalendarViewContainer;
import org.exoplatform.calendar.webui.UICalendars;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormTabPane;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIFormTabPane.gtmpl",
    events = {
      @EventConfig(listeners = UICalendarSettingForm.SaveActionListener.class),
      @EventConfig(listeners = UICalendarSettingForm.ChangeLocaleActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UICalendarSettingForm.ShowAllTimeZoneActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UICalendarSettingForm.CancelActionListener.class, phase = Phase.DECODE),
      //@EventConfig(listeners = UICalendarSettingForm.DeleteActionListener.class, phase = Phase.DECODE),
      //@EventConfig(listeners = UICalendarSettingForm.CalendarFeedActionListener.class, phase = Phase.DECODE),
      //@EventConfig(listeners = UICalendarSettingForm.EditActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UICalendarSettingForm.AddActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UICalendarSettingForm.SelectTabActionListener.class, phase = Phase.DECODE)
    }
)
public class UICalendarSettingForm extends UIFormTabPane implements UIPopupComponent{
  final private static String SETTING_CALENDAR_TAB = "setting".intern() ;
  final private static String DEFAULT_CALENDAR_TAB = "defaultCalendarTab".intern() ;
  final private static String DEFAULT_CALENDARS = "defaultCalendars".intern() ;
  final private static String FEED_TAB = "feedTab".intern();
  final private static String DEFAULT_CALENDARS_NOTE = "note".intern() ;
  private Map<String, String> names_ = new HashMap<String, String>() ;
  public String[] sharedCalendarColors_  = null ;
  public UICalendarSettingForm() throws Exception{
    super("UICalendarSettingForm") ;
    UICalendarSettingTab setting = new UICalendarSettingTab(SETTING_CALENDAR_TAB) ;//.setRendered(true) ;
    addUIFormInput(setting) ;
    setSelectedTab(setting.getId()) ;
    UICalendarSettingDisplayTab defaultCalendarsTab  = new UICalendarSettingDisplayTab(DEFAULT_CALENDAR_TAB) ;    
    addUIFormInput(defaultCalendarsTab) ;
    // TODO Add Feed Tab
    UICalendarSettingFeedTab uiFeedTab = new UICalendarSettingFeedTab(FEED_TAB);
    addUIFormInput(uiFeedTab);
  }

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}
  public Map<String, String> getChildIds() {return names_ ;}
  public void init(CalendarSetting calendarSetting, CalendarService cservice) throws Exception{
    names_.clear() ;
    String username = CalendarUtils.getCurrentUser() ;
    if(calendarSetting != null) {
      sharedCalendarColors_ = calendarSetting.getSharedCalendarsColors() ;
      UICalendarSettingTab settingTab = getChildById(SETTING_CALENDAR_TAB) ;
      settingTab.setViewType(calendarSetting.getViewType()) ;
      settingTab.setTimeInterval(String.valueOf(calendarSetting.getTimeInterval())) ;
      settingTab.setWeekStartOn(calendarSetting.getWeekStartOn()) ;
      settingTab.setDateFormat(calendarSetting.getDateFormat()) ;
      settingTab.setTimeFormat(calendarSetting.getTimeFormat()) ;
      settingTab.getUIFormSelectBox(UICalendarSettingTab.WORKINGTIME_BEGIN).setOptions(CalendarUtils.getTimesSelectBoxOptions(calendarSetting.getTimeFormat(), 30)) ;
      settingTab.getUIFormSelectBox(UICalendarSettingTab.WORKINGTIME_END).setOptions(CalendarUtils.getTimesSelectBoxOptions(calendarSetting.getTimeFormat(), 30)) ;
      if(calendarSetting.getLocation() == null) {
        calendarSetting.setLocation(Util.getPortalRequestContext().getLocale().getISO3Country()) ;
      }
      settingTab.setLocale(calendarSetting.getLocation()) ;     
      settingTab.setTimeZone(calendarSetting.getTimeZone()) ;
      settingTab.setShowWorkingTimes(calendarSetting.isShowWorkingTime()) ;
      if(calendarSetting.isShowWorkingTime()) {
        settingTab.setWorkingBegin(calendarSetting.getWorkingTimeBegin(), CalendarUtils.DATEFORMAT + " " + calendarSetting.getTimeFormat()) ;
        settingTab.setWorkingEnd(calendarSetting.getWorkingTimeEnd(), CalendarUtils.DATEFORMAT + " " + calendarSetting.getTimeFormat()) ;
      }
      //TODO cs-764
      settingTab.setSendOption(calendarSetting.getSendOption()) ;
      if(calendarSetting.getBaseURL() == null) calendarSetting.setBaseURL(CalendarUtils.getServerBaseUrl() + "calendar/iCalRss") ;
      //settingTab.setBaseUrl(calendarSetting.getBaseURL()) ;
    }
    UICalendarSettingDisplayTab defaultCalendarsTab = getChildById(DEFAULT_CALENDAR_TAB) ;    
    List<String> filteredCalendars = new ArrayList<String>() ;
    if(calendarSetting != null && calendarSetting.getFilterPrivateCalendars() != null) {
      filteredCalendars.addAll(Arrays.asList(calendarSetting.getFilterPrivateCalendars())) ;
    }
    if(calendarSetting != null && calendarSetting.getFilterPublicCalendars() != null) {
      filteredCalendars.addAll(Arrays.asList(calendarSetting.getFilterPublicCalendars())) ;
    }
    if(calendarSetting != null && calendarSetting.getFilterSharedCalendars() != null) {
      filteredCalendars.addAll(Arrays.asList(calendarSetting.getFilterSharedCalendars())) ;
    }
    List<Calendar> privateCals = getPrivateCalendars(cservice, username) ;
    defaultCalendarsTab.addChild(new UIFormInputInfo(DEFAULT_CALENDARS, DEFAULT_CALENDARS, getLabel(DEFAULT_CALENDARS_NOTE))) ;
    if(privateCals != null && !privateCals.isEmpty()) {
      defaultCalendarsTab.addChild(new UIFormInputInfo(CalendarUtils.PRIVATE_CALENDARS, CalendarUtils.PRIVATE_CALENDARS, null)) ;    
      for(Calendar calendar : privateCals) {
        names_.put(calendar.getId(), calendar.getName()) ;
        UIFormCheckBoxInput checkBox = defaultCalendarsTab.getChildById(calendar.getId()) ;
        if(checkBox == null) {
          checkBox = new UIFormCheckBoxInput<Boolean>(calendar.getId(), calendar.getId(), true) ;
          defaultCalendarsTab.addUIFormInput(checkBox) ;
        }
        checkBox.setChecked(true) ;
      }
    }
    List<Calendar> sharedCals =  getSharedCalendars(cservice, username) ;
    if(sharedCals != null && !sharedCals.isEmpty()) {
      defaultCalendarsTab.addChild(new UIFormInputInfo(CalendarUtils.SHARED_CALENDARS, CalendarUtils.SHARED_CALENDARS, null)) ; 
      for(Calendar calendar : sharedCals) {
        names_.put(calendar.getId(), calendar.getName()) ;
        UIFormCheckBoxInput checkBox = defaultCalendarsTab.getChildById(calendar.getId()) ;
        if(checkBox == null) {
          checkBox = new UIFormCheckBoxInput<Boolean>(calendar.getId(), calendar.getId(), true) ;
          defaultCalendarsTab.addUIFormInput(checkBox) ;
        }
        checkBox.setChecked(true) ;
      }
    }
    List<Calendar> publicCals = getPublicCalendars(cservice, username) ;
    if(publicCals != null && !publicCals.isEmpty()) {
      defaultCalendarsTab.addChild(new UIFormInputInfo(CalendarUtils.PUBLIC_CALENDARS, CalendarUtils.PUBLIC_CALENDARS, null)) ; 
      for(Calendar calendar : publicCals) {
        names_.put(calendar.getId(), calendar.getName()) ;
        UIFormCheckBoxInput checkBox = defaultCalendarsTab.getChildById(calendar.getId()) ;
        if(checkBox == null) {
          checkBox = new UIFormCheckBoxInput<Boolean>(calendar.getId(), calendar.getId(), true) ;
          defaultCalendarsTab.addUIFormInput(checkBox) ;
        }
        checkBox.setChecked(true) ;
      }
    }
    for(String calId : filteredCalendars) {
      UIFormCheckBoxInput<Boolean> input = defaultCalendarsTab.getChildById(calId) ;
      if(input != null) input.setChecked(false) ;
    }
    
    
  }
 
  protected List<Calendar> getPrivateCalendars(CalendarService calendarService, String username) throws Exception{
    boolean showAll = true;
    List<GroupCalendarData> groupCalendars = calendarService.getCalendarCategories(username, showAll) ;
    List<Calendar> calendars = new ArrayList<Calendar>() ;
    for(GroupCalendarData group : groupCalendars) {      
      for (Calendar calendar : group.getCalendars()) {
        if (calendar.getId().equals(Utils.getDefaultCalendarId(username)) && calendar.getName().equals(NewUserListener.DEFAULT_CALENDAR_NAME)) {
          String newName = CalendarUtils.getResourceBundle("UICalendars.label." + NewUserListener.DEFAULT_CALENDAR_ID);
          calendar.setName(newName);
        }
        calendars.add(calendar);
      }
    }
    return calendars;
  }

  protected List<Calendar> getPublicCalendars(CalendarService calendarService, String username) throws Exception{
    String[] groups = CalendarUtils.getUserGroups(username) ;
    List<GroupCalendarData> groupCalendars = calendarService.getGroupCalendars(groups, true, CalendarUtils.getCurrentUser()) ;
    List<Calendar> calendars = new ArrayList<Calendar>() ;
    Map<String,Calendar> mapCal = new HashMap<String,Calendar>();
    for(GroupCalendarData group : groupCalendars) {
      for(Calendar cal:group.getCalendars()){
        mapCal.put(cal.getId(), cal);
      }
    }
    calendars.addAll(mapCal.values());
    
    return calendars ;
  }

  protected List<Calendar> getSharedCalendars(CalendarService calendarService, String username) throws Exception{
    GroupCalendarData groupCalendars = calendarService.getSharedCalendars(username, true) ;
    List<Calendar> calendars = new ArrayList<Calendar>(); 
    if(groupCalendars != null) {    
      for (Calendar calendar : groupCalendars.getCalendars()) {
        if (calendar.getId().equals(Utils.getDefaultCalendarId(calendar.getCalendarOwner())) && calendar.getName().equals(NewUserListener.DEFAULT_CALENDAR_NAME)) {
          String newName = CalendarUtils.getResourceBundle("UICalendars.label." + NewUserListener.DEFAULT_CALENDAR_ID);
          calendar.setName(newName);
        }
        calendars.add(calendar);
      }
    }    
    return calendars ;
  }
  public String getLabel(ResourceBundle res, String id) {
    if(names_.get(id) != null) return names_.get(id) ;
    String label = getId() + ".label." + id;    
    return res.getString(label);
  }
  public String getLabel(String id) {
    String label = id ;
    try {
      label = super.getLabel(id) ;
    } catch (Exception e) {
    }
    return label ;
  }
  protected List<String> getUnCheckedList(List<Calendar> calendars) {
    List<String> list = new ArrayList<String>() ;
    for(Calendar cal : calendars) {
      UIFormCheckBoxInput<Boolean> input = ((UIFormInputWithActions)getChildById(DEFAULT_CALENDAR_TAB)).getChildById(cal.getId()) ;
      if(input != null && !input.isChecked()) list.add(input.getId()) ;
    }
    return list ;
  }
  public String[] getActions(){
    return new String[]{"Save", "Cancel"} ;
  }
  static  public class SaveActionListener extends EventListener<UICalendarSettingForm> {
    public void execute(Event<UICalendarSettingForm> event) throws Exception {
      UICalendarSettingForm uiForm = event.getSource() ;      
      CalendarSetting calendarSetting = new CalendarSetting() ;
      UICalendarSettingTab settingTab = uiForm.getChildById(UICalendarSettingForm.SETTING_CALENDAR_TAB) ;
      calendarSetting.setSharedCalendarsColors(uiForm.sharedCalendarColors_) ;
      calendarSetting.setViewType(settingTab.getViewType()) ;
      calendarSetting.setTimeInterval(Long.parseLong(settingTab.getTimeInterval())) ;
      calendarSetting.setWeekStartOn(settingTab.getWeekStartOn()) ;
      calendarSetting.setDateFormat(settingTab.getDateFormat()) ;
      calendarSetting.setTimeFormat(settingTab.getTimeFormat()) ;
      calendarSetting.setLocation(settingTab.getLocale()) ;
      calendarSetting.setTimeZone(settingTab.getTimeZone()) ;
      calendarSetting.setBaseURL(CalendarUtils.getServerBaseUrl() + "calendar/iCalRss") ;
      //TODO cs-764
      calendarSetting.setSendOption(settingTab.getSendOption()) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      if(settingTab.getShowWorkingTimes()) {
        if(settingTab.getWorkingBegin().equals(settingTab.getWorkingEnd()) || settingTab.getWorkingBeginTime().after(settingTab.getWorkingEndTime())) {
          uiApp.addMessage(new ApplicationMessage("UICalendarSettingForm.msg.working-time-logic", null, ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        calendarSetting.setShowWorkingTime(settingTab.getShowWorkingTimes()) ;
        calendarSetting.setWorkingTimeBegin(settingTab.getWorkingBegin()) ;
        calendarSetting.setWorkingTimeEnd(settingTab.getWorkingEnd()) ;
      }
      CalendarService calendarService = CalendarUtils.getCalendarService() ;
      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      UICalendars uiCalendars = calendarPortlet.findFirstComponentOfType(UICalendars.class) ;
      String username = CalendarUtils.getCurrentUser() ;
      List<String> defaultFilterCalendars = new ArrayList<String>() ;
      List<String> unCheckList = new ArrayList<String>() ;
      defaultFilterCalendars = uiForm.getUnCheckedList(uiForm.getPrivateCalendars(calendarService, username)) ;
      if(!defaultFilterCalendars.isEmpty()){
        calendarSetting.setFilterPrivateCalendars(defaultFilterCalendars.toArray(new String[] {})) ;
        unCheckList.addAll(defaultFilterCalendars) ;
        defaultFilterCalendars.clear() ;
      }
      defaultFilterCalendars = uiForm.getUnCheckedList(uiForm.getPublicCalendars(calendarService, username)) ;
      if(!defaultFilterCalendars.isEmpty()){
        calendarSetting.setFilterPublicCalendars(defaultFilterCalendars.toArray(new String[] {})) ;
        unCheckList.addAll(defaultFilterCalendars) ;
        defaultFilterCalendars.clear() ;
      }
      defaultFilterCalendars = uiForm.getUnCheckedList(uiForm.getSharedCalendars(calendarService, username)) ;
      if(!defaultFilterCalendars.isEmpty()){
        calendarSetting.setFilterSharedCalendars(defaultFilterCalendars.toArray(new String[] {})) ;
        unCheckList.addAll(defaultFilterCalendars) ;
        defaultFilterCalendars.clear() ;
      }
      uiCalendars.checkAll() ;
      calendarService.saveCalendarSetting(CalendarUtils.getCurrentUser(), calendarSetting) ;
      calendarPortlet.setCalendarSetting(calendarSetting) ;
      String viewType = UICalendarViewContainer.TYPES[Integer.parseInt(calendarSetting.getViewType())] ;
      UICalendarViewContainer uiViewContainer = calendarPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      uiViewContainer.initView(viewType) ;
      uiViewContainer.applySeting() ;
      uiViewContainer.refresh() ;
      
      // TODO CS-4165
      calendarPortlet.findFirstComponentOfType(UICalendarView.class).setCalendarSetting(calendarSetting);
      
      calendarPortlet.findFirstComponentOfType(UIActionBar.class).setCurrentView(viewType) ;
      calendarPortlet.cancelAction() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(calendarPortlet) ;
    }
  }
  static  public class ChangeLocaleActionListener extends EventListener<UICalendarSettingForm> {
    public void execute(Event<UICalendarSettingForm> event) throws Exception {
      UICalendarSettingForm uiForm = event.getSource() ;
      String locale = uiForm.getUIFormSelectBox(UICalendarSettingTab.LOCATION).getValue() ;
      UICalendarSettingTab calendarSettingTab = uiForm.getChildById(UICalendarSettingForm.SETTING_CALENDAR_TAB) ;
      uiForm.getUIFormSelectBox(UICalendarSettingTab.TIMEZONE).setOptions(calendarSettingTab.getTimeZones(locale)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupAction.class)) ;
    }
  }
  static  public class ShowAllTimeZoneActionListener extends EventListener<UICalendarSettingForm> {
    public void execute(Event<UICalendarSettingForm> event) throws Exception {
      UICalendarSettingForm uiForm = event.getSource() ;
      UICalendarSettingTab calendarSettingTab = uiForm.getChildById(UICalendarSettingForm.SETTING_CALENDAR_TAB) ;
      uiForm.getUIFormSelectBox(UICalendarSettingTab.TIMEZONE).setOptions(calendarSettingTab.getTimeZones(null)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupAction.class)) ;
    }
  }
  static  public class CancelActionListener extends EventListener<UICalendarSettingForm> {
    public void execute(Event<UICalendarSettingForm> event) throws Exception {
      UICalendarSettingForm uiForm = event.getSource() ;
      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      calendarPortlet.cancelAction() ;
    }
  }
  
  static public class SelectTabActionListener extends EventListener<UICalendarSettingForm> {
    public void execute(Event<UICalendarSettingForm> event) throws Exception {
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource()) ;      
    }
  }  
    
  static  public class AddActionListener extends EventListener<UICalendarSettingForm> {
    public void execute(Event<UICalendarSettingForm> event) throws Exception {
      UICalendarSettingForm uiform = event.getSource() ;   
      UIPopupContainer popupContainer = uiform.getAncestorOfType(UIPopupContainer.class) ;
      UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class) ;
      UIEditFeed uiEditFeed = popupAction.activate(UIEditFeed.class, 500) ;
      uiEditFeed.setNew(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;      
    }
  }
  
}
