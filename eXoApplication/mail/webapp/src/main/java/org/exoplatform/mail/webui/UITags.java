/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.mail.webui ;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.Tag;
import org.exoplatform.mail.webui.popup.UIMailSettings;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
    template =  "app:/templates/mail/webui/UITagContainer.gtmpl",
    events = {
        @EventConfig(listeners = UITags.ChangeTagActionListener.class),
        @EventConfig(listeners = UITags.RenameTagActionListener.class),
        @EventConfig(listeners = UITags.RemoveTagActionListener.class),
        @EventConfig(listeners = UITags.EmptyTagActionListener.class)
    }
)

public class UITags extends UIComponent {
  public UITags() throws Exception {}
  
  public List<Tag> getTags() throws Exception {
    List<Tag> tagList = new ArrayList<Tag>();
    MailService mailService = (MailService)PortalContainer.getComponent(MailService.class) ;
    UIMailPortlet uiPortlet = getAncestorOfType(UIMailPortlet.class);
    String username = uiPortlet.getCurrentUser() ;
    String accountId = uiPortlet.findFirstComponentOfType(UISelectAccount.class).getSelectedValue() ;
    if (accountId != null && accountId != "")  
      tagList = mailService.getTags(username, accountId);
    return tagList;
  }
  
  static public class ChangeTagActionListener extends EventListener<UITags> {
    public void execute(Event<UITags> event) throws Exception {
      String tagname = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UITags uiTags = event.getSource();
      UIMailPortlet uiPortlet = uiTags.getAncestorOfType(UIMailPortlet.class);
      UIMessageList uiMessageList = uiPortlet.findFirstComponentOfType(UIMessageList.class) ;
      uiMessageList.setSelectedFolderId(null);
      MailService mailSrv = uiPortlet.getApplicationComponent(MailService.class);
      String username = uiPortlet.getCurrentUser();
      String accountId = uiPortlet.findFirstComponentOfType(UISelectAccount.class).getSelectedValue();
      uiMessageList.setMessagePageList(mailSrv.getMessagePagelistByTag(username, accountId, tagname));
      uiMessageList.setSelectedTagName(tagname);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTags);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiMessageList);
    }
  }
  
  static public class RenameTagActionListener extends EventListener<UITags> {
    public void execute(Event<UITags> event) throws Exception {
      String tagName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UITags uiTags = event.getSource();  
      System.out.println("============>>>> Rename Tag Action Listener");
    }
  }
  
  static public class RemoveTagActionListener extends EventListener<UITags> {
    public void execute(Event<UITags> event) throws Exception {
      String tagName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UITags uiTags = event.getSource();     
      System.out.println("============>>>> Remove Tag Action Listener");
    }
  }
  
  static public class EmptyTagActionListener extends EventListener<UITags> {
    public void execute(Event<UITags> event) throws Exception {
      String tagName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UITags uiTags = event.getSource();     
      System.out.println("============>>>> Empty Tag Action Listener");
    }
  }
}