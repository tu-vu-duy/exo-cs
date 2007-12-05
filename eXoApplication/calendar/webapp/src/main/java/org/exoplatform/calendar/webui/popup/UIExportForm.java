/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.webui.popup;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;

import net.fortuna.ical4j.model.ValidationException;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.SessionsUtils;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarImportExport;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.calendar.webui.UICalendarPortlet;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.download.DownloadResource;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIExportForm.SaveActionListener.class),      
      @EventConfig(listeners = UIExportForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UIExportForm extends UIForm implements UIPopupComponent{
  final static private String NAME = "name".intern() ;
  final static private String TYPE = "type".intern() ;
  private String calType = "0" ;
  public UIExportForm() throws Exception {
    CalendarService calendarService = CalendarUtils.getCalendarService();
    addUIFormInput(new UIFormStringInput(NAME, NAME, null)) ;
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ; 
    for(String exportType : calendarService.getExportImportType()) {
      options.add(new SelectItemOption<String>(exportType, exportType)) ;
    }
    addUIFormInput(new UIFormSelectBox(TYPE, TYPE, options)) ;
  }
  public void setCalType(String type) {calType = type ; }
  public void update(String type, String selectedCalendarId) throws Exception {
    calType = type ;
    Iterator iter = getChildren().iterator() ;
    while(iter.hasNext()) {
      if(iter instanceof UIFormCheckBoxInput) {
        iter.remove() ;
      }
      iter.next() ;
    }
    CalendarService calendarService = CalendarUtils.getCalendarService();
    List<Calendar> calendars = new ArrayList<Calendar>();
    if(calType.equals("0")) {
      calendars = calendarService.getUserCalendars(SessionsUtils.getSessionProvider(), CalendarUtils.getCurrentUser()) ;
    }else if(calType.equals("1")) {
      calendars = calendarService.getSharedCalendars(SessionsUtils.getSystemProvider(), CalendarUtils.getCurrentUser()).getCalendars() ;
    }else if(calType.equals("2")){
      List<GroupCalendarData> groups = calendarService.getGroupCalendars(SessionsUtils.getSystemProvider(), CalendarUtils.getUserGroups(CalendarUtils.getCurrentUser())) ;
      for(GroupCalendarData group : groups) {
        calendars.addAll(group.getCalendars()) ;
      }
    }
    initCheckBox(calendars, selectedCalendarId) ;
  }
  public void initCheckBox(List<Calendar> calendars, String selectedCalendarId) {
    for(Calendar calendar : calendars) {
      UIFormCheckBoxInput checkBox = new UIFormCheckBoxInput<String>(calendar.getName(), calendar.getId(), null);
      if(calendar.getId().equals(selectedCalendarId)) checkBox.setChecked(true) ; 
      else checkBox.setChecked(false) ;       
      addUIFormInput(checkBox) ;
    }
  }
  public String getLabel(String id) throws Exception {
    try {
      return  super.getLabel(id) ;
    } catch (MissingResourceException mre) {
      return id ;
    }
  } 
  
  public void activate() throws Exception {}
  public void deActivate() throws Exception {}
  
  static  public class SaveActionListener extends EventListener<UIExportForm> {
    public void execute(Event<UIExportForm> event) throws Exception {
      UIExportForm uiForm = event.getSource() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      CalendarService calendarService = CalendarUtils.getCalendarService() ;
      List<UIComponent> children = uiForm.getChildren() ;
      List<String> calendarIds = new ArrayList<String> () ;
      for(UIComponent child : children) {
        if(child instanceof UIFormCheckBoxInput) {
          if(((UIFormCheckBoxInput)child).isChecked()) {
            calendarIds.add(((UIFormCheckBoxInput)child).getBindingField()) ;
          }
        }
      }
      if(calendarIds.isEmpty()) {
        uiApp.addMessage(new ApplicationMessage("UIExportForm.msg.calendar-does-not-existing", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      String type = uiForm.getUIFormSelectBox(uiForm.TYPE).getValue() ;
      String name = uiForm.getUIStringInput(uiForm.NAME).getValue() ;
      CalendarImportExport importExport = calendarService.getCalendarImportExports(type) ;
      OutputStream out = null ;
      try {
        out = importExport.exportCalendar(SessionsUtils.getSystemProvider(), CalendarUtils.getCurrentUser(), calendarIds, uiForm.calType) ;        
      }catch(ValidationException e) {
        uiApp.addMessage(new ApplicationMessage("UIExportForm.msg.event-does-not-existing", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      ByteArrayInputStream is = new ByteArrayInputStream(out.toString().getBytes()) ;
      DownloadResource dresource = new InputStreamDownloadResource(is, "text/iCalendar") ;
      DownloadService dservice = (DownloadService)PortalContainer.getInstance().getComponentInstanceOfType(DownloadService.class) ;
      if(name != null && name.length() > 0) {
        if(name.length() > 4 && name.substring(name.length() - 4).equals(".ics") )dresource.setDownloadName(name);
        else dresource.setDownloadName(name + ".ics");
      }else {
        dresource.setDownloadName("eXoICalendar.ics");
      }
      String downloadLink = dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');") ;
      calendarPortlet.cancelAction() ;      
    }
  }
  
  static  public class CancelActionListener extends EventListener<UIExportForm> {
    public void execute(Event<UIExportForm> event) throws Exception {
      UIExportForm uiForm = event.getSource() ;
      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      calendarPortlet.cancelAction() ;
    }
  }  
}
