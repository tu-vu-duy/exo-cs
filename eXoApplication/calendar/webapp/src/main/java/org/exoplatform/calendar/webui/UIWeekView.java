/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.webui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.CalendarSetting;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.webui.popup.UIPopupAction;
import org.exoplatform.calendar.webui.popup.UIQuickAddEvent;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

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
      @EventConfig(listeners = UIWeekView.SaveEventActionListener.class),
      @EventConfig(listeners = UIWeekView.MoveNextActionListener.class), 
      @EventConfig(listeners = UIWeekView.MovePreviousActionListener.class)
    }

)
public class UIWeekView extends UICalendarView {

  protected Map<String, Map<String, CalendarEvent>> eventData_ = new HashMap<String, Map<String, CalendarEvent>>() ;
  protected LinkedHashMap<String, CalendarEvent> allWeekData_ = new LinkedHashMap<String,  CalendarEvent>() ;
  protected LinkedHashMap<String, CalendarEvent> dataMap_ = new LinkedHashMap<String,  CalendarEvent>() ;
  protected  List<CalendarEvent> daysData_  = new ArrayList<CalendarEvent>() ;
  protected boolean isShowCustomView_ = false ;
  protected Date beginDate_ ;
  protected Date endDate_ ;

  public UIWeekView() throws Exception {
    super() ;
  }

  public void refresh() throws Exception {
    System.out.println("\n\n>>>>>>>>>> WEEK VIEW") ;
    eventData_.clear() ;
    allWeekData_.clear() ;
    int i = 0 ;
    Calendar c = getBeginDateOfWeek() ;
    while(i++ <7) {
      Map<String, CalendarEvent> list = new HashMap<String, CalendarEvent>() ;
      String key = keyGen(c.get(Calendar.DATE), c.get(Calendar.MONTH), c.get(Calendar.YEAR)) ;
      eventData_.put(key, list) ;
      c.add(Calendar.DATE, 1) ;
    }

    /* for(Calendar c : getDaysOfWeek()) {
      Map<String, CalendarEvent> list = new HashMap<String, CalendarEvent>() ;
      String key = keyGen(c.get(Calendar.DATE), c.get(Calendar.MONTH), c.get(Calendar.YEAR)) ;
      eventData_.put(key, list) ;
    }*/
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    String username = Util.getPortalRequestContext().getRemoteUser() ;
    EventQuery eventQuery = new EventQuery() ;
    eventQuery.setFromDate(getBeginDateOfWeek()) ;
    eventQuery.setToDate(getEndDateOfWeek()) ;
    List<CalendarEvent> allEvents = calendarService.getEvent(username, eventQuery, getPublicCalendars())  ;
    Iterator iter = allEvents.iterator() ;
    while(iter.hasNext()) {
      CalendarEvent event = (CalendarEvent)iter.next() ;
      Date beginEvent = event.getFromDateTime() ;
      Date endEvent = event.getToDateTime() ;
      long eventAmount = endEvent.getTime() - beginEvent.getTime() ;
      i = 0 ;
      c = getBeginDateOfWeek();
      while(i++ < 7) {
        String key = keyGen(c.get(Calendar.DATE), c.get(Calendar.MONTH), c.get(Calendar.YEAR)) ;
        if(isSameDate(c.getTime(), beginEvent) && (isSameDate(c.getTime(), endEvent)) || c.getTime().equals(endEvent)) {
          if(eventAmount > 0 && eventAmount < CalendarUtils.MILISECONS_OF_DAY) {
            eventData_.get(key).put(event.getId(), event) ;
            dataMap_.put(event.getId(), event) ;
            iter.remove() ;
          }
        } 
        c.add(Calendar.DATE, 1) ;
      }
    }
    for( CalendarEvent ce : allEvents) {
      allWeekData_.put(ce.getId(), ce) ;
      dataMap_.put(ce.getId(), ce) ;
    }
  }
  protected void moveTo(int weeks) {
    calendar_.add(Calendar.WEEK_OF_YEAR, weeks) ;
  }

  /* protected List<Calendar> getDaysOfWeek() throws Exception {
    List<Calendar> calendarData = new ArrayList<Calendar>() ;
     Calendar cl = getBeginDay(GregorianCalendar.getInstance()) ;
    if(!isShowCustomView_) {
      CalendarSetting calSetting  = null ;
      try{
        calSetting = getAncestorOfType(UICalendarPortlet.class).getCalendarSetting() ;
      } catch (Exception e) {
        CalendarService calService = getApplicationComponent(CalendarService.class) ;
        calSetting  = calService.getCalendarSetting(Util.getPortalRequestContext().getRemoteUser()) ;
      }
      cl.setFirstDayOfWeek(Integer.parseInt(calSetting.getWeekStartOn())) ;
      cl.set(Calendar.WEEK_OF_YEAR, week) ;
      int amount = cl.getFirstDayOfWeek() - cl.get(Calendar.DAY_OF_WEEK) ;
      cl.add(Calendar.DATE, amount) ;
      cl = getBeginDay(cl) ;
      cl = getBeginDay(getDateByValue(year, month, day, UICalendarView.TYPE_DATE, amount)) ;
      endDate = getEndDay(cl) ;
      endDate.add(Calendar.WEEK_OF_MONTH, 1) ;
      int count = 0 ;
      while(count++ < 7) {
        calendarData.add(cl) ;
        cl.add(Calendar.DATE, 1) ;
      }
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
    Calendar cal = getBeginDay(new GregorianCalendar()) ;
    cal.setTime(getBeginDateOfWeek().getTime()) ;
    while(cal.before(getEndDateOfWeek())) {
      calendarData.add(cal) ;
      cal.add(Calendar.DATE, 1) ;
      System.out.println("\n\n cal " + cal.getTime() );
    }
    System.out.println("\n\n beginOfWeek " + getBeginDateOfWeek().getTime());
    System.out.println("\n\n endOfWeek " + getEndDateOfWeek().getTime());
    return calendarData ;
  }*/
  public java.util.Calendar getBeginDateOfWeek() throws Exception{
    java.util.Calendar temCal = GregorianCalendar.getInstance() ;
    temCal.setTime(calendar_.getTime()) ;
    CalendarSetting calSetting  = null ;
    try{
      calSetting = getAncestorOfType(UICalendarPortlet.class).getCalendarSetting() ;
    } catch (Exception e) {
      CalendarService calService = getApplicationComponent(CalendarService.class) ;
      calSetting  = calService.getCalendarSetting(Util.getPortalRequestContext().getRemoteUser()) ;
    }
    temCal.setFirstDayOfWeek(Integer.parseInt(calSetting.getWeekStartOn())) ;
    temCal.set(java.util.Calendar.WEEK_OF_YEAR, getCurrentWeek()) ;
    int amout = temCal.getFirstDayOfWeek() - calendar_.get(Calendar.DAY_OF_WEEK) ;
    temCal.add(Calendar.DATE, amout) ;
    return getBeginDay(temCal) ;
  }

  public java.util.Calendar getEndDateOfWeek() throws Exception{
    java.util.Calendar temCal = GregorianCalendar.getInstance() ;
    CalendarSetting calSetting  = null ;
    try{
      calSetting = getAncestorOfType(UICalendarPortlet.class).getCalendarSetting() ;
    } catch (Exception e) {
      CalendarService calService = getApplicationComponent(CalendarService.class) ;
      calSetting  = calService.getCalendarSetting(Util.getPortalRequestContext().getRemoteUser()) ;
    }
    temCal.setFirstDayOfWeek(Integer.parseInt(calSetting.getWeekStartOn())) ;
    temCal.setTime(getBeginDateOfWeek().getTime()) ;
    temCal.add(Calendar.DATE, 6) ;
    return getEndDay(temCal) ;
  }

  protected Map<String, Map<String, CalendarEvent>> getEventData() {return eventData_ ;}

  protected LinkedHashMap<String, CalendarEvent>  getEventList() {
    return allWeekData_ ;
  }
  public LinkedHashMap<String, CalendarEvent> getDataMap() {
    return dataMap_ ;
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
      System.out.println("\n\n start " + startTime);
      System.out.println("\n\n finish " + finishTime);
      CalendarService calendarService = CalendarUtils.getCalendarService() ;
      if(calType.equals(CalendarUtils.PRIVATE_TYPE)) {
        eventCalendar = calendarService.getUserEvent(username, calendarId, eventId) ;
      } else if(calType.equals(CalendarUtils.SHARED_TYPE)) {
        eventCalendar = calendarService.getUserEvent(username, calendarId, eventId) ;
      } else if(calType.equals(CalendarUtils.PUBLIC_TYPE)) {
        eventCalendar = calendarService.getGroupEvent(calendarId, eventId) ;
      }
      Calendar calBegin = GregorianCalendar.getInstance() ;
      Calendar calEnd = GregorianCalendar.getInstance() ;
      calBegin.setTimeInMillis(Long.parseLong(currentDate)) ;
      calEnd.setTimeInMillis(Long.parseLong(currentDate)) ;

      int hoursBg = (Integer.parseInt(startTime)/60) ;
      int minutesBg = (Integer.parseInt(startTime)%60) ;

      int hoursEnd = (Integer.parseInt(finishTime)/60) ;
      int minutesEnd = (Integer.parseInt(finishTime)%60) ;
      calBegin.set(Calendar.HOUR_OF_DAY, hoursBg) ;
      calBegin.set(Calendar.MINUTE, minutesBg) ;
      eventCalendar.setFromDateTime(calBegin.getTime()) ;
      calEnd.set(Calendar.HOUR_OF_DAY, hoursEnd) ;
      calEnd.set(Calendar.MINUTE, minutesEnd) ;
      eventCalendar.setToDateTime(calEnd.getTime()) ;
      if(eventCalendar.getToDateTime().before(eventCalendar.getFromDateTime())) {
        System.out.println("\n\n UIWeekView updateEvent to date must after from date");
        return ;
      }
      if(calType.equals(CalendarUtils.PRIVATE_TYPE)) {
        calendarService.saveUserEvent(username, calendarId, eventCalendar, false) ;
      }else if(calType.equals(CalendarUtils.SHARED_TYPE)){
        calendarService.saveEventToSharedCalendar(username, calendarId, eventCalendar, false) ;
      }else if(calType.equals(CalendarUtils.PUBLIC_TYPE)){
        calendarService.saveGroupEvent(calendarId, eventCalendar, false) ;          
      }
      calendarview.setLastUpdatedEventId(eventId) ;
      calendarview.refresh() ;
      UIMiniCalendar uiMiniCalendar = calendarview.getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UIMiniCalendar.class) ;
      uiMiniCalendar.updateMiniCal() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMiniCalendar) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
    }
  }

  static  public class SaveEventActionListener extends EventListener<UIWeekView> {
    public void execute(Event<UIWeekView> event) throws Exception {
      UIWeekView calendarview = event.getSource() ;
      String eventId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String calendarId = event.getRequestContext().getRequestParameter(CALENDARID) ;
      String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
      String startTime = event.getRequestContext().getRequestParameter("startTime") ;
      String finishTime = event.getRequestContext().getRequestParameter("finishTime") ;
      try {
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
        Calendar calBegin = GregorianCalendar.getInstance() ;
        Calendar calEnd = GregorianCalendar.getInstance() ;
        long unit = 15*60*1000 ;
        calBegin.setTimeInMillis((Long.parseLong(startTime)/unit)*unit) ;
        eventCalendar.setFromDateTime(calBegin.getTime()) ;
        calEnd.setTimeInMillis((Long.parseLong(finishTime)/unit)*unit) ;
        eventCalendar.setToDateTime(calEnd.getTime()) ;
        if(eventCalendar.getToDateTime().before(eventCalendar.getFromDateTime())) {
          System.out.println("\n\n UIWeekView updateEvent to date must after from date");
          return ;
        }
        if(calType.equals(CalendarUtils.PRIVATE_TYPE)) {
          calendarService.saveUserEvent(username, calendarId, eventCalendar, false) ;
        }else if(calType.equals(CalendarUtils.SHARED_TYPE)){
          calendarService.saveEventToSharedCalendar(username, calendarId, eventCalendar, false) ;
        }else if(calType.equals(CalendarUtils.PUBLIC_TYPE)){
          calendarService.saveGroupEvent(calendarId, eventCalendar, false) ;          
        }
        calendarview.setLastUpdatedEventId(eventId) ;
        calendarview.refresh() ;
        UIMiniCalendar uiMiniCalendar = calendarview.getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UIMiniCalendar.class) ;
        uiMiniCalendar.updateMiniCal() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiMiniCalendar) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
      } catch (Exception e) {
        e.printStackTrace() ;
      }
    }
  }
}
