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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PathNotFoundException;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
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
      @EventConfig(listeners = UICalendarView.DeleteEventActionListener.class, confirm="UICalendarView.msg.confirm-delete"),
      @EventConfig(listeners = UICalendarView.GotoDateActionListener.class),
      @EventConfig(listeners = UICalendarView.AddCategoryActionListener.class),
      @EventConfig(listeners = UICalendarView.SwitchViewActionListener.class),
      @EventConfig(listeners = UICalendarView.QuickAddActionListener.class), 
      @EventConfig(listeners = UICalendarView.ViewActionListener.class),
      @EventConfig(listeners = UICalendarView.EditActionListener.class), 
      @EventConfig(listeners = UICalendarView.DeleteActionListener.class, confirm="UICalendarView.msg.confirm-delete"),
      @EventConfig(listeners = UICalendarView.MoveNextActionListener.class), 
      @EventConfig(listeners = UICalendarView.MovePreviousActionListener.class),
      @EventConfig(listeners = UIWeekView.UpdateEventActionListener.class),
      @EventConfig(listeners = UICalendarView.ExportEventActionListener.class),
      @EventConfig(listeners = UIWeekView.SaveEventActionListener.class)
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
    eventData_.clear() ;
    allWeekData_.clear() ;
    int i = 0 ;
    Calendar c = getBeginDateOfWeek() ;
    int maxDay = 7 ;
    if(isShowCustomView_) maxDay = 5 ;
    while(i++ <maxDay) {
      Map<String, CalendarEvent> list = new HashMap<String, CalendarEvent>() ;
      String key = keyGen(c.get(Calendar.DATE), c.get(Calendar.MONTH), c.get(Calendar.YEAR)) ;
      eventData_.put(key, list) ;
      c.add(Calendar.DATE, 1) ;
    }
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    String username = CalendarUtils.getCurrentUser() ;
    EventQuery eventQuery = new EventQuery() ;
    eventQuery.setFromDate(getBeginDateOfWeek()) ;
    //eventQuery.setToDate(getEndDateOfWeek()) ; 
    //Fix for CS-2986
    Calendar endDateOfWeek = getEndDateOfWeek();
    Date toDate = endDateOfWeek.getTime();
    toDate.setTime(toDate.getTime()-1);
    endDateOfWeek.setTime(toDate);
    eventQuery.setToDate(endDateOfWeek) ; 
    List<CalendarEvent> allEvents = calendarService.getEvents(username, eventQuery, getPublicCalendars())  ;
    Iterator iter = allEvents.iterator() ;
    while(iter.hasNext()) {
      CalendarEvent event = (CalendarEvent)iter.next() ;
      Date beginEvent = event.getFromDateTime() ;
      Date endEvent = event.getToDateTime() ;
      long eventAmount = endEvent.getTime() - beginEvent.getTime() ;
      i = 0 ;
      c = getBeginDateOfWeek();
      while(i++ < maxDay) {
        String key = keyGen(c.get(Calendar.DATE), c.get(Calendar.MONTH), c.get(Calendar.YEAR)) ;
        if(isSameDate(c.getTime(), beginEvent) && (isSameDate(c.getTime(), endEvent)) && eventAmount < CalendarUtils.MILISECONS_OF_DAY){
          eventData_.get(key).put(event.getId(), event) ;
          //dataMap_.put(event.getId(), event) ;
          iter.remove() ;
        }  
        c.add(Calendar.DATE, 1) ;
      }
    }
    for( CalendarEvent ce : allEvents) {
      allWeekData_.put(ce.getId(), ce) ;
      //dataMap_.put(ce.getId(), ce) ;
    } 
  }
  public java.util.Calendar getBeginDateOfWeek() throws Exception{
    java.util.Calendar temCal = getInstanceTempCalendar() ;
    temCal.setTime(calendar_.getTime()) ;
    /*CalendarSetting calSetting = new CalendarSetting() ;
    try {
      calSetting = getAncestorOfType(UICalendarPortlet.class).getCalendarSetting() ;
    }
    catch (Exception e) {
      CalendarService calService = getApplicationComponent(CalendarService.class) ;
      calSetting  = calService.getCalendarSetting(getSession(),CalendarUtils.getCurrentUser()) ;
    } */
    if(isShowCustomView_) temCal.setFirstDayOfWeek(Calendar.SUNDAY) ; 
    else temCal.setFirstDayOfWeek(Integer.parseInt(calendarSetting_.getWeekStartOn())) ;
    if(temCal.getFirstDayOfWeek() > calendar_.get(Calendar.DAY_OF_WEEK)) {
      temCal.set(java.util.Calendar.WEEK_OF_YEAR, getCurrentWeek()-1) ;
    } else {
      temCal.set(java.util.Calendar.WEEK_OF_YEAR, getCurrentWeek()) ;

    }
    temCal.setTime(calendar_.getTime()) ;
    int amout = temCal.getFirstDayOfWeek() - calendar_.get(Calendar.DAY_OF_WEEK) ;
    if(isShowCustomView_) amout = amout + 1 ;
    temCal.add(Calendar.DATE, amout) ;
    return getBeginDay(temCal) ;
  }

  public java.util.Calendar getEndDateOfWeek() throws Exception{
    java.util.Calendar temCal = getInstanceTempCalendar() ;
    temCal.setTime(calendar_.getTime()) ;
    if(isShowCustomView_) temCal.setFirstDayOfWeek(Calendar.SUNDAY) ; 
    else temCal.setFirstDayOfWeek(Integer.parseInt(calendarSetting_.getWeekStartOn())) ;
    temCal.setTime(getBeginDateOfWeek().getTime()) ;
    int amout = 6 ;
    if(isShowCustomView_) amout = amout - 2 ;
    temCal.add(Calendar.DATE, amout) ;
    return getEndDay(temCal) ;
  }

  protected Map<String, Map<String, CalendarEvent>> getEventData() {return eventData_ ;}

  protected LinkedHashMap<String, CalendarEvent>  getEventList() {
    return allWeekData_ ;
  }
  public LinkedHashMap<String, CalendarEvent> getDataMap() {
    LinkedHashMap<String, CalendarEvent> dataMap = new LinkedHashMap<String,  CalendarEvent>() ;
    dataMap.putAll(allWeekData_) ;
    for(String key :eventData_.keySet()) {
      dataMap.putAll(eventData_.get(key)) ;
    }
    return dataMap ;
  }
  public boolean isShowCustomView() {return isShowCustomView_ ;}
  
  static  public class UpdateEventActionListener extends EventListener<UIWeekView> {
    public void execute(Event<UIWeekView> event) throws Exception {
      UIWeekView calendarview = event.getSource() ;
      UICalendarPortlet uiCalendarPortlet = calendarview.getAncestorOfType(UICalendarPortlet.class) ;
      String eventId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String calendarId = event.getRequestContext().getRequestParameter(CALENDARID) ;
      String calType = event.getRequestContext().getRequestParameter(CALTYPE) ;
      String startTime = event.getRequestContext().getRequestParameter("startTime") ;
      String finishTime = event.getRequestContext().getRequestParameter("finishTime") ;
      String currentDate = event.getRequestContext().getRequestParameter("currentDate") ;
      String username = CalendarUtils.getCurrentUser() ;
      CalendarEvent eventCalendar = calendarview.getDataMap().get(eventId) ;
      CalendarService calendarService = CalendarUtils.getCalendarService() ;
      if(eventCalendar != null) {
        CalendarService calService = CalendarUtils.getCalendarService() ;
        try {
          org.exoplatform.calendar.service.Calendar calendar = null ;
          if(eventCalendar.getCalType().equals(CalendarUtils.PRIVATE_TYPE)) {
            calendar = calService.getUserCalendar(username, calendarId) ;
          } else if(eventCalendar.getCalType().equals(CalendarUtils.SHARED_TYPE)){
            if(calService.getSharedCalendars(username, true) != null)
              calendar = 
                calService.getSharedCalendars(username, true).getCalendarById(calendarId) ;
          } else if(eventCalendar.getCalType().equals(CalendarUtils.PUBLIC_TYPE)) {
            calendar = calService.getGroupCalendar(calendarId) ;
          }
          if(calendar == null) {
            UIApplication uiApp = calendarview.getAncestorOfType(UIApplication.class) ;
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          } else {
            if(!CalendarUtils.PRIVATE_TYPE.equals(calType) && !CalendarUtils.canEdit(calendarview.getApplicationComponent(OrganizationService.class), calendar.getEditPermission(), username)) {
              UIApplication uiApp = calendarview.getAncestorOfType(UIApplication.class) ;
              uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-permission-to-edit-event", null, 1)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
              calendarview.refresh() ;
              event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
              return ;
            }
            Calendar cal = calendarview.getInstanceTempCalendar() ;
            int hoursBg = (Integer.parseInt(startTime)/60) ;
            int minutesBg = (Integer.parseInt(startTime)%60) ;
            int hoursEnd = (Integer.parseInt(finishTime)/60) ;
            int minutesEnd = (Integer.parseInt(finishTime)%60) ;
            try {
              cal.setTimeInMillis(Long.parseLong(currentDate)) ;
              if(hoursBg < cal.getMinimum(Calendar.HOUR_OF_DAY)) {
                hoursBg = 0 ;
                minutesBg = 0 ;
              }
              cal.set(Calendar.HOUR_OF_DAY, hoursBg) ;
              cal.set(Calendar.MINUTE, minutesBg) ;
              eventCalendar.setFromDateTime(cal.getTime()) ;
              if(hoursEnd >= 24) {
                hoursEnd = 23 ;
                minutesEnd = 59 ;
              }
              cal.set(Calendar.HOUR_OF_DAY, hoursEnd) ;
              cal.set(Calendar.MINUTE, minutesEnd) ;
              eventCalendar.setToDateTime(cal.getTime()) ;
            } catch (Exception e) {
              e.printStackTrace() ;
              return ;
            }
            if(eventCalendar.getToDateTime().before(eventCalendar.getFromDateTime())) {
              return ;
            }
            if(calType.equals(CalendarUtils.PRIVATE_TYPE)) {
              calendarService.saveUserEvent(username, calendarId, eventCalendar, false) ;
            }else if(calType.equals(CalendarUtils.SHARED_TYPE)){
              calendarService.saveEventToSharedCalendar(username, calendarId, eventCalendar, false) ;
            }else if(calType.equals(CalendarUtils.PUBLIC_TYPE)){
              calendarService.savePublicEvent(calendarId, eventCalendar, false) ;          
            }
            calendarview.setLastUpdatedEventId(eventId) ;
            calendarview.refresh() ;
            UIMiniCalendar uiMiniCalendar = uiCalendarPortlet.findFirstComponentOfType(UIMiniCalendar.class) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiMiniCalendar) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
          }
        } catch (PathNotFoundException e) {
          e.printStackTrace() ;
          UIApplication uiApp = calendarview.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        }
      }
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
        String username = CalendarUtils.getCurrentUser() ;
        CalendarEvent eventCalendar = calendarview.getDataMap().get(eventId) ;
        if(eventCalendar != null) {
          CalendarService calendarService = CalendarUtils.getCalendarService() ;
          Calendar calBegin = calendarview.getInstanceTempCalendar() ;
          Calendar calEnd = calendarview.getInstanceTempCalendar() ;
          long unit = 15*60*1000 ;
          calBegin.setTimeInMillis((Long.parseLong(startTime)/unit)*unit) ;
          eventCalendar.setFromDateTime(calBegin.getTime()) ;
          calEnd.setTimeInMillis((Long.parseLong(finishTime)/unit)*unit) ;
          eventCalendar.setToDateTime(calEnd.getTime()) ;
          if(eventCalendar.getToDateTime().before(eventCalendar.getFromDateTime())) {
            return ;
          }
          org.exoplatform.calendar.service.Calendar calendar = null ;
          if(CalendarUtils.PRIVATE_TYPE.equals(calType)) {
            calendar = calendarService.getUserCalendar(username, calendarId) ;
          } else if(CalendarUtils.SHARED_TYPE.equals(calType)) {
            if(calendarService.getSharedCalendars(username, true) != null)
              calendar = 
                calendarService.getSharedCalendars(username, true).getCalendarById(calendarId) ;
          } else if(CalendarUtils.PUBLIC_TYPE.equals(calType)) {
            calendar = calendarService.getGroupCalendar(calendarId) ;
          }
          if(calendar == null) {
            UIApplication uiApp = calendarview.getAncestorOfType(UIApplication.class) ;
            uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-calendar", null, 1)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          } else {
            if(!CalendarUtils.PRIVATE_TYPE.equals(calType) && !CalendarUtils.canEdit(calendarview.getApplicationComponent(OrganizationService.class), calendar.getEditPermission(), username)) {
              UIApplication uiApp = calendarview.getAncestorOfType(UIApplication.class) ;
              uiApp.addMessage(new ApplicationMessage("UICalendars.msg.have-no-permission-to-edit-event", null, 1)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
              calendarview.refresh() ;
              event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
              return ;
            }
            if(calType.equals(CalendarUtils.PRIVATE_TYPE)) {
              calendarService.saveUserEvent(username, calendarId, eventCalendar, false) ;
            }else if(calType.equals(CalendarUtils.SHARED_TYPE)){
              calendarService.saveEventToSharedCalendar(username, calendarId, eventCalendar, false) ;
            }else if(calType.equals(CalendarUtils.PUBLIC_TYPE)){
              calendarService.savePublicEvent(calendarId, eventCalendar, false) ;          
            }
            calendarview.setLastUpdatedEventId(eventId) ;
            calendarview.refresh() ;
            UIMiniCalendar uiMiniCalendar = calendarview.getAncestorOfType(UICalendarPortlet.class).findFirstComponentOfType(UIMiniCalendar.class) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiMiniCalendar) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(calendarview.getParent()) ;
          }
        }
      } catch (Exception e) {
        e.printStackTrace() ;
        return ;
      }
    }
  }
}
