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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.services.jcr.util.IdGenerator;

/**
 * Created by The eXo Platform SARL
 * Author : Phung Nam <phunghainam@gmail.com>
 *          Tuan Nguyen <tuan.nguyen@exoplatform.com>
 * Jun 23, 2007  
 * 
 */
public class Account {
  private String id ;
  private String label ;
  private String userDisplayName ;
  private String emailAddress ;
  private String emailReplyAddress ;
  private String signature ;  
  private String description ;
  private boolean checkedAuto_ ;
  private boolean emptyTrashWhenExit ;
  private String placeSignature;
  
  private Map<String, String> serverProperties ;  
  private Map<String, String> popServerProperties ;
  private Map<String, String> imapServerProperties ;
  private List<Folder> defaultFolders ;
  private List<Folder> userFolders ;  

  //  private MailServerConfiguration mailServerConfiguration ;
  
  public Account() {
    id = Utils.KEY_ACCOUNT + IdGenerator.generate() ;
    setPopServerProperty(Utils.SVR_POP_LEAVE_ON_SERVER, "true") ;
    setPopServerProperty(Utils.SVR_IMAP_MARK_AS_DELETE, "true") ;
  }
  
  /**
   * The id of the account for ex: GmailAccount, YahooAccount
   * @return the id of the account
   */
  public String getId()  { return id ; }
  public void   setId(String s) { id = s ; }
  
  /**
   * The display label of the account for ex:  Google Mail, Yahoo Mail
   * @return The label of the account
   */
  public String getLabel() { return label ; }
  public void   setLabel(String s) { label = s ; }
  
  /**
   * @return Return a list of the default folder: Inbox, Sent, Draft, Spam and Trash
   */
  public List<Folder> getDefaultFolder() { return defaultFolders ; }
  public void setDefaultFolder(List<Folder> folders) { defaultFolders = folders ; }
  
  /**
   * @return Return a list of the folder that is created by the user
   */
  public List<Folder> getUserFolder() { return userFolders ; }
  public void setUserFolder(List<Folder> folders) { userFolders = folders ; }
  
  /**
   * @return Return a description_ of account
   */
  public String getDescription() { return description ; }
  public void setDescription(String s) { description = s ; }
  
  /**
   * @return Return a signature of account
   */
  public String getSignature() { return signature ; }
  public void setSignature(String s) { signature = s ; }
  
  /**
   * @return Return a reply email address name of account
   */
  public String getEmailReplyAddress() { return emailReplyAddress ; }
  public void setEmailReplyAddress(String s) { emailReplyAddress = s ; }
  
  /**
   * @return Return a email address name of account
   */
  public String getEmailAddress() { return emailAddress ; }
  public void setEmailAddress(String s) { emailAddress = s ; }
  
  /**
   * @return Return a display name of account
   */
  public String getUserDisplayName() { return userDisplayName ; }
  public void setUserDisplayName(String s) { userDisplayName = s ; }
  
  /**
   * @return Return a boolean value that will set check mail automatically
   */
  public boolean checkedAuto() { return checkedAuto_; }
  public void setCheckedAuto(boolean checkedAuto) { checkedAuto_ = checkedAuto; }
  
  /**
   * @return Return a boolean value that will set to empty trash folder when exit
   */
  public boolean isEmptyTrashWhenExit() { return emptyTrashWhenExit; }
  public void setEmptyTrashWhenExit(boolean bool) { emptyTrashWhenExit = bool; }
  
  /**
   * @return Return a string display place to include email signature (head , foot ...)
   */
  public String getPlaceSignature() { return placeSignature; }
  public void setPlaceSignature(String placeSig) { placeSignature = placeSig; }
  
  /**
   * @return Return a mail server configuration of account
   */
//  public MailServerConfiguration getConfiguration() { return mailServerConfiguration ; }
//  public void setConfiguration(MailServerConfiguration config) { mailServerConfiguration = config ; }
  
  public Folder  getFolderByName(String name) { return null ; }
  
  /**
   * Manages the server properties, based on the serverProperties attribute
   */
  public void setServerProperty(String key, String value) {
    if (serverProperties == null) serverProperties = new HashMap<String, String>();
    serverProperties.put(key, value) ;
  }
  
  public Map<String, String> getServerProperties() { return serverProperties ; }
  
  public String getProtocol()  { return serverProperties.get(Utils.SVR_PROTOCOL) ; }
  public void setProtocol(String protocol) { 
    setServerProperty(Utils.SVR_PROTOCOL, protocol) ; 
  }
  
  public String getIncomingHost()  { return serverProperties.get(Utils.SVR_INCOMING_HOST) ; }
  public void setIncomingHost(String host) { 
    setServerProperty(Utils.SVR_INCOMING_HOST, host) ; 
  }
  
  public String getIncomingPort()  { return serverProperties.get(Utils.SVR_INCOMING_PORT) ; }
  public void setIncomingPort(String port) { 
    setServerProperty(Utils.SVR_INCOMING_PORT, port) ; 
  }
  
  public String getOutgoingHost() { return serverProperties.get(Utils.SVR_OUTGOING_HOST) ;}
  public void setOutgoingHost(String host) { 
    setServerProperty(Utils.SVR_OUTGOING_HOST, host) ;
  }
  
  public String getOutgoingPort() { return serverProperties.get(Utils.SVR_OUTGOING_PORT) ;}
  public void setOutgoingPort(String port) { 
    setServerProperty(Utils.SVR_OUTGOING_PORT, port) ;
  }
  
  public String getIncomingFolder() { return serverProperties.get(Utils.SVR_INCOMING_FOLDER) ; }
  public void setIncomingFolder(String folder)  { 
    setServerProperty(Utils.SVR_INCOMING_FOLDER, folder) ; 
  }
  
  public String getIncomingUser()  { return serverProperties.get(Utils.SVR_INCOMING_USERNAME) ; }
  public void setIncomingUser(String user)  { 
    setServerProperty(Utils.SVR_INCOMING_USERNAME, user) ; 
  }
  
  public String getIncomingPassword()  { return serverProperties.get(Utils.SVR_INCOMING_PASSWORD) ; }
  public void setIncomingPassword(String password)  { 
    setServerProperty(Utils.SVR_INCOMING_PASSWORD, password) ; 
  }
  
  public boolean isIncomingSsl()  { return serverProperties.get(Utils.SVR_INCOMING_SSL).equalsIgnoreCase("true");  }
  public void setIncomingSsl(boolean b) { 
    setServerProperty(Utils.SVR_INCOMING_SSL, String.valueOf(b)); 
  }
  
  public void setPopServerProperty(String key, String value) {
    if (popServerProperties == null) popServerProperties = new HashMap<String, String>();
    popServerProperties.put(key, value) ;
  }
  
  public Map<String, String> getPopServerProperties() { return popServerProperties ; }
  
  public void setImapServerProperty(String key, String value) {
    if (imapServerProperties == null) imapServerProperties = new HashMap<String, String>();
    imapServerProperties.put(key, value) ;
  }
  
  public Map<String, String> getImapServerProperties() { return imapServerProperties ; }
}
