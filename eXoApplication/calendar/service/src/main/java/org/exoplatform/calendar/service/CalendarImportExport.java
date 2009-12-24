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
package org.exoplatform.calendar.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.exoplatform.services.jcr.ext.common.SessionProvider;


/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Jul 2, 2007  
 */
public interface CalendarImportExport {  
  
  /**
   * The method imports events form icalendar(.ics) or outlook calendar exported .csv file to the system
   * @param userSession session of current user
   * @param username current user name or id
   * @param icalInputStream data input stream
   * @param calendarName given calendar name, if the name is null, default calendar name is file name
   * @throws Exception
   */
  public void importCalendar(SessionProvider userSession, String username, InputStream icalInputStream, String calendarName) throws Exception ;
  
  /**
   * The method imports events form icalendar(.ics) or outlook calendar exported .csv file to the system
   * @param userSession session of current user
   * @param username current user name or id
   * @param icalInputStream data input stream
   * @param calendarId given  existed calendar id  
   * @throws Exception
   */
  public void importToCalendar(SessionProvider userSession, String username, InputStream icalInputStream, String calendarId) throws Exception ;
  
  /**
   * The method exports events form calendar to icalendar file (.ics) or .csv file
   * @param username current user name or id
   * @param calendarIds the group calendar ids, if you want to export events from public calendars
   * @param type The type of calendar will be exported
   * @return data output stream
   * @throws Exception
   */
  public OutputStream exportCalendar(String username, List<String> calendarIds, String type) throws Exception ;
  
  public OutputStream exportCalendar(String username, List<String> calendarIds, String type, int number) throws Exception ;
  
  /**
   * The method export calendar event to output stream by given event id
   * @param userSession session of current user
   * @param username current user name or id
   * @param calendarId given calendar id, the calendar event belong to
   * @param type The type of calendar will be exported
   * @param eventId given event id
   * @return data output stream
   * @throws Exception
   */
  public OutputStream exportEventCalendar(SessionProvider userSession, String username, String calendarId, String type, String eventId) throws Exception ;
  
  /**
   * The method maps the input stream to event object
   * @param icalInputStream the input stream
   * @return List of calendar event objects contant infomations
   * @throws Exception
   */
  public List<CalendarEvent> getEventObjects(InputStream icalInputStream) throws Exception ;
  
  /**
   * The method return true if the input stream is correct format 
   * @param icalInputStream the input stream
   * @throws Exception
   */
  public boolean isValidate(InputStream icalInputStream) throws Exception ;
  
}
