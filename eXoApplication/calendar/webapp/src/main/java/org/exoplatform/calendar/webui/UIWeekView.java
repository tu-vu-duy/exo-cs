/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.webui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIViewRoot;

import org.apache.commons.collections.map.LinkedMap;
import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.webui.popup.UIPopupAction;
import org.exoplatform.calendar.webui.popup.UIQuickAddEvent;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.omg.PortableInterceptor.LOCATION_FORWARD;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle =UIFormLifecycle.class,
    template = "app:/templates/calendar/webui/UIWeekView.gtmpl",
    events = {
      @EventConfig(listeners = UICalendarView.AddEventActionListener.class),  
      @EventConfig(listeners = UICalendarView.DeleteEventActionListener.class),
      @EventConfig(listeners = UICalendarView.GotoDateActionListener.class),
      @EventConfig(listeners = UICalendarView.AddCategoryActionListener.class),
      @EventConfig(listeners = UICalendarView.SwitchViewActionListener.class),
      @EventConfig(listeners = UICalendarView.QuickAddActionListener.class), 
      @EventConfig(listeners = UICalendarView.ViewActionListener.class),
      @EventConfig(listeners = UICalendarView.EditActionListener.class), 
      @EventConfig(listeners = UICalendarView.DeleteActionListener.class),
      @EventConfig(listeners = UIWeekView.UpdateEventActionListener.class),
      @EventConfig(listeners = UIWeekView.MoveNextActionListener.class), 
      @EventConfig(listeners = UIWeekView.MovePreviousActionListener.class)
    }

)
public class UIWeekView extends UICalendarView {

  protected Map<String, List<CalendarEvent>> eventData_ = new HashMap<String, List<CalendarEvent>>() ;
  protected LinkedHashMap<String, CalendarEvent> allWeekData_ = new LinkedHashMap<String,  CalendarEvent>() ;
  protected  List<CalendarEvent> daysData_  = new ArrayList<CalendarEvent>() ;
  protected boolean isShowCustomView_ = false ;
  protected Date beginDate_ ;
  protected Date endDate_ ;

  public UIWeekView() throws Exception {
    super() ;
  }

  public void refresh() throws Exception {
    System.out.println("\n\n>>>>>>>>>> WEEK VIEW") ;
    int week = getCurrentWeek() ;
    eventData_.clear() ;
    allWeekData_.clear() ;
    for(Calendar c : getDaysOfWeek(week)) {
      List<CalendarEvent> list = new ArrayList<CalendarEvent>() ;
      String key = keyGen(c.get(Calendar.DATE), c.get(Calendar.MONTH), c.get(Calendar.YEAR)) ;
      eventData_.put(key, list) ;
    }
    CalendarService calendarService = getApplicationComponent(CalendarService.class) ;
    String username = Util.getPortalRequestContext().getRemoteUser() ;
    EventQuery eventQuery = new EventQuery() ;
    List<Calendar> days =  getDaysOfWeek(week) ;
    java.util.Calendar fromcalendar = days.get(0) ;
    fromcalendar.set(Calendar.HOUR, 0) ;
    eventQuery.setFromDate(fromcalendar) ;
    java.util.Calendar tocalendar = days.get(days.size() - 1) ;
    tocalendar.set(Calendar.HOUR, 0) ;
    tocalendar.add(Calendar.DATE, 1) ;
    eventQuery.setToDate(tocalendar) ;
    List<CalendarEvent> allEvents = calendarService.getUserEvents(username, eventQuery);    
    allEvents.addAll(calendarService.getPublicEvents(eventQuery))  ;
    Iterator iter = allEvents.iterator() ;
    while(iter.hasNext()) {
      CalendarEvent event = (CalendarEvent)iter.next() ;
      Date beginEvent = event.getFromDateTime() ;
      Date endEvent = event.getToDateTime() ;
      for(Calendar c : getDaysOfWeek(week)) {
        String key = keyGen(c.get(Calendar.DATE), c.get(Calendar.MONTH), c.get(Calendar.YEAR)) ;
        if(isSameDate(c.getTime(), beginEvent) &&  isSameDate(c.getTime(), endEvent)) { 
          eventData_.get(key).add(event) ;
          iter.remove() ;
        } 
      }
    }
    for( CalendarEvent ce : allEvents) {
      allWeekData_.put(ce.getId(), ce) ;
    }
  }
  protected void moveTo(int weeks) {
    calendar_.add(Calendar.WEEK_OF_YEAR, weeks) ;
  }

  protected List<Calendar> getDaysOfWeek(int week) {
    List<Calendar> calendarData = new ArrayList<Calendar>() ;
    Calendar cl = GregorianCalendar.getInstance() ;
    if(!isShowCustomView_) {
      cl.set(Calendar.WEEK_OF_YEAR, week) ;
      int day = cl.get(Calendar.DATE) ;
      int month = cl.get(Calendar.MONTH) ;
      int year = cl.get(Calendar.YEAR) ;
      int amount = cl.getFirstDayOfWeek() - cl.get(Calendar.DAY_OF_WEEK) ;
      cl = getDateByValue(year, month, day, UICalendarView.TYPE_DATE, amount) ;
      calendarData.add(cl) ;
      day = cl.get(Calendar.DATE) ;
      month = cl.get(Calendar.MONTH) ;
      year = cl.get(Calendar.YEAR) ;
      for(int d = 1 ;  d < 7 ; d++) {
        calendarData.add(getDateByValue(year, month, day, UICalendarView.TYPE_DATE, d)) ;
      }
    } else {
      cl.setTime(beginDate_) ;
      while(cl.before(endDate_)) {
        calendarData.add(cl) ;
        cl.add(Calendar.DATE, 1) ;
      }
    }
    return calendarData ;
  }

  protected Map<String, List<CalendarEvent>> getEventData() {return eventData_ ;}

  protected LinkedHashMap<String, CalendarEvent>  getEventList() {
    return allWeekData_ ;
  }

  static  public class QuickAddActionListener extends EventListener<UIWeekView> {
    public void execute(Event<UIWeekView> event) throws Exception {
      System.out.println("QuickAddActionListener");
      UIWeekView calendarview = event.getSource() ;
      String type = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String startTime = event.getRequestContext().getRequestParameter("startTime") ;
      String finishTime = event.getRequestContext().getRequestParameter("finishTime") ;
      UICalendarPortlet uiPortlet = calendarview.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction uiPopupAction = uiPortlet.getChild(UIPopupAction.class) ;
      UIQuickAddEvent uiQuickAddEvent = uiPopupAction.activate(UIQuickAddEvent.class, 600) ;
      if(CalendarEvent.TYPE_EVENT.equals(type)) {
        uiQuickAddEvent.setEvent(true) ;
        uiQuickAddEvent.setId("UIQuickAddEvent") ;
      } else {
        uiQuickAddEvent.setEvent(false) ;
        uiQuickAddEvent.setId("UIQuickAddTask") ;
      }
      try {
        Long.parseLong(startTime) ;
      }catch (Exception e) {
        startTime = null ;
      }
      try {
        Long.parseLong(finishTime) ;
      }catch (Exception e) {
        finishTime = null ;
      }
      uiQuickAddEvent.init(uiPortlet.getCalendarSetting(), startTime, finishTime) ;
      uiQuickAddEvent.update(CalendarUtils.PRIVATE_TYPE, null) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
    }
  }

  static  public class MoveNextActionListener extends EventListener<UIWeekView> {
    public void execute(Event<UIWeekView> event) throws Exception {
      UIWeekView calendarview = event.getSource() ;
      calendarview.moveTo(1) ;
      calendarview.refresh() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
    }
  }

  static  public class MovePreviousActionListener extends EventListener<UIWeekView> {
    public void execute(Event<UIWeekView> event) throws Exception {
      UIWeekView calendarview = event.getSource() ;
      calendarview.moveTo(-1) ;
      calendarview.refresh() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
    }
  }
  static  public class UpdateEventActionListener extends EventListener<UIWeekView> {
    public void execute(Event<UIWeekView> event) throws Exception {
      UIWeekView calendarview = event.getSource() ;
      String eventId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String calendarId = event.getRequestContext().getRequestParameter(CALENDARID) ;
      String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
      String startTime = event.getRequestContext().getRequestParameter("startTime") ;
      String finishTime = event.getRequestContext().getRequestParameter("finishTime") ;
      String currentDate = event.getRequestContext().getRequestParameter("currentDate") ;
      String username = event.getRequestContext().getRemoteUser() ;
      CalendarEvent eventCalendar = null ;
      CalendarService calendarService = CalendarUtils.getCalendarService() ;
      if(calType.equals(CalendarUtils.PRIVATE_TYPE)) {
        eventCalendar = calendarService.getUserEvent(username, calendarId, eventId) ;
      } else if(calType.equals(CalendarUtils.SHARED_TYPE)) {
        eventCalendar = calendarService.getUserEvent(username, calendarId, eventId) ;
      } else if(calType.equals(CalendarUtils.PUBLIC_TYPE)) {
        eventCalendar = calendarService.getGroupEvent(calendarId, eventId) ;
      }
      Calendar cal = GregorianCalendar.getInstance() ;
      DateFormat df = new SimpleDateFormat(calendarview.getDateFormat()) ;
      cal.setTimeInMillis(Long.parseLong(currentDate)) ;
      
      int hoursBg = (Integer.parseInt(startTime)/60) ;
      int minutesBg = (Integer.parseInt(startTime)%60) ;
      int hoursEnd = (Integer.parseInt(finishTime)/60) ;
      int minutesEnd = (Integer.parseInt(finishTime)%60) ;
      
      cal.set(Calendar.HOUR, hoursBg) ;
      cal.set(Calendar.MINUTE, minutesBg) ;
      eventCalendar.setFromDateTime(cal.getTime()) ;
      
      cal.set(Calendar.HOUR, hoursEnd) ;
      cal.set(Calendar.MINUTE, minutesEnd) ;
      eventCalendar.setToDateTime(cal.getTime()) ;
      
      if(calType.equals(CalendarUtils.PRIVATE_TYPE)) {
        calendarService.saveUserEvent(username, calendarId, eventCalendar, false) ;
      }else if(calType.equals(CalendarUtils.SHARED_TYPE)){
        calendarService.saveEventToSharedCalendar(username, calendarId, eventCalendar, false) ;
      }else if(calType.equals(CalendarUtils.PUBLIC_TYPE)){
        calendarService.saveGroupEvent(calendarId, eventCalendar, false) ;          
      }
      calendarview.refresh() ;
      UIMiniCalendar uiMiniCalendar = calendarview.getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UIMiniCalendar.class) ;
      uiMiniCalendar.updateMiniCal() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMiniCalendar) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
    }
  }
}
