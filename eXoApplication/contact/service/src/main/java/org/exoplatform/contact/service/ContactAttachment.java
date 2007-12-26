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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * July 2, 2007  
 * 
 */
public class ContactAttachment {
  private String id ;
  private String fileName ;
  private String mimeType ;
  private String workspace ;
  private InputStream inputStream ;
  
  public String getId() { return id ; }
  public void   setId(String s) { id = s ; }
  
  public String getWorkspace() { return workspace ; }
  public void setWorkspace(String ws) { workspace = ws ; }
  
  public String getFileName()  { return fileName ; }
  public void   setFileName(String s) { fileName = s ; }
  
  public String getMimeType() { return mimeType ; }
  public void setMimeType(String s) { mimeType = s ;}
  
  public void setInputStream(InputStream input) throws Exception {
    inputStream = input ;
  }
  public InputStream getInputStream() throws Exception {
    if(inputStream != null) return inputStream ;
    Node attachment ;
    try{
      
      //System.out.println("\n\n atttttt:" + getSesison().getItem(getId()) + "\n\n");
      
      attachment = (Node)getSesison().getItem(getId()) ;      
    }catch (ItemNotFoundException e) {
      //System.out.println("\n\n item not founddddddddd \n\n");
      
      return null ;
    }
    return attachment.getNode("jcr:content").getProperty("jcr:data").getStream() ;
  }
  
  private Session getSesison()throws Exception {
    RepositoryService repoService = (RepositoryService)PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class) ;
    return repoService.getDefaultRepository().getSystemSession(workspace) ;
  }
  
}
