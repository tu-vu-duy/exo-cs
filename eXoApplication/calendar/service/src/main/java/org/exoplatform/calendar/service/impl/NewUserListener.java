/*
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
 */
package org.exoplatform.calendar.service.impl;

import java.util.List;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarCategory;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.CalendarSetting;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

/**
 * Created by The eXo Platform SARL Author : Hung Nguyen Quang
 * hung.nguyen@exoplatform.com Nov 23, 2007 3:09:21 PM
 */
public class NewUserListener extends UserEventListener {

  //Calendar params
  final public static String EVENT_CATEGORIES = "defaultEventCategories".intern() ;
  final public static String CALENDAR_CATEGORIES = "defaultCalendarCategory".intern() ;
  final public static String CALENDAR_NAME  = "defaultCalendar".intern() ;
  final public static String COMA = ",".intern() ;

  //Calendar Setting params
  final public static String ST_VIEW_TYPE = "viewType".intern() ;
  final public static String ST_TIME_INTEVAL = "timeInterval".intern() ;
  final public static String ST_WEEK_START = "weekStartOn".intern() ;
  final public static String ST_DATE_FORMAT = "dateFormat".intern() ;
  final public static String ST_TIME_FORMAT = "timeFormat".intern() ;
  final public static String ST_LOCALE = "localeId".intern() ;
  final public static String ST_TIMEZONE = "timezoneId".intern() ;
  final public static String ST_BASE_URL = "baseUrlForRss".intern() ;
  final public static String ST_WORKINGTIME = "isShowWorkingTime".intern() ; 
  final public static String ST_TIME_BEGIN = "workingTimeBegin".intern() ;
  final public static String ST_TIME_END = "workingTimeEnd".intern() ;


  private CalendarService cservice_;
  private String[] defaultEventCategories_;
  private String defaultCalendarCategory_;
  private String[] defaultCalendar_;

  private CalendarSetting defaultCalendarSetting_ ;

  public NewUserListener(CalendarService cservice, InitParams params)
  throws Exception {
    cservice_ = cservice;
    String defaultEventCategories = params.getValueParam(EVENT_CATEGORIES).getValue();
    if (defaultEventCategories != null && defaultEventCategories.length() > 0) {
      defaultEventCategories_ = defaultEventCategories.split(COMA);
    }
    defaultCalendarCategory_ = params.getValueParam(CALENDAR_CATEGORIES).getValue();
    String defaultCalendar = params.getValueParam(CALENDAR_NAME).getValue();
    if (defaultCalendar != null && defaultCalendar.length() > 0) {
      defaultCalendar_ = defaultCalendar.split(COMA);
    }
    defaultCalendarSetting_ = new CalendarSetting() ;
    if(params.getValueParam(ST_VIEW_TYPE) != null) {
      defaultCalendarSetting_.setViewType(params.getValueParam(ST_VIEW_TYPE).getValue()) ;
    }
    if(params.getValueParam(ST_TIME_INTEVAL) != null) {
      defaultCalendarSetting_.setTimeInterval(Long.parseLong(params.getValueParam(ST_TIME_INTEVAL).getValue())) ;
    }

    if(params.getValueParam(ST_WEEK_START) != null) {
      defaultCalendarSetting_.setWeekStartOn(params.getValueParam(ST_WEEK_START).getValue()) ;
    }
    if(params.getValueParam(ST_DATE_FORMAT) != null) {
      defaultCalendarSetting_.setDateFormat(params.getValueParam(ST_DATE_FORMAT).getValue()) ;
    }
    if(params.getValueParam(ST_TIME_FORMAT) != null) {
      defaultCalendarSetting_.setTimeFormat(params.getValueParam(ST_TIME_FORMAT).getValue()) ;
    }
    if(params.getValueParam(ST_LOCALE) != null) {
      defaultCalendarSetting_.setLocation(params.getValueParam(ST_LOCALE).getValue()) ;
    }
    if(params.getValueParam(ST_TIMEZONE) != null) {
      defaultCalendarSetting_.setTimeZone(params.getValueParam(ST_TIMEZONE).getValue()) ;
    }
    if(params.getValueParam(ST_BASE_URL) != null) {
      defaultCalendarSetting_.setBaseURL(params.getValueParam(ST_BASE_URL).getValue()) ;
    }
    if(params.getValueParam(ST_WORKINGTIME) != null) {
      defaultCalendarSetting_.setShowWorkingTime(Boolean.parseBoolean(params.getValueParam(ST_WORKINGTIME).getValue())) ;
      if(defaultCalendarSetting_.isShowWorkingTime()) {
        if(params.getValueParam(ST_TIME_BEGIN) != null) {
          defaultCalendarSetting_.setWorkingTimeBegin(params.getValueParam(ST_TIME_BEGIN).getValue()) ;
        }
        if(params.getValueParam(ST_TIME_END) != null) {
          defaultCalendarSetting_.setWorkingTimeEnd(params.getValueParam(ST_TIME_END).getValue()) ;
        }
      }
    }

  }

  public void postSave(User user, boolean isNew) throws Exception {
    if(!isNew) return ;
    SessionProvider sysProvider = SessionProvider.createSystemProvider();
    try {
      if (defaultEventCategories_ != null
          && defaultEventCategories_.length > 0) {
        for (String evCategory : defaultEventCategories_) {
          EventCategory eventCategory = new EventCategory();
          eventCategory.setName(evCategory);
          eventCategory.setDataInit(true) ;
          cservice_.saveEventCategory(sysProvider, user.getUserName(),
              eventCategory, null, true);
        }
      }
      if (defaultCalendarCategory_ != null && defaultCalendarCategory_.length() > 0) {
        CalendarCategory calCategory = new CalendarCategory();
        calCategory.setName(defaultCalendarCategory_);
        calCategory.setDataInit(true) ;
        cservice_.saveCalendarCategory(sysProvider, user.getUserName(),	calCategory, true);
        if (defaultCalendar_ != null && defaultCalendar_.length > 0) {
          for (String calendar : defaultCalendar_) {
            Calendar cal = new Calendar();
            cal.setName(calendar);
            cal.setCategoryId(calCategory.getId());
            cal.setDataInit(true) ;
            cal.setCalendarOwner(user.getUserName()) ;
            if(defaultCalendarSetting_ != null) {
              if(defaultCalendarSetting_.getLocation() != null)
                cal.setLocale(defaultCalendarSetting_.getLocation()) ;
              if(defaultCalendarSetting_.getTimeZone() != null)
                cal.setTimeZone(defaultCalendarSetting_.getTimeZone()) ;
            }
            cservice_.saveUserCalendar(sysProvider, user.getUserName(),	cal, true);
          }
        }
      }    
      if(defaultCalendarSetting_ != null && user != null) {
        cservice_.saveCalendarSetting(sysProvider, user.getUserName(), defaultCalendarSetting_) ;
      }
    } catch (Exception e) {
      e.printStackTrace() ;
    } finally {
      sysProvider.close();
    }
  }

  @Override
  public void postDelete(User user) throws Exception {
    SessionProvider session = SessionProvider.createSystemProvider(); ;
    String username = user.getUserName() ;
    List<GroupCalendarData> gCalData = cservice_.getCalendarCategories(session, username, true) ;
    try {
      if(!gCalData.isEmpty())
        for (GroupCalendarData gCal : gCalData) {
          cservice_.removeCalendarCategory(session, username, gCal.getId()) ;
        }
      List<EventCategory> eCats = cservice_.getEventCategories(session, username) ;
      if(!eCats.isEmpty())
        for(EventCategory ecat : eCats) {
          cservice_.removeEventCategory(session, username, ecat.getId()) ;
        }
      GroupCalendarData   calData = cservice_.getSharedCalendars(session, username, true) ;
      if(calData != null && !calData.getCalendars().isEmpty())
        for(Calendar cal : calData.getCalendars()) {
          cservice_.removeSharedCalendar(session, username, cal.getId()) ;
        }
    } catch (Exception e) {
      e.printStackTrace() ;
    } finally {
      session.close() ;
    }
    super.postDelete(user);
  }
}