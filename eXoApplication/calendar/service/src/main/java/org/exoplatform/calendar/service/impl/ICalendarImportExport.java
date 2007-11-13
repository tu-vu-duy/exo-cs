/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarCategory;
import org.exoplatform.calendar.service.CalendarImportExport;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.service.Reminder;


/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Jul 2, 2007  
 */
public class ICalendarImportExport implements CalendarImportExport{
  private static final String PRIVATE_TYPE = "0".intern() ;
  private static final String SHARED_TYPE = "1".intern() ;
  private static final String PUBLIC_TYPE = "2".intern() ;
  private JCRDataStorage storage_ ;

  public ICalendarImportExport(JCRDataStorage storage) throws Exception {
    storage_ = storage ;
  }

  public OutputStream exportCalendar(String username, List<String> calendarIds, String type) throws Exception {
    List<CalendarEvent> events = new ArrayList<CalendarEvent>();
    if(type.equals(PRIVATE_TYPE)) {
      events = storage_.getUserEventByCalendar(username, calendarIds) ;
    }else if(type.equals(SHARED_TYPE)) {
      events = storage_.getSharedEventByCalendars(username, calendarIds) ;
    }else if(type.equals(PUBLIC_TYPE)){
      events = storage_.getGroupEventByCalendar(calendarIds) ;
    }
    net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
    calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
    calendar.getProperties().add(Version.VERSION_2_0);
    calendar.getProperties().add(CalScale.GREGORIAN);

    for(CalendarEvent exoEvent : events) {
      if(exoEvent.getEventType().equals(CalendarEvent.TYPE_EVENT)){
		  long start = exoEvent.getFromDateTime().getTime() ;
	      long end = exoEvent.getToDateTime().getTime() ;
	      String summary = exoEvent.getSummary() ;
	      VEvent event ;
	      if(end > 0) {
	        event = new VEvent(new DateTime(start), new DateTime(end), summary);
	        event.getProperties().getProperty(Property.DTEND).getParameters()
	        .add(net.fortuna.ical4j.model.parameter.Value.DATE_TIME);
	      }else {
	        event = new VEvent(new DateTime(start), summary);            
	      }
	      event.getProperties().getProperty(Property.DTSTART).getParameters()
	      .add(net.fortuna.ical4j.model.parameter.Value.DATE_TIME); 
	
	      event.getProperties().add(new Description(exoEvent.getDescription()));
	      event.getProperties().getProperty(Property.DESCRIPTION).getParameters()
	      .add(net.fortuna.ical4j.model.parameter.Value.TEXT);
	
	      event.getProperties().add(new Location(exoEvent.getLocation()));
	      event.getProperties().getProperty(Property.LOCATION).getParameters()
	      .add(net.fortuna.ical4j.model.parameter.Value.TEXT);
	
	      if(exoEvent.getEventCategoryId() != null){
	        event.getProperties().add(new Categories(exoEvent.getEventCategoryId())) ;
	        //EventCategory category = storage_.getEventCategory(username, calendarId, exoEvent.getEventCategoryId()) ;  
	        event.getProperties().getProperty(Property.CATEGORIES).getParameters()
	        .add(net.fortuna.ical4j.model.parameter.Value.TEXT);
	      }
	      if(exoEvent.getPriority() != null) {
	        event.getProperties().add(new Priority(Integer.parseInt(exoEvent.getPriority())));
	        event.getProperties().getProperty(Property.PRIORITY).getParameters()
	        .add(net.fortuna.ical4j.model.parameter.Value.INTEGER);  
	      }
	
	      if(exoEvent.getEventType().equals(CalendarEvent.TYPE_TASK)) {
	        long completed = exoEvent.getCompletedDateTime().getTime() ;
	        event.getProperties().add(new Completed(new DateTime(completed)));
	        event.getProperties().getProperty(Property.COMPLETED).getParameters()
	        .add(net.fortuna.ical4j.model.parameter.Value.DATE_TIME);
	
	        event.getProperties().add(new Due(new DateTime(end)));
	        event.getProperties().getProperty(Property.DUE).getParameters()
	        .add(net.fortuna.ical4j.model.parameter.Value.DATE_TIME);
	
	        event.getProperties().add(new Status(exoEvent.getStatus()));
	        event.getProperties().getProperty(Property.STATUS).getParameters()
	        .add(net.fortuna.ical4j.model.parameter.Value.TEXT);
	      }
	      String[] attendees = exoEvent.getInvitation() ;
	      if(attendees != null && attendees.length > 0) {
	        for(int i = 0; i < attendees.length; i++ ) {
	          if(attendees[i] != null) {
	            event.getProperties().add(new Attendee(attendees[i]));          
	          }
	        }
	        event.getProperties().getProperty(Property.ATTENDEE).getParameters()
	        .add(net.fortuna.ical4j.model.parameter.Value.TEXT);
	      }
	
	      Uid id = new Uid(exoEvent.getId()) ; 
	      event.getProperties().add(id) ; 
	      calendar.getComponents().add(event);
      }
    }

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    CalendarOutputter output = new CalendarOutputter();
    output.output(calendar, bout) ;
    return bout;
  }

  public void importCalendar(String username, InputStream icalInputStream, String calendarName) throws Exception {
    CalendarBuilder calendarBuilder = new CalendarBuilder() ;
    net.fortuna.ical4j.model.Calendar iCalendar = calendarBuilder.build(icalInputStream) ;
    GregorianCalendar currentDateTime = new GregorianCalendar() ;
    NodeIterator iter = storage_.getCalendarCategoryHome(username).getNodes() ;
    Node cat = null;
    String categoryId ;
    boolean isExists = false ;
    while(iter.hasNext()) {
      cat = iter.nextNode() ;
      if(cat.getProperty("exo:name").getString().equals("Imported")) {
        isExists = true ;
      }
    }
    if(!isExists) {
      CalendarCategory calendarCate = new CalendarCategory() ;
      currentDateTime = new GregorianCalendar() ;
      calendarCate.setDescription("Imported icalendar category") ;
      calendarCate.setName("Imported") ;
      categoryId = calendarCate.getId() ;
      storage_.saveCalendarCategory(username, calendarCate, true) ;
    }else {
      categoryId = cat.getProperty("exo:id").getString() ;
    }
    Calendar exoCalendar = new Calendar() ;
    exoCalendar.setName(calendarName) ;
    exoCalendar.setDescription(iCalendar.getProductId().getValue()) ;
    exoCalendar.setCategoryId(categoryId) ;
    exoCalendar.setPublic(true) ;
    storage_.saveUserCalendar(username, exoCalendar, true) ;   

    ComponentList componentList = iCalendar.getComponents() ;
    VEvent event ;
    CalendarEvent exoEvent ;
    for(Object obj : componentList) {
      if(obj instanceof VEvent){
        event = (VEvent)obj ;
        String eventCategoryId = null ;
        if(event.getProperty(Property.CATEGORIES) != null) {
          currentDateTime = new GregorianCalendar() ;
          EventCategory evCate = new EventCategory() ;
          evCate.setName(event.getProperty(Property.CATEGORIES).getValue()) ;
          evCate.setDescription(event.getProperty(Property.CATEGORIES).getValue()) ;
          try{
            storage_.saveEventCategory(username, evCate, null, true) ;
          }catch(Exception e){            
          }
          eventCategoryId = evCate.getName() ;
        }
        exoEvent = new CalendarEvent() ;
        currentDateTime = new GregorianCalendar() ;
        exoEvent.setCalendarId(exoCalendar.getId()) ;
        exoEvent.setEventCategoryId(eventCategoryId) ;
        if(event.getSummary() != null) exoEvent.setSummary(event.getSummary().getValue()) ;
        if(event.getDescription() != null) exoEvent.setDescription(event.getDescription().getValue()) ;
        if(event.getStatus() != null) exoEvent.setStatus(event.getStatus().getValue()) ;
        exoEvent.setEventType("event") ;
        if(event.getStartDate() != null) exoEvent.setFromDateTime(event.getStartDate().getDate()) ;
        if(event.getEndDate() != null) exoEvent.setToDateTime(event.getEndDate().getDate()) ;
        if(event.getLocation() != null) exoEvent.setLocation(event.getLocation().getValue()) ;
        if(event.getPriority() != null) exoEvent.setPriority(event.getPriority().getValue()) ;
        exoEvent.setPrivate(true) ;
        Reminder reminder = new Reminder() ;
        currentDateTime = new GregorianCalendar() ;
        reminder.setEventId(exoEvent.getId()) ;
        List<Reminder> reminders = new ArrayList<Reminder>() ;
        reminders.add(reminder) ;
        exoEvent.setReminders(reminders) ;
        PropertyList attendees = event.getProperties(Property.ATTENDEE) ;
        if(attendees.size() < 1) {
          exoEvent.setInvitation(new String[]{}) ;
        }else {
          String[] invitation = new String[attendees.size()] ;
          for(int i = 0; i < attendees.size(); i ++) {
            invitation[i] = ((Attendee)attendees.get(i)).getValue() ;
          }
          exoEvent.setInvitation(invitation) ;
        }
        storage_.saveUserEvent(username, exoCalendar.getId(), exoEvent, true) ;
      }
    }
  }  

}

