/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.contact;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.exoplatform.contact.service.Contact;
import org.exoplatform.contact.service.ContactAttachment;
import org.exoplatform.contact.service.ContactService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.GroupImpl;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen Quang
 *          hung.nguyen@exoplatform.com
 * Jul 11, 2007  
 */
public class ContactUtils {
  
  static public String getCurrentUser() throws Exception {
    return Util.getPortalRequestContext().getRemoteUser() ; 
  }
  
  static public ContactService getContactService() throws Exception {
    return (ContactService)PortalContainer.getComponent(ContactService.class) ;
  }
  
  public static boolean isEmpty(String s) {
    if (s == null || s.length() == 0) return true ;
    return false ;    
  }
  public static String[] getUserGroups() throws Exception{
    OrganizationService organizationService = (OrganizationService)PortalContainer.getComponent(OrganizationService.class) ;
    Object[] objGroupIds = organizationService.getGroupHandler().findGroupsOfUser(getCurrentUser()).toArray() ;
    String[] groupIds = new String[objGroupIds.length];
    for (int i = 0; i < groupIds.length; i++) {
      groupIds[i] = ((GroupImpl)objGroupIds[i]).getId() ;
    }
    return groupIds ;
  }
  
  public static String getImageSource(Contact contact, DownloadService dservice) throws Exception {    
    ContactAttachment contactAttachment = contact.getAttachment();
    if (contactAttachment != null) {
      InputStream input = contactAttachment.getInputStream() ;
      byte[] imageBytes = null ;
      if (input != null) {
        imageBytes = new byte[input.available()] ;
        input.read(imageBytes) ;
        ByteArrayInputStream byteImage = new ByteArrayInputStream(imageBytes) ;
        InputStreamDownloadResource dresource = new InputStreamDownloadResource(byteImage, "image") ;
        dresource.setDownloadName(contactAttachment.getFileName()) ;
        return  dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;        
      }
    }
    return null ;
  }
  
}
