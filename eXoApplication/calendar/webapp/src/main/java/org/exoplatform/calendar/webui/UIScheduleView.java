/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.webui;

import java.util.LinkedHashMap;

import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
    template =  "app:/templates/calendar/webui/UIScheduleView.gtmpl"
)
public class UIScheduleView extends UICalendarView  {
  public UIScheduleView() throws Exception {
    
  }

  @Override
  public void refresh() throws Exception {
    System.out.println("\n\n>>>>>>>>>> SCHEDULE VIEW") ;
    // TODO Auto-generated method stub
  }

  @Override
  LinkedHashMap<String, CalendarEvent> getDataMap() {
    // TODO Auto-generated method stub
    return null;
  }  
}
