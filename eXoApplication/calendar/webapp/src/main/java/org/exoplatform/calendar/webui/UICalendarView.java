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
import java.util.List;
import java.util.Map;


import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.webui.popup.UIEventCategoryForm;
import org.exoplatform.calendar.webui.popup.UIEventForm;
import org.exoplatform.calendar.webui.popup.UIPopupAction;
import org.exoplatform.calendar.webui.popup.UIPopupContainer;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;


/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

public class UICalendarView extends UIForm {
  final static protected String EVENT_CATEGORIES = "eventCategories".intern() ;

  final public static String JANUARY = "January".intern() ;
  final public static String FEBRUARY = "February".intern() ;
  final public static String MARCH = "March".intern() ;
  final public static String APRIL = "April".intern() ;
  final public static String MAY = "May".intern() ;
  final public static String JUNE = "June".intern() ;
  final public static String JULY = "July".intern() ;
  final public static String AUGUST = "August".intern() ;
  final public static String SEPTEMBER = "September".intern() ;
  final public static String OCTOBER = "October".intern() ;
  final public static String NOVEMBER = "November".intern() ;
  final public static String DECEMBER = "December".intern() ;
  final public static String[] MONTHS = {JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER} ;

  final public static String MONDAY = "Monday".intern() ;
  final public static String TUESDAY = "Tuesday".intern() ;
  final public static String WEDNESDAY = "Wednesday".intern() ;
  final public static String THURSDAY = "Thursday".intern() ;
  final public static String FRIDAY = "Friday".intern() ;
  final public static String SATURDAY = "Saturday".intern() ;
  final public static String SUNDAY = "Sunday".intern() ;
  final public static String[] DAYS = {SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY} ;

  final public static String ACT_NEXT = "MoveNext".intern() ;

  final public static String ACT_PREVIOUS  = "MovePrevious".intern() ;

  public Calendar calendar_ = GregorianCalendar.getInstance() ;

  protected boolean isShowEvent = true;

  protected List<String> privateCalendarIds = new ArrayList<String>() ;
  protected List<String> publicCalendarIds = new ArrayList<String>() ;

  final public static Map<Integer, String> monthsName_ = new HashMap<Integer, String>() ;
  private Map<Integer, String> daysMap_ = new HashMap<Integer, String>() ;
  private Map<Integer, String> monthsMap_ = new HashMap<Integer, String>() ;

  public UICalendarView() throws Exception{
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    List<EventCategory> eventCategories = calendarService.getEventCategories(Util.getPortalRequestContext().getRemoteUser()) ;
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    options.add(new SelectItemOption<String>("all", "")) ;
    for(EventCategory category : eventCategories) {
      options.add(new SelectItemOption<String>(category.getName(), category.getName())) ;
    }
    addUIFormInput(new UIFormSelectBox(EVENT_CATEGORIES, EVENT_CATEGORIES, options)) ;
    calendar_.setLenient(false) ;
    int i = 0 ; 
    for(String month : MONTHS) {
      monthsMap_.put(i, month) ;
      i++ ;
    }
    int j = 1 ;
    for(String month : DAYS) {
      daysMap_.put(j, month) ;
      j++ ;
    }
  }
  protected String[] getMonthsName() { 
    return MONTHS ;
  }
  protected String[] getDaysName() { 
    return DAYS ;
  }
  protected int getDaysInMonth() {
    Calendar cal = GregorianCalendar.getInstance() ;
    cal.set(getCurrentYear(), getCurrentMonth(), getCurrentDay()) ;
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH) ;
  }
  protected int getDaysInMonth(int month, int year) {
    Calendar cal = GregorianCalendar.getInstance() ;
    cal.set(year, month, getCurrentDay()) ;
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH) ;
  }
  protected int getDayOfWeek(int year, int month, int day) {
    GregorianCalendar gc = new GregorianCalendar(year, month, day) ;
    return gc.get(java.util.Calendar.DAY_OF_WEEK) ;
  }
  protected  String getMonthName(int month) {return monthsMap_.get(month).toString() ;} ;
  protected  String getDayName(int day) {return daysMap_.get(day).toString() ;} ;

  public void update() throws Exception {
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    List<EventCategory> eventCategories = calendarService.getEventCategories(Util.getPortalRequestContext().getRemoteUser()) ;
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    options.add(new SelectItemOption<String>("all", "")) ;
    for(EventCategory category : eventCategories) {
      options.add(new SelectItemOption<String>(category.getName(), category.getName())) ;
    }
    getUIFormSelectBox(EVENT_CATEGORIES).setOptions(options) ;
  }

  public List<CalendarEvent> getList() throws Exception {
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    List<CalendarEvent> events = new ArrayList<CalendarEvent>() ;
    if(privateCalendarIds.size() > 0) {
      events = calendarService.getUserEventByCalendar(Util.getPortalRequestContext().getRemoteUser(), privateCalendarIds)  ;
    }
    if(publicCalendarIds.size() > 0) {
      if(events.size() > 0) {
        List<CalendarEvent> publicEvents = 
          calendarService.getGroupEventByCalendar(publicCalendarIds) ;
        for(CalendarEvent event : publicEvents) {
          events.add(event) ;
        }
      }else {
        events = calendarService.getGroupEventByCalendar(publicCalendarIds)  ;
      }
    }
    return events ;
  }

  protected void DayMove(int day, int month, int year) {
    setCurrentDay(day) ;
    setCurrentDay(month) ;
    setCurrentDay(year) ;
  }
  protected boolean isToday(int day, int month, int year) {
    Calendar currentCal = Calendar.getInstance() ;
    boolean isCurrentDay = currentCal.get(Calendar.DATE) == day ;
    boolean isCurrentMonth = currentCal.get(Calendar.MONTH) == month ;
    boolean isCurrentYear = currentCal.get(Calendar.YEAR) == year ;
    return (isCurrentDay && isCurrentMonth && isCurrentYear ) ;
  }

  protected boolean isCurrentMonth(int month, int year) {
    Calendar currentCal = Calendar.getInstance() ;
    boolean isCurrentMonth = currentCal.get(Calendar.MONTH) == month ;
    boolean isCurrentYear = currentCal.get(Calendar.YEAR) == year ;
    return (isCurrentMonth && isCurrentYear ) ;
  }

  protected Date getCurrentDate() {return calendar_.getTime() ;} 
  protected void setCurrentDate(Date value) {calendar_.setTime(value) ;} 

  protected int getCurrentDay() {return calendar_.get(Calendar.DATE) ;}
  protected void setCurrentDay(int day) {calendar_.set(Calendar.DATE, day) ;}

  protected int getCurrentWeek() {return calendar_.get(Calendar.WEEK_OF_YEAR) ;}
  protected void setCurrentWeek(int week) {calendar_.set(Calendar.WEEK_OF_YEAR, week) ;}
  
  protected int getCurrentMonth() {return calendar_.get(Calendar.MONTH) ;}
  protected void setCurrentMonth(int month) {calendar_.set(Calendar.MONTH, month) ;}

  protected int getCurrentYear() {return calendar_.get(Calendar.YEAR) ;}
  protected void setCurrentYear(int year) {calendar_.set(Calendar.YEAR, year) ;}

  static  public class RefreshActionListener extends EventListener<UICalendarView> {
    public void execute(Event<UICalendarView> event) throws Exception {
      UICalendarView uiForm = event.getSource() ;
      System.out.println(" ===========> RefreshActionListener") ;
    }
  }
  static  public class AddEventActionListener extends EventListener<UICalendarView> {
    public void execute(Event<UICalendarView> event) throws Exception {
      UICalendarView uiForm = event.getSource() ;
      System.out.println(" ===========> AddEventActionListener") ;
      CalendarService calendarService = uiForm.getApplicationComponent(CalendarService.class) ;
      String username = event.getRequestContext().getRemoteUser() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      if(calendarService.getUserCalendars(username).size() <= 0) {
        uiApp.addMessage(new ApplicationMessage("UICalendarView.msg.calendar-list-empty", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      } else {
        UICalendarPortlet uiPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
        UIPopupAction uiParenPopup = uiPortlet.getChild(UIPopupAction.class) ;
        UIPopupContainer uiPopupContainer = uiPortlet.createUIComponent(UIPopupContainer.class, null, null) ;
        uiPopupContainer.addChild(UIEventForm.class, null, null) ;
        uiParenPopup.activate(uiPopupContainer, 600, 0, true) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiParenPopup) ;
      }
    }
  }
  static  public class DeleteEventActionListener extends EventListener<UICalendarView> {
    public void execute(Event<UICalendarView> event) throws Exception {
      UICalendarView uiForm = event.getSource() ;
      System.out.println(" ===========> DeleteEventActionListener") ;
    }
  }
  static  public class ChangeCategoryActionListener extends EventListener<UICalendarView> {
    public void execute(Event<UICalendarView> event) throws Exception {
      UICalendarView uiForm = event.getSource() ;
      System.out.println(" ===========> ChangeCategoryActionListener") ;
    }
  }

  static  public class AddCategoryActionListener extends EventListener<UICalendarView> {
    public void execute(Event<UICalendarView> event) throws Exception {
      UICalendarView listView = event.getSource() ;
      UICalendarPortlet calendarPortlet = listView.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = calendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.activate(UIEventCategoryForm.class, 600) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
}
