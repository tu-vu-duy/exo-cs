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
package org.exoplatform.mail.service;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
/**
 * @author Hung Nguyen (hung.nguyen@exoplatform.com)
 *         Phung Nam (phunghainam@gmail.com) 
 * @since July 25, 2007
 */
public class MessagePageList extends JCRPageList {
  
  private NodeIterator iter_ = null ;
  private boolean isQuery_ = false ;
  private String value_ ;
  Map<String, Message> previousListPage ;
  private boolean hasStructure_ ;
  
  public MessagePageList(NodeIterator iter, long pageSize, String value, boolean isQuery , boolean hasStructure) throws Exception{
    super(pageSize) ;
    iter_ = iter ;
    value_ = value ;
    isQuery_ = isQuery ;
    hasStructure_ = hasStructure ;
    setAvailablePage(iter.getSize()) ;    
  }
  
  protected void populateCurrentPage(long page, String username) throws Exception  {
    if(iter_ == null) {
      Session session = getJCRSession(username) ;
      if(isQuery_) {
        QueryManager qm = session.getWorkspace().getQueryManager() ;
        Query query = qm.createQuery(value_, Query.XPATH);
        QueryResult result = query.execute();
        iter_ = result.getNodes();
      } else {
        Node node = (Node)session.getItem(value_) ;
        iter_ = node.getNodes() ;
      }
      session.logout() ;
    }
    setAvailablePage(iter_.getSize()) ;
    Node currentNode ;
    long pageSize = getPageSize() ;
    long position = 0 ;
    if(page == 1) position = 0;
    else {
      position = (page-1) * pageSize ;
      iter_.skip(position) ;
    }
    previousListPage = new HashMap<String, Message>() ;
    if (currentListPage_ != null) previousListPage = currentListPage_;
    currentListPage_ = new LinkedHashMap<String, Message>() ;
    
    for(int i = 0; i < pageSize; i ++) {
      if(iter_.hasNext()){
        currentNode = iter_.nextNode() ;
          Value[] values = {};
          boolean existRefNode = false ; 
          String curMsgFolder = currentNode.getProperty(Utils.EXO_FOLDERS).getValues()[0].getString() ;
          String accId = currentNode.getProperty(Utils.EXO_ACCOUNT).getString() ;
          String sentFolderId = Utils.createFolderId(accId, Utils.FD_SENT, false) ;
          String[] refFolders = new String[] {sentFolderId, curMsgFolder} ;
          if (hasStructure_) {
            try {
              values = currentNode.getProperty("exo:conversationId").getValues();
              for (int j = 0; j < values.length; j++) {
                Value value = values[j];
                String uuid = value.getString();
                Node refNode = currentNode.getSession().getNodeByUUID(uuid);
                String refMsgFolder = refNode.getProperty(Utils.EXO_FOLDERS).getValues()[0].getString() ;
                if (refMsgFolder.equals(curMsgFolder)) existRefNode = true ;
                if (refMsgFolder.equals(sentFolderId)) {
                  existRefNode = true ; 
                  Message refMsg = getMessage(refNode, refFolders) ;
                  if (refMsg.getFolders() != null && refMsg.getFolders().length > 0) currentListPage_.put(refMsg.getId(), refMsg) ;
                  currentListPage_ = getMessageList(currentListPage_, refNode, curMsgFolder, refFolders) ;
                }
              }
            } catch(Exception e) { }
            if (Utils.SHOWCONVERSATION && (!currentNode.isNodeType("exo:messageMixin") || !existRefNode)) {
              Message msg = getMessage(currentNode, refFolders) ;
              if (msg.getFolders() != null && msg.getFolders().length > 0) currentListPage_.put(msg.getId(), msg) ;
              currentListPage_ = getMessageList(currentListPage_, currentNode, curMsgFolder, refFolders) ;
            }
          } else {
            Message msg = getMessage(currentNode, null) ;
            if (msg.getFolders() != null && msg.getFolders().length > 0) currentListPage_.put(msg.getId(), msg) ;
          }
        } else {
          break ;
        }
    }
    iter_ = null ; 
  }
  
  private LinkedHashMap<String, Message> getMessageList(LinkedHashMap<String, Message> listPage, Node currentNode, String folderId, String[] refFolders) throws Exception {
    PropertyIterator prosIter = currentNode.getReferences() ;
    String accId = currentNode.getProperty(Utils.EXO_ACCOUNT).getString() ;
    String sentFolderId = Utils.createFolderId(accId, Utils.FD_SENT, false) ;
    Node msgNode ;
    while (prosIter.hasNext()) {
      msgNode = prosIter.nextProperty().getParent() ;
      if (msgNode.isNodeType("exo:message")) {
        try {
          String msgFolder = msgNode.getProperty(Utils.EXO_FOLDERS).getValues()[0].getString() ;
          if (folderId.equals(msgFolder) || sentFolderId.equals(msgFolder)) {
            Message msg = getMessage(msgNode, refFolders) ;
            msg.setIsRootConversation(false) ;
            if (msg.getFolders() != null && msg.getFolders().length > 0) listPage.put(msg.getId(), msg) ;
            if (msgNode.isNodeType("mix:referenceable")) listPage = getMessageList(listPage, msgNode, folderId, refFolders) ; 
          }
        } catch(Exception e) {}
      }
    }
    return listPage ;
  }
  
  /**
   * 
   * @param node
   * @param refFolders
   * @return Matching this node to message object is necessary or not. We can get it in old page if it is avaiable instead of rematch it.
   * @throws Exception
   */
  private boolean isAvaiableMessage(Node node, String[] refFolders) throws Exception {
    return previousListPage.size() > 0 && 
      previousListPage.containsKey(node.getName()) && 
      (refFolders == null || previousListPage.get(node.getName()).getReferedMessageIds().size() != getReferedMessageIds(node, refFolders).size()) ;
  }
  
  /**
   * 
   * @param messageNode
   * @param refFolders
   * @return message object, that is new one or have some properties that have to update in this page.
   * @throws Exception
   */
  private Message getMessage(Node messageNode, String[] refFolders) throws Exception {
    if (isAvaiableMessage(messageNode, refFolders)) return previousListPage.get(messageNode.getName()) ;
    
    Message msg = new Message();
    if (messageNode.hasProperty(Utils.EXO_ID)) msg.setId(messageNode.getProperty(Utils.EXO_ID).getString());
    msg.setPath(messageNode.getPath());
    try { 
      msg.setAccountId(messageNode.getProperty(Utils.EXO_ACCOUNT).getString()) ;
    } catch(Exception e) { } 
    try { 
      msg.setFrom(messageNode.getProperty(Utils.EXO_FROM).getString());
    } catch(Exception e) { }
    try { 
      msg.setMessageTo(messageNode.getProperty(Utils.EXO_TO).getString());
    } catch(Exception e) { }
    try { 
      msg.setSubject(messageNode.getProperty(Utils.EXO_SUBJECT).getString());
    } catch(Exception e) { }
    try { 
      msg.setMessageCc(messageNode.getProperty(Utils.EXO_CC).getString());
    } catch(Exception e) { }
    try { 
      msg.setMessageBcc(messageNode.getProperty(Utils.EXO_BCC).getString());
    } catch(Exception e) { }
    try { 
      msg.setReplyTo(messageNode.getProperty(Utils.EXO_REPLYTO).getString());
    } catch(Exception e) { }
    try { 
      msg.setMessageBody(messageNode.getProperty(Utils.EXO_BODY).getString());
    } catch(Exception e) { }
    try { 
      msg.setSize(messageNode.getProperty(Utils.EXO_SIZE).getLong());
    } catch(Exception e) { }
    try { 
      msg.setHasAttachment(messageNode.getProperty(Utils.EXO_HASATTACH).getBoolean());
    } catch(Exception e) { }
    try { 
      msg.setHasStar(messageNode.getProperty(Utils.EXO_STAR).getBoolean());
    } catch(Exception e) { }
    try { 
      msg.setPriority(messageNode.getProperty(Utils.EXO_PRIORITY).getLong());
    } catch(Exception e) { }
    try {
      msg.setUnread(messageNode.getProperty(Utils.EXO_ISUNREAD).getBoolean());
    } catch(Exception e) { }
    try {
      msg.setContentType(messageNode.getProperty(Utils.EXO_CONTENT_TYPE).getString());
    } catch(Exception e) { }
    try {
      Value[] propTags = messageNode.getProperty(Utils.EXO_TAGS).getValues();
      String[] tags = new String[propTags.length];
      for (int i = 0; i < propTags.length; i++) {
        tags[i] = propTags[i].getString();
      }
      msg.setTags(tags);
    } catch(Exception e) { }
    try {
      Value[] propFolders = messageNode.getProperty(Utils.EXO_FOLDERS).getValues();
      String[] folders = new String[propFolders.length];
      for (int i = 0; i < propFolders.length; i++) {
        folders[i] = propFolders[i].getString();
      }
      msg.setFolders(folders);
    } catch(Exception e) { }
    
    try {
      Value[] properties = messageNode.getProperty(Utils.EXO_HEADERS).getValues();
      for (int i=0; i<properties.length; i++) {
        String property = properties[i].getString();
        int index = property.indexOf('=');
        if (index != -1) msg.setHeader(property.substring(0, index), property.substring(index+1));
      }
    } catch(Exception e) { }
    
    GregorianCalendar cal = new GregorianCalendar();
    try {
      cal.setTimeInMillis(messageNode.getProperty(Utils.EXO_RECEIVEDDATE).getLong());
      msg.setReceivedDate(cal.getTime());
    } catch(Exception e) { }

    try {
      cal.setTimeInMillis(messageNode.getProperty(Utils.EXO_SENDDATE).getLong());
      msg.setSendDate(cal.getTime());
    } catch(Exception e) { }
    
    if (hasStructure_) {
      if (refFolders == null) refFolders = new String[]{ msg.getFolders()[0] } ;
      List<String> referedMessageIds = getReferedMessageIds(messageNode, refFolders) ;
      msg.setReferedMessageIds(referedMessageIds);
      
      //update refMessageIds in refered message if this message also contains child of current message
      Value[] values = {};
      try {
        values = messageNode.getProperty("exo:conversationId").getValues();
        for (int j = 0; j < values.length; j++) {
          Value value = values[j];
          String uuid = value.getString();
          Node refNode = messageNode.getSession().getNodeByUUID(uuid);
          Message refMsg = currentListPage_.get(refNode.getName()) ;
          List<String> sibling = refMsg.getReferedMessageIds() ;
          sibling.removeAll(referedMessageIds) ;
          if (refMsg != null) {
            refMsg.setReferedMessageIds((new ArrayList<String>(sibling))) ;
            if (refMsg.getFolders() != null && refMsg.getFolders().length > 0) currentListPage_.put(refNode.getName(),  refMsg) ;
          }
        }
      } catch(Exception e) { }
      
      List<String> groupedMessageIds = new ArrayList<String>() ;
      groupedMessageIds = getGroupedMessageIds(groupedMessageIds, messageNode, refFolders) ; 
      msg.setGroupedMessageIds(groupedMessageIds);
    }
    
    return msg ;
  }
  
  public List<String> getReferedMessageIds(Node node, String[] refFolders) throws Exception {
    List<String> referedMessageIds = new ArrayList<String>() ;
    PropertyIterator prosIter = node.getReferences() ;
    Node msgNode ;
    while (prosIter.hasNext()) {
      msgNode = prosIter.nextProperty().getParent() ;
      for (int i = 0; i < refFolders.length; i ++) {
        try {
          if (refFolders[i].equals(msgNode.getProperty(Utils.EXO_FOLDERS).getValues()[0].getString())) {
            String id = msgNode.getProperty(Utils.EXO_ID).getString() ;
            if (!referedMessageIds.contains(id)) referedMessageIds.add(id) ;
            break ;
          }
        } catch(Exception e) {}
      }
    }
    return referedMessageIds ;
  }
  
  private List<String> getGroupedMessageIds(List<String> list, Node currentNode, String[] refFolders) throws Exception {
    PropertyIterator prosIter = currentNode.getReferences() ;
    Node msgNode ;
    while (prosIter.hasNext()) {
      msgNode = prosIter.nextProperty().getParent() ;
      if (msgNode.isNodeType("exo:message")) {
        try {
          String msgFolderId = msgNode.getProperty(Utils.EXO_FOLDERS).getValues()[0].getString() ;
          String msgNodeId = msgNode.getProperty(Utils.EXO_ID).getString() ;
          for(int i=0; i < refFolders.length; i ++) {
            if (refFolders[i].equals(msgFolderId)) {
              if (!list.contains(msgNodeId)) list.add(msgNodeId) ;
              if (msgNode.isNodeType("mix:referenceable")) {
                list = getGroupedMessageIds(list, msgNode, refFolders) ;
              }
              break ;
            }
          }
        } catch(Exception e) { }
      }
    }
    return list ;
  }
  
  
/*  private String [] ValuesToStrings(Value[] Val) throws Exception {
  	if(Val.length == 1)
  		return new String[]{Val[0].getString()};
		String[] Str = new String[Val.length];
		for(int i = 0; i < Val.length; ++i) {
		  Str[i] = Val[i].getString();
		}
		return Str;
  }*/
  
	@Override
  public List<Message> getAll() throws Exception { return null; }
	
  public List<Message> getAll(String username) throws Exception { 
    List<Message> messageList = new ArrayList<Message>();
    for (int i = 1; i <= getAvailablePage(); i++) {
      messageList.addAll(getPage(i, username));
    }
    return messageList;
  }

  private Session getJCRSession(String username) throws Exception {
    RepositoryService  repositoryService = (RepositoryService)PortalContainer.getComponent(RepositoryService.class) ;
    SessionProvider sessionProvider = SessionProvider.createSystemProvider() ;
    String defaultWS = 
      repositoryService.getDefaultRepository().getConfiguration().getDefaultWorkspaceName() ;
    return sessionProvider.getSession(defaultWS, repositoryService.getCurrentRepository()) ;
  }

}
