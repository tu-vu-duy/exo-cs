/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.mail.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.exoplatform.mail.service.Account;
import org.exoplatform.mail.service.Contact;
import org.exoplatform.mail.service.Folder;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.Message;
import org.exoplatform.mail.service.MessageFilter;
import org.exoplatform.mail.service.MessageHeader;
import org.exoplatform.registry.JCRRegistryService;
import org.exoplatform.services.jcr.RepositoryService;
/**
 * Created by The eXo Platform SARL
 * Author : Tuan Nguyen
 *          tuan.nguyen@exoplatform.com
 * Jun 23, 2007  
 */
public class MailServiceImpl implements MailService{
  
  private JCRDataStorage storage_ ;
  
  public MailServiceImpl(RepositoryService  repositoryService, 
                         JCRRegistryService jcrRegistryService) throws Exception {
    storage_ = new JCRDataStorage(repositoryService, jcrRegistryService) ;      
  }
  
  /**
   * @param username
   * @return
   * @throws Exception
   */
  public List<Account> getAccounts(String username) throws Exception {
    return storage_.getAccounts(username);
  }
  
  public Account getAccountById(String username, String id) throws Exception {
    return storage_.getAccountById(username, id);
  }
  
  public void saveAccount(String username, Account account, boolean isNew) throws Exception {
    storage_.saveAccount(username, account, isNew);
  }
  
  public void updateAccount(String username, Account account) throws Exception {
    saveAccount(username, account, false);
  }
  
  public void removeAccount(String username, Account account) throws Exception {
    storage_.removeAccount(username, account);
  }
  
  public Folder getFolder(String username, String accountId, String folderName) throws Exception {
    // gets the folder of the specified id (from any account of the user username)
    Folder folder = null;
    Node folderHome = storage_.getFolderHome(username, accountId);
    Node node = null;
    // if this folder exists, creates the object and returns it
    if (folderHome.hasNode(folderName)) {
      node = folderHome.getNode(folderName);
      folder = new Folder();
      folder.setLabel(node.getProperty("exo:label").getString());
      folder.setName(node.getProperty("exo:name").getString());
      folder.setNumberOfUnreadMessage((int)node.getProperty("exo:unreadMessages").getLong());
    }
    return folder ;
  }
  
  public void saveUserFolder(String username, String accountId, Folder folder) throws Exception {
    // gets the specified account node
    Node home = storage_.getFolderHome(username, accountId);
    Node myFolder = null;
    if (home.hasNode(folder.getName())) { // if it exists, gets it
      myFolder = home.getNode(folder.getName());
    } else { // if it doesn't exist, creates it
      myFolder = home.addNode(folder.getName(), "exo:folder");
    }
    myFolder.setProperty("exo:label", folder.getLabel());
    myFolder.setProperty("exo:unreadMessages", folder.getNumberOfUnreadMessage());
    myFolder.setProperty("exo:name", folder.getName());

    home.getSession().save();
  }
  

  public void removeUserFolder(String username, Folder folder) throws Exception {
    // gets the specified folder
    Session sess = storage_.getMailHomeNode(username).getSession();
    QueryManager qm = sess.getWorkspace().getQueryManager();
    StringBuffer queryString = new StringBuffer("//element(*,exo:folder)[@exo:name='").
                                  append(folder.getName()).
                                  append("']");
    Query query = qm.createQuery(queryString.toString(), Query.XPATH);
    QueryResult result = query.execute();
    NodeIterator it = result.getNodes();
    // if it exists, removes it
    if (it.hasNext()) it.nextNode().remove();
    sess.save();
  }
  
  public void removeUserFolder(String username, Account account, Folder folder) throws Exception {
    //  gets the specified folder
    Node folderHome = storage_.getFolderHome(username, account.getId());
    Node myFolder = folderHome.getNode(folder.getName());
    if (myFolder != null) {
      myFolder.remove();
    }
    folderHome.getSession().save();
  }
  
  public Message getMessageById(String username, String messageName, String accountId) throws Exception {
    return storage_.getMessageById(username, accountId, messageName);
  }
  
  public void removeMessage(String username, String messageName, String accountId) throws Exception {
    storage_.removeMessage(username, accountId, messageName);
  }
  
  public void removeMessage(String username, String[] messageName, String accountId) throws Exception {
    storage_.removeMessage(username, accountId, messageName);
  }
  
  public List<MessageHeader> getMessageByFolder(String username, Folder folder, String accountId) throws Exception {
    // gets all the messages from the specified folder, using a filter
    MessageFilter filter = new MessageFilter("filter by folder "+folder.getName());
    filter.setAccountId(accountId);
    String[] folders = {folder.getName()};
    filter.setFolder(folders);
    return storage_.getMessages(username, filter);
  }
 
  
  public List<MessageHeader> getMessageByFilter(String username, MessageFilter filter) throws Exception {
    return storage_.getMessages(username, filter);
  }

  public void saveMessage(String username, String accountId, Message message, boolean isNew) throws Exception {
    storage_.saveMessage(username, accountId, message, isNew);
  }
  
  public void sendMessage(Message message) throws Exception {
    // sends an email with the parameters in message
    Properties props = new Properties();
    props.put("mail.smtp.host", "smtp.jcom.net");
    javax.mail.Session session = javax.mail.Session.getDefaultInstance(props, null);
    javax.mail.Message msg = new MimeMessage(session);
    InternetAddress addressFrom = new InternetAddress("");
    msg.setFrom(addressFrom);

    InternetAddress[] addressTo = new InternetAddress[1];
    addressTo[0] = new InternetAddress(message.getMessageTo());
    msg.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);
   
    // Optional : You can also set your custom headers in the Email if you Want
    msg.addHeader("MyHeaderName", "myHeaderValue");

    // Setting the Subject and Content Type
    msg.setSubject(message.getSubject());
    msg.setContent(message.getMessageBody(), "text/plain");
    Transport.send(msg);
  }

  public void addTag(String username, Message message, String tag) throws Exception {
    Node homeTags = storage_.getTagHome(username, message.getAccountId());
    if (!homeTags.hasNode(tag)) { // if the tag doesn't exist in jcr, we create it
      homeTags.addNode(tag, "exo:tag");
    }
    // gets the tags from the message
    String[] tags = message.getTags();
    String[] newtags = new String[tags.length+1];

    boolean addTag = true;
    // if the message already has the tag, addTag will be set to false
    for (int i=0; i<tags.length && addTag; i++) {
      addTag &= !tags[i].equalsIgnoreCase(tag);
      newtags[i] = tags[i];
    }
    if (addTag) {
      newtags[tags.length] = tag;
      Node homeMsg = storage_.getMessageHome(username, message.getAccountId());
      NodeIterator it = homeMsg.getNodes();
      while (it.hasNext()) {
        Node msg = it.nextNode();
        // if we find the node representing the message, we modify its property tags
        if (msg.getProperty("exo:receivedDate").getLong() == message.getReceivedDate().getTime()) {
          msg.setProperty("exo:tags", newtags);
          homeMsg.getSession().save();
          break;
        }
      }
    }
    homeTags.getSession().save();
  }
  
  public void removeTag(String username, Account account, String tag) throws Exception {
    MessageFilter filter = new MessageFilter("filter by tag "+tag);
    filter.setAccountId(account.getId());
    String[] tags = {tag};
    filter.setTag(tags);
    List<MessageHeader> list = storage_.getMessages(username, filter);
    if (list.size() > 0) {
      Iterator<MessageHeader> it = list.iterator();
      while (it.hasNext()) {
        MessageHeader header = it.next();
        Message message = getMessageById(username, header.getId(), account.getId());
        removeTag(username, message, tag);
      }
    }
    Node homeTags = storage_.getTagHome(username, account.getId());
    if (homeTags.hasNode(tag)) homeTags.getNode(tag).remove();
    homeTags.getSession().save();
  }

  public void removeTag(String username, Message message, String tag) throws Exception {
    String[] tags = message.getTags();
    Node msgNode = null;
    Node homeMsg = storage_.getMessageHome(username, message.getAccountId());
    NodeIterator it = homeMsg.getNodes();
    while (it.hasNext()) {
      Node msg = it.nextNode();
      // if we find the node representing the message, we modify its property tags
      if (msg.getProperty("exo:receivedDate").getLong() == message.getReceivedDate().getTime()) {
        msgNode = msg;
        break;
      }
    }
    if (msgNode != null) {
      String[] newtags = new String[tags.length];
      for (int i=0; i<tags.length; i++) {
        if (!tags[i].equalsIgnoreCase(tag)) newtags[i] = tags[i];
      }
      msgNode.setProperty("exo:tags", newtags);
    }
    homeMsg.getSession().save();
  }
  
  public int checkNewMessage(String username, Account account) throws Exception {
    // TODO Auto-generated method stub
    return 0;
  }


  public void createAccount(String username, Account account) throws Exception {
    saveAccount(username, account, true);
  }  


}