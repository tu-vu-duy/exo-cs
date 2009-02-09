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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.service.EventPageList;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.calendar.webui.UIActionBar;
import org.exoplatform.calendar.webui.UICalendarPortlet;
import org.exoplatform.calendar.webui.UICalendarViewContainer;
import org.exoplatform.calendar.webui.UIFormDateTimePicker;
import org.exoplatform.calendar.webui.UIListView;
import org.exoplatform.calendar.webui.UIPreview;
import org.exoplatform.calendar.webui.UIWeekView;
import org.exoplatform.calendar.webui.UIListView.CalendarEventComparator;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
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
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIAdvancedSearchForm.SearchActionListener.class),
      @EventConfig(listeners = UIAdvancedSearchForm.OnchangeActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIAdvancedSearchForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UIAdvancedSearchForm extends UIForm implements UIPopupComponent{
  final static  private String TEXT = "text" ;
  final static  private String TYPE = "type" ;
  final static  private String CALENDAR = "calendar" ;
  final static  private String CATEGORY = "category" ;
  final static  private String PRIORITY = "priority" ;
  final static  private String STATE = "state" ;
  final static  private String FROMDATE = "fromDate" ;
  final static  private String TODATE = "toDate" ;

  public UIAdvancedSearchForm() throws Exception{
    addChild(new UIFormStringInput(TEXT, TEXT, "")) ;
    List<SelectItemOption<String>> types = new ArrayList<SelectItemOption<String>>() ;
    types.add(new SelectItemOption<String>("", "")) ;
    types.add(new SelectItemOption<String>(CalendarEvent.TYPE_EVENT, CalendarEvent.TYPE_EVENT)) ;
    types.add(new SelectItemOption<String>(CalendarEvent.TYPE_TASK, CalendarEvent.TYPE_TASK)) ;
    UIFormSelectBox type =  new UIFormSelectBox(TYPE, TYPE, types) ;
    type.setOnChange("Onchange") ;
    addChild(type) ;
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    String username = CalendarUtils.getCurrentUser() ;
    CalendarService cservice = CalendarUtils.getCalendarService() ;
    options.add(new SelectItemOption<String>("", "")) ;
    for(Calendar cal : cservice.getUserCalendars(username, true)) {
      options.add(new SelectItemOption<String>(cal.getName(), Calendar.TYPE_PRIVATE + CalendarUtils.COLON + cal.getId())) ;
    }
    List<GroupCalendarData> groupCals  = cservice.getGroupCalendars(getSystemSession(), CalendarUtils.getUserGroups(username), true, username) ;
    for(GroupCalendarData groupData : groupCals) {
      if(groupData != null) {
        for(Calendar cal : groupData.getCalendars()) {
          options.add(new SelectItemOption<String>(cal.getName(), Calendar.TYPE_PUBLIC + CalendarUtils.COLON + cal.getId())) ;
        }
      }
    }
    GroupCalendarData sharedData  = cservice.getSharedCalendars(getSystemSession(), CalendarUtils.getCurrentUser(), true) ;
    if(sharedData != null) {
      for(Calendar cal : sharedData.getCalendars()) {
        String owner = "" ;
        if(cal.getCalendarOwner() != null) owner = cal.getCalendarOwner() + "- " ;
        options.add(new SelectItemOption<String>(owner + cal.getName(), Calendar.TYPE_SHARED + CalendarUtils.COLON + cal.getId())) ;
      }
    }
    addChild(new UIFormSelectBox(CALENDAR, CALENDAR, options)) ;
    options = new ArrayList<SelectItemOption<String>>() ;
    options.add(new SelectItemOption<String>("", "")) ;
    for(EventCategory cat : cservice.getEventCategories(getSession(), CalendarUtils.getCurrentUser())) {
      options.add(new SelectItemOption<String>(cat.getName(), cat.getId())) ;
    }
    addChild(new UIFormSelectBox(CATEGORY, CATEGORY, options)) ;
    addChild(new UIFormSelectBox(STATE, STATE, getStatus()).setRendered(false)) ;
    addChild(new UIFormSelectBox(PRIORITY, PRIORITY, getPriority())) ;
    addChild(new UIFormDateTimePicker(FROMDATE, FROMDATE, null, false)) ;
    addChild(new UIFormDateTimePicker(TODATE, TODATE, null, false)) ;
  }
  public void activate() throws Exception {}
  public void deActivate() throws Exception {

  }
  public void setSearchValue(String searchValue) {
    getUIStringInput(TEXT).setValue(searchValue) ;
  }  
  public UIFormDateTimePicker getUIFormDateTimePicker(String id){
    return findComponentById(id) ;
  }
  public String getFromDateValue() {
    return getUIFormDateTimePicker(FROMDATE).getValue() ;
  }
  public String getToDateValue() {
    return getUIFormDateTimePicker(TODATE).getValue() ;
  }
  public Date getFromDate() {
    DateFormat df = new SimpleDateFormat(CalendarUtils.DATEFORMAT) ;
    df.setCalendar(CalendarUtils.getInstanceTempCalendar()) ;
    if(getFromDateValue() != null) 
      try {
        return df.parse(getFromDateValue()) ;
      }  catch (Exception e) {
        return null ;
      }
      return null ;
  }
  public Date getToDate() {
    DateFormat df = new SimpleDateFormat(CalendarUtils.DATEFORMAT) ;
    df.setCalendar(CalendarUtils.getInstanceTempCalendar()) ;
    if(getToDateValue() != null) 
      try {
        return df.parse(getToDateValue()) ;
      }  catch (Exception e) {
        return null ;
      }
      return null ;
  }
  private List<SelectItemOption<String>> getPriority() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    options.add(new SelectItemOption<String>("", "")) ;
    options.add(new SelectItemOption<String>("normal", "normal")) ;
    options.add(new SelectItemOption<String>("high", "high")) ;
    options.add(new SelectItemOption<String>("low", "low")) ;
    return options ;
  }

  private List<SelectItemOption<String>> getStatus() {
    List<SelectItemOption<String>> status = new ArrayList<SelectItemOption<String>>() ;
    status.add(new SelectItemOption<String>("", "")) ;
    for(String taskStatus : CalendarEvent.TASK_STATUS) {
      status.add(new SelectItemOption<String>(taskStatus, taskStatus)) ;
    }
    return status ;
  }
  public String[] getPublicCalendars() throws Exception{
    String[] groups = CalendarUtils.getUserGroups(CalendarUtils.getCurrentUser()) ;
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    Map<String, String> map = new HashMap<String, String> () ;    
    for(GroupCalendarData group : calendarService.getGroupCalendars(getSystemSession(), groups, true, CalendarUtils.getCurrentUser())) {
      for(org.exoplatform.calendar.service.Calendar calendar : group.getCalendars()) {
        map.put(calendar.getId(), calendar.getId()) ;          
      }
    }
    return map.values().toArray(new String[map.values().size()] ) ;
  }
  private SessionProvider getSession() {
    return SessionProviderFactory.createSessionProvider() ;
  }
  private SessionProvider getSystemSession() {
    return SessionProviderFactory.createSystemProvider() ;
  }
  public boolean isSearchTask() {
    return getUIFormSelectBox(TYPE).getValue().equals(CalendarEvent.TYPE_TASK) ; 
  }
  public String getTaskState() {
    return getUIFormSelectBox(STATE).getValue() ;
  }
  public String[] getActions() {
    return new String[]{"Search","Cancel"} ;
  }
  public Boolean isValidate(){
    String value = getUIStringInput(TEXT).getValue();
    if(value == null) value = "" ;
    String formData = "";
    formData += value;
    formData += getUIFormSelectBox(TYPE).getValue();
    formData += getUIFormSelectBox(CALENDAR).getValue();
    formData += getUIFormSelectBox(CATEGORY).getValue();
    formData += getUIFormSelectBox(PRIORITY).getValue();
    formData += getFromDateValue() ;
    formData += getToDateValue() ;
    return !CalendarUtils.isEmpty(formData);
  }
  static  public class SearchActionListener extends EventListener<UIAdvancedSearchForm> {
    public void execute(Event<UIAdvancedSearchForm> event) throws Exception {
      UIAdvancedSearchForm uiForm = event.getSource() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      if(!CalendarUtils.isEmpty(uiForm.getFromDateValue()) && uiForm.getFromDate() == null){
        uiApp.addMessage(new ApplicationMessage("UIAdvancedSearchForm.msg.from-date-time-invalid", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ; 
      }
      if(!CalendarUtils.isEmpty(uiForm.getToDateValue()) && uiForm.getToDate() == null)  {
        uiApp.addMessage(new ApplicationMessage("UIAdvancedSearchForm.msg.to-date-time-invalid", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      
      if(uiForm.getFromDate() != null && uiForm.getToDate() != null) {
        if(uiForm.getFromDate().after(uiForm.getToDate())){
          uiApp.addMessage(new ApplicationMessage("UIAdvancedSearchForm.msg.date-time-invalid", null)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }
      String text = uiForm.getUIStringInput(UIAdvancedSearchForm.TEXT).getValue() ;
      if(!CalendarUtils.isEmpty(text)) {
        if(!CalendarUtils.isNameValid(text, CalendarUtils.EXTENDEDKEYWORD)) {
          uiApp.addMessage(new ApplicationMessage("UISearchForm.msg.error-text-to-search", null)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }      
      if(!uiForm.isValidate()){
        uiApp.addMessage(new ApplicationMessage("UISearchForm.msg.no-text-to-search", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      try {
        EventQuery query = new EventQuery() ;
        //query.setQueryType(Query.SQL) ;
        if(! CalendarUtils.isEmpty(text)) query.setText(CalendarUtils.encodeJCRText(text)) ;
        query.setEventType(uiForm.getUIFormSelectBox(UIAdvancedSearchForm.TYPE).getValue()) ;
        if(uiForm.isSearchTask()) query.setState(uiForm.getTaskState()) ; 
        String calendarId = uiForm.getUIFormSelectBox(UIAdvancedSearchForm.CALENDAR).getValue() ;
        if(calendarId != null && calendarId.trim().length() > 0) query.setCalendarId(new String[]{calendarId}) ;
        String categoryId = uiForm.getUIFormSelectBox(UIAdvancedSearchForm.CATEGORY).getValue() ;
        if(categoryId != null && categoryId.trim().length() > 0) query.setCategoryId(new String[]{categoryId}) ;
        java.util.Calendar cal = CalendarUtils.getInstanceTempCalendar() ;
        if(uiForm.getFromDate() != null && uiForm.getToDate() != null) {
          cal.setTime(uiForm.getFromDate()) ;
          query.setFromDate(CalendarUtils.getBeginDay(cal)) ;
          cal.setTime(uiForm.getToDate()) ;
          query.setToDate(CalendarUtils.getEndDay(cal)) ;
        } else if (uiForm.getFromDate() !=null) {
          cal.setTime(uiForm.getFromDate()) ;
          query.setFromDate(CalendarUtils.getBeginDay(cal)) ;
          //query.setToDate(CalendarUtils.getEndDay(cal)) ;
        } else if (uiForm.getToDate() !=null) {
          cal.setTime(uiForm.getToDate()) ;
          //query.setFromDate(CalendarUtils.getBeginDay(cal)) ;
          query.setToDate(CalendarUtils.getEndDay(cal)) ;
        }
        String priority = uiForm.getUIFormSelectBox(UIAdvancedSearchForm.PRIORITY).getValue() ;
        if(priority != null && priority.trim().length() > 0) query.setPriority(priority) ;
        String username = CalendarUtils.getCurrentUser() ;
        UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
        
        // cs-1953
        List<CalendarEvent> resultList =  
          CalendarUtils.getCalendarService().searchEvent(uiForm.getSession(), username, query, uiForm.getPublicCalendars()).getAll() ;
        UICalendarViewContainer calendarViewContainer = 
          calendarPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
        String currentView = calendarViewContainer.getRenderedChild().getId() ;
        if(calendarViewContainer.getRenderedChild() instanceof UIWeekView) {
          if(((UIWeekView)calendarViewContainer.getRenderedChild()).isShowCustomView()) currentView = UICalendarViewContainer.WORKING_VIEW;
        }
        calendarViewContainer.initView(UICalendarViewContainer.LIST_VIEW) ;
        UIListView uiListView = calendarViewContainer.findFirstComponentOfType(UIListView.class) ;
        calendarPortlet.cancelAction() ;
        
        CalendarEventComparator ceCompare = uiListView.ceCompare_ ;
        ceCompare.setCompareField(CalendarEventComparator.EVENT_SUMMARY);
        
        uiListView.ceCompare_.setCompareField(CalendarEventComparator.EVENT_SUMMARY) ;
        boolean order = false ;
        uiListView.ceCompare_.setRevertOrder(order) ;
        /*uiListView.setSortedField(CalendarEventComparator.EVENT_SUMMARY);
        
        ceCompare.setRevertOrder(order);
        uiListView.setIsAscending(order);*/
        Collections.sort(resultList, uiListView.ceCompare_);
        
        EventPageList pageList = new EventPageList(resultList ,10) ;
        uiListView.update(pageList) ;
        calendarViewContainer.setRenderedChild(UICalendarViewContainer.LIST_VIEW) ;
        uiListView.setViewType(UIListView.TYPE_BOTH) ;
        if(!uiListView.isDisplaySearchResult()) uiListView.setLastViewId(currentView) ;
        uiListView.setDisplaySearchResult(true) ;
        uiListView.setSelectedEvent(null) ;
        uiListView.setLastUpdatedEventId(null) ;
        calendarViewContainer.findFirstComponentOfType(UIPreview.class).setEvent(null) ;
        UIActionBar uiActionBar = calendarPortlet.findFirstComponentOfType(UIActionBar.class) ;
        uiActionBar.setCurrentView(UICalendarViewContainer.LIST_VIEW) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiActionBar) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(calendarViewContainer) ;
      } catch (Exception e) {
        e.printStackTrace() ;
        return ;
      }
    }
  }
  static  public class OnchangeActionListener extends EventListener<UIAdvancedSearchForm> {
    public void execute(Event<UIAdvancedSearchForm> event) throws Exception {
      UIAdvancedSearchForm uiForm = event.getSource() ;
      uiForm.getUIFormSelectBox(STATE).setRendered(uiForm.isSearchTask()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
    }
  }
  static  public class CancelActionListener extends EventListener<UIAdvancedSearchForm> {
    public void execute(Event<UIAdvancedSearchForm> event) throws Exception {
      UIAdvancedSearchForm uiForm = event.getSource() ;
      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      calendarPortlet.cancelAction() ;
    }
  }
}
