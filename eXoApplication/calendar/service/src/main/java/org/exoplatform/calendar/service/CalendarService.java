/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
/**
 * TODO: make  2 set of api:
 * 1. user calendar
 * 2. group calendar
 */
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
  public Calendar getCalendar(String username, String calendarId) throws Exception ;
  //TODO:  getUserCalendars
  public List<Calendar> getAllCalendars(String username) throws Exception ;
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
  //TODO: getGroupCalendar(String groupId, calendarId)
  public Calendar getCalendar(String calendarId) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check exists of calendar list in cache
   * 3. Get calendar list by category and return
   * @param username
   * @return Calendar list
   * @throws Exception
   */
  //TODO: move to the user section
  public List<Calendar> getCalendarsByCategory(String username, String calendarCategoryId) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node
   * 2. Check exists of calendar list in cache
   * 3. Get calendar list
   * @param username
   * @return Calendar list
   * @throws Exception
   */
  //TODO: groupName to groupID, rename to getGroupCalendars
  public List<Calendar> getCalendarsByGroup(String groupName) throws Exception ;  
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
  //TODO: rename to saveUserCalendar and saveGroupCalendar
  public void saveCalendar(String username, Calendar calendar, boolean isNew) throws Exception ;
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
  //TODO: removeUserCanlendar and removeGroupCalendar
  public Calendar removeCalendar(String username, String calendarId) throws Exception ;
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
  public Calendar removeCalendar(String calendarId) throws Exception ;
  
  //TODO: move event category to Calendar object, should have only the method removeEventCategory
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Get calendar by id
   * 3. Get Category and return Category object
   * @param username
   * @param calendarId
   * @param eventCategoryId
   * @return Category
   * @throws Exception
   */
  //TODO: maybe we do not need this
  public EventCategory getEventCategory(String username, String calendarId, String eventCategoryId) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check exists of categories list in cache
   * 3. Get calendar by id
   * 4. Get and Return category list and put category list to cache if not exists
   * @param username
   * @param calendarId
   * @return Category list
   * @throws Exception
   */  
  public List<EventCategory> getEventCategories(String username, String calendarId) throws Exception ;  
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
  public void saveEventCategory(String username, String calendarId, EventCategory eventCategory, boolean isNew) throws Exception ;
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
  public EventCategory removeEventCategory(String username, String calendarId, String eventCategoryId) throws Exception ;
  
  
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
  //TODO: we do not need eventCategory
  public Event getEvent(String username, String calendarId, String eventCategoryId, String eventId) throws Exception ;
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
  public Event getEvent(String calendarId, String eventCategoryId, String eventId) throws Exception ;
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
  public List<Event> getEventByCalendar(String username, String calendarId) throws Exception ;
  /**
   * This method should:
   * 1. Get calendar service root node by current user
   * 2. Check exists of event list in cache
   * 3. Get event list and return
   * @param calendarId
   * @return event list
   * @throws Exception
   */
  //TODO: missing parameter EventQuery
  public List<Event> getEventByCalendar(String calendarId) throws Exception ;
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
  //TODO: use Calendar object instead of calendarId. no need of eventCategoryID and isPublic...
  public void saveEvent(String username, String calendarId, String eventCategoryId, Event event, boolean isNew, boolean isPublicCalendar) throws Exception ;
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
  public Event removeEvent(String username, String calendarId, String eventCategoryId, String eventId, boolean isPublicCalendar) throws Exception ;
  
  //TODO: missing the method List<CalendarCateory> getGroupCalendarForUser(String username) 
  
  //public void importICalendar(String username, InputStream icalInputStream) throws Exception ;
  //public String exportICalendar(String username, String calendarId) throws Exception ;
  //TOTO: use CalendarImportExport
  
  public Map<String, CalendarImportExport>  getCalendarImportExports() ;
  public void importCalendar(String username, String calendarType, InputStream icalInputStream) throws Exception ;
  public OutputStream exportCalendar(String username, String calendarId, String calendarType) throws Exception ;
}
