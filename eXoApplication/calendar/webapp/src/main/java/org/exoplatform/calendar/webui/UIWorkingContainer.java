/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.calendar.webui;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
    template =  "app:/templates/contact/webui/UIWorkingContainer.gtmpl"
)
public class UIWorkingContainer extends UIContainer  {
  public UIWorkingContainer() throws Exception {
    //addChild(UICalendarContainer.class, null, null) ;
    //addChild(UICalendarView.class, null, null) ;
  }  
}
