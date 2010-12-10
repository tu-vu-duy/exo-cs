/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.calendar.webui.action;

import org.exoplatform.calendar.webui.listener.ActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 18, 2010  
 */
@ComponentConfig(

                 events = {
                     @EventConfig(listeners = QuickAddEvent.AddActionListener.class)
                 }
)

public class QuickAddEvent extends UIComponent {

  static public class AddActionListener extends ActionListener<QuickAddEvent> {
    protected void processEvent(Event<QuickAddEvent> event) throws Exception {
      // TODO Auto-generated method stub
      
    }
  }

}
