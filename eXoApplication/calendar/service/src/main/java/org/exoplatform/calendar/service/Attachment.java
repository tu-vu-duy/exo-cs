/**
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
 **/
package org.exoplatform.calendar.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.util.IdGenerator;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * Sep 28, 2007  
 */

public class Attachment  {
  private String id ;
  private String name ;
  private String mimeType ;
  private long size ;
  //private InputStream data ;
  private  byte[] data ;
  private Calendar lastModified ;
  private String workspace ;
  public Attachment() {
    id =  "Attachment" + IdGenerator.generate() ;
  }

  public String getId() { return id ; }
  public void setId(String id) { this.id = id ; }

  public String getMimeType() { return mimeType ; }
  public void setMimeType(String mimeType_) { this.mimeType = mimeType_ ; }

  public long getSize() { return size ; }
  public void setSize(long size_) { this.size = size_ ; }

  public String getName() { return name ; }
  public void setName(String name_) { this.name = name_ ; }


  public InputStream getInputStream() throws Exception {
    if(data != null) return new ByteArrayInputStream(data) ;  
    Node attachmentData ;
    try{
      attachmentData = (Node)getSesison().getItem(getId()) ;      
    }catch (ItemNotFoundException e) {
      e.printStackTrace() ;
      return null ;
    }
    return attachmentData.getNode("jcr:content").getProperty("jcr:data").getStream() ;
  }
  public String getDataPath() throws Exception {
    Node attachmentData ;
    try{
      attachmentData = (Node)getSesison().getItem(getId()) ;      
    }catch (ItemNotFoundException e) {
      e.printStackTrace() ;
      return null ;
    }
    return attachmentData.getNode("jcr:content").getPath() ;
  }
  private Session getSesison()throws Exception {
    RepositoryService repoService = (RepositoryService)PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class) ;
    return repoService.getDefaultRepository().getSystemSession(workspace) ;
  }
  public void setInputStream(InputStream input) throws Exception {
    if (input != null) {
      data = new byte[input.available()] ; 
      input.read(data) ;
    }
    else data = null ;
  }

  public void setLastModified(Calendar lastModified) {
    this.lastModified = lastModified;
  }

  public Calendar getLastModified() {
    return lastModified;
  }

  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  public String getWorkspace() {
    return workspace;
  }
}