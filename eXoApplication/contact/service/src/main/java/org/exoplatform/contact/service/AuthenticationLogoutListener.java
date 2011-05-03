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
package org.exoplatform.contact.service;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SAS
 * Author : Quang Hung 
 *          quanghung@gmail.com
 * Feb 25, 2011  
 */
public class AuthenticationLogoutListener extends Listener<ConversationRegistry, ConversationState> {

  public AuthenticationLogoutListener() throws Exception {
  }

  @Override
  public void onEvent(Event<ConversationRegistry, ConversationState> event) throws Exception {
      PortalContainer container = PortalContainer.getInstance();
      if (container.isStarted()) {
        ContactService cService = (ContactService) container.getComponentInstanceOfType(ContactService.class);
        String username = event.getData().getIdentity().getUserId();
        List<String> tempContact = new ArrayList<String>();
        tempContact.add(Utils.contactTempId);
        cService.removeContacts(username, tempContact);
      }
  }
}
