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
package org.exoplatform.contact.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Author : Huu-Dung Kieu
 *          huu-dung.kieu@bull.be
 * 16 oct. 07  
 */
public interface ContactImportExport {
  public void importContact(SessionProvider sProvider, String username, InputStream input, String groupId) throws Exception ;
  public OutputStream exportContact(String username, List<Contact> contacts) throws Exception ;
  public OutputStream exportContact(SessionProvider sProvider, String username, String[] addressBookIds) throws Exception ;
}
