/*
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
 */
package org.exoplatform.contact.webui;

import org.exoplatform.contact.webui.popup.UIPopupAction;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupMessages;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 * Author : Nguyen Quang Hung
 *          hung.nguyen@exoplatform.com
 * Aug 01, 2007
 */
@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class,
    template = "app:/templates/contact/webui/UIContactPortlet.gtmpl"
)
public class UIContactPortlet extends UIPortletApplication {
  
  // replace when portal fix bug show image
  private boolean isRefreshed = false ;
  public void setRefreshed(boolean b) { isRefreshed = b ; }
  public boolean isRefreshed() { return isRefreshed ; }

  public UIContactPortlet() throws Exception {
    //addChild(UIBannerContainer.class, null, null) ;
    addChild(UIActionBar.class, null, null) ;
    addChild(UIWorkingContainer.class, null, null) ;
    addChild(UIPopupAction.class, null, null) ;
  }

  public void renderPopupMessages() throws Exception {
    UIPopupMessages popupMess = getUIPopupMessages();
    if(popupMess == null)  return ;
    WebuiRequestContext  context =  WebuiRequestContext.getCurrentInstance() ;
    popupMess.processRender(context);
  }
  
  public void cancelAction() throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    UIPopupAction popupAction = getChild(UIPopupAction.class) ;
    popupAction.deActivate() ;
    context.addUIComponentToUpdateByAjax(popupAction) ;
  }
}