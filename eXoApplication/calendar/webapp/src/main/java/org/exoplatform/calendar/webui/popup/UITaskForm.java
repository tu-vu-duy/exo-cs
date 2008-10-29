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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.Attachment;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.CalendarSetting;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.calendar.service.Reminder;
import org.exoplatform.calendar.webui.CalendarView;
import org.exoplatform.calendar.webui.UICalendarPortlet;
import org.exoplatform.calendar.webui.UICalendarViewContainer;
import org.exoplatform.calendar.webui.UICalendars;
import org.exoplatform.calendar.webui.UIFormComboBox;
import org.exoplatform.calendar.webui.UIFormDateTimePicker;
import org.exoplatform.calendar.webui.UIListContainer;
import org.exoplatform.calendar.webui.UIMiniCalendar;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.download.DownloadResource;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItem;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormTabPane;

/**
 * Created by The eXo Platform SARL
 * Author : Tuan Pham
 *          tuan.pham@exoplatform.com
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIFormTabPane.gtmpl", 
    events = {
      @EventConfig(listeners = UITaskForm.SaveActionListener.class),
      @EventConfig(listeners = UITaskForm.AddCategoryActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UITaskForm.AddEmailAddressActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UITaskForm.AddAttachmentActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UITaskForm.DownloadAttachmentActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UITaskForm.RemoveAttachmentActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UITaskForm.SelectUserActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UITaskForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UITaskForm extends UIFormTabPane implements UIPopupComponent, UISelector{
  final public static String TAB_TASKDETAIL = "eventDetail".intern() ;
  final public static String TAB_TASKREMINDER = "eventReminder".intern() ;
  final public static String ITEM_PUBLIC = "public".intern() ;
  final public static String ITEM_PRIVATE = "private".intern() ;
  final public static String ITEM_AVAILABLE = "available".intern() ;
  final public static String ITEM_BUSY = "busy".intern() ;
  final public static String ITEM_REPEAT = "true".intern() ;
  final public static String ITEM_UNREPEAT = "false".intern() ;
  final public static String ACT_REMOVE = "RemoveAttachment".intern() ;
  final public static String ACT_DOWNLOAD = "DownloadAttachment".intern() ;
  final public static String ACT_ADDEMAIL = "AddEmailAddress".intern() ;
  final public static String ACT_ADDCATEGORY = "AddCategory".intern() ;
  final public static String ACT_SELECTUSER = "SelectUser".intern() ;

  private boolean isAddNew_ = true ;
  private CalendarEvent calendarEvent_ = null ;
  private String errorMsg_ = null ;
  private String calType_ = "0" ;

  private String oldCalendarId_ = null ;
  private String newCalendarId_ = null ;
  private String newCategoryId_ = null ;
  private Map<String, String> delegators_ = new LinkedHashMap<String, String>() ;

  public UITaskForm() throws Exception {
    super("UIEventForm");
    UITaskDetailTab uiTaskDetailTab =  new UITaskDetailTab(TAB_TASKDETAIL) ;
    addChild(uiTaskDetailTab) ;
    UIEventReminderTab eventReminderTab =  new UIEventReminderTab(TAB_TASKREMINDER) ;
    addChild(eventReminderTab) ;
    setSelectedTab(uiTaskDetailTab.getId()) ;
  }
  public String getLabel(String id) {
    String label = id ;
    try {
      label = super.getLabel(id) ;
    } catch (Exception e) {
      //e.printStackTrace() ;
    }
    return label ;
  }
  public void reset() {
    super.reset() ;
    calendarEvent_ = null;
  }
  private SessionProvider getSession() {
    return SessionProviderFactory.createSessionProvider() ;
  }
  private SessionProvider getSystemSession() {
    return SessionProviderFactory.createSystemProvider() ;
  }
  public void setSelectedEventState(String value) {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    taskDetailTab.getUIFormSelectBox(UITaskDetailTab.FIELD_STATUS).setValue(value) ;
  }

  public void initForm(CalendarSetting calSetting, CalendarEvent eventCalendar, String formTime) throws Exception {
    reset() ;
    String dateFormat = calSetting.getDateFormat() ;
    String timeFormat = calSetting.getTimeFormat() ;

    UITaskDetailTab taskDetailTab = getChildById(TAB_TASKDETAIL) ;
    ((UIFormDateTimePicker)taskDetailTab.getChildById(UITaskDetailTab.FIELD_FROM)).setDateFormatStyle(dateFormat) ;
    ((UIFormDateTimePicker)taskDetailTab.getChildById(UITaskDetailTab.FIELD_TO)).setDateFormatStyle(dateFormat) ;
    List<SelectItemOption<String>> fromTimes 
    = CalendarUtils.getTimesSelectBoxOptions(calSetting.getTimeFormat(), calSetting.getTimeFormat(), calSetting.getTimeInterval()) ;
    List<SelectItemOption<String>> toTimes 
    = CalendarUtils.getTimesSelectBoxOptions(calSetting.getTimeFormat(), calSetting.getTimeFormat(), calSetting.getTimeInterval()) ;
    taskDetailTab.getUIFormComboBox(UITaskDetailTab.FIELD_FROM_TIME).setOptions(fromTimes) ;
    taskDetailTab.getUIFormComboBox(UITaskDetailTab.FIELD_TO_TIME).setOptions(toTimes) ;
    if(eventCalendar != null) {
      oldCalendarId_ = eventCalendar.getCalType() + CalendarUtils.COLON + eventCalendar.getCalendarId();
      isAddNew_ = false ;
      calendarEvent_ = eventCalendar ;
      setEventSumary(eventCalendar.getSummary()) ;
      setEventDescription(eventCalendar.getDescription()) ;
      setEventAllDate(CalendarUtils.isAllDayEvent(eventCalendar)) ;
      setEventFromDate(eventCalendar.getFromDateTime(),dateFormat, timeFormat) ;
      setEventToDate(eventCalendar.getToDateTime(),calSetting.getDateFormat(),  calSetting.getTimeFormat()) ;
      setSelectedCalendarId(eventCalendar.getCalendarId()) ;
      setSelectedCategory(eventCalendar.getEventCategoryId()) ;
      setEventDelegation(eventCalendar.getTaskDelegator()) ;
      setSelectedEventPriority(eventCalendar.getPriority()) ;
      setEventReminders(eventCalendar.getReminders()) ;
      setAttachments(eventCalendar.getAttachment()) ;
      setSelectedEventState(eventCalendar.getEventState()) ;
      if(CalendarUtils.SHARED_TYPE.equals(calType_) || CalendarUtils.PUBLIC_TYPE.equals(calType_)){
        boolean isContains = false ;
        CalendarService calService = CalendarUtils.getCalendarService();
        List<EventCategory> listCategory = 
          calService.getEventCategories(SessionProviderFactory.createSessionProvider(), CalendarUtils.getCurrentUser());
        for(EventCategory eventCat : listCategory) {
          isContains = eventCat.getName().toLowerCase().equals(eventCalendar.getEventCategoryId().toLowerCase()) ;
          if(isContains) break ;
        }
        if(!isContains && eventCalendar.getEventCategoryId() != null) {
          SelectItemOption<String> item = new SelectItemOption<String>(eventCalendar.getEventCategoryId(), eventCalendar.getEventCategoryId()) ;
          UIFormSelectBox uiSelectBox = taskDetailTab.getUIFormSelectBox(UIEventDetailTab.FIELD_CATEGORY) ;
          uiSelectBox.getOptions().add(item) ;
          newCategoryId_ = eventCalendar.getEventCategoryId() ;
          uiSelectBox.setValue(eventCalendar.getEventCategoryId());
          if(!isAddNew_ && String.valueOf(Calendar.TYPE_SHARED).equals(calType_)){
            uiSelectBox.setDisabled(true) ;
            taskDetailTab.getUIFormSelectBoxGroup(UITaskDetailTab.FIELD_CALENDAR).setDisabled(true) ;
            taskDetailTab.setActionField(UIEventDetailTab.FIELD_CATEGORY, null) ;
          }
        }
      }
    } else {
      UIMiniCalendar miniCalendar = getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UIMiniCalendar.class) ;
      java.util.Calendar cal = CalendarUtils.getInstanceTempCalendar() ;
      try {
        cal.setTimeInMillis(Long.parseLong(formTime)) ;
      } catch (Exception e) {
        cal.setTime(miniCalendar.getCurrentCalendar().getTime()) ;
      }
      Long beginMinute = (cal.get(java.util.Calendar.MINUTE)/calSetting.getTimeInterval())*calSetting.getTimeInterval() ;
      cal.set(java.util.Calendar.MINUTE, beginMinute.intValue()) ;
      setEventFromDate(cal.getTime(),dateFormat, timeFormat) ;
      cal.add(java.util.Calendar.MINUTE, (int)calSetting.getTimeInterval()*2) ;
      setEventToDate(cal.getTime(),calSetting.getDateFormat(), calSetting.getTimeFormat()) ;
      setEventDelegation(CalendarUtils.getCurrentUser()) ;
    }
  }

  public static List<SelectItemOption<String>> getCategory() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    List<EventCategory> eventCategories = calendarService.getEventCategories(SessionProviderFactory.createSessionProvider(), CalendarUtils.getCurrentUser()) ;
    for(EventCategory category : eventCategories) {
      options.add(new SelectItemOption<String>(category.getName(), category.getName())) ;
    }
    return options ;
  }

  protected void refreshCategory()throws Exception {
    UIFormInputWithActions taskDetailTab = getChildById(TAB_TASKDETAIL) ;
    taskDetailTab.getUIFormSelectBox(UITaskDetailTab.FIELD_CATEGORY).setOptions(getCategory()) ;
  }
  protected String getStatus() {
    UITaskDetailTab uiTaskDetailTab = getChildById(TAB_TASKDETAIL) ;
    return uiTaskDetailTab.getUIFormSelectBox(UITaskDetailTab.FIELD_STATUS).getValue() ;
  }
  protected void setStatus(String value) {
    UITaskDetailTab uiTaskDetailTab = getChildById(TAB_TASKDETAIL) ;
    uiTaskDetailTab.getUIFormSelectBox(UITaskDetailTab.FIELD_STATUS).setValue(value) ;
  }
  public String[] getActions() {
    return new String[]{"AddAttachment","Save", "Cancel"} ;
  }
  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public void updateSelect(String selectField, String value) throws Exception {
    if(value.lastIndexOf("/") > 0) value = value.substring(value.lastIndexOf("/") + 1) ;
    delegators_.put(value, value) ;
    StringBuffer sb = new StringBuffer() ;
    for(String s : delegators_.values()) {
      if(sb.length() > 0) sb.append(CalendarUtils.COMMA) ;
      sb.append(s) ;
    }
    getUIStringInput(selectField).setValue(sb.toString()) ;
  }

  protected boolean isEventDetailValid(CalendarSetting calendarSetting){
    if(CalendarUtils.isEmpty(getCalendarId())) {
      errorMsg_ = getId() + ".msg.event-calendar-required" ;
      return false ;
    } 
    if(CalendarUtils.isEmpty(getEventCategory())) {
      errorMsg_ = getId() + ".msg.event-category-required" ;
      return false ;
    }
    if(CalendarUtils.isEmpty(getEventFormDateValue())) {
      errorMsg_ = getId() + ".msg.event-fromdate-required" ;
      return false ;
    } 
    if(!getEventAllDate()) {
      if(CalendarUtils.isEmpty(getEventToDateValue())){
        errorMsg_ = getId() + ".msg.event-todate-required" ;
        return false ;
      } 
    }
    try {
      getEventFromDate(calendarSetting.getDateFormat(), calendarSetting.getTimeFormat()) ;
    } catch (Exception e) {
      e.printStackTrace() ;
      errorMsg_ = getId() +  ".msg.event-fromdate-notvalid" ;
      return false ;
    }
    try {
      getEventToDate(calendarSetting.getDateFormat(), calendarSetting.getTimeFormat()) ;
    } catch (Exception e) {
      e.printStackTrace() ;
      errorMsg_ = getId() +  ".msg.event-fromdate-notvalid" ;
      return false ;
    }
    if(getEmailReminder()) {
      if(CalendarUtils.isEmpty(getEmailAddress())) {
        errorMsg_ = "UITaskForm.msg.event-email-required" ;
        return false ;
      }
      else if(!CalendarUtils.isAllEmailValid(getEmailAddress())) {
        errorMsg_ = "UITaskForm.msg.event-email-invalid" ;
        return false ;
      } 
    }
    errorMsg_ = null ;
    return true ;
  }

  protected String getEventSumary() {
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    return taskDetailTab.getUIStringInput(UITaskDetailTab.FIELD_EVENT).getValue() ;
  }
  protected void setEventSumary(String value) {
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    taskDetailTab.getUIStringInput(UITaskDetailTab.FIELD_EVENT).setValue(value) ;
  }
  protected String getEventDescription() {
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    return taskDetailTab.getUIFormTextAreaInput(UITaskDetailTab.FIELD_DESCRIPTION).getValue() ;
  }
  protected void setEventDescription(String value) {
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    taskDetailTab.getUIFormTextAreaInput(UITaskDetailTab.FIELD_DESCRIPTION).setValue(value) ;
  }
  protected String getCalendarId() {
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    String value = taskDetailTab.getUIFormSelectBoxGroup(UITaskDetailTab.FIELD_CALENDAR).getValue() ;
    if(oldCalendarId_ != null) newCalendarId_ = value ;
    if(value != null && value.trim().length() > 0 && value.split(CalendarUtils.COLON).length > 0) {
      calType_ = value.split(CalendarUtils.COLON)[0] ;
      return value.split(CalendarUtils.COLON)[1] ;
    }
    return null ;
  }
  public void setSelectedCalendarId(String value) {
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    value = calType_ + CalendarUtils.COLON + value ;
    taskDetailTab.getUIFormSelectBoxGroup(UITaskDetailTab.FIELD_CALENDAR).setValue(value) ;
  }

  protected String getEventCategory() {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    return taskDetailTab.getUIFormSelectBox(UITaskDetailTab.FIELD_CATEGORY).getValue() ;
  }
  public void setSelectedCategory(String value) {
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    taskDetailTab.getUIFormSelectBox(UITaskDetailTab.FIELD_CATEGORY).setValue(value) ;
  }

  protected Date getEventFromDate(String dateFormat, String timeFormat) throws Exception {
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    UIFormComboBox timeField = taskDetailTab.getUIFormComboBox(UITaskDetailTab.FIELD_FROM_TIME) ;
    UIFormDateTimePicker fromField = taskDetailTab.getChildById(UITaskDetailTab.FIELD_FROM) ;
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    Locale locale = context.getParentAppRequestContext().getLocale() ;
    if(getEventAllDate()) {
      DateFormat df = new SimpleDateFormat(dateFormat, locale) ;
      df.setCalendar(CalendarUtils.getInstanceTempCalendar()) ;
      return CalendarUtils.getBeginDay(df.parse(fromField.getValue())).getTime();
    } 
    DateFormat df = new SimpleDateFormat(dateFormat + " "  + timeFormat, locale) ;
    df.setCalendar(CalendarUtils.getInstanceTempCalendar()) ;
    return df.parse(fromField.getValue() + " " + timeField.getValue()) ;
  }
  protected String getEventFormDateValue () {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    UIFormDateTimePicker fromField = taskDetailTab.getChildById(UITaskDetailTab.FIELD_FROM) ;
    return fromField.getValue() ;
  }
  protected void setEventFromDate(Date date,String dateFormat, String timeFormat) throws Exception{
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    Locale locale = context.getParentAppRequestContext().getLocale() ;
    ((UIFormDateTimePicker)taskDetailTab.getChildById(UITaskDetailTab.FIELD_FROM))
    .setValue(CalendarUtils.parse(date, dateFormat, locale)) ;
    taskDetailTab.getUIFormComboBox(UITaskDetailTab.FIELD_FROM_TIME)
    .setValue(CalendarUtils.parse(date,timeFormat, locale)) ;    

  }

  protected Date getEventToDate(String dateFormat, String timeFormat) throws Exception {
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    UIFormComboBox timeField = taskDetailTab.getUIFormComboBox(UITaskDetailTab.FIELD_TO_TIME) ;
    UIFormDateTimePicker toField = taskDetailTab.getChildById(UITaskDetailTab.FIELD_TO) ;
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    Locale locale = context.getParentAppRequestContext().getLocale() ;
    if(getEventAllDate()) {
      DateFormat df = new SimpleDateFormat(dateFormat, locale) ;
      df.setCalendar(CalendarUtils.getInstanceTempCalendar()) ;
      return CalendarUtils.getEndDay(df.parse(toField.getValue())).getTime();
    } 
    DateFormat df = new SimpleDateFormat(dateFormat + " " + timeFormat, locale) ;
    df.setCalendar(CalendarUtils.getInstanceTempCalendar()) ;
    return df.parse(toField.getValue() + " " + timeField.getValue()) ;
  }
  protected String getEventToDateValue () {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    UIFormDateTimePicker toField = taskDetailTab.getChildById(UITaskDetailTab.FIELD_TO) ;
    return toField.getValue() ;
  }
  protected void setEventToDate(Date date,String dateFormat,  String timeFormat) throws Exception{
    UITaskDetailTab taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    Locale locale = context.getParentAppRequestContext().getLocale() ;
    ((UIFormDateTimePicker)taskDetailTab.getChildById(UITaskDetailTab.FIELD_TO))
    .setValue(CalendarUtils.parse(date, dateFormat, locale)) ;
    taskDetailTab.getUIFormComboBox(UITaskDetailTab.FIELD_TO_TIME)
    .setValue(CalendarUtils.parse(date, timeFormat, locale)) ; 
  }

  protected boolean getEventAllDate() {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    return taskDetailTab.getUIFormCheckBoxInput(UITaskDetailTab.FIELD_CHECKALL).isChecked() ;
  }
  protected void setEventAllDate(boolean isCheckAll) {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    taskDetailTab.getUIFormCheckBoxInput(UITaskDetailTab.FIELD_CHECKALL).setChecked(isCheckAll) ;
  }
  protected String getEventDelegation() throws Exception {
    delegators_.clear() ;
    String values = getEventDelegationValue() ;
    StringBuffer sb = new StringBuffer() ;
    if(!CalendarUtils.isEmpty(values)) {
      for(String s : values.split(CalendarUtils.COMMA)) {
        s = s.trim() ;
        delegators_.put(s.trim(),s.trim()) ; 
      }
      for(String s : delegators_.values()) {
        if(!CalendarUtils.isEmpty(s)) {
          if(sb.length() > 0) sb.append(CalendarUtils.COMMA) ;
          sb.append(s) ;
        }
      }
      return sb.toString() ; 
    } else {
      return null ;
    } 
  }
  protected String[] getEventDelegationAll() {
    delegators_.clear() ;
    String values = getEventDelegationValue() ;
    if(!CalendarUtils.isEmpty(values)) {
      for(String s : values.split(CalendarUtils.COMMA)) {
        s = s.trim() ;
        delegators_.put(s.trim(),s.trim()) ; 
      }
      return delegators_.values().toArray(new String[delegators_.values().size()]) ;
    } else {
      return null ;
    } 
  }
  protected String getEventDelegationValue() {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    return taskDetailTab.getUIStringInput(UITaskDetailTab.FIELD_DELEGATION).getValue();
  }
  protected void setEventDelegation(String value) {
    if(!CalendarUtils.isEmpty(value)) {
      for(String s : value.split(CalendarUtils.COMMA)) {
        s = s.trim() ;
        delegators_.put(s, s) ;
      }
    } else {
      delegators_.clear() ;
    }
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKDETAIL) ;
    taskDetailTab.getUIStringInput(UITaskDetailTab.FIELD_DELEGATION).setValue(value) ;
  }

  protected boolean getEmailReminder() {
    UIEventReminderTab taskReminderTab =  getChildById(TAB_TASKREMINDER) ;
    return taskReminderTab.getUIFormCheckBoxInput(UIEventReminderTab.REMIND_BY_EMAIL).isChecked() ;
  }
  protected void setEmailReminder(boolean isChecked) {
    UIEventReminderTab taskReminderTab =  getChildById(TAB_TASKREMINDER) ;
    taskReminderTab.getUIFormCheckBoxInput(UIEventReminderTab.REMIND_BY_EMAIL).setChecked(isChecked) ;
  }
  protected String getEmailRemindBefore() {
    UIEventReminderTab taskReminderTab =  getChildById(TAB_TASKREMINDER) ;
    return taskReminderTab.getUIStringInput(UIEventReminderTab.EMAIL_REMIND_BEFORE).getValue() ;
  }
  protected String isEmailRepeat() {
    UIFormInputWithActions eventDetailTab =  getChildById(TAB_TASKREMINDER) ;
    return String.valueOf("repeat".equals(eventDetailTab.getUIFormSelectBox(UIEventReminderTab.EMAIL_IS_REPEAT).getValue())) ;
  }
  public void setEmailRepeat(String value) {
    UIFormInputWithActions eventReminderTab =  getChildById(TAB_TASKREMINDER) ;
    if(Boolean.parseBoolean(value)) value = "repeat" ;
    else value = "no-repeat" ;
    eventReminderTab.getUIFormSelectBox(UIEventReminderTab.EMAIL_IS_REPEAT).setValue(value) ;
  }
  protected String getEmailRepeatInterVal() {
    UIFormInputWithActions eventDetailTab =  getChildById(TAB_TASKREMINDER) ;
    return eventDetailTab.getUIStringInput(UIEventReminderTab.EMAIL_REPEAT_INTERVAL).getValue() ;
  }
  protected void setEmailReminderBefore(String value) {
    UIEventReminderTab taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    taskDetailTab.getUIStringInput(UIEventReminderTab.EMAIL_REMIND_BEFORE).setValue(value) ;
  }

  protected String getEmailAddress() {
    UIEventReminderTab taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    return taskDetailTab.getUIStringInput(UIEventReminderTab.FIELD_EMAIL_ADDRESS).getValue() ;
  }

  protected void setEmailAddress(String value) {
    UIEventReminderTab taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    taskDetailTab.getUIFormTextAreaInput(UIEventReminderTab.FIELD_EMAIL_ADDRESS).setValue(value) ;
  }

  protected boolean getPopupReminder() {
    UIEventReminderTab taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    return taskDetailTab.getUIFormCheckBoxInput(UIEventReminderTab.REMIND_BY_POPUP).isChecked() ;
  }
  protected void setPopupReminder(boolean isChecked) {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    taskDetailTab.getUIFormCheckBoxInput(UIEventReminderTab.REMIND_BY_POPUP).setChecked(isChecked) ;
  }
  protected String getPopupReminderTime() {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    return taskDetailTab.getUIStringInput(UIEventReminderTab.POPUP_REMIND_BEFORE).getValue() ;
  }
  protected String isPopupRepeat() {
    UIFormInputWithActions eventDetailTab =  getChildById(TAB_TASKREMINDER) ;
    return String.valueOf("repeat".equals(eventDetailTab.getUIFormSelectBox(UIEventReminderTab.POPUP_IS_REPEAT).getValue())) ;
  }
  protected void setPopupRepeat(String value) {
    UIFormInputWithActions eventReminderTab =  getChildById(TAB_TASKREMINDER) ;
    if(Boolean.parseBoolean(value)) value = "repeat" ;
    else value = "no-repeat" ;
    eventReminderTab.getUIFormSelectBox(UIEventReminderTab.POPUP_IS_REPEAT).setValue(value) ;
  }

  protected String getPopupRepeatInterVal() {
    UIFormInputWithActions eventDetailTab =  getChildById(TAB_TASKREMINDER) ;
    return eventDetailTab.getUIStringInput(UIEventReminderTab.POPUP_REPEAT_INTERVAL).getValue() ;
  }
  protected void setPopupReminderTime(String value) {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    taskDetailTab.getUIStringInput(UIEventReminderTab.POPUP_REMIND_BEFORE).setValue(value) ;
  }
  protected long getPopupReminderSnooze() {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    try {
      String time =  taskDetailTab.getUIFormSelectBox(UIEventReminderTab.POPUP_REPEAT_INTERVAL).getValue() ;
      return Long.parseLong(time) ;
    } catch (Exception e){
      e.printStackTrace() ;
    }
    return 0 ;
  }
  protected void setPopupReminderSnooze(long value) {
    UIFormInputWithActions taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    taskDetailTab.getUIFormSelectBox(UIEventReminderTab.POPUP_REPEAT_INTERVAL).setValue(String.valueOf(value)) ;
  }
  protected List<Attachment>  getAttachments(String eventId, boolean isAddNew) {
    UITaskDetailTab taskDetailTab = getChild(UITaskDetailTab.class) ;
    return taskDetailTab.getAttachments() ;
  }
  protected void setAttachments(List<Attachment> attachment) throws Exception {
    UITaskDetailTab taskDetailTab = getChild(UITaskDetailTab.class) ;
    taskDetailTab.setAttachments(attachment) ;
    taskDetailTab.refreshUploadFileList() ;
  }
  protected void setEventReminders(List<Reminder> reminders){
    UIEventReminderTab taskDetailTab =  getChildById(TAB_TASKREMINDER) ;
    for(Reminder r : reminders) {
      if(Reminder.TYPE_EMAIL.equals(r.getReminderType())) {
        setEmailReminder(true) ;
        setEmailAddress(r.getEmailAddress()) ;
        setEmailRepeat(String.valueOf(r.isRepeat())) ;
        setEmailReminderBefore(String.valueOf(r.getAlarmBefore())) ;
        //taskDetailTab.getUIFormSelectBox(UIEventReminderTab.EMAIL_IS_REPEAT).setValue(String.valueOf(r.isRepeat())) ;
        taskDetailTab.getUIFormSelectBox(UIEventReminderTab.EMAIL_REPEAT_INTERVAL).setValue(String.valueOf(r.getRepeatInterval())) ;
      }else if(Reminder.TYPE_POPUP.equals(r.getReminderType())) {
        setPopupReminder(true) ;
        setPopupRepeat(String.valueOf(r.isRepeat())) ;
        taskDetailTab.getUIFormSelectBox(UIEventReminderTab.POPUP_REMIND_BEFORE).setValue(String.valueOf(r.getAlarmBefore())) ;
        //taskDetailTab.getUIFormSelectBox(UIEventReminderTab.POPUP_IS_REPEAT).setValue(String.valueOf(r.isRepeat())) ;
        taskDetailTab.getUIFormSelectBox(UIEventReminderTab.POPUP_REPEAT_INTERVAL).setValue(String.valueOf(r.getRepeatInterval())) ;
      } 
    }
  }

  protected List<Reminder>  getEventReminders(Date fromDateTime) throws Exception {
    List<Reminder> reminders = new ArrayList<Reminder>() ;
    if(getEmailReminder()) { 
      Reminder email = new Reminder() ;
      email.setReminderType(Reminder.TYPE_EMAIL) ;
      email.setAlarmBefore(Long.parseLong(getEmailRemindBefore())) ;
      email.setEmailAddress(getEmailAddress()) ;
      email.setRepeate(Boolean.parseBoolean(isEmailRepeat())) ;
      email.setRepeatInterval(Long.parseLong(getEmailRepeatInterVal())) ;
      email.setFromDateTime(fromDateTime) ;
      reminders.add(email) ;
    }
    if(getPopupReminder()) {
      Reminder popup = new Reminder() ;
      popup.setReminderType(Reminder.TYPE_POPUP) ;
      popup.setAlarmBefore(Long.parseLong(getPopupReminderTime())) ;
      popup.setRepeate(Boolean.parseBoolean(isPopupRepeat())) ;
      popup.setRepeatInterval(Long.parseLong(getPopupRepeatInterVal())) ;
      popup.setFromDateTime(fromDateTime) ;
      StringBuffer sb = new StringBuffer() ;
      boolean isExist = false ;
      if(getEventDelegationAll() != null) {
        for(String s : getEventDelegationAll()) {
          if(s.equals(CalendarUtils.getCurrentUser())) {
            isExist = true ;
            break ;
          }
        }
        for(String s : getEventDelegationAll()) {
          if(sb.length() > 0) sb.append(CalendarUtils.COMMA) ;
          sb.append(s) ;
        }
      }
      if(!isExist) {
        if(sb.length() >0) sb.append(CalendarUtils.COMMA);
        sb.append(CalendarUtils.getCurrentUser()) ;
      }
      popup.setReminderOwner(sb.toString()) ;
      reminders.add(popup) ;
    }
    return reminders ;
  }

  protected String getEventPriority() {
    UIFormInputWithActions eventDetailTab =  getChildById(TAB_TASKDETAIL) ;
    return eventDetailTab.getUIFormSelectBox(UIEventDetailTab.FIELD_PRIORITY).getValue() ;
  }
  protected void setSelectedEventPriority(String value) {
    UIFormInputWithActions eventDetailTab =  getChildById(TAB_TASKDETAIL) ;
    eventDetailTab.getUIFormSelectBox(UIEventDetailTab.FIELD_PRIORITY).setValue(value) ;
  }

  public void update(String calType, List<SelectItem> options) throws Exception{
    UITaskDetailTab uiTaskDetailTab = getChildById(TAB_TASKDETAIL) ;
    if(options != null) {
      uiTaskDetailTab.getUIFormSelectBoxGroup(UITaskDetailTab.FIELD_CALENDAR).setOptions(options) ;
    }else {
      uiTaskDetailTab.getUIFormSelectBoxGroup(UITaskDetailTab.FIELD_CALENDAR).setOptions(getCalendars()) ;
    }
    calType_ = calType ;
  }

  private List<SelectItem> getCalendars() throws Exception {
    return CalendarUtils.getCalendarOption() ;
  }
  protected long getTotalAttachment() {
    UITaskDetailTab uiTaskDetailTab = getChild(UITaskDetailTab.class) ;
    long attSize = 0 ; 
    for(Attachment att : uiTaskDetailTab.getAttachments()) {
      attSize = attSize + att.getSize() ;
    }
    return attSize ;
  }

  public Attachment getAttachment(String attId) {
    UITaskDetailTab uiDetailTab = getChildById(TAB_TASKDETAIL) ;
    for (Attachment att : uiDetailTab.getAttachments()) {
      if(att.getId().equals(attId)) {
        return att ;
      }
    }
    return null;
  }
  static  public class DownloadAttachmentActionListener extends EventListener<UITaskForm> {
    public void execute(Event<UITaskForm> event) throws Exception {
      UITaskForm uiForm = event.getSource() ;
      String attId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Attachment attach = uiForm.getAttachment(attId) ;
      if(attach != null) {
        String mimeType = attach.getMimeType().substring(attach.getMimeType().indexOf("/")+1) ;
        DownloadResource dresource = new InputStreamDownloadResource(attach.getInputStream(), mimeType);
        DownloadService dservice = (DownloadService)PortalContainer.getInstance().getComponentInstanceOfType(DownloadService.class);
        dresource.setDownloadName(attach.getName());
        String downloadLink = dservice.getDownloadLink(dservice.addDownloadResource(dresource));
        event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getChildById(TAB_TASKDETAIL)) ;
      }
    }
  }
  static  public class AddCategoryActionListener extends EventListener<UITaskForm> {
    public void execute(Event<UITaskForm> event) throws Exception {
      UITaskForm uiForm = event.getSource() ;
      UIPopupContainer uiContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
      UIPopupAction uiChildPopup = uiContainer.getChild(UIPopupAction.class) ;
      UIEventCategoryManager uiCategoryMan = uiChildPopup.activate(UIEventCategoryManager.class, 470) ;
      uiForm.setSelectedTab(TAB_TASKDETAIL) ;
      uiCategoryMan.categoryId_ = uiForm.getEventCategory() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiChildPopup) ;
    }
  }
  static  public class AddEmailAddressActionListener extends EventListener<UITaskForm> {
    public void execute(Event<UITaskForm> event) throws Exception {
      UITaskForm uiForm = event.getSource() ;
      uiForm.setSelectedTab(TAB_TASKREMINDER) ;
      if(!uiForm.getEmailReminder()) {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UITaskForm.msg.email-reminder-required", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      } else {
        UIPopupContainer uiPopupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
        UIPopupAction uiPopupAction  = uiPopupContainer.getChild(UIPopupAction.class) ;
        UIAddressForm uiAddressForm = uiPopupAction.activate(UIAddressForm.class, 640) ;
        uiAddressForm.setContactList("") ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
      }
    }
  }
  static  public class AddAttachmentActionListener extends EventListener<UITaskForm> {
    public void execute(Event<UITaskForm> event) throws Exception {
      UITaskForm uiForm = event.getSource() ;
      UIPopupContainer uiContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
      UIPopupAction uiChildPopup = uiContainer.getChild(UIPopupAction.class) ;
      UIAttachFileForm uiAttachFileForm = uiChildPopup.activate(UIAttachFileForm.class, 500) ;
      uiAttachFileForm.setAttSize(uiForm.getTotalAttachment()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiChildPopup) ;
    }
  }
  static  public class RemoveAttachmentActionListener extends EventListener<UITaskForm> {
    public void execute(Event<UITaskForm> event) throws Exception {
      UITaskForm uiForm = event.getSource() ;
      UIPopupContainer uiContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
      if(uiContainer != null) uiContainer.deActivate() ;
      UITaskDetailTab uiTaskDetailTab = uiForm.getChild(UITaskDetailTab.class) ;
      String attFileId = event.getRequestContext().getRequestParameter(OBJECTID);
      Attachment attachfile = new Attachment();
      for (Attachment att : uiTaskDetailTab.attachments_) {
        if (att.getId().equals(attFileId)) {
          attachfile = (Attachment) att;
        }
      }
      uiTaskDetailTab.removeFromUploadFileList(attachfile);
      uiTaskDetailTab.refreshUploadFileList() ;
      uiForm.setSelectedTab(TAB_TASKDETAIL) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }
  static  public class AddCalendarActionListener extends EventListener<UITaskForm> {
    public void execute(Event<UITaskForm> event) throws Exception {
    }
  }

  static  public class SelectUserActionListener extends EventListener<UITaskForm> {
    public void execute(Event<UITaskForm> event) throws Exception {
      UITaskForm uiForm = event.getSource() ;
      String value = uiForm.getEventDelegation() ;
      UIPopupContainer uiPopupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
      UIPopupAction uiPopupAction = uiPopupContainer.getChild(UIPopupAction.class) ;
      UIGroupSelector uiGroupSelector = uiPopupAction.activate(UIGroupSelector.class,500) ;
      uiGroupSelector.setType(UISelectComponent.TYPE_USER) ;
      uiGroupSelector.setSelectedGroups(null) ;
      uiGroupSelector.setFilter(false) ;
      uiForm.setEventDelegation(value) ;
      uiGroupSelector.setComponent(uiForm,new String[]{UITaskDetailTab.FIELD_DELEGATION}) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
    }
  }

  static  public class SaveActionListener extends EventListener<UITaskForm> {
    public void execute(Event<UITaskForm> event) throws Exception {
      UITaskForm uiForm = event.getSource() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction uiPopupAction = uiForm.getAncestorOfType(UIPopupAction.class) ;
      UICalendarViewContainer uiViewContainer = calendarPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      CalendarService calService = CalendarUtils.getCalendarService();
      if(uiForm.isEventDetailValid(calendarPortlet.getCalendarSetting())) {
        String username = CalendarUtils.getCurrentUser() ;
        String calendarId = uiForm.getCalendarId() ;
        String summary = uiForm.getEventSumary() ;
        if(!CalendarUtils.isNameValid(summary, CalendarUtils.SIMPLECHARACTER)){
          uiApp.addMessage(new ApplicationMessage("UIEventForm.msg.summary-invalid", CalendarUtils.SIMPLECHARACTER, ApplicationMessage.WARNING) ) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        String description = uiForm.getEventDescription() ;
        if(!CalendarUtils.isEmpty(description)) description = description.replaceAll(CalendarUtils.GREATER_THAN, "").replaceAll(CalendarUtils.SMALLER_THAN,"") ;
        CalendarEvent calendarEvent = null ;
        if(uiForm.isAddNew_){
          calendarEvent = new CalendarEvent() ; 
          calendarEvent.setEventType(CalendarEvent.TYPE_TASK) ;
        } else {
          calendarEvent = uiForm.calendarEvent_ ;
        }
        calendarEvent.setEventType(CalendarEvent.TYPE_TASK) ;
        calendarEvent.setSummary(summary) ;
        calendarEvent.setDescription(description) ;
        String delegation = uiForm.getEventDelegationValue() ;
        if(!CalendarUtils.isEmpty(delegation)) {
          OrganizationService orgService = CalendarUtils.getOrganizationService() ;
          for(String s : delegation.split(CalendarUtils.COMMA)) {
            s = s.trim() ;
            if(!CalendarUtils.isEmpty(s))
              if(orgService.getUserHandler().findUserByName(s) == null) {
                uiApp.addMessage(new ApplicationMessage("UIEventForm.msg.name-not-correct", new Object[]{s}, ApplicationMessage.WARNING)) ;
                event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
                return ;
              }  
          }
        }
        calendarEvent.setTaskDelegator(uiForm.getEventDelegation()) ;
        Date from = uiForm.getEventFromDate(calendarPortlet.getCalendarSetting().getDateFormat(), calendarPortlet.getCalendarSetting().getTimeFormat()) ;
        Date to = uiForm.getEventToDate(calendarPortlet.getCalendarSetting().getDateFormat(), calendarPortlet.getCalendarSetting().getTimeFormat()) ;
        if(from.after(to)) {
          uiApp.addMessage(new ApplicationMessage(uiForm.getId() + ".msg.event-date-time-logic", null, ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        } else if(from.equals(to)) {
          to = CalendarUtils.getEndDay(from).getTime() ;
        } 
        if(uiForm.getEventAllDate()) {
          java.util.Calendar tempCal = CalendarUtils.getInstanceTempCalendar() ;
          tempCal.setTime(to) ;
          tempCal.add(java.util.Calendar.MILLISECOND, -1) ;
          to = tempCal.getTime() ;
        }

        Calendar currentCalendar = null ;
        if(uiForm.calType_.equals(CalendarUtils.PRIVATE_TYPE)) {
          currentCalendar = calService.getUserCalendar(uiForm.getSession(), username, calendarId) ; 
        } else if(uiForm.calType_.equals(CalendarUtils.SHARED_TYPE)) {
          GroupCalendarData gCalendarData = calService.getSharedCalendars(uiForm.getSystemSession(), username, true) ;
          if( gCalendarData!= null && gCalendarData.getCalendarById(calendarId) != null) currentCalendar = gCalendarData.getCalendarById(calendarId) ;
        } else  if(uiForm.calType_.equals(CalendarUtils.PUBLIC_TYPE)) {
          currentCalendar = calService.getGroupCalendar(uiForm.getSystemSession(), calendarId) ;
        }
        if(currentCalendar == null) {
          uiPopupAction.deActivate() ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(calendarPortlet) ;
          uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        } else {
          boolean canEdit = false ;
          if(uiForm.calType_.equals(CalendarUtils.SHARED_TYPE)) {
            canEdit = CalendarUtils.canEdit(null, currentCalendar.getEditPermission(), username) ;
          } else if(uiForm.calType_.equals(CalendarUtils.PUBLIC_TYPE)) {
            canEdit = CalendarUtils.canEdit(CalendarUtils.getOrganizationService(), currentCalendar.getEditPermission(), username) ;
          }
          if(!canEdit && !uiForm.calType_.equals(CalendarUtils.PRIVATE_TYPE) ) {
            uiPopupAction.deActivate() ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-permission-to-edit", null, 1));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
        }

        calendarEvent.setCalType(uiForm.calType_) ;
        calendarEvent.setFromDateTime(from) ;
        calendarEvent.setToDateTime(to);
        calendarEvent.setCalendarId(calendarId) ;
        calendarEvent.setEventCategoryId(uiForm.getEventCategory()) ;
        calendarEvent.setEventState(uiForm.getStatus()) ;
        calendarEvent.setPriority(uiForm.getEventPriority()) ; 
        calendarEvent.setAttachment(uiForm.getAttachments(calendarEvent.getId(), uiForm.isAddNew_)) ;
        calendarEvent.setReminders(uiForm.getEventReminders(from)) ;
        try {
          if(uiForm.isAddNew_){
            if(uiForm.calType_.equals(CalendarUtils.PRIVATE_TYPE)) {
              CalendarUtils.getCalendarService().saveUserEvent(uiForm.getSession(), username, calendarId, calendarEvent, uiForm.isAddNew_) ;
            }else if(uiForm.calType_.equals(CalendarUtils.SHARED_TYPE)){
              CalendarUtils.getCalendarService().saveEventToSharedCalendar(uiForm.getSystemSession(), username, calendarId, calendarEvent, uiForm.isAddNew_) ;
            }else if(uiForm.calType_.equals(CalendarUtils.PUBLIC_TYPE)){
              CalendarUtils.getCalendarService().savePublicEvent(uiForm.getSystemSession(), calendarId, calendarEvent, uiForm.isAddNew_) ;          
            }
          } else {
            String fromCal = uiForm.oldCalendarId_.split(CalendarUtils.COLON)[1].trim() ;
            String toCal = uiForm.newCalendarId_.split(CalendarUtils.COLON)[1].trim() ;
            String fromType = uiForm.oldCalendarId_.split(CalendarUtils.COLON)[0].trim() ;
            String toType = uiForm.newCalendarId_.split(CalendarUtils.COLON)[0].trim() ;

            /*if((uiForm.calType_.equals(CalendarUtils.SHARED_TYPE) || uiForm.calType_.equals(CalendarUtils.PUBLIC_TYPE)) && uiForm.newCategoryId_ != null){
              EventCategory evc = new EventCategory() ;
              evc.setName(uiForm.newCategoryId_ ) ;
              calService.saveEventCategory(uiForm.getSession(), username, evc, null, true) ;
              uiViewContainer.updateCategory() ;
            }*/

            List<CalendarEvent> listEvent = new ArrayList<CalendarEvent>();
            listEvent.add(calendarEvent) ;
            calService.moveEvent(uiForm.getSession(), fromCal, toCal, fromType, toType, listEvent, username) ;
          }

          CalendarView calendarView = (CalendarView)uiViewContainer.getRenderedChild() ;
          if (calendarView instanceof UIListContainer)((UIListContainer)calendarView).setDisplaySearchResult(false) ;
          uiViewContainer.refresh() ;
          calendarView.setLastUpdatedEventId(calendarEvent.getId()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
          UIMiniCalendar uiMiniCalendar = calendarPortlet.findFirstComponentOfType(UIMiniCalendar.class) ;
          UICalendars uiCalendars = calendarPortlet.findFirstComponentOfType(UICalendars.class) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiMiniCalendar) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiCalendars) ;
          uiPopupAction.deActivate() ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
        }catch (Exception e) {
          uiApp.addMessage(new ApplicationMessage(uiForm.getId() + ".msg.add-event-error", null));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          e.printStackTrace() ;
        }
      } else {
        uiApp.addMessage(new ApplicationMessage(uiForm.errorMsg_, null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        uiForm.setSelectedTab(TAB_TASKDETAIL) ;
      }
    }
  }
  static  public class CancelActionListener extends EventListener<UITaskForm> {
    public void execute(Event<UITaskForm> event) throws Exception {
      UITaskForm uiForm = event.getSource() ;
      UIPopupAction uiPopupAction = uiForm.getAncestorOfType(UIPopupAction.class);
      uiPopupAction.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
    }
  }
}

