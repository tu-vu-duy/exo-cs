/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.mail.webui.popup;

import org.exoplatform.mail.webui.UIMailPortlet;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/mail/webui/UIComposeForm.gtmpl",
    events = {
      @EventConfig(listeners = UIComposeForm.SendActionListener.class),      
      @EventConfig(listeners = UIComposeForm.SaveDraftActionListener.class),
      @EventConfig(listeners = UIComposeForm.DiscardChangeActionListener.class),
      @EventConfig(listeners = UIComposeForm.AttachmentActionListener.class),
      @EventConfig(listeners = UIComposeForm.RemoveAttachmentActionListener.class),
      @EventConfig(listeners = UIComposeForm.PriorityActionListener.class),
      @EventConfig(listeners = UIComposeForm.SelectContactActionListener.class)
    }
)
public class UIComposeForm extends UIForm implements UIPopupComponent{
  
  public UIComposeForm() {
    
  }
  
  public void activate() throws Exception {
    // TODO Auto-generated method stub
    
  }
  public void deActivate() throws Exception {
    // TODO Auto-generated method stub
    
  }
  
  static  public class SendActionListener extends EventListener<UIComposeForm> {
    public void execute(Event<UIComposeForm> event) throws Exception {
      UIComposeForm uiForm = event.getSource() ;
      System.out.println(" ==========> SendActionListener") ;
    }
  }
  static  public class SaveDraftActionListener extends EventListener<UIComposeForm> {
    public void execute(Event<UIComposeForm> event) throws Exception {
      UIComposeForm uiForm = event.getSource() ;
      System.out.println(" ==========> SaveDraftActionListener") ;
    }
  }
  static  public class DiscardChangeActionListener extends EventListener<UIComposeForm> {
    public void execute(Event<UIComposeForm> event) throws Exception {
      UIComposeForm uiForm = event.getSource() ;
      UIMailPortlet mailPortlet = event.getSource().getAncestorOfType(UIMailPortlet.class) ;
      mailPortlet.cancelAction() ;
    }
  }
  static  public class AttachmentActionListener extends EventListener<UIComposeForm> {
    public void execute(Event<UIComposeForm> event) throws Exception {
      UIComposeForm uiForm = event.getSource() ;
      System.out.println(" ==========> AttachmentActionListener") ;
    }
  }
  static  public class RemoveAttachmentActionListener extends EventListener<UIComposeForm> {
    public void execute(Event<UIComposeForm> event) throws Exception {
      UIComposeForm uiForm = event.getSource() ;
      System.out.println(" ==========> RemoveAttachmentActionListener") ;
    }
  }
  static  public class PriorityActionListener extends EventListener<UIComposeForm> {
    public void execute(Event<UIComposeForm> event) throws Exception {
      UIComposeForm uiForm = event.getSource() ;
      System.out.println(" ==========> PriorityActionListener") ;
    }
  }
  static  public class SelectContactActionListener extends EventListener<UIComposeForm> {
    public void execute(Event<UIComposeForm> event) throws Exception {
      UIComposeForm uiForm = event.getSource() ;
      System.out.println(" ==========> SelectContactActionListener") ;
    }
  }
  
  
}
