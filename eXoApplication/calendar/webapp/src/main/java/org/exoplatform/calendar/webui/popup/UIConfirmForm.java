/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.calendar.webui.popup;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 20, 2009  
 */
@ComponentConfig (
                  lifecycle  = UIFormLifecycle.class,
                  template =  "system:/groovy/webui/form/UIForm.gtmpl"
)

public class UIConfirmForm extends UIForm implements UIPopupComponent{

  public static String CONFIRM_TRUE = "true".intern();
  public static String CONFIRM_FALSE = "false".intern();
  private String config_id = "";
  public UIConfirmForm() {
    addUIFormInput(new UIFormInputInfo("confirm", "confirm", null)) ;
  }


  public void setConfirmMessage(String confirmMessage) {
    getUIFormInputInfo("confirm").setValue(confirmMessage) ;
    getUIFormInputInfo("confirm").setLabel("") ;
  }


  @Override
   public String event(String name) throws Exception {
    StringBuilder b = new StringBuilder() ;
    b.append("javascript:eXo.webui.UIForm.submitForm('").append(getConfig_id()).append("','");
    b.append(name).append("',true)");
    return b.toString() ;
  } 

  public void setConfig_id(String config_id) {
    this.config_id = config_id;
  }

  public String getConfig_id() {
    return config_id;
  }

  
  
  public void activate() throws Exception {
    // TODO Auto-generated method stub
    
  }


  public void deActivate() throws Exception {
    // TODO Auto-generated method stub
    
  }

}
