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
package org.exoplatform.mail.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.mail.SessionsUtils;
import org.exoplatform.mail.service.Account;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.Utils;
import org.exoplatform.mail.webui.UIMailPortlet;
import org.exoplatform.mail.webui.UISelectAccount;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * Sep 4, 2007  
 */
@ComponentConfig(
    template = "app:/templates/mail/webui/UIGridWithButton.gtmpl",
    events = {
        @EventConfig(listeners = UIAccountList.DeleteActionListener.class, confirm = "UIAccountList.msg.confirm-delete"),
        @EventConfig(listeners = UIAccountList.CloseActionListener.class)
    }
)
public class UIAccountList extends UIGrid  implements UIPopupComponent{
  private static String[] BEAN_FIELD = {"name", "email", "server","protocol"} ;
  private static String[] BEAN_ACTION = {"Delete"} ;

  public UIAccountList() throws Exception {
    configure("id", BEAN_FIELD, BEAN_ACTION) ;
    updateGrid() ;
  }

  public void updateGrid() throws Exception {
    List<AccountData> accounts = new ArrayList<AccountData>() ;
    String userId = Util.getPortalRequestContext().getRemoteUser() ;
    MailService mailSvr = getApplicationComponent(MailService.class) ;
    for(Account acc : mailSvr.getAccounts(SessionsUtils.getSessionProvider(), userId)) {
      accounts.add(new AccountData(acc.getId(), acc.getUserDisplayName(), acc.getEmailAddress(), 
          acc.getServerProperties().get(Utils.SVR_INCOMING_HOST), acc.getProtocol())) ;
    }

    ObjectPageList objPageList = new ObjectPageList(accounts, 10) ;
    getUIPageIterator().setPageList(objPageList) ; 
  }

  public void activate() throws Exception {
    // TODO Auto-generated method stub

  }
  public void deActivate() throws Exception {
    // TODO Auto-generated method stub

  }
  public String[] getButtons(){
    return new String[] {"Close"} ;
  }
  public class AccountData {
    String id ;
    String name ;
    String email ;
    String server ;
    String protocol ;

    public AccountData(String iId, String iName, String iEmail, String iServer, String iProtocol){
      id = iId ;
      name = iName ;
      email = iEmail ;
      server = iServer ;
      protocol = iProtocol ;
    }
    public String getId() {return id ;} ;
    public String getName() {return name ;}
    public String getEmail () {return email ;}
    public String getServer() {return server ;}
    public String getProtocol() {return protocol ;}
  }

  static  public class DeleteActionListener extends EventListener<UIAccountList> {
    public void execute(Event<UIAccountList> event) throws Exception {
      System.out.println("=====>>> DeleteActionListener");
      UIAccountList uiAccountList = event.getSource() ;
      UIMailPortlet uiPortlet = uiAccountList.getAncestorOfType(UIMailPortlet.class) ;
      UISelectAccount uiSelectAccount = uiPortlet.findFirstComponentOfType(UISelectAccount.class) ;
      String currAccountId = uiSelectAccount.getSelectedValue();
      String accId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIApplication uiApp = uiAccountList.getAncestorOfType(UIApplication.class) ;
      MailService mailSvr = uiAccountList.getApplicationComponent(MailService.class) ;
      String username = event.getRequestContext().getRemoteUser() ;

      Account account = mailSvr.getAccountById(SessionsUtils.getSessionProvider(), username, accId) ;
      try {
        mailSvr.removeAccount(SessionsUtils.getSessionProvider(), username, account) ;
        uiSelectAccount.refreshItems() ;
        uiAccountList.updateGrid() ;
        if (currAccountId.equals(accId)) {
          if (mailSvr.getAccounts(SessionsUtils.getSessionProvider(), username).size() == 0) {
            uiSelectAccount.setSelectedValue("");
            mailSvr.updateCurrentAccount(SessionsUtils.getSessionProvider(), username, "");
          }
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet) ; 
        } else {
          event.getRequestContext().addUIComponentToUpdateByAjax(uiAccountList.getAncestorOfType(UIPopupAction.class)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiSelectAccount) ;
        }
      } catch (Exception e) {
        uiApp.addMessage(new ApplicationMessage("UIAccountList.msg.remove-accout-error", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        e.printStackTrace() ;
      }
    }
  }
  static  public class CloseActionListener extends EventListener<UIAccountList> {
    public void execute(Event<UIAccountList> event) throws Exception {
      UIPopupAction uiPopup = event.getSource().getAncestorOfType(UIPopupAction.class);
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }
}
