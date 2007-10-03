/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.webui.popup;

import java.io.Writer;
import java.util.List;

import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.webui.UICalendarPortlet;
import org.exoplatform.calendar.webui.UICalendars;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Tuan Pham
 *          tuan.pham@exoplatform.com
 * Oct 3, 2007  
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {
      @EventConfig(listeners = UIEventCategoryManager.EditActionListener.class),
      @EventConfig(listeners = UIEventCategoryManager.DeleteActionListener.class, confirm = "UIEventCategoryManager.msg.confirm-delete")
    }
)
public class UIEventCategoryManager extends UIContainer implements UIPopupComponent {
  public static String[] BEAN_FIELD = {"name"};
  private static String[] ACTION = {"Edit", "Delete"} ;
  public UIEventCategoryManager() throws Exception {
    this.setName("UIEventCategoryManager") ;
    UIGrid categoryList = addChild(UIGrid.class, null , "UIEventCategoryList") ;
    categoryList.configure("name", BEAN_FIELD, ACTION) ;
    categoryList.getUIPageIterator().setId("EventCategoryIterator");
    addChild(UIEventCategoryForm.class, null, null) ;
    updateGrid() ;
  }

  public void activate() throws Exception {
    // TODO Auto-generated method stub

  }

  public void deActivate() throws Exception {
    // TODO Auto-generated method stub

  }
  public void processRender(WebuiRequestContext context) throws Exception {
    Writer w =  context.getWriter() ;
    w.write("<div id=\"UIEventCategoryManager\" class=\"UIEventCategoryManager\">");
    renderChildren();
    w.write("</div>");
  }
  public void updateGrid() throws Exception {
    CalendarService calService = getApplicationComponent(CalendarService.class) ;
    String username = Util.getPortalRequestContext().getRemoteUser() ;
    List<EventCategory>  categories = calService.getEventCategories(username) ;
    UIGrid uiGrid = getChild(UIGrid.class) ; 
    ObjectPageList objPageList = new ObjectPageList(categories, 10) ;
    uiGrid.getUIPageIterator().setPageList(objPageList) ;   
  }

  static  public class EditActionListener extends EventListener<UIEventCategoryManager> {
    public void execute(Event<UIEventCategoryManager> event) throws Exception {
      UIEventCategoryManager uiManager = event.getSource() ;
      UIEventCategoryForm uiForm = uiManager.getChild(UIEventCategoryForm.class) ;
      uiForm.setAddNew(false) ;
      String categoryId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      CalendarService calService = uiManager.getApplicationComponent(CalendarService.class) ;
      String username = event.getRequestContext().getRemoteUser() ;
      EventCategory category = calService.getEventCategory(username, categoryId) ;
      uiForm.setEventCategory(category) ;
      uiForm.setCategoryName(category.getName()) ;
      uiForm.setCategoryDescription(category.getDescription()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
  static  public class DeleteActionListener extends EventListener<UIEventCategoryManager> {
    public void execute(Event<UIEventCategoryManager> event) throws Exception {
      UIEventCategoryManager uiManager = event.getSource() ;
      UICalendarPortlet calendarPortlet = uiManager.getAncestorOfType(UICalendarPortlet.class) ;
      String eventCategoryName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      CalendarService calService = uiManager.getApplicationComponent(CalendarService.class) ;
      String username = event.getRequestContext().getRemoteUser() ;
      calService.removeEventCategory(username, eventCategoryName) ;
      UICalendars uiCalendars = calendarPortlet.findFirstComponentOfType(UICalendars.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiCalendars) ; 
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }
}
