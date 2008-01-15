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

import org.exoplatform.mail.SessionsUtils;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.MessageFilter;
import org.exoplatform.mail.service.Utils;
import org.exoplatform.mail.webui.popup.UIAdvancedSearchForm;
import org.exoplatform.mail.webui.popup.UIPopupAction;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Phung Nam
 *          phunghainam@gmail.com
 * Nov 02, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/mail/webui/UISearchForm.gtmpl",
    events = {
      @EventConfig(listeners = UISearchForm.SearchActionListener.class),
      @EventConfig(listeners = UISearchForm.AdvancedActionListener.class)
    }
)
public class UISearchForm extends UIForm {
  final static private String FIELD_SEARCHVALUE = "search" ;
  
  public UISearchForm() {
    addChild(new UIFormStringInput(FIELD_SEARCHVALUE, FIELD_SEARCHVALUE, null)) ;
  }
  
  static  public class SearchActionListener extends EventListener<UISearchForm> {
    public void execute(Event<UISearchForm> event) throws Exception {
      UISearchForm uiSearchForm = event.getSource();
      UIMailPortlet uiPortlet = uiSearchForm.getAncestorOfType(UIMailPortlet.class) ;
      String text = uiSearchForm.getUIStringInput(FIELD_SEARCHVALUE).getValue();
      MessageFilter filter = new MessageFilter("Search"); 
      UIApplication uiApp = uiSearchForm.getAncestorOfType(UIApplication.class) ;
      String accId = uiPortlet.findFirstComponentOfType(UISelectAccount.class).getSelectedValue() ;
      if(Utils.isEmptyField(accId)) {
        uiApp.addMessage(new ApplicationMessage("UISearchForm.msg.account-list-empty", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      if(text == null || text.length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UISearchForm.msg.no-text-to-search", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      } else {
        text = text.replace("&", "&amp;");
        text = text.replace("<", "&lt;");
        text = text.replace(">", "&gt;");
        text = text.replace("'", "&apos;");
        text = text.replace("\"", "&quot;");
        String searchQuery = "(jcr:contains(@" + Utils.EXO_TO + ", '" + text + "'))" +
        " or (jcr:contains(@" + Utils.EXO_FROM + ", '" + text + "'))" +
        " or (jcr:contains(@" + Utils.EXO_SUBJECT + ", '" + text + "'))" +
        " or (jcr:contains(@" + Utils.EXO_BODY + ", '" + text + "'))";
        filter.setSearchQuery(searchQuery);
      }
      filter.setAccountId(accId);      
      
      UIMessageList uiMessageList = uiPortlet.findFirstComponentOfType(UIMessageList.class);      
      String username = uiPortlet.getCurrentUser();
      MailService mailService = uiPortlet.getApplicationComponent(MailService.class);
      
      uiMessageList.setMessagePageList(mailService.getMessages(SessionsUtils.getSessionProvider(), username, filter));
      uiMessageList.setSelectedFolderId(null);
      uiMessageList.setSelectedTagId(null);
      uiMessageList.setMessageFilter(filter);
      UIFolderContainer uiFolderContainer = uiPortlet.findFirstComponentOfType(UIFolderContainer.class);
      uiFolderContainer.setSelectedFolder(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFolderContainer);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMessageList.getAncestorOfType(UIMessageArea.class));

    }
  }
 
  static  public class AdvancedActionListener extends EventListener<UISearchForm> {
    public void execute(Event<UISearchForm> event) throws Exception {
      UISearchForm uiSearchForm = event.getSource() ;
      UIMailPortlet uiPortlet = uiSearchForm.getAncestorOfType(UIMailPortlet.class) ;
      UIApplication uiApp = uiSearchForm.getAncestorOfType(UIApplication.class) ;
      String accId = uiPortlet.findFirstComponentOfType(UISelectAccount.class).getSelectedValue() ;
      if(Utils.isEmptyField(accId)) {
        uiApp.addMessage(new ApplicationMessage("UISearchForm.msg.account-list-empty", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      UIPopupAction uiPopupAction = uiPortlet.getChild(UIPopupAction.class) ;
      UIAdvancedSearchForm uiAdvanceSearch = uiPopupAction.createUIComponent(UIAdvancedSearchForm.class, null, null);
      uiAdvanceSearch.init(accId);
      uiPopupAction.activate(uiAdvanceSearch, 600, 0 , false);    
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;     
    }
  } 
}
