/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.hibernate.util.GetGeneratedKeysHelper;

/**
 * Author : Huu-Dung Kieu huu-dung.kieu@bull.be 14 f�vr. 08
 * 
 * This is a plugin running every time a new group is create.
 * The goal is to create a default calendar for each group.
 * The plugin configuration is defined in the portal/conf/cs/cs-plugin-configuration.xml file. 
 *
 */
public class NewGroupListener extends GroupEventListener {

	protected CalendarService calendarService_;

	private String defaultCalendarDescription;
	private String defaultLocale ;
	private String defaultTimeZone ;
	private String[] editPermission ; ;
	private String[] viewPermission ;
	/**
	 * 
	 * @param calendarService Calendar service geeting from the Portlet Container
	 * @param params  parameters defined in the plugin configuration
	 */
	public NewGroupListener(CalendarService calendarService, InitParams params) {

		calendarService_ = calendarService;

		if(params.getValueParam("defaultEditPermission") != null)
			editPermission = params.getValueParam("defaultEditPermission").getValue().split(",") ;
		if(params.getValueParam("defaultViewPermission") != null)
			viewPermission = params.getValueParam("defaultViewPermission").getValue().split(",") ;
		if(params.getValueParam("defaultCalendarDescription") != null)
			defaultCalendarDescription = params.getValueParam("defaultCalendarDescription").getValue() ;
		if(params.getValueParam("defaultLocale") != null) defaultLocale = params.getValueParam("defaultLocale").getValue() ;
		if(params.getValueParam("defaultTimeZone") != null) defaultTimeZone = params.getValueParam("defaultTimeZone").getValue() ;
	}

	public void postSave(Group group, boolean isNew) throws Exception { 
		if (!isNew) return;
		String groupId = group.getId();
		SessionProvider sProvider = SessionProvider.createSystemProvider();
	    try {
		boolean isPublic = true;
		Calendar calendar = new Calendar() ;
		calendar.setName(group.getGroupName()+" calendar") ;
		if(defaultCalendarDescription != null)
			calendar.setDescription(defaultCalendarDescription) ;
		calendar.setGroups(new String[]{groupId}) ;
		calendar.setPublic(isPublic) ;
		if(defaultLocale != null) calendar.setLocale(defaultLocale) ;
		if(defaultTimeZone != null) calendar.setTimeZone(defaultTimeZone) ;
		calendar.setCalendarColor(Calendar.SEASHELL);
		List<String> perms = new ArrayList<String>() ;
		for(String s : viewPermission) {
			if(!perms.contains(s)) perms.add(s) ;
		}
		calendar.setViewPermission(perms.toArray(new String[perms.size()])) ;
		perms.clear() ;
		for(String s : editPermission) {
      String groupKey = groupId + "/:" + s ;
			if(!perms.contains(groupKey)) perms.add(groupKey) ;
		}
		calendar.setEditPermission(perms.toArray(new String[perms.size()])) ;
		calendarService_.savePublicCalendar(sProvider, calendar, isNew, null) ;
	    } finally {
	    	if (sProvider != null) sProvider.close();
	    }		
	}
  @Override
  public void postDelete(Group group) throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
    List<GroupCalendarData> gCalData = calendarService_.getGroupCalendars(sProvider, new String[]{group.getId()}, true, null) ;
    for (GroupCalendarData gc : gCalData) {
      if(gc != null && !gc.getCalendars().isEmpty()) {
       for(Calendar c : gc.getCalendars()) {
         calendarService_.removePublicCalendar(sProvider, c.getId()) ;
       }
      }
    }
    super.postDelete(group);
    } finally {
    	if (sProvider != null) sProvider.close();
    }
  }
}
