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
package org.exoplatform.mail.webui;


import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.MailSetting;
import org.exoplatform.mail.webui.popup.UIPopupAction;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupMessages;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;

/**
 * Author : Nhu Dinh Thuan
 *          nhudinhthuan@yahoo.com
 * May 30, 2006
 */
@ComponentConfig(
   lifecycle = UIApplicationLifecycle.class,
   template = "app:/templates/mail/webui/UIMailPortlet.gtmpl"
)
public class UIMailPortlet extends UIPortletApplication {
  private boolean isRefreshed = false ;
  public void setRefreshed(boolean b) { isRefreshed = b ;}
  public boolean isRefreshed() { return isRefreshed ; }
  
  public UIMailPortlet() throws Exception {
//    addChild(UIBannerContainer.class, null, null) ;
    addChild(UIActionBar.class, null, null) ;
    addChild(UINavigationContainer.class, null, null) ;
    String accId = getChild(UINavigationContainer.class).getChild(UISelectAccount.class).getSelectedValue();
    UIMessageArea uiMessageArea = createUIComponent(UIMessageArea.class, null, null);
    uiMessageArea.init(accId);
    addChild(uiMessageArea);
    addChild(UIPopupAction.class, null, null) ;
    //addChild(UIMailContainer.class, null, null) ;
  }
  
  public String getAccountId() {
    return getChild(UINavigationContainer.class).getChild(UISelectAccount.class).getSelectedValue();
  }
  
  public String getCurrentUser() {
    return Util.getPortalRequestContext().getRemoteUser() ;
  }
  
  public long getPeriodCheckAuto() throws Exception {
    MailService mailSrv = getApplicationComponent(MailService.class) ;
    MailSetting mailSetting = mailSrv.getMailSetting(SessionProviderFactory.createSystemProvider(), getCurrentUser()) ;
    Long period = mailSetting.getPeriodCheckAuto() * 60 * 1000 ;
    return period ;
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
  public String getRemoteUser() throws Exception {
    return CalendarUtils.getCurrentUser() ;
  }
  public String getUserToken()throws Exception {
    ContinuationService continuation = getApplicationComponent(ContinuationService.class) ;
    return continuation.getUserToken(this.getRemoteUser());
  }
} 
