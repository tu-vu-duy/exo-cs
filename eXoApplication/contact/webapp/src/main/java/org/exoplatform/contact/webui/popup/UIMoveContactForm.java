/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.contact.webui.popup;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/contact/webui/UIMoveContactForm.gtmpl",
    events = {
      @EventConfig(listeners = UIMoveContactForm.SaveActionListener.class),      
      @EventConfig(listeners = UIMoveContactForm.CancelActionListener.class)
    }
)
public class UIMoveContactForm extends UIForm {
  
  public UIMoveContactForm() {
    
  }
  
  static  public class SaveActionListener extends EventListener<UIMoveContactForm> {
    public void execute(Event<UIMoveContactForm> event) throws Exception {
      UIMoveContactForm uiForm = event.getSource() ;
    }
  }
  
  static  public class CancelActionListener extends EventListener<UIMoveContactForm> {
    public void execute(Event<UIMoveContactForm> event) throws Exception {
      UIMoveContactForm uiForm = event.getSource() ;
    }
  }
}
