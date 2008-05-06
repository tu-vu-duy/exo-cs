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
package org.exoplatform.contact.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.contact.service.ContactService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.GroupImpl;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen Quang
 *          hung.nguyen@exoplatform.com
 * Nov 23, 2007 3:09:21 PM
 */
public class NewMembershipListener extends MembershipEventListener {
  private ContactService cservice_ ;
  private NodeHierarchyCreator nodeHierarchyCreator_ ;
  public NewMembershipListener(ContactService cservice, NodeHierarchyCreator nodeHierarchyCreator) throws Exception {
  	cservice_ = cservice ;
    nodeHierarchyCreator_ = nodeHierarchyCreator ;
  }
  
  public void postSave(Membership m, boolean isNew) throws Exception {
  	cservice_.addGroupToPersonalContact(m.getUserName(), m.getGroupId()) ;
    JCRDataStorage storage_ = new JCRDataStorage(nodeHierarchyCreator_) ;
    Node publicContactHome = storage_.getPublicContactHome(SessionProvider.createSystemProvider()) ;      
    String usersPath = nodeHierarchyCreator_.getJcrPath(JCRDataStorage.USERS_PATH) ;
    QueryManager qm = publicContactHome.getSession().getWorkspace().getQueryManager();
    List<String> recievedUser = new ArrayList<String>() ;
    recievedUser.add(m.getUserName()) ;
    
    StringBuffer queryString = new StringBuffer("/jcr:root" + usersPath 
        + "//element(*,exo:contactGroup)[@exo:viewPermissionGroups='").append( m.getGroupId() + "']") ;        
    Query query = qm.createQuery(queryString.toString(), Query.XPATH);
    QueryResult result = query.execute();
    NodeIterator nodes = result.getNodes() ;
    while (nodes.hasNext()) {
      Node address = nodes.nextNode() ;
      storage_.shareAddressBook(SessionProvider.createSystemProvider(), address.getProperty("exo:sharedUserId")
          .getString(), address.getProperty("exo:id").getString(),recievedUser) ;
    }
    
//  lookup shared contacts
    queryString = new StringBuffer("/jcr:root" + usersPath 
        + "//element(*,exo:contact)[@exo:viewPermissionGroups='").append(m.getGroupId() + "']") ;        
    query = qm.createQuery(queryString.toString(), Query.XPATH);
    result = query.execute();
    nodes = result.getNodes() ;
    while (nodes.hasNext()) {
      Node contactNode = nodes.nextNode() ;
      String split = "/" ;
      String temp = contactNode.getPath().split(usersPath)[1] ;
      String userId = temp.split(split)[1] ;
      storage_.shareContact(SessionProvider.createSystemProvider(), userId,
          new String[] {contactNode.getProperty("exo:id").getString()}, recievedUser) ;
    }
  }
  
  public void preDelete(Membership m) throws Exception {
    OrganizationService organizationService = 
      (OrganizationService)PortalContainer.getComponent(OrganizationService.class) ;
    Object[] objGroupIds = organizationService.getGroupHandler().findGroupsOfUser(m.getUserName()).toArray() ;
    List<String> groupIds = new ArrayList<String>() ;
    for (Object object : objGroupIds) {
      groupIds.add(((GroupImpl)object).getId()) ;
    }
    groupIds.remove(m.getGroupId()) ;
    
    try {
      JCRDataStorage storage_ = new JCRDataStorage(nodeHierarchyCreator_) ;
      Node publicContactHome = storage_.getPublicContactHome(SessionProvider.createSystemProvider()) ;      
      String usersPath = nodeHierarchyCreator_.getJcrPath(JCRDataStorage.USERS_PATH) ;
      QueryManager qm = publicContactHome.getSession().getWorkspace().getQueryManager();
      StringBuffer queryString = new StringBuffer("/jcr:root" + usersPath 
          + "//element(*,exo:contactGroup)[@exo:viewPermissionGroups='").append( m.getGroupId() + "']") ;        
      Query query = qm.createQuery(queryString.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes() ;
      while (nodes.hasNext()) {
        Node address = nodes.nextNode() ;
        storage_.removeUserShareAddressBook(
            SessionProvider.createSystemProvider(), address.getProperty("exo:sharedUserId")
            .getString(), address.getProperty("exo:id").getString(), m.getUserName()) ;
        
//      user shared if belong another groups shared
        for (Value groupShared : address.getProperty("exo:viewPermissionGroups").getValues()) {
          if (groupIds.contains(groupShared.getString())) {
            List<String> reciever = new ArrayList<String>() ;
            reciever.add(m.getUserName()) ;
            storage_.shareAddressBook( SessionProvider.createSystemProvider(), address.getProperty("exo:sharedUserId")
            .getString(), address.getProperty("exo:id").getString(), reciever) ;
          }
        }
      }
      
//    lookup shared contacts
      queryString = new StringBuffer("/jcr:root" + usersPath 
          + "//element(*,exo:contact)[@exo:viewPermissionGroups='").append(m.getGroupId() + "']") ;        
      query = qm.createQuery(queryString.toString(), Query.XPATH);
      result = query.execute();
      nodes = result.getNodes() ;
      while (nodes.hasNext()) {
        Node contactNode = nodes.nextNode() ;
        String split = "/" ;
        String temp = contactNode.getPath().split(usersPath)[1] ;
        String userId = temp.split(split)[1] ;
        storage_.removeUserShareContact(SessionProvider.createSystemProvider()
            , userId, contactNode.getProperty("exo:id").getString(), m.getUserName()) ;

        // user shared if belong another groups shared
        for (Value groupShared : contactNode.getProperty("exo:viewPermissionGroups").getValues()) {
          if (groupIds.contains(groupShared.getString())) {
            List<String> reciever = new ArrayList<String>() ;
            reciever.add(m.getUserName()) ;
            storage_.shareContact(SessionProvider.createSystemProvider(), 
                userId, new String [] {contactNode.getProperty("exo:id").getString()}, reciever) ;
          }
        }        
      }      
    } catch (ReferentialIntegrityException e) {
      
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}