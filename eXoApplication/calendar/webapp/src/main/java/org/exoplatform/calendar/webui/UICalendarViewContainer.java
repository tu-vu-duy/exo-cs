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

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.CalendarSetting;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
    template =  "app:/templates/calendar/webui/UICalendarViewContainer.gtmpl"
)
public class UICalendarViewContainer extends UIContainer  {

  final public static String DAY_VIEW = "UIDayView".intern() ;
  final public static String WEEK_VIEW = "UIWeekView".intern() ;
  final public static String MONTH_VIEW = "UIMonthView".intern() ;
  final public static String YEAR_VIEW = "UIYearView".intern() ;
  final public static String LIST_VIEW = "UIListContainer".intern() ;
  final public static String SCHEDULE_VIEW = "UIScheduleView".intern() ;

  final public static String[] TYPES = {DAY_VIEW, WEEK_VIEW, MONTH_VIEW, YEAR_VIEW, LIST_VIEW, SCHEDULE_VIEW} ;

  public UICalendarViewContainer() throws Exception {
    initView(null) ;
  }  
  public void initView(String viewType) throws Exception {
    if(viewType == null) {
      CalendarSetting calendarSetting = new CalendarSetting() ;
      try {
        calendarSetting = getAncestorOfType(UICalendarPortlet.class).getCalendarSetting() ;
      }catch (Exception e) {
        CalendarService cservice = CalendarUtils.getCalendarService() ;
        String username = Util.getPortalRequestContext().getRemoteUser() ;
        calendarSetting =  cservice.getCalendarSetting(SessionProviderFactory.createSessionProvider(), username) ;
      }
      viewType = TYPES[Integer.parseInt(calendarSetting.getViewType())] ;
    }
    if(DAY_VIEW.equals(viewType)) {
      UIDayView uiView = getChild(UIDayView.class) ;
      if(uiView == null) uiView =  addChild(UIDayView.class, null, null) ;
      if(getRenderedChild() != null) uiView.setCurrentCalendar(((CalendarView)getRenderedChild()).getCurrentCalendar()) ;
      setRenderedChild(viewType) ;
    } else
      if(WEEK_VIEW.equals(viewType)) {
        UIWeekView uiView = getChild(UIWeekView.class) ;
        if(uiView == null) uiView =  addChild(UIWeekView.class, null, null) ;
        if(getRenderedChild() != null) uiView.setCurrentCalendar(((CalendarView)getRenderedChild()).getCurrentCalendar()) ;
        setRenderedChild(viewType) ;
      } else
        if(MONTH_VIEW.equals(viewType)) {
          UIMonthView uiView = getChild(UIMonthView.class) ;
          if(uiView == null) uiView =  addChild(UIMonthView.class, null, null) ;
          if(getRenderedChild() != null) uiView.setCurrentCalendar(((CalendarView)getRenderedChild()).getCurrentCalendar()) ;
          setRenderedChild(viewType) ;
        } else
          if(YEAR_VIEW.equals(viewType)) {
            UIYearView uiView = getChild(UIYearView.class) ;
            if(uiView == null) uiView =  addChild(UIYearView.class, null, null) ;
            if(getRenderedChild() != null) uiView.setCurrentCalendar(((CalendarView)getRenderedChild()).getCurrentCalendar()) ;
            setRenderedChild(viewType) ;
          } else
            if(LIST_VIEW.equals(viewType)) {
              UIListContainer uiView = getChild(UIListContainer.class) ;
              if(uiView == null) uiView =  addChild(UIListContainer.class, null, null) ;
              UIListView uiListView = uiView.getChild(UIListView.class) ;
              uiListView.setShowEventAndTask(false) ;
              uiListView.setDisplaySearchResult(false) ;
              uiListView.setCategoryId(null) ;
              uiListView.refresh() ;
              uiListView.isShowEvent_ = true ;
              if(getRenderedChild() != null) uiView.setCurrentCalendar(((CalendarView)getRenderedChild()).getCurrentCalendar()) ;
              setRenderedChild(viewType) ;
            } else
              if(SCHEDULE_VIEW.equals(viewType)) {
                UIScheduleView uiView = getChild(UIScheduleView.class) ;
                if(uiView == null) uiView =  addChild(UIScheduleView.class, null, null) ;
                if(getRenderedChild() != null) uiView.setCurrentCalendar(((CalendarView)getRenderedChild()).getCurrentCalendar()) ;
                setRenderedChild(viewType) ;
              }
    refresh() ;
    //((CalendarView)getRenderedChild()).setLastUpdatedEventId(null) ;
  }
  public void refresh() throws Exception {
    for(UIComponent comp : getChildren()) {
      if(comp.isRendered() && comp instanceof CalendarView){
        ((CalendarView)comp).refresh() ;
      }
    }
  }
  protected boolean isShowPane() {
    return getAncestorOfType(UICalendarWorkingContainer.class).getChild(UICalendarContainer.class).isRendered() ;
  }
  public UIComponent getRenderedChild() {
    for(UIComponent comp : getChildren()) {
      if(comp.isRendered()) return comp ;
    }
    return null ;
  }
  public void updateCategory() throws Exception{
    for(UIComponent comp : getChildren()) {
      if(comp instanceof CalendarView) {
        ((CalendarView)comp).update() ;
      }
    }
  }
  public void applySeting() throws Exception {
    for(UIComponent comp : getChildren()) {
      if((comp instanceof CalendarView) &&  comp.isRendered()) ((CalendarView)comp).applySeting() ;  
    }
  }
}
