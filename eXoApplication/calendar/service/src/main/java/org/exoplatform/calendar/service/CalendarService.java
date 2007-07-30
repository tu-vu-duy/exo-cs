/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.service;

import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen Quang
 *          hung.nguyen@exoplatform.com
 * Jul 11, 2007  
 */
public interface CalendarService {
  
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check exists of Calendar category list in cache
   * 3. Get calendar category list and return
   * @param username
   * @return Calendar Category list
   * @throws Exception
   */
  public List<CalendarCategory> getCalendarCategories(String username) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Get calendar category
   * @param username
   * @param calendarCategoryId
   * @return calendarCategory
   * @throws Exception
   */
  public CalendarCategory getCalendarCategory(String username, String calendarCategoryId) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check isNew parameter
   * 3. Create new or update Calendar category
   * 4. Invalidate cache
   * @param username
   * @param CalendarCategory
   * @param isNew
   * @throws Exception
   */
  public void saveCalendarCategory(String username, CalendarCategory calendarCategory, boolean isNew) throws Exception ; 
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Remove calendarCategory
   * 3. Invalidate cache
   * @param username
   * @param calendarCategoryId
   * @return CalendarCategory
   * @throws Exception
   */
  public CalendarCategory removeCalendarCategory(String username, String calendarCategoryId) throws Exception ;
  
  
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Get calendar from private folder
   * @param username
   * @param calendarId
   * @return Calendar
   * @throws Exception
   */
  public Calendar getUserCalendar(String username, String calendarId) throws Exception ;
  public List<Calendar> getUserCalendars(String username) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check exists of calendar list in cache
   * 3. Get calendar list by category and return
   * @param username
   * @return Calendar list
   * @throws Exception
   */  
  public List<Calendar> getUserCalendarsByCategory(String username, String calendarCategoryId) throws Exception ;
  /**
   * This method should:
   * 1. Check calendar is private or public
   * 2. Check isNew parameter
   * 3. Create new of update calendar in private folder or service folder(public) 
   * 3. Invalidate cache
   * @param username
   * @param calendar
   * @param isNew
   * @throws Exception
   */
  public void saveUserCalendar(String username, Calendar calendar, boolean isNew) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node
   * 2. Get group node
   * 3. Remove calendar
   * 4. Invalidate cache
   * @param username
   * @param groupId
   * @param calendarId
   * @return Calendar
   * @throws Exception
   */
  public Calendar removeUserCalendar(String username, String calendarId) throws Exception ;
  
  
  /**
   * 
   * This method should:
   * 1. Get calendar service root node
   * 2. Get group node
   * 3. Get calendar
   * @param username
   * @param groupId
   * @param calendarId
   * @return Calendar
   * @throws Exception
   */
  public Calendar getGroupCalendar(String calendarId) throws Exception ;  
  /**
   * This method should:
   * 1. Get calendar service root node
   * 2. Check exists of calendar list in cache
   * 3. Get calendar list
   * @param username
   * @return Calendar list
   * @throws Exception
   */
  public List<GroupCalendarData> getGroupCalendars(String[] groupId) throws Exception ;  
  /**
   * This method should:
   * 1. Check calendar is private or public
   * 2. Check isNew parameter
   * 3. Create new of update calendar in private folder or service folder(public) 
   * 3. Invalidate cache
   * @param username
   * @param calendar
   * @param isNew
   * @throws Exception
   */
  public void saveGroupCalendar(Calendar calendar, boolean isNew) throws Exception ;  
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Remove calendar
   * 3. Invalidate cache
   * @param username
   * @param calendarId
   * @return Calendar
   * @throws Exception
   */
  public Calendar removeGroupCalendar(String calendarId) throws Exception ;
  
  
  
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check isNew parameter
   * 3. Create new or update category
   * 4. Invalidate cache 
   * @param username
   * @param calendarId
   * @param category
   * @param isNew
   * @throws Exception
   */
  public void saveUserEventCategory(String username, Calendar calendar, EventCategory eventCategory, boolean isNew) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Get calendar by id 
   * 3. Remove event category 
   * 4. Invalidate cache
   * @param username
   * @param calendarId
   * @param categoryId
   * @return EventCategory
   * @throws Exception
   */
  public EventCategory removeUserEventCategory(String username, String calendarId, String eventCategoryId) throws Exception ;  
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check isNew parameter
   * 3. Create new or update category
   * 4. Invalidate cache 
   * @param username
   * @param calendarId
   * @param category
   * @param isNew
   * @throws Exception
   */
  public void saveGroupEventCategory(Calendar calendar, EventCategory eventCategory, boolean isNew) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Get calendar by id 
   * 3. Remove event category 
   * 4. Invalidate cache
   * @param username
   * @param calendarId
   * @param categoryId
   * @return EventCategory
   * @throws Exception
   */
  public EventCategory removeGroupEventCategory(String calendarId, String eventCategoryId) throws Exception ;
  
  
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Get calendar and event category by parameters id 
   * 3. Get and return Event object
   * @param username
   * @param calendarId
   * @param eventCategoryId
   * @param eventId
   * @return Event
   * @throws Exception
   */
  public Event getUserEvent(String username, String calendarId, String eventCategoryId, String eventId) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check exists of event list in cache
   * 3. Get event list and return
   * @param username
   * @param calendarId
   * @return event list
   * @throws Exception
   */
  public List<Event> getUserEventByCalendar(String username, String calendarId) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check exists of event list in cache
   * 3. Get event list and return
   * @param EventQuery
   * @return event list
   * @throws Exception
   */
  public List<Event> getEvents(EventQuery eventQuery) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by username
   * 2. Get calendar and even catetory by parameters id
   * 3. Check isNew parameter
   * 4. Save new or update event
   * 5. Invalidate cache
   * @param username
   * @param calendarId
   * @param eventCategoryId
   * @param event
   * @param isNew
   * @throws Exception
   */  
  public void saveUserEvent(String username, String calendarId, Event event, boolean isNew) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Get calendar and category by parameters id 
   * 3. Remove event
   * 4. Invalidate cache
   * @param username
   * param calendarId
   * param eventCategoryId
   * @param eventId
   * @return Event
   * @throws Exception
   */
  public Event removeUserEvent(String username, String calendarId, String categoryId, String eventId) throws Exception ;
  
  
  /**
   * This method should:
   * 1. Get calendar service root node
   * 2. Get calendar and event category by parameters id 
   * 3. Get and return Event object
   * @param calendarId
   * @param eventCategoryId
   * @param eventId
   * @return Event
   * @throws Exception
   */
  public Event getGroupEvent(String calendarId, String eventCategoryId, String eventId) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check exists of event list in cache
   * 3. Get event list and return
   * @param username
   * @param calendarId
   * @return event list
   * @throws Exception
   */
  public List<Event> getGroupEventByCalendar(String calendarId) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by username
   * 2. Get calendar and even catetory by parameters id
   * 3. Check isNew parameter
   * 4. Save new or update event
   * 5. Invalidate cache
   * @param username
   * @param calendarId
   * @param eventCategoryId
   * @param event
   * @param isNew
   * @throws Exception
   */  
  public void saveGroupEvent(String calendarId, Event event, boolean isNew) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Get calendar and category by parameters id 
   * 3. Remove event
   * 4. Invalidate cache
   * @param username
   * param calendarId
   * param eventCategoryId
   * @param eventId
   * @return Event
   * @throws Exception
   */
  public Event removeGroupEvent(String calendarId, String categoryId, String eventId) throws Exception ;
  
  //TODO: missing the method List<CalendarCateory> getGroupCalendarForUser(String username) 
  
  public Map<String, CalendarImportExport>  getCalendarImportExports() ;  
}
