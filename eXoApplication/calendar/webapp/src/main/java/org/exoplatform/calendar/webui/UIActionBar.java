/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.webui;

import org.exoplatform.calendar.webui.popup.UICalendarSettingForm;
import org.exoplatform.calendar.webui.popup.UIPopupAction;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
    template =  "app:/templates/calendar/webui/UIActionBar.gtmpl", 
    events = {
        @EventConfig(listeners = UIActionBar.ChangeViewActionListener.class),
        @EventConfig(listeners = UIActionBar.SettingActionListener.class)
    }
)
public class UIActionBar extends UIContainer  {
  final public static String DAY_VIEW = "dayview" ;
  final public static String WEEK_VIEW = "weekview" ;
  final public static String MONTH_VIEW = "monthview" ;
  
  public UIActionBar() throws Exception {    
  } 

  static public class ChangeViewActionListener extends EventListener<UIActionBar> {
    public void execute(Event<UIActionBar> event) throws Exception {
      UIActionBar uiActionBar = event.getSource() ;     
      String viewType = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UICalendarPortlet uiPortlet = uiActionBar.getAncestorOfType(UICalendarPortlet.class) ;
      UICalendarViewContainer uiViewContainer = uiPortlet.findFirstComponentOfType(UICalendarViewContainer.class) ;
      if(DAY_VIEW.equals(viewType)) {
        uiViewContainer.setRenderedChild(UIDayView.class);
      } else if(MONTH_VIEW.equals(viewType)) {
        uiViewContainer.setRenderedChild(UIMonthView.class);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }  

  static public class SettingActionListener extends EventListener<UIActionBar> {
    public void execute(Event<UIActionBar> event) throws Exception {
      UIActionBar uiActionBar = event.getSource() ;
      UICalendarPortlet calendarPortlet = uiActionBar.getAncestorOfType(UICalendarPortlet.class) ;
      UIPopupAction popupAction = calendarPortlet.getChild(UIPopupAction.class) ;
      popupAction.activate(UICalendarSettingForm.class, 600) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }

}
