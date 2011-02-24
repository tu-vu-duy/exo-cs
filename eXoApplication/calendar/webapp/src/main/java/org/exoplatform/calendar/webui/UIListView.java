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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.EventPageListQuery;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(   
    lifecycle = UIFormLifecycle.class,
    events = {
      @EventConfig(listeners = UICalendarView.AddEventActionListener.class),      
      @EventConfig(listeners = UICalendarView.DeleteEventActionListener.class, confirm="UICalendarView.msg.confirm-delete"),
      @EventConfig(listeners = UICalendarView.AddCategoryActionListener.class), 
      @EventConfig(listeners = UICalendarView.SwitchViewActionListener.class),
      @EventConfig(listeners = UICalendarView.GotoDateActionListener.class),
      @EventConfig(listeners = UICalendarView.ViewActionListener.class),
      @EventConfig(listeners = UICalendarView.EditActionListener.class), 
      @EventConfig(listeners = UICalendarView.DeleteActionListener.class, confirm="UICalendarView.msg.confirm-delete"),
      @EventConfig(listeners = UIListView.CloseSearchActionListener.class),
      @EventConfig(listeners = UIListView.ViewDetailActionListener.class),
      @EventConfig(listeners = UICalendarView.MoveNextActionListener.class), 
      @EventConfig(listeners = UICalendarView.MovePreviousActionListener.class), 
      @EventConfig(listeners = UIListView.ShowPageActionListener.class ),
      @EventConfig(listeners = UICalendarView.ExportEventActionListener.class),
      @EventConfig(listeners = UIListView.OnchangeActionListener.class ),   
      @EventConfig(listeners = UIListView.SortActionListener.class )

    }
)
public class UIListView extends UICalendarView {
  private LinkedHashMap<String, CalendarEvent> eventMap_ = new LinkedHashMap<String, CalendarEvent>() ;
  private EventPageListQuery pageList_ = null ;
  private String selectedEvent_ = null ;
  private boolean isShowEventAndTask = true ;
  private boolean isSearchResult = false ;
  private String lastViewId_ = null ;
  private String categoryId_ = null ;
  private String keyWords_ = null ;
  private int currentPage_ = 0 ;
  private EventQuery query = null;
  
  public static final String EVENT_SUMMARY = Utils.EXO_SUMMARY;
  public static final String EVENT_PRIORITY = Utils.EXO_PRIORITY;
  public static final String EVENT_DESCRIPTION = Utils.EXO_DESCRIPTION;
  public static final String EVENT_START = Utils.EXO_FROM_DATE_TIME;
  public static final String EVENT_END = Utils.EXO_TO_DATE_TIME;
  private String sortedField_ = EVENT_SUMMARY;
  private boolean isAscending_ = true;
  
  public UIListView() throws Exception{
    if(getEvents().length > 0 ) {
      selectedEvent_ = getEvents()[0].getId() ;
    }
  } 

  public String getTemplate() {
    if( getViewType().equals(TYPE_TASK)) {
      return "app:/templates/calendar/webui/UIListTask.gtmpl" ;
    } else if(getViewType().equals(TYPE_EVENT)) {
      return "app:/templates/calendar/webui/UIListEvent.gtmpl" ;
    } else {
      return "app:/templates/calendar/webui/UIListView.gtmpl" ;
    }
  }
  
  public void setSortedField(String field) { sortedField_ = field; }
  public String getSortedField() { return sortedField_; }
  
  public void setIsAscending(boolean b) { isAscending_ = b; }
  public boolean isAscending() { return isAscending_; }
  
  public void setEventQuery(EventQuery eventQuery) { query = eventQuery; }
  public EventQuery getEventQuery() { return query; }
  
  public void refresh() throws Exception{
    UIListContainer uiListContainer = getParent() ;
    if (uiListContainer.isDisplaySearchResult()) return ;
    String username = CalendarUtils.getCurrentUser() ;
    query = new EventQuery() ;
    if(!CalendarUtils.isEmpty(categoryId_) && !categoryId_.toLowerCase().equals("null")&& !categoryId_.equals("calId")) query.setCategoryId(new String[]{categoryId_}) ;
    java.util.Calendar fromcalendar = getBeginDay(new GregorianCalendar(getCurrentYear(),  getCurrentMonth(),  getCurrentDay())) ;
    query.setFromDate(fromcalendar) ;
    java.util.Calendar tocalendar = getEndDay(new GregorianCalendar(getCurrentYear(), getCurrentMonth(), getCurrentDay())) ;
    if(tocalendar.get(Calendar.MILLISECOND) == 0) tocalendar.add(Calendar.MILLISECOND, -1);
    query.setToDate(tocalendar) ;
    if(!getViewType().equals(TYPE_BOTH)) {
      query.setEventType(getViewType()) ;
    }
    
    query.setExcludeRepeatEvent(true);
   
    // TODO CS-3152
    UICalendars uiCalendars = getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UICalendars.class);
    List<String> checkedCals = uiCalendars.getCheckedCalendars() ;
    List<String> calendarIds = new ArrayList<String>() ; 
    for (GroupCalendarData groupCalendarData : uiCalendars.getPrivateCalendars())
      for (org.exoplatform.calendar.service.Calendar cal : groupCalendarData.getCalendars())
        if (checkedCals.contains(cal.getId())) calendarIds.add(cal.getId());
    for (GroupCalendarData calendarData : uiCalendars.getPublicCalendars())
      for (org.exoplatform.calendar.service.Calendar  calendar : calendarData.getCalendars())
        if (checkedCals.contains(calendar.getId())) calendarIds.add(calendar.getId());
    GroupCalendarData shareClas = uiCalendars.getSharedCalendars();
    if (shareClas != null)
      for (org.exoplatform.calendar.service.Calendar cal : shareClas.getCalendars())
        if (checkedCals.contains(cal.getId())) {
          calendarIds.add(cal.getId());
        }
    
    if (calendarIds.size() > 0)
      query.setCalendarId(calendarIds.toArray(new String[] {}));
    else {
      query.setCalendarId(new String[] {"null"});
    }
    query.setOrderBy(new String[] {Utils.EXO_SUMMARY});
    if(uiListContainer.isDisplaySearchResult())  { update(pageList_) ;
    } else {
      update(new EventPageListQuery(username, query.getQueryStatement(),10)) ;
    }
    if(currentPage_ > 0 && currentPage_ <= pageList_.getAvailablePage()) {
      updateCurrentPage(currentPage_) ;
    }
    UIFormSelectBox uiCategory = getUIFormSelectBox(EVENT_CATEGORIES) ;
    uiCategory.setValue(categoryId_) ;
    uiCategory.setOnChange("Onchange") ;
    UIListContainer uiContainer = getParent() ;
    UIPreview view = uiContainer.getChild(UIPreview.class) ;
    if(CalendarUtils.isEmpty(getSelectedEvent())) {
      if(getEvents().length > 0) { 
        String eventId = getEvents()[0].getId() ;
        setSelectedEvent(eventId) ;  
        //setLastViewId(eventId) ;  
        setLastUpdatedEventId(eventId) ;
        view.setEvent(getEvents()[0]) ;
      } else {
        setSelectedEvent(null) ;
        view.setEvent(null) ;
        //setLastViewId(null) ;
        setLastUpdatedEventId(null) ;
      }
    } else {
      if(getEvents().length > 0) {
        for(CalendarEvent cal : getEvents()) {
          if(cal.getId().equals(getSelectedEvent())) {
            view.setEvent(cal) ;
            setLastUpdatedEventId(getSelectedEvent()) ;
            break ;
          }
        }

      } else {
        setSelectedEvent(null) ;
        view.setEvent(null) ;
        setLastUpdatedEventId(null) ;
      }
    }
  }
  public void update(EventPageListQuery pageList) throws Exception {
    pageList_ = pageList ;
    updateCurrentPage(pageList_.getCurrentPage()) ;
  }
  @SuppressWarnings("unchecked")
  protected void updateCurrentPage(long page) throws Exception{
    getChildren().clear() ;
    initCategories() ;
    UIFormSelectBox uiCategory = getUIFormSelectBox(EVENT_CATEGORIES) ;
    uiCategory.setValue(categoryId_) ;
    uiCategory.setOnChange("Onchange") ;
    eventMap_.clear();
    if(pageList_ != null) {
      for(CalendarEvent calendarEvent : pageList_.getPage(page ,CalendarUtils.getCurrentUser())) {
        UIFormCheckBoxInput<Boolean> checkbox = new UIFormCheckBoxInput<Boolean>(calendarEvent.getId(),calendarEvent.getId(), false) ;
        addUIFormInput(checkbox);
        if(getViewType().equals(TYPE_BOTH)){
          eventMap_.put(calendarEvent.getId(), calendarEvent) ;
        }
        else if(getViewType().equals(calendarEvent.getEventType())) {
          eventMap_.put(calendarEvent.getId(), calendarEvent) ;
        }
      }
    }
  }

  public CalendarEvent[] getEvents() throws Exception {
    if(eventMap_ == null || eventMap_.size() == 0) return new CalendarEvent[]{} ;
    return eventMap_.values().toArray(new CalendarEvent[]{}) ;    
  }
  public long getAvailablePage(){
    return pageList_.getAvailablePage() ; 
  }
  
  public void setCurrentPage(int page) { currentPage_ = page ;} 
  public long getCurrentPage() { return pageList_.getCurrentPage();}
  protected boolean isShowEvent() {return isShowEvent_ ;}

  protected boolean isShowEventAndTask() {return isShowEventAndTask ;}
  public void setShowEventAndTask(boolean show) {isShowEventAndTask = show ;}

  public boolean isDisplaySearchResult() {return isSearchResult ;}
  public void setDisplaySearchResult(boolean show) {isSearchResult = show ;}

  public void setSelectedEvent(String selectedEvent) { this.selectedEvent_ = selectedEvent ; }
  public String getSelectedEvent() { return selectedEvent_ ;}

  public LinkedHashMap<String, CalendarEvent> getDataMap(){
    return eventMap_ ;
  }
  static public class ViewDetailActionListener extends EventListener<UIListView> {
    public void execute(Event<UIListView> event) throws Exception {
      UIListView uiListView = event.getSource();
      String eventId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIListContainer uiListContainer = uiListView.getAncestorOfType(UIListContainer.class);
      UIPreview uiPreview = uiListContainer.getChild(UIPreview.class);
      CalendarEvent calendarEvent = null ;
      if(uiListView.getDataMap() != null) {
        calendarEvent = uiListView.getDataMap().get(eventId) ;
        if(calendarEvent != null) {
          uiListView.setLastUpdatedEventId(eventId) ;
          uiListView.setSelectedEvent(calendarEvent.getId()) ;
          uiPreview.setEvent(calendarEvent);
        } else {
          uiListView.setLastUpdatedEventId(eventId) ;
          uiListView.setSelectedEvent(null) ;
          uiPreview.setEvent(null);
        } 
        event.getRequestContext().addUIComponentToUpdateByAjax(uiListContainer);
      }
    }
  }
  public List<CalendarEvent> getSelectedEvents() {
    List<CalendarEvent> events = new ArrayList<CalendarEvent>() ;
    for(CalendarEvent ce : eventMap_.values()) {
      UIFormCheckBoxInput<Boolean>  checkbox = getChildById(ce.getId())  ;
      if(checkbox != null && checkbox.isChecked()) events.add(ce) ;
    }
    return events ; 
  }
  public void setLastViewId(String lastViewId_) {
    this.lastViewId_ = lastViewId_;
  }

  public String getLastViewId() {
    return lastViewId_;
  }
  public void setCategoryId(String catetoryId) {
    categoryId_  = catetoryId ;
    setSelectedCategory(catetoryId) ;
  }
  public String getSelectedCategory() {
    return categoryId_ ;
  }
  static public class CloseSearchActionListener extends EventListener<UIListView> {
    public void execute(Event<UIListView> event) throws Exception {
      UIListView uiListView = event.getSource() ;
      uiListView.setDisplaySearchResult(false) ;
      uiListView.setCategoryId(null) ;
      uiListView.refresh() ;
      UICalendarPortlet uiPortlet = uiListView.getAncestorOfType(UICalendarPortlet.class) ;
      UISearchForm uiSearchForm = uiPortlet.findFirstComponentOfType(UISearchForm.class) ;
      uiSearchForm.reset() ;
      UIActionBar uiActionBar = uiPortlet.findFirstComponentOfType(UIActionBar.class) ;
      uiActionBar.setCurrentView(uiListView.getLastViewId()) ;
      UICalendarViewContainer uiCalViewContainer = uiPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      uiCalViewContainer.initView(uiListView.getLastViewId()) ;
      uiListView.setLastViewId(null) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchForm) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionBar) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiCalViewContainer) ;
    }
  }
  static  public class ShowPageActionListener extends EventListener<UIListView> {
    public void execute(Event<UIListView> event) throws Exception {
      UIListView uiListView = event.getSource() ;
      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID)) ;
      uiListView.currentPage_ = page ;
      uiListView.updateCurrentPage(page) ; 
      event.getRequestContext().addUIComponentToUpdateByAjax(uiListView.getParent());           
    }
  }
  static  public class OnchangeActionListener extends EventListener<UIListView> {
    public void execute(Event<UIListView> event) throws Exception {
      UIListView uiListView = event.getSource() ;
      String categoryId = uiListView.getUIFormSelectBox(EVENT_CATEGORIES).getValue() ;
      uiListView.setCategoryId(categoryId) ;
      uiListView.refresh() ;
      //uiListView.updateCurrentPage(uiListView.pageList_.getCurrentPage()) ;
      UIMiniCalendar uiMiniCalendar = uiListView.getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UIMiniCalendar.class) ;
      uiMiniCalendar.setCategoryId(categoryId) ;
      UIPreview uiPreview = uiListView.getAncestorOfType(UIListContainer.class).getChild(UIPreview.class) ;
      if(uiListView.getEvents().length >0) {
        uiPreview.setEvent(uiListView.getEvents()[0]) ;
      } else {
        uiPreview.setEvent(null) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiListView.getParent());           
    }
  }
  public CalendarEvent getSelectedEventObj() {
    return eventMap_.get(selectedEvent_) ;
  }

  public void setKeyWords(String keyWords) {
    this.keyWords_ = keyWords;
  }

  public String getKeyWords() {
    return keyWords_;
  }
  
  static  public class SortActionListener extends EventListener<UIListView> {
    public void execute(Event<UIListView> event) throws Exception {
      UIListView uiListView = event.getSource() ;
      long currentPage = uiListView.getCurrentPage();
      String fieldId =  event.getRequestContext().getRequestParameter(OBJECTID) ;
      EventQuery query = uiListView.query ;
      UICalendars uiCalendars = uiListView.getAncestorOfType(UICalendarPortlet.class)
        .findFirstComponentOfType(UICalendars.class);
      List<String> checkedCals = uiCalendars.getCheckedCalendars() ;
      List<String> calendarIds = new ArrayList<String>() ; 
      for (GroupCalendarData groupCalendarData : uiCalendars.getPrivateCalendars())
        for (org.exoplatform.calendar.service.Calendar cal : groupCalendarData.getCalendars())
          if (checkedCals.contains(cal.getId())) calendarIds.add(cal.getId());
      for (GroupCalendarData calendarData : uiCalendars.getPublicCalendars())
        for (org.exoplatform.calendar.service.Calendar  calendar : calendarData.getCalendars())
          if (checkedCals.contains(calendar.getId())) calendarIds.add(calendar.getId());
      GroupCalendarData shareClas = uiCalendars.getSharedCalendars();
      if (shareClas != null)
        for (org.exoplatform.calendar.service.Calendar cal : shareClas.getCalendars())
          if (checkedCals.contains(cal.getId())) {
            calendarIds.add(cal.getId());
          }
      if (calendarIds.size() > 0)
        query.setCalendarId(calendarIds.toArray(new String[] {}));
      else {
        query.setCalendarId(new String[] {"null"});
      }      
      query.setOrderBy(new String[] {fieldId});
      uiListView.setIsAscending(!uiListView.isAscending());
      uiListView.setSortedField(fieldId);
      if (uiListView.isAscending()) query.setOrderType(Utils.ASCENDING);
      else query.setOrderType(Utils.DESCENDING);
      uiListView.update(new EventPageListQuery(CalendarUtils.getCurrentUser(), query.getQueryStatement(), 10));
      uiListView.updateCurrentPage(currentPage);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiListView); 
    }
  }
}

