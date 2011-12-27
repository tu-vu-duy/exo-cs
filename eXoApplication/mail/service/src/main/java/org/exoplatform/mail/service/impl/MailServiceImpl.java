/*
 * Copyright (C) 20\03-2007 eXo Platform SAS .
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
package org.exoplatform.mail.service.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MailcapCommandMap;
import javax.jcr.Node;
import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.RecipientStringTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.SubjectTerm;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.mail.connection.Connector;
import org.exoplatform.mail.connection.impl.BaseConnector;
import org.exoplatform.mail.connection.impl.ImapConnector;
import org.exoplatform.mail.connection.impl.Pop3Connector;
import org.exoplatform.mail.service.Account;
import org.exoplatform.mail.service.Attachment;
import org.exoplatform.mail.service.BufferAttachment;
import org.exoplatform.mail.service.CheckMailJob;
import org.exoplatform.mail.service.CheckingInfo;
import org.exoplatform.mail.service.Folder;
import org.exoplatform.mail.service.Info;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.MailSetting;
import org.exoplatform.mail.service.MailSettingConfigPlugin;
import org.exoplatform.mail.service.MailUpdateStorageEventListener;
import org.exoplatform.mail.service.Message;
import org.exoplatform.mail.service.MessageFilter;
import org.exoplatform.mail.service.MessagePageList;
import org.exoplatform.mail.service.MimeMessageParser;
import org.exoplatform.mail.service.ServerConfiguration;
import org.exoplatform.mail.service.SpamFilter;
import org.exoplatform.mail.service.Tag;
import org.exoplatform.mail.service.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.JobInfo;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.services.scheduler.PeriodInfo;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;
import org.gatein.common.util.ParameterValidation;
import org.picocontainer.Startable;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.smtp.SMTPSendFailedException;
import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.MailSSLSocketFactory;

/**
 * Created by The eXo Platform SARL Author : Tuan Nguyen
 * tuan.nguyen@exoplatform.com Jun 23, 2007
 */
public class MailServiceImpl implements MailService, Startable {
  
  public static final String COMMA = ",";

  private static final Log             logger         = ExoLogger.getLogger("cs.mail.service");

  private JCRDataStorage               storage_;

  private EMLImportExport              emlImportExport_;

  private Map<String, CheckingInfo>    checkingLog_;

  private JobSchedulerService          schedulerService_;

  private ContinuationService          continuationService_;
  
  private RepositoryService            repositoryService;

  private String                       folderStr      = "";

  private Map<String, MailSettingConfigPlugin> settingPlugins = new HashMap<String, MailSettingConfigPlugin>();
  
  private Map<String, Map<String, Object>> connectionStore = new HashMap<String, Map<String,Object>>();
  
  private List<MailUpdateStorageEventListener> listeners_ = new ArrayList<MailUpdateStorageEventListener>();

  public MailServiceImpl(InitParams initParams,
                         NodeHierarchyCreator nodeHierarchyCreator,
                         JobSchedulerService schedulerService,
                         RepositoryService reposervice, 
                         ContinuationService continuationService) throws Exception {
    storage_ = new JCRDataStorage(nodeHierarchyCreator, reposervice);
    emlImportExport_ = new EMLImportExport(storage_);
    checkingLog_ = new ConcurrentHashMap<String, CheckingInfo>();
    this.repositoryService = reposervice;
    this.schedulerService_ = schedulerService;
    this.continuationService_ = continuationService;
  }

  public String getMailHierarchyNode() throws Exception {
    return storage_.getMailHierarchyNode();
  }

  public void removeCheckingInfo(String userName, String accountId) throws Exception {
    String key = userName + ":" + accountId;
    checkingLog_.remove(key);
  }

  public CheckingInfo getCheckingInfo(String userName, String accountId) {
    String key = userName + ":" + accountId;
    CheckingInfo info = checkingLog_.get(key);
    return info;
  }

  /**
   * @param username
   * @return
   * @throws Exception
   */
  public List<Account> getAccounts(String userName) throws Exception {
    return storage_.getAccounts(userName);
  }

  public Account getAccountById(String userName, String id) throws Exception {
    return storage_.getAccountById(userName, id);
  }

  private void saveAccount(String userName, Account account, boolean isNew) throws Exception {
    storage_.saveAccount(userName, account, isNew);
  }

  public void updateAccount(String userName, Account account) throws Exception {
    saveAccount(userName, account, false);
  }

  public void updateErrorAccount(String userName, Account account) throws Exception {
    saveAccount(userName, account, false);
    
    String accountId = account.getId();
    CheckingInfo info = getCheckingInfo(userName, accountId);
    String key = userName + ":" + accountId;
    if (info == null) {
      checkingLog_.put(key, new CheckingInfo());
    }

    if (account.getProtocol().equals(Utils.IMAP)) {
      synchImapFolders(userName, accountId);
    }
  }

  public void removeAccount(String userName, String accountId) throws Exception {
    stopAllJobs(userName, accountId);
    try {
      Map<String, String> persm = storage_.getAccountById(userName, accountId).getPermissions();
      if (persm != null) {
        for (String key : persm.keySet()) {
          storage_.removeDelegateAccount(key, accountId);
        }
      }
    } catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug(" remmove deletage account error :  " + e.getMessage());
    }
    storage_.removeAccount(userName, accountId);
  }
  
  public Folder getFolderById(String username, String accountId, String folderId) throws Exception {
    return storage_.getFolderById(username, accountId, folderId);
  }

  public Folder getFolder(String userName, String accountId, String folderPath) throws Exception {
    return storage_.getFolder(userName, accountId, folderPath);
  }

  public String getFolderParentId(String userName, String accountId, String folderId) throws Exception {
    return storage_.getFolderParentId(userName, accountId, folderId);
  }

  public boolean isExistFolder(String userName, String accountId, String parentId, String folderName) throws Exception {
    return storage_.isExistFolder(userName, accountId, parentId, folderName);
  }

  public void saveFolder(String userName, String accountId, Folder folder) throws Exception {
    Account account = getAccountById(userName, accountId);
    saveFolder(userName, account, folder, true);
  }

  public void saveFolderImapOnline(String userName, String accountId, Folder folder) throws Exception {
    Account delegatedAcc = getDelegatedAccount(userName, accountId);
    if (delegatedAcc != null) {
      userName = delegatedAcc.getDelegateFrom();
    }
    
    Account account = getAccountById(userName, accountId);
    if (account.getProtocol().equalsIgnoreCase(Utils.IMAP) && folder.isPersonalFolder()) {
      IMAPFolder imapFolder = null;
      try {
        ImapConnector connector = openIMAPConnection(userName, account, null);
        imapFolder = (IMAPFolder) connector.createFolder(folder);
        saveFolder(userName, account, null, imapFolder);
      } finally {
        if (imapFolder != null && imapFolder.isOpen()) {
          imapFolder.close(true);
        }
      }
    } else {
      storage_.saveFolder(userName, accountId, folder);
    }
  }

  private void saveFolder(String userName, Account account, Folder folder, boolean b) throws Exception {
    if (account.getProtocol().equalsIgnoreCase(Utils.IMAP) && folder.isPersonalFolder() && b) {
      IMAPFolder imapFolder = null;
      try {
        ImapConnector connector = openIMAPConnection(userName, account, null);
        imapFolder = connector.createFolder(folder);
        saveFolder(userName, account, null, imapFolder);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Exception in method saveFolder", e);
        }
        return;
      } finally {
        if ((imapFolder != null) && imapFolder.isOpen()) {
          imapFolder.close(true);
        }
      }
    } else {
      storage_.saveFolder(userName, account.getId(), folder);
    }
  }

  public void saveFolder(String userName, String accountId, String parentId, Folder folder) throws Exception {
    Account account = getAccountById(userName, accountId);
    Folder parentFolder = getFolderById(userName, accountId, parentId);
    saveFolder(userName, account, parentFolder, folder, true);
  }

  public void saveFolderImapOnline(String userName, String accountId, String parentId, Folder folder) throws Exception {
    Account account = getAccountById(userName, accountId);
    if (account.getProtocol().equalsIgnoreCase(Utils.IMAP) && folder.isPersonalFolder()) {
    Folder parentFolder = getFolderById(userName, accountId, parentId);
    ImapConnector connector = openIMAPConnection(userName, account, null);
    IMAPFolder imapFolder = connector.createFolder(parentFolder, folder);
    if (imapFolder != null) {
      saveFolder(userName, account, parentFolder, imapFolder);
    }
    } else {// pop3
      storage_.saveFolder(userName, accountId, parentId, folder);
    }
  }

  private void saveFolder(String userName, Account account, Folder parentFolder, Folder folder, boolean b) throws Exception {
    String accountId = account.getId();
    if (account.getProtocol().equalsIgnoreCase(Utils.IMAP) && folder.isPersonalFolder() && b) {
      try {
        ImapConnector connector = openIMAPConnection(userName, account, null);
        IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(parentFolder, folder);
        if (imapFolder != null) {
          saveFolder(userName, account, parentFolder, imapFolder);
        }
      } catch (Exception e) {
        return;
      }
    } else {
      storage_.saveFolder(userName, accountId, parentFolder.getId(), folder);
    }
  }

  private void saveFolder(String userName, Account account, Folder parentFolder, javax.mail.Folder serverFolder) throws Exception {
    String accountId = account.getId();
    String folderId;
    Folder folder;
    if (serverFolder.getType() != javax.mail.Folder.HOLDS_FOLDERS) {
      folderId = Utils.generateFID(accountId, String.valueOf(((IMAPFolder) serverFolder).getUIDValidity()), true);
    } else {
      folderId = Utils.escapeIllegalJcrChars(serverFolder.getName());
    }
    folder = storage_.getFolderById(userName, accountId, folderId);
    if (folder == null) {
      folder = new Folder();
      folder.setId(folderId);
      folder.setName(serverFolder.getName());
      folder.setURLName(serverFolder.getURLName().toString());
      folder.setNumberOfUnreadMessage(0);
      folder.setTotalMessage(0);
      folder.setPersonalFolder(true);
      folder.setType(serverFolder.getType());
      try {
        if (parentFolder == null) {
          storage_.saveFolder(userName, accountId, folder);
        } else {
          storage_.saveFolder(userName, accountId, parentFolder.getId(), folder);
        }
      } catch (Exception e) {
      }
    }
  }

  public void renameFolder(String userName, String accountId, String newName, String folderId) throws Exception {
    Account account = getAccountById(userName, accountId);
    Folder folder = getFolderById(userName, accountId, folderId);
    if (account.getProtocol().equalsIgnoreCase(Utils.IMAP) && folder.isPersonalFolder()) {
      try {
        ImapConnector connector = openIMAPConnection(userName, account, null);
        folder = connector.renameFolder(newName, folder);
      } catch (Exception e) {
        return;
      }
    }
    
    if (folder != null) {
      storage_.renameFolder(userName, accountId, newName, folder);
    }
  }

  private void deleteLocalFolder(String userName, Account account, Folder folder) throws Exception {
    storage_.removeUserFolder(userName, account.getId(), folder.getId());
  }

  public void removeUserFolder(String userName, String accountId, String folderId) throws Exception {
    Account account = getAccountById(userName, accountId);
    Folder folder = getFolderById(userName, accountId, folderId);
    boolean success = true;
    if (account.getProtocol().equalsIgnoreCase(Utils.IMAP) && folder.isPersonalFolder()) {
      ImapConnector connector = openIMAPConnection(userName, account, null);
      success = connector.deleteFolder(folder);
    }
    
    if (success) {
      storage_.removeUserFolder(userName, accountId, folderId);
    }
  }

  public List<MessageFilter> getFilters(String userName, String accountId) throws Exception {
    Account delegatorAcc = getDelegatedAccount(userName, accountId);
    if (Utils.isDelegatedAccount(delegatorAcc, userName)) {
      userName = getDelegatedAccount(userName, accountId).getDelegateFrom();
    }
    return storage_.getFilters(userName, accountId);
  }

  public MessageFilter getFilterById(String userName, String accountId, String filterId) throws Exception {
    Account delegatorAcc = getDelegatedAccount(userName, accountId);
    if (Utils.isDelegatedAccount(delegatorAcc, userName)) {
      userName = getDelegatedAccount(userName, accountId).getDelegateFrom();
    }
    return storage_.getFilterById(userName, accountId, filterId);
  }

  public void saveFilter(String userName, String accountId, MessageFilter filter, boolean applyAll) throws Exception {
    String toFolder = filter.getApplyFolder();
    List<Message> messageList = storage_.getMessagePageList(userName, filter).getAll(userName);
    String fromFolder = null;
    if (messageList.size() > 0) {
      fromFolder = messageList.get(0).getFolders()[0];
    }
    moveMessages(userName, accountId, messageList, fromFolder, toFolder);

    storage_.saveFilter(userName, accountId, filter, applyAll);
  }

  public void removeFilter(String userName, String accountId, String filterId) throws Exception {
    storage_.removeFilter(userName, accountId, filterId);
  }

  public Message getMessageById(String userName, String accountId, String msgId) throws Exception {
    return storage_.getMessageById(userName, accountId, msgId);
  }

  public void removeMessage(String userName, String accountId, Message message) throws Exception {
    storage_.removeMessage(userName, accountId, message);
  }

  public void removeMessages(String userName, String accountId, List<Message> messages, boolean moveReference) throws Exception {
    storage_.removeMessages(userName, accountId, messages, moveReference);
  }

  public List<Message> moveMessages(String userName, String accountId, List<Message> msgList, String currentFolderId, String destFolderId) throws Exception {
    moveMessages(userName, accountId, msgList, currentFolderId, destFolderId, false);
    return msgList;
  }

  public List<Message> moveMessages(String userName, String accountId, List<Message> msgList, String currentFolderId, String destFolderId, boolean updateReference) throws Exception {
    Account account = getAccountById(userName, accountId);
    Folder currentFolder = getFolderById(userName, accountId, currentFolderId);
    Folder destFolder = getFolderById(userName, accountId, destFolderId);
    
    // Move messages
    if (account.getProtocol().equalsIgnoreCase(Utils.IMAP)) {
      ImapConnector connector = openIMAPConnection(userName, account, null);
      AppendUID[] appendUIDs = connector.moveMessage(msgList, currentFolder, destFolder);
      long[] newUUIDs = null;
      if (appendUIDs != null) {
        newUUIDs = new long[appendUIDs.length];
        for (int i = 0; i < appendUIDs.length; i++) {
          newUUIDs[i] = appendUIDs[i].uid;
        }
      }
      storage_.moveMessages(userName, accountId, msgList, currentFolderId, destFolderId, updateReference, newUUIDs);
    } else {
      storage_.moveMessages(userName, accountId, msgList, currentFolderId, destFolderId, updateReference, null);
    }
    
    // Syn desfolder
//    if (account.getProtocol().equalsIgnoreCase(Utils.IMAP)) {
//      ImapConnector connector = openIMAPConnection(userName, account, null);
//      String folderUrl = Utils.isEmptyField(destFolder.getURLName()) ? destFolder.getName() : destFolder.getURLName();
//      javax.mail.Folder destImapFolder = connector.getFolder(folderUrl);
//      synchImapMessage(userName, account, destImapFolder, null);
//    }
    return msgList;
  }

  public Message moveMessage(String userName, String accountId, Message msg, String currentFolderId, String destFolderId) throws Exception {
    moveMessage(userName, accountId, msg, currentFolderId, destFolderId, true);
    return msg;
  }

  public void moveMessage(String userName, String accountId, Message msg, String currentFolderId, String destFolderId, boolean updateReference) throws Exception {
    List<Message> msgList = new ArrayList<Message>();
    msgList.add(msg);
    moveMessages(userName, accountId, msgList, currentFolderId, destFolderId, updateReference);
  }

  public MessagePageList getMessagePageList(String userName, MessageFilter filter) throws Exception {
    Account delegatorAcc = getDelegatedAccount(userName, filter.getAccountId());
    if (Utils.isDelegatedAccount(delegatorAcc, userName)) {
      userName = getDelegatedAccount(userName, filter.getAccountId()).getDelegateFrom();
    }
    return storage_.getMessagePageList(userName, filter);
  }

  public boolean saveMessage(String userName, Account account, String targetMsgPath, Message message, boolean isNew) throws Exception {
    List<Message> msgList = new ArrayList<Message>();
    List<Message> successList = null;
    msgList.add(message);
    String folderId = message.getFolders()[0];
    Folder destFolder = null;
    boolean success = false;
    if (folderId != null) {
      destFolder = getFolderById(userName, account.getId(), folderId);
    }
    if (destFolder != null && account.getProtocol().equalsIgnoreCase(Utils.IMAP)) {
      ImapConnector connector = openIMAPConnection(userName, account, null);
      try {
        successList = connector.createMessage(msgList, destFolder);
      } catch (Exception e) {
        logger.warn("Cannot add sent message into \"" + destFolder.getName() + "\" folder on server");
      }
    } else if (account.getProtocol().equalsIgnoreCase(Utils.POP3)) {
      success = true;
    }
    
    storage_.saveMessage(userName, account.getId(), targetMsgPath, message, isNew);
    if (successList != null && successList.size() > 0) {
      success = true;
    }
    return success;
  }

  public void saveMessage(String userName, String accountId, String targetMsgPath, Message message, boolean isNew) throws Exception {
    Account account = getAccountById(userName, accountId);
    saveMessage(userName, account, targetMsgPath, message, isNew);
  }

  public List<Message> getMessagesByTag(String userName, String accountId, String tagId) throws Exception {
    MessageFilter filter = new MessageFilter("Tag");
    filter.setAccountId(accountId);
    filter.setFolder(new String[] { tagId });
    if (!Utils.isEmptyField(getDelegatorUserName(userName, accountId)))
      userName = getDelegatorUserName(userName, accountId);
    return getMessages(userName, filter);
  }

  public List<Message> getMessagesByFolder(String userName, String accountId, String folderId) throws Exception {
    MessageFilter filter = new MessageFilter("Folder");
    filter.setAccountId(accountId);
    filter.setFolder(new String[] { folderId });
    List<Message> list = getMessages(userName, filter);
    return list;
  }

  public List<Message> getMessages(String userName, MessageFilter filter) throws Exception {
    return storage_.getMessages(userName, filter);
  }

  public void saveMessage(String userName, String accountId, Message message, boolean isNew) throws Exception {
    storage_.saveMessage(userName, accountId, message, isNew);
  }

  public Message sendMessage(String userName, String accId, Message message) throws Exception {
    Account acc = getAccountById(userName, accId);
    return sendMessage(userName, acc, message);
  }
  
  /**
   * make suitable properties for SMTP session.  
   * @param acc - specified mail account 
   * @return
   */
  private Properties generateSMTPConfig(Account acc) {
    Properties props = new Properties();
    fillGeneralSMTPConfig(props, acc);
    if (Utils.STARTTLS.equalsIgnoreCase(acc.getSecureAuthsOutgoing())) {
      // STARTTLS is supported by smtp protocol.
      fillSMTPConfig4STARTTLS(props, acc, Utils.SVR_SMTP);
    } else if (Utils.TLS_SSL.equalsIgnoreCase(acc.getSecureAuthsOutgoing())) {
      // SSLTLS requires smtps protocol
      fillSMTPConfig4SSLTLS(props, acc, Utils.SVR_SMTPS);
    }
    return props;
  }
  
  private void fillGeneralSMTPConfig(Properties props, Account acc) {
    String protocol = Utils.SVR_SMTP;
    if (Utils.TLS_SSL.equalsIgnoreCase(acc.getSecureAuthsOutgoing())) {
      protocol = Utils.SVR_SMTPS;
    }
    props.put(Utils.SVR_TRANSPORT_PROTOCOL, protocol);
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_AUTH), acc.isOutgoingAuthentication());
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_USER), acc.getOutgoingUserName());
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_PASSWORD), acc.getOutgoingPassword());
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_HOST), acc.getOutgoingHost());
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_PORT), acc.getOutgoingPort());
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SMTP_AUTH_MECHS), acc.getAuthMechsOutgoing());
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SMTP_DNS_NOTIFY), "SUCCESS,FAILURE ORCPT=rfc822;" + acc.getEmailAddress());
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SMTP_DNS_RET), "FULL");
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_SOCKET_FACTORY_CLASS), Utils.SOCKET_FACTORY);
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_SOCKET_FACTORY_FALLBACK), false);
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_SOCKET_FACTORY_PORT), acc.getOutgoingPort());
  }
  
  private void fillSMTPConfig4STARTTLS(Properties props, Account acc, String protocol) {
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_STARTTLS_ENABLE), true);
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SMTP_SSL_FACTORY), getSSLSocketFactory(acc.getOutgoingHost()));
  }
  
  private void fillSMTPConfig4SSLTLS(Properties props, Account acc, String protocol) {
    props.put(Utils.SVR_TRANSPORT_PROTOCOL, protocol);
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_SSL_ENABLE), true);
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SMTP_SSL_FACTORY), getSSLSocketFactory(acc.getOutgoingHost()));
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SVR_SMTP_SSL_SOCKET_FACTORY_PORT), acc.getOutgoingPort());
    props.put(Utils.getMailConfigPropertyName(protocol, Utils.SMTP_SSL_PROTOCOLS), "SSLv3 TLSv1");
  }
  
  @Override
  public Message sendMessage(String userName, Account acc, Message message) throws Exception {
    ParameterValidation.throwIllegalArgExceptionIfNull(acc, "acc");
    ParameterValidation.throwIllegalArgExceptionIfNullOrEmpty(userName, "username", null);
    ParameterValidation.throwIllegalArgExceptionIfNull(message, "message");
    Properties props = generateSMTPConfig(acc);
    String smtpUser = acc.getIncomingUser();
    String outgoingHost = acc.getOutgoingHost();
    String outgoingPort = acc.getOutgoingPort();
    
    Session session = Session.getInstance(props, null);
    if (logger.isDebugEnabled()) 
      logger.debug(String.format(" #### Sending email with account '%1$s' ...", acc.getOutgoingUserName()));
    if (Utils.TLS_SSL.equalsIgnoreCase(acc.getSecureAuthsOutgoing())) {
      session.setProtocolForAddress("rfc822", Utils.SVR_SMTPS);
    }
    SMTPTransport transport = (SMTPTransport) session.getTransport();
    try {
      if (!acc.isOutgoingAuthentication()) {
        transport.connect();
      } else if (acc.useIncomingSettingForOutgoingAuthent()) {
        transport.connect(outgoingHost, Integer.parseInt(outgoingPort), smtpUser, acc.getIncomingPassword());
      } else {
        transport.connect(outgoingHost, Integer.parseInt(outgoingPort), acc.getOutgoingUserName(), acc.getOutgoingPassword());
      }
    } catch (Exception ex) {
      if (logger.isDebugEnabled())
        logger.debug("#### Can not connect to smtp server ...", ex);
      throw ex;
    }
    Message msg = null;
    try {
      msg = send(session, transport, message);
    } finally {
      transport.close();
    }
    return msg;
  }

  public Message sendMessage(String userName, Message message) throws Exception {
    return sendMessage(userName, message.getAccountId(), message);
  }

  public void sendMessage(Message message) throws Exception {
    List<Message> msgList = new ArrayList<Message>();
    msgList.add(message);
    sendMessages(msgList, message.getServerConfiguration());
  }
  /**
   * @Deprecated using {@link MailService#sendMessage(String, Account, Message)} instead 
   */
  @Deprecated
  public void sendMessages(List<Message> msgList, ServerConfiguration serverConfig) throws Exception {
    Properties props = System.getProperties();
    String protocolName = Utils.SVR_SMTP;
    String propSmtpPort = Utils.SVR_SMTP_SOCKET_FACTORY_PORT;
    String smtpSslProtocols = "mail.smtp.ssl.protocols";
    String smtpSsl = Utils.SVR_SMTP_SSL_ENABLE;
    String smtpAuth = Utils.SVR_SMTP_AUTH;
    String propSmtpSslSocketFactory = Utils.SMTP_SSL_FACTORY;
    boolean isSMTPAuth = serverConfig.isOutgoingAuthentication();

    if (serverConfig.isOutgoingSsl()) {
      protocolName = Utils.SVR_SMTPS;
      MailSSLSocketFactory socketFactory = this.getSSLSocketFactory(serverConfig.getOutgoingHost());
      props.put(propSmtpSslSocketFactory.replace("smtp", "smtps"), socketFactory);
      propSmtpPort = Utils.SVR_SMTP_SSL_SOCKET_FACTORY_PORT;
      props.put(smtpSsl.replace("smtp", "smtps"), true);
      props.put(smtpSslProtocols.replace("smtp", "smtps"), "SSLv3 TLSv1");
    }

    if (Utils.isGmailAccount(serverConfig.getUserName())) {
      protocolName = Utils.SVR_SMTPS;
      if (isSMTPAuth)
        props.put(smtpAuth.replace("smtp", "smtps"), true);
    }

    if (protocolName.equalsIgnoreCase(Utils.SVR_SMTP)) {
      props.put(Utils.SVR_INCOMING_USERNAME, serverConfig.getUserName());
      props.put(Utils.SVR_INCOMING_PASSWORD, serverConfig.getPassword());
      props.put(Utils.SVR_SMTP_USER, serverConfig.getUserName());
      props.put(Utils.SVR_SMTP_HOST, serverConfig.getOutgoingHost());
      props.put(Utils.SVR_SMTP_PORT, serverConfig.getOutgoingPort());
      props.put(propSmtpPort, serverConfig.getOutgoingPort());
      props.put(Utils.SVR_SMTP_SOCKET_FACTORY_FALLBACK, "false");
      if (isSMTPAuth)
        props.put(Utils.SVR_SMTP_AUTH, true);
      else
        props.put(Utils.SVR_SMTP_AUTH, false);
    } else {
      props.put(Utils.SVR_INCOMING_USERNAME, serverConfig.getUserName());
      props.put(Utils.SVR_INCOMING_PASSWORD, serverConfig.getPassword());
      props.put(Utils.SVR_SMTP_USER.replace("smtp", "smtps"), serverConfig.getUserName());
      props.put(Utils.SVR_SMTP_HOST.replace("smtp", "smtps"), serverConfig.getOutgoingHost());
      props.put(Utils.SVR_SMTP_PORT.replace("smtp", "smtps"), serverConfig.getOutgoingPort());
      props.put(propSmtpPort.replace("smtp", "smtps"), serverConfig.getOutgoingPort());
      props.put(Utils.SVR_SMTP_SOCKET_FACTORY_FALLBACK.replace("smtp", "smtps"), "false");
      if (isSMTPAuth)
        props.put(Utils.SVR_SMTP_AUTH.replace("smtp", "smtps"), true);
      else
        props.put(Utils.SVR_SMTP_AUTH.replace("smtp", "smtps"), false);
    }

    Session session = Session.getDefaultInstance(props, null);
    Transport transport = session.getTransport(protocolName);
    try {
      if (!isSMTPAuth) {
        transport.connect();
      } else {
        transport.connect(serverConfig.getOutgoingHost(), serverConfig.getUserName(), serverConfig.getPassword());
      }
    } catch (Exception e) {
      try {
        transport.connect();
      } catch (Exception ex) {
        logger.debug("#### Can not connect to smtp server ...");
        return;
      }
    }
    logger.debug(" #### Sending email ... ");
    int i = 0;
    for (Message msg : msgList) {
      msg.setServerConfiguration(serverConfig);
      try {
        send(session, transport, msg);
        i++;
      } catch (Exception e) {
        logger.error(" #### Info : send fail at message " + i + " \n", e);
        StringWriter sw = new StringWriter();
        StringBuffer sb = sw.getBuffer();
        logger.error(sb.toString());
      }
    }
    logger.debug(" #### Info : Sent " + i + " email(s)");
    transport.close();
  }

  private Message send(Session session, Transport transport, Message message) throws Exception {
    MimeMessage mimeMessage = new MimeMessage(session);
    SMTPMessage smtpMessage = null;
    String status = "";
    InternetAddress addressFrom;
    mimeMessage.setHeader("Message-ID", message.getId());
    mimeMessage.setHeader("Content-Transfer-Encoding", "utf-8");

    if (message.getFrom() != null)
      addressFrom = new InternetAddress(message.getFrom());
    else
      addressFrom = new InternetAddress(session.getProperties().getProperty(Utils.SVR_SMTP_USER));

    mimeMessage.setFrom(addressFrom);
    if (message.getMessageTo() != null)
      mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(message.getMessageTo()));

    if (message.getMessageCc() != null)
      mimeMessage.setRecipients(javax.mail.Message.RecipientType.CC, InternetAddress.parse(message.getMessageCc(), true));

    if (message.getMessageBcc() != null)
      mimeMessage.setRecipients(javax.mail.Message.RecipientType.BCC, InternetAddress.parse(message.getMessageBcc(), false));

    if (message.getReplyTo() != null)
      mimeMessage.setReplyTo(Utils.getInternetAddress(message.getReplyTo()));

    mimeMessage.setSubject(message.getSubject(), "UTF-8");
    mimeMessage.setSentDate(message.getSendDate());

    List<Attachment> attachList = message.getAttachments();
    if (attachList != null && attachList.size() > 0) {
      MimeBodyPart contentPartRoot = new MimeBodyPart();
      if (message.getContentType() != null && message.getContentType().indexOf("text/plain") > -1)
        contentPartRoot.setContent(message.getMessageBody(), "text/plain; charset=utf-8");
      else
        contentPartRoot.setContent(message.getMessageBody(), "text/html; charset=utf-8");

      MimeMultipart multipPartContent = new MimeMultipart("mixed");
      MimeBodyPart mimeBodyPart1 = new MimeBodyPart();
      mimeBodyPart1.setContent(message.getMessageBody(), message.getContentType());
      multipPartContent.addBodyPart(mimeBodyPart1);

      for (Attachment att : attachList) {
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        InputStream inputStream = att.getInputStream();
        String contentType = att.getMimeType();

        DataSource dataSource = new ByteArrayDataSource(inputStream, contentType);
        mimeBodyPart.setDataHandler(new DataHandler(dataSource));
        mimeBodyPart.setDisposition(Part.ATTACHMENT);
        String fileName = att.getName();
        if (fileName != null && fileName.length() > 0) {
          mimeBodyPart.setFileName(MimeUtility.encodeText(fileName, "utf-8", null));
        }
        multipPartContent.addBodyPart(mimeBodyPart);

      }
      mimeMessage.setContent(multipPartContent);
    } else {
      if (message.getContentType() != null && message.getContentType().indexOf("text/plain") > -1)
        mimeMessage.setContent(message.getMessageBody(), "text/plain; charset=utf-8");
      else
        mimeMessage.setContent(message.getMessageBody(), "text/html; charset=utf-8");
    }

    if (message.isReturnReceipt()) {
      mimeMessage.setHeader("Disposition-Notification-To", message.getReplyTo());
    }
    mimeMessage = Utils.setHeader(mimeMessage, message);
    try {
      smtpMessage = new SMTPMessage(mimeMessage);
      smtpMessage.setNotifyOptions(SMTPMessage.NOTIFY_FAILURE);// need improve to custom in form compose new mail.
      smtpMessage.saveChanges();
    } catch (Exception ex) {
    }
    try {
      transport.sendMessage(smtpMessage, smtpMessage.getAllRecipients());

      message.setId(MimeMessageParser.getMessageId(smtpMessage));
      Enumeration enu = smtpMessage.getAllHeaders();
      while (enu.hasMoreElements()) {
        Header header = (Header) enu.nextElement();
        message.setHeader(header.getName(), header.getValue());
      }
      status = "Mail Delivered !";
    } catch (AddressException e) {
      status = "There was an error parsing the addresses. Sending Failed !" + e.getMessage();
      throw e;
    } catch (AuthenticationFailedException e) {
      status = "The Username or Password may be wrong. Sending Failed !" + e.getMessage();
      throw e;
    } catch (SMTPSendFailedException e) {
      status = "Sorry, There was an error sending the message. Sending Failed !" + e.getMessage();
      throw e;
    } catch (MessagingException e) {
      status = "There was an unexpected error. Sending Failed ! " + e.getMessage();
      throw e;
    } catch (Exception e) {
      status = "There was an unexpected error. Sending Falied !" + e.getMessage();
      throw e;
    } finally {
      logger.debug(" #### Info : " + status);
    }
    logger.debug(" #### Info : " + status);

    return message;
  }

  public void checkMail(String userName, String accountId) throws Exception {
    Account account = getAccountById(userName, accountId);
    loadCheckmailJob(userName, account);
  }

  public void checkMail(String userName, String accountId, String folderId) throws Exception {
    if (Utils.isEmptyField(folderId))
      checkMail(userName, accountId);
    else {
      Account account = getAccountById(userName, accountId);
      Folder folder = getFolderById(userName, accountId, folderId);
      loadCheckmailJob(userName, account, folder);
    }
  }

  private void executeJob(JobDetail job) {
    if (job != null) {
      try {
        schedulerService_.executeJob(job.getName(), job.getGroup(), job.getJobDataMap());
      } catch (Exception e) {
        logger.error("Failed to execute job " + job.getName(), e);
      }
    }
  }

  public void stopCheckMail(String userName, String accountId) {
    String accoutOwner = userName;
    try {
      Account delegateAcc = getDelegatedAccount(userName, accountId);
      if (isDelegatedAccount(userName, accountId))
        accoutOwner = delegateAcc.getDelegateFrom();
    } catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Exception in method stopCheckMail", e);
      }
    }

    CheckingInfo checkingInfo = getCheckingInfo(accoutOwner, accountId);
    if (checkingInfo != null) {
      if (logger.isDebugEnabled())
        logger.info(" user [ " + userName + " ] request to stop checking emails");
      checkingInfo.setRequestStop(true);
      checkingInfo.setStatusCode(CheckingInfo.FINISHED_CHECKMAIL_STATUS);
      updateCheckingMailStatusByCometd(userName, accountId, checkingInfo);

    }
  }

  public void stopAllJobs(String userName, String accountId) throws Exception {
    JobInfo info = CheckMailJob.getJobInfo(userName, accountId);
    stopCheckMail(userName, accountId);
    schedulerService_.removeJob(info);
  }

  private JobDetail loadCheckmailJob(String userName, Account account) throws Exception {
    return loadCheckmailJob(userName, account, null);
  }

  /**
   * Load or register the CheckMailJob against scheduler
   * 
   * @return
   * @throws Exception
   */
  private JobDetail loadCheckmailJob(String userName, Account account, Folder folder) throws Exception {
    String accountId = account.getId();
    JobInfo info = CheckMailJob.getJobInfo(userName, accountId);
    JobDetail job = findCheckmailJob(userName, accountId);
    JobDataMap jobData = new JobDataMap();
    jobData.put(CheckMailJob.REPO_NAME, repositoryService.getCurrentRepository().getConfiguration().getName());
    jobData.put(CheckMailJob.USERNAME, userName);
    jobData.put(CheckMailJob.ACCOUNTID, accountId);
    
    if (folder != null) {
      jobData.put(CheckMailJob.FOLDERID, folder.getId()); 
    }
    
    if (job == null) {
      PeriodInfo periodInfo = new PeriodInfo(null, null, 0, 24 * 60 * 60 * 1000);
      schedulerService_.addPeriodJob(info, periodInfo, jobData);
    } else {
      JobDetail activeJob = findActiveCheckmailJob(userName, accountId);
      job.setJobDataMap(jobData);
      if (activeJob == null) {
        executeJob(job);
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("job [ " + job.getName() + " ] still exist. Can not start new job!");
        }
        job = new JobDetail(info.getJobName(), info.getGroupName(), info.getJob());
      }
    }
    return job;
  }

  private JobDetail findActiveCheckmailJob(String userName, String accountId) throws Exception {
    // / Need to upgrade to 2.0.3 and use this instead :
    // schedulerService_.getJob(info)
    List list = schedulerService_.getAllExcutingJobs();
    for (Object obj : list) {
      JobExecutionContext jec = (JobExecutionContext) obj;
      JobDetail tmp = jec.getJobDetail();
      if (tmp.getName().equals(userName + ":" + accountId)) {
        return tmp;
      }
    }
    return null;
  }

  private JobDetail findCheckmailJob(String userName, String accountId) throws Exception {
    // / Need to upgrade to 2.0.3 and use this instead :
    // schedulerService_.getJob(info)
    List<Object> list = schedulerService_.getAllJobs();
    for (Object obj : list) {
      JobDetail tmp = (JobDetail) obj;
      if (tmp.getName().equals(userName + ":" + accountId)) {
        return tmp;
      }
    }
    return null;
  }

  private LinkedHashMap<javax.mail.Message, List<String>> getMessages(LinkedHashMap<javax.mail.Message, List<String>> msgMap, javax.mail.Folder folder, boolean isImap, Date fromDate, Date toDate, List<MessageFilter> filters) throws Exception {
    javax.mail.Message[] messages;
    SearchTerm searchTerm = null;

    if (fromDate != null && toDate != null && fromDate.equals(toDate))
      return msgMap;

    if (!folder.isOpen())
      folder.open(javax.mail.Folder.READ_WRITE);
    if (fromDate == null && toDate == null) {
      messages = folder.getMessages();
    } else {
      searchTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
      SearchTerm dateTerm = null;
      if (fromDate != null) {
        dateTerm = new SentDateTerm(ComparisonTerm.GT, fromDate);
      }
      if (toDate != null) {
        if (dateTerm != null) {
          dateTerm = new AndTerm(dateTerm, new SentDateTerm(ComparisonTerm.LE, toDate));
        } else {
          dateTerm = new SentDateTerm(ComparisonTerm.LE, toDate);
        }
      }
      if (!isImap)
        searchTerm = new OrTerm(searchTerm, dateTerm);
      else
        searchTerm = dateTerm;
      messages = folder.search(searchTerm);
    }

    boolean beforeTime = false;
    boolean betweenTime = false;
    List<String> filterList;
    int filteredMsgNumber = 0;
    int getFrom = 0;

    SearchTerm st;
    javax.mail.Message[] filteredMsg;
    javax.mail.Message msg;

    for (MessageFilter filter : filters) {
      beforeTime = false;
      betweenTime = false;
      st = getSearchTerm(searchTerm, filter);
      filteredMsg = folder.search(st);
      filteredMsgNumber = filteredMsg.length;

      if (filteredMsgNumber > 0) {
        getFrom = 0;
        if (fromDate != null) {
          for (int k = 0; k < filteredMsgNumber; k++) {
            if (MimeMessageParser.getReceivedDate(filteredMsg[k]).getTime().before(fromDate)) {
              getFrom++;
            } else {
              break;
            }
          }
        }

        for (int k = filteredMsgNumber - 1; k >= 0; k--) {
          msg = filteredMsg[k];
          if (msgMap.containsKey(msg)) {
            filterList = msgMap.get(msg);
          } else {
            filterList = new ArrayList<String>();
          }

          if (filterList == null)
            filterList = new ArrayList<String>();

          if (!filterList.contains(filter.getId()))
            filterList.add(filter.getId());

          if (fromDate != null && toDate != null) {
            if (betweenTime || (!(isImap && !MimeMessageParser.getReceivedDate(msg).getTime().before(toDate)))) {
              betweenTime = true;
              if (!(isImap && !(k > getFrom))) {
                msgMap.put(msg, filterList);
              } else {
                betweenTime = false;
              }
            }
          } else if (fromDate != null) {
            if (!(isImap && !(k > getFrom))) {
              msgMap.put(msg, filterList);
            }
          } else if (toDate != null) {
            if (beforeTime || !(isImap && !MimeMessageParser.getReceivedDate(msg).getTime().before(toDate))) {
              beforeTime = true;
              msgMap.put(msg, filterList);
            }
          } else {
            msgMap.put(msg, filterList);
          }
        }
      }
    }

    if (messages.length > 0) {
      beforeTime = false;
      betweenTime = false;
      getFrom = 0;
      Date receivedDate;
      if (fromDate != null) {
        for (int l = 0; l < messages.length; l++) {
          receivedDate = MimeMessageParser.getReceivedDate(messages[l]).getTime();
          if (receivedDate.before(fromDate) || receivedDate.equals(fromDate)) {
            getFrom++;
          } else {
            break;
          }
        }
      }

      for (int l = messages.length; l > 0; l--) {
        msg = messages[l - 1];
        if (!msgMap.containsKey(msg)) {
          if (fromDate != null && toDate != null) {
            if (betweenTime || (!(isImap && !MimeMessageParser.getReceivedDate(msg).getTime().before(toDate)))) {
              betweenTime = true;
              if (!(isImap && !(l > getFrom))) {
                msgMap.put(msg, null);
              } else {
                betweenTime = false;
              }
            }
          } else if (fromDate != null) {
            if (!(isImap && !(l > getFrom))) {
              msgMap.put(msg, null);
            }
          } else if (toDate != null) {
            if (beforeTime || !(isImap && !MimeMessageParser.getReceivedDate(msg).getTime().before(toDate))) {
              beforeTime = true;
              msgMap.put(msg, null);
            }
          } else {
            msgMap.put(msg, null);
          }
        } else {
          List<String> temp = msgMap.get(msg);
          msgMap.remove(msg);
          msgMap.put(msg, temp);
        }
      }
    }
    return msgMap;
  }

  public void synchImapFolders(String userName, String accountId) throws Exception {
    CheckingInfo info = getCheckingInfo(userName, accountId);
    try {
      Account dAccount = getDelegatedAccount(userName, accountId);
      String uId = userName;
      if (isDelegatedAccount(userName, accountId)) {
        uId = dAccount.getDelegateFrom();
      }
      Account account = getAccountById(uId, accountId);
      
      ImapConnector connector = openIMAPConnection(uId, account, info);
      if (connector != null) {
        String key = userName + ":" + accountId;
        if (info == null) {
          info = new CheckingInfo();
          checkingLog_.put(key, info);
        }
        synchImapFolders(uId, account, null, connector.getDefaultFolder().list());
        info.setSyncFolderStatus(CheckingInfo.FINISH_SYNC_FOLDER);
      }
    } catch (Exception e) {
      if (info != null) {
        info.setSyncFolderStatus(CheckingInfo.FINISH_SYNC_FOLDER);
      }
      
      if (logger.isDebugEnabled()) {
        logger.debug("Exception in method synchImapFolders", e);
      }
    } finally {
      if (info != null) {
        info.setSyncFolderStatus(CheckingInfo.FINISH_SYNC_FOLDER);
      }
    }
  }

  private List<javax.mail.Folder> synchImapFolders(String userName, Account account, Folder parentFolder, javax.mail.Folder[] folders) throws Exception {
    List<javax.mail.Folder> folderList = new ArrayList<javax.mail.Folder>();
    List<String> serverFolderId = new ArrayList<String>();
    String folderId, folderName;
    String accountId = account.getId();

    for (javax.mail.Folder fd : folders) {
      folderName = fd.getName();
      if (parentFolder == null && (folderName.equalsIgnoreCase(Utils.FD_DRAFTS) || folderName.equalsIgnoreCase(Utils.FD_SENT) || folderName.equalsIgnoreCase(Utils.FD_SPAM)) || folderName.equalsIgnoreCase(Utils.FD_TRASH)) {
        continue;
      }

      int folderType = fd.getType();
      if (!folderName.equalsIgnoreCase(Utils.FD_INBOX)) {
        if (folderType != javax.mail.Folder.HOLDS_FOLDERS) {
          folderId = Utils.generateFID(accountId, String.valueOf(((IMAPFolder) fd).getUIDValidity()), true);
        } else {
          folderId = Utils.escapeIllegalJcrChars(folderName);
        }

        serverFolderId.add(folderId);
        Folder folder = storage_.getFolderById(userName, accountId, folderId);

        if (folder == null) {
          folder = new Folder();
          folder.setId(folderId);
          folder.setName(folderName);
          folder.setURLName(fd.getURLName().toString());
          folder.setNumberOfUnreadMessage(0);
          folder.setTotalMessage(0);
          folder.setPersonalFolder(true);
          folder.setType(folderType);
          try {
            if (parentFolder == null) {
              storage_.saveFolder(userName, accountId, folder);
            } else {
              storage_.saveFolder(userName, accountId, parentFolder.getId(), folder);
            }
            folder = storage_.getFolderById(userName, accountId, folderId);
          } catch (Exception e) {
            logger.warn(e);
          }

          // update available one
        } else {
          if (!folder.getName().equalsIgnoreCase(folderName)) {
            folder.setName(folderName);
            folder.setURLName(fd.getURLName().toString());
            saveFolder(userName, account, folder, false);
          }
        }

        folderList.add(fd);
        if ((folderType == 2) || (fd.list().length > 0)) {
          folderList.addAll(synchImapFolders(userName, account, folder, fd.list()));
        }
      } else {
        Folder inbox = getFolderById(userName, accountId, Utils.generateFID(accountId, Utils.FD_INBOX, false));
        inbox.setNumberOfUnreadMessage(fd.getNewMessageCount());
        inbox.setURLName(fd.getURLName().toString());
        saveFolder(userName, account, inbox, false);
      }
    }

    List<Folder> localFolders = null;
    if (parentFolder == null) {
      localFolders = getFolders(userName, accountId, true);
    } else {
      localFolders = getSubFolders(userName, accountId, parentFolder.getPath());
    }
    if (localFolders != null) {
      for (Folder f : localFolders) {
        if (!serverFolderId.contains(f.getId())) {
          deleteLocalFolder(userName, account, f);
        }
      }
    }
    return folderList;
  }

  private MailSSLSocketFactory getSSLSocketFactory(String host) {
    MailSSLSocketFactory sslsocket = null;
    try {
      sslsocket = new MailSSLSocketFactory();// default protocol is TLS
      sslsocket.setTrustedHosts(new String[] { host });
      TrustManager[] trusts = new TrustManager[] { new ExoMailTrustManager(null, false, host, null) };
      sslsocket.setTrustManagers(trusts);
    } catch (GeneralSecurityException gse) {
      logger.error("Imap SSL: Cannot create a ssl socket between client and server. All host will trusted.", gse);
      sslsocket = trustAllHost();
    } catch (Exception e) {
      logger.error("Your email was not trusted by Mail server", e);
    }
    return sslsocket;
  }

  private MailSSLSocketFactory trustAllHost() {
    MailSSLSocketFactory sslsocket = null;
    try {
      sslsocket = new MailSSLSocketFactory();
      sslsocket.setTrustAllHosts(true);
      sslsocket.setTrustManagers(new TrustManager[] { new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
          // do nothing
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
      } });

    } catch (Exception e) {
      logger.warn("Your sun cacerts file may be broken. Pls check it at " + ExoMailTrustManager.PATH_CERTS_FILE + "/cacerts", e);
    }
    return sslsocket;
  }

  private ImapConnector openIMAPConnection(String userName, Account account, CheckingInfo info) {
    Map<String, Object> connectionCache = connectionStore.get(userName);
    if (connectionCache == null) {
      connectionCache = new HashMap<String, Object>();
      connectionStore.put(userName, connectionCache);
    }
    
    String key = account.getId() + "_" + account.getProtocol();
    ImapConnector imapConnector = (ImapConnector) connectionCache.get(key);
    
    if (imapConnector == null || !imapConnector.isConnected()) {
      imapConnector = getIMAPConnection(userName, account, info);
      if (imapConnector != null) {
        connectionCache.put(key, imapConnector);
      }
    }
    return imapConnector;
  }

  private ImapConnector getIMAPConnection(String userName, Account account, CheckingInfo info) {
    try {
      if (logger.isDebugEnabled())  {
        logger.debug(" #### Opening IMap connection to " + account.getIncomingHost() + " ... !");
      }
      
      MailSSLSocketFactory sslSocket = null;
      if (account.isIncomingSsl()) {
        sslSocket = this.getSSLSocketFactory(account.getIncomingHost());
      }
      
      try {
        return new ImapConnector(account, sslSocket);
      } catch (AuthenticationFailedException e) {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Authentication failed with email '%s'", account.getIncomingUser()), e);
        }
        
        if (!account.isSavePassword()) {
          account.setIncomingPassword("");
          updateAccount(userName, account);
        }
        
        if (info != null) {
          info.setStatusCode(CheckingInfo.RETRY_PASSWORD);
          updateCheckingMailStatusByCometd(userName, account.getId(), info);
        }
      } catch (MessagingException e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Exception while connecting to server : " + e.getMessage());
        }
        
        if (info != null) {
          info.setStatusCode(CheckingInfo.CONNECTION_FAILURE);
          updateCheckingMailStatusByCometd(userName, account.getId(), info);
        }
      } catch (IllegalStateException e) {
        logger.error("Exception while connecting to server", e);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Exception while connecting to server : ", e);
        }
        
        StringWriter sw = new StringWriter();
        StringBuffer sb = sw.getBuffer();
        logger.error(sb.toString());
        if (info != null) {
          info.setStatusCode(CheckingInfo.CONNECTION_FAILURE);
          updateCheckingMailStatusByCometd(userName, account.getId(), info);
        }
      }
    } catch (Exception ex) {
      logger.error("Exception while connecting to server", ex);
    }
    return null;
  }

  private Pop3Connector openPOPConnection(String userName, Account account, CheckingInfo info) {
    Map<String, Object> connectionCache = connectionStore.get(userName);
    if (connectionCache == null) {
      connectionCache = new HashMap<String, Object>();
      connectionStore.put(userName, connectionCache);
    }
    
    String key = account.getId() + "_" + account.getProtocol();
    Pop3Connector pop3Connector = (Pop3Connector) connectionCache.get(key);
    
    if (pop3Connector == null || !pop3Connector.isConnected()) {
      pop3Connector = getPOPConnection(userName, account, info);
      if (pop3Connector != null) {
        connectionCache.put(key, pop3Connector);
      }
    }
    
    return pop3Connector;
  }
  
  private Pop3Connector getPOPConnection(String userName, Account account, CheckingInfo info) {
    try {
      logger.debug(" #### Opening IMap connection to " + account.getIncomingHost() + " ... !");
      if (info != null) {
        info.setStatusCode(CheckingInfo.START_CHECKMAIL_STATUS);
        updateCheckingMailStatusByCometd(userName, account.getId(), info);
      }
      
      MailSSLSocketFactory socketFactory = null;
      if (account.isIncomingSsl()) {
        try {
          socketFactory = this.getSSLSocketFactory(account.getIncomingHost());
        } catch (Exception e) {
          logger.error("Pop3 SSL: Cannot create a ssl socket between client and server. All host will trusted.");
          socketFactory = trustAllHost();
        }
      }
      
      try {
        return new Pop3Connector(account, socketFactory);
      } catch (AuthenticationFailedException e) {
        if (!account.isSavePassword()) {
          account.setIncomingPassword("");
          updateAccount(userName, account);
          logger.debug("Exception while connecting to server", e);
        }
        if (info != null) {
          info.setStatusCode(CheckingInfo.RETRY_PASSWORD);
          updateCheckingMailStatusByCometd(userName, account.getId(), info);
        }
      } catch (MessagingException e) {
        logger.debug("Exception while connecting to server", e);
        if (info != null) {
          info.setStatusCode(CheckingInfo.CONNECTION_FAILURE);
          updateCheckingMailStatusByCometd(userName, account.getId(), info);
        }
      } catch (Exception e) {
        logger.debug("Exception while connecting to server", e);
        if (info != null) {
          info.setStatusCode(CheckingInfo.CONNECTION_FAILURE);
          updateCheckingMailStatusByCometd(userName, account.getId(), info);
        }
      }
    } catch (Exception ex) {
      logger.debug("Exception while connecting to server", ex);
    }
    return null;
  }

  private void mergeMessageBetweenJcrAndServerMail(ImapConnector connector, String userName, Account account, Folder folder, CheckingInfo info) throws Exception {
    if (folder == null || Utils.isEmptyField(folder.getURLName())) {
      folder = getFolderById(userName, account.getId(), Utils.generateFID(account.getId(), Utils.FD_INBOX, false));
    }
    
    javax.mail.Folder mailServerFolder = connector.openFolderForReadWrite(folder.getURLName());
    if (mailServerFolder != null) {
      synchImapMessage(userName, account, mailServerFolder, info);
    }
  }

  private CheckingInfo createCheckingInfo(String userName, String accountId) {
    String key = userName + ":" + accountId;
    CheckingInfo info = new CheckingInfo();
    checkingLog_.put(key, info);
    return info;
  }
  
  private void getSynchnizeImapServer(String userName, Account account, Folder folder, boolean synchFolders) throws Exception {
    String accountId = account.getId();
    CheckingInfo info = getCheckingInfo(userName, accountId);
    if (info == null) {
      info = createCheckingInfo(userName, accountId);
    } else {
      // reset CheckingInfo to default
      info.resetValues();
    }
    info.setAccountId(accountId);
    info.setStatusCode(CheckingInfo.START_CHECKMAIL_STATUS);

    updateCheckingMailStatusByCometd(userName, accountId, info);
    
    // Get store
    ImapConnector connector = openIMAPConnection(userName, account, info);
    
    // after connect to server, we check stopping mail request of user.
    if (info.isRequestStop()) {
      throw new CheckMailInteruptedException("stopped checking emails!");
    }
    
    if (connector != null) {
      List<javax.mail.Folder> folderList = null;
      if (synchFolders) {
        info.setSyncFolderStatus(CheckingInfo.START_SYNC_FOLDER);
        info.setStatusCode(CheckingInfo.START_SYNC_FOLDER);
        updateCheckingMailStatusByCometd(userName, accountId, info); // update checking mail job by cometd.
        folderList = synchImapFolders(userName, account, null, connector.getDefaultFolder().list());
        info.setSyncFolderStatus(CheckingInfo.FINISH_SYNC_FOLDER);
        info.setStatusCode(CheckingInfo.FINISH_SYNC_FOLDER);
        updateCheckingMailStatusByCometd(userName, accountId, info);
      }

      if (info.isRequestStop()) {
        throw new CheckMailInteruptedException("stopped checking emails!");
      }
      
//      if (folder == null) {
//        if (folderList != null && folderList.size() > 0) {
//          for (javax.mail.Folder folder1 : folderList) {
//            if (!Utils.isEmptyField(info.getRequestingForFolder_()) && !info.getRequestingForFolder_().equals("checkall")) {
//              break;
//            }
//            if (info != null && info.isRequestStop()) {
//              if (logger.isDebugEnabled()) {
//                logger.debug("Stop requested on checkmail for " + account.getId());
//              }
//              throw new CheckMailInteruptedException("Stop getting mails from folder " + folder1.getName() + " !");
//            }
//            synchImapMessage(userName, account, folder1, info);
//          }
//        }
//      }
      mergeMessageBetweenJcrAndServerMail(connector, userName, account, folder, info);

      logger.debug("/////////////////////////////////////////////////////////////");
      logger.debug("/////////////////////////////////////////////////////////////");
    } else {
      return;
    }

    // after release all resource, we send finished status to client to announce that the job has finished.
    // Note: we check my status with finished status to ensure that my status has been set before or not.
    // If it hasn't been set yet, we will create a new finish message.
    if (info != null) {
      if (info.getStatusCode() != CheckingInfo.FINISHED_CHECKMAIL_STATUS) {
        info.setStatusCode(CheckingInfo.FINISHED_CHECKMAIL_STATUS);
      }
      updateCheckingMailStatusByCometd(userName, accountId, info);
    }
  }
  
  private List<String> getListOfMessageIdsInFolder(String username, String accountId, String folderId, Calendar fromDate, Calendar toDate) throws Exception {
    MessageFilter filter = new MessageFilter("Folder");
    filter.setAccountId(accountId);
    filter.setFolder(new String[] { folderId });
    if (fromDate != null) 
      filter.setFromDate(fromDate);
    if (toDate != null) 
      filter.setToDate(toDate);
    return storage_.getListOfMessageIds(username, filter);
  }
  
  private void synchImapMessage(String username, Account account, javax.mail.Folder folder, CheckingInfo info) throws Exception {
    String accountId = account.getId();
    boolean saved = false;
    int totalNew = -1;
    if (folder == null) {
      return;
    }
    
    String folderId = null;
    String folderName = folder.getName();
    
    try {
      if (!folder.isOpen()) {
        folder.open(javax.mail.Folder.READ_ONLY);
      }
    } catch (MessagingException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Can not open folder: " + folder.getName(), ex);
      }
      return;
    }
    
    if (info != null) {
      info.setStatusCode(CheckingInfo.DOWNLOADING_MAIL_STATUS);
      updateCheckingMailStatusByCometd(username, accountId, info);
    }
    
    folderId = Utils.generateFID(accountId, String.valueOf(((IMAPFolder) folder).getUIDValidity()), true);
    String[] localFolders = Utils.DEFAULT_FOLDERS;
    for (String localFolder : localFolders) {
      if (localFolder.equalsIgnoreCase(folderName)) {
        folderId = Utils.generateFID(accountId, localFolder, false);
      }
    }
    
    Folder eXoFolder = getFolderById(username, accountId, folderId);
    if (eXoFolder != null) {
      long unreadMsgCount = eXoFolder.getNumberOfUnreadMessage();
      Date checkFromDate = eXoFolder.getCheckFromDate();

      if (account.getCheckFromDate() == null) {
        checkFromDate = null;
      } else if ((checkFromDate == null) || checkFromDate.before(account.getCheckFromDate())) {
        checkFromDate = account.getCheckFromDate();
      }

      boolean isImap = account.getProtocol().equals(Utils.IMAP);
      boolean leaveOnserver = isImap && Boolean.valueOf(account.getServerProperties().get(Utils.SVR_LEAVE_ON_SERVER));
      
      // after check folder, we see the stopping request of user.
      if ((info != null) && info.isRequestStop()) {
        throw new CheckMailInteruptedException("stopped checking emails!");
      }

      LinkedHashMap<javax.mail.Message, List<String>> msgMap = getMessageMap(username, account, folder, null, checkFromDate, null);
      // after get messages map, we see the stopping request of user.
      if ((info != null) && info.isRequestStop()) {
        throw new CheckMailInteruptedException("stopped checking emails!");
      }
      
      // get list of messages in folder
      Calendar c = null;
      if (checkFromDate != null) {
        c = Calendar.getInstance();
        c.setTime(checkFromDate);
      }
      HashSet<String> savedMsgList = new HashSet<String>(getListOfMessageIdsInFolder(username, accountId, folderId, c , null));
      totalNew = msgMap.size();

      logger.debug(" #### Folder " + folderName + " contains " + totalNew + " messages !");
      if (totalNew > 0) {
        int i = 0;
        javax.mail.Message msg;

        Date lastFromDate = null, receivedDate = null;
        List<javax.mail.Message> msgList = new ArrayList<javax.mail.Message>(msgMap.keySet());
        
        if (info != null) {
          info.setStatusCode(CheckingInfo.DOWNLOADING_MAIL_STATUS);
          updateCheckingMailStatusByCometd(username, accountId, info);
        }
        
        while (i < totalNew) {
          if ((info != null) && info.isRequestStop()) {
            if (logger.isDebugEnabled()) {
              logger.debug("Stop requested on checkmail for " + account.getId());
            }
            throw new CheckMailInteruptedException("Stop getting mails from folder " + folder.getName() + " !");
          } 
          
          if ((info != null) && !Utils.isEmptyField(info.getRequestingForFolder_()) && !String.valueOf(((IMAPFolder) folder).getUIDValidity()).equals(Utils.getFolderNameFromFolderId(info.getRequestingForFolder_()))) {
            break;
          }
          
          msg = msgList.get(i);
          MimeMessage mimeMessage = (MimeMessage) msg;
          String msgId = mimeMessage.getMessageID();
          try {
            if (savedMsgList.contains(msgId)) {
              // if the message has been saved to db, remove it from list and ignore.
              savedMsgList.remove(msgId);
              i++;
              continue;
            }
            
            saved = saveMessage(true, folderId, msgMap, username, accountId, msg, folder);
            if (saved) {
              if (!leaveOnserver) {
                msg.setFlag(Flags.Flag.DELETED, true);
              }
              if (!msg.isSet(Flag.SEEN)) {
                unreadMsgCount++;
              }
            }

            receivedDate = MimeMessageParser.getReceivedDate(msg).getTime();

            if (i == 0) {
              lastFromDate = receivedDate;
            }
            eXoFolder.setLastCheckedDate(receivedDate);
            if ((i == (totalNew - 1))) {
              eXoFolder.setCheckFromDate(lastFromDate);
            }

            if ((lastFromDate != null) && (eXoFolder.getLastStartCheckingTime() == null || eXoFolder.getLastStartCheckingTime().before(lastFromDate))) {
              eXoFolder.setLastStartCheckingTime(lastFromDate);
            }
          } catch (Exception e) {
            if (logger.isDebugEnabled()) {
              logger.debug("Exception in method synchImapMessage", e);
            }
          }
          i++;
        }
        
        eXoFolder.setNumberOfUnreadMessage(unreadMsgCount - savedMsgList.size());
        saveFolder(username, account, eXoFolder, false);
        
        // Remove all message that not exist in server IMAP
        if (savedMsgList.size() > 0) {
          Iterator<String> messageIdIterator = savedMsgList.iterator();
          while (messageIdIterator.hasNext()) {
            Message message = storage_.getMessageById(username, accountId, messageIdIterator.next());
            storage_.removeMessage(username, accountId, message);
          }
        }
      }
    }
  }

  private LinkedHashMap<javax.mail.Message, List<String>> getMessageMap(String userName, Account account, javax.mail.Folder folder, Date lastCheckedDate, Date checkFromDate, Date lastCheckedFromDate) throws Exception {
    LinkedHashMap<javax.mail.Message, List<String>> msgMap = new LinkedHashMap<javax.mail.Message, List<String>>();
    String accountId = account.getId();
    List<MessageFilter> filters = getFilters(userName, accountId);
    boolean isImap = account.getProtocol().equals(Utils.IMAP) ? true : false;
    if (checkFromDate == null) {
      if (lastCheckedDate != null && lastCheckedFromDate != null) {
        msgMap = getMessages(msgMap, folder, isImap, lastCheckedFromDate, null, filters);
        msgMap = getMessages(msgMap, folder, isImap, null, lastCheckedDate, filters);
      } else if (lastCheckedFromDate != null) {
        msgMap = getMessages(msgMap, folder, isImap, lastCheckedFromDate, null, filters);
      } else if (lastCheckedDate != null) {
        msgMap = getMessages(msgMap, folder, isImap, null, lastCheckedDate, filters);
      } else {
        msgMap = getMessages(msgMap, folder, isImap, null, null, filters);
      }
    } else {
      if (lastCheckedDate != null && lastCheckedFromDate != null) {
        msgMap = getMessages(msgMap, folder, isImap, lastCheckedFromDate, null, filters);
        msgMap = getMessages(msgMap, folder, isImap, checkFromDate, lastCheckedDate, filters);
      } else if (lastCheckedFromDate != null) {
        msgMap = getMessages(msgMap, folder, isImap, lastCheckedFromDate, null, filters);
      } else if (lastCheckedDate != null && lastCheckedDate.after(checkFromDate)) {
        msgMap = getMessages(msgMap, folder, isImap, checkFromDate, lastCheckedDate, filters);
      } else {
        msgMap = getMessages(msgMap, folder, isImap, checkFromDate, null, filters);
      }
    }
    return msgMap;
  }

  public void checkNewMessage(String userName, String accountId) throws Exception {
    checkNewMessage(userName, accountId, null);
  }

  public void checkNewMessage(String username, String accountId, String folderId) throws Exception {
    String reciever = username;
    Account dAccount = getDelegatedAccount(username, accountId);
    if (isDelegatedAccount(username, accountId)) {
      reciever = dAccount.getDelegateFrom();
    }
    Account account = getAccountById(reciever, accountId);
    
    if (account != null) {
      try {
        if (account.getProtocol().equals(Utils.POP3)) {
          checkPop3Server(reciever, account);
        } else if (account.getProtocol().equals(Utils.IMAP)) {
          if (reciever != null) {
            Folder folder = Utils.isEmptyField(folderId) ? null : getFolderById(username, accountId, folderId);
            boolean isSyncFolder = getFolders(username, accountId, true).size() == 0;
            getSynchnizeImapServer(reciever, account, folder, isSyncFolder);
          }
        }
      } catch (CheckMailInteruptedException cme) {
        CheckingInfo info = getCheckingInfo(username, accountId);
        info.setRequestStop(false);
        info.assignInterruptedStatus();
        updateCheckingMailStatusByCometd(username, accountId, info);
      } catch (MessagingException e) {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Error when getting new messages for user '%s' account '%s', folderid '%s'", reciever, accountId, folderId), e);
        }
        CheckingInfo info = getCheckingInfo(username, accountId);
        if (info != null) {
          info.setStatusCode(CheckingInfo.CONNECTION_FAILURE);
          updateCheckingMailStatusByCometd(username, accountId, info);
        }
      } finally {
        if (!account.isSavePassword()) {
          account.setIncomingPassword("");
          if (reciever != null) {
            updateAccount(reciever, account);
          } else {
            updateAccount(username, account);
          }
        }
      }
    }
  }

  private void checkPop3Server(String userName, Account account) throws Exception {
    String accountId = account.getId();
    if (account != null) {
      CheckingInfo info = null;
      info = getCheckingInfo(userName, accountId);
      if (info == null) {
        info = createCheckingInfo(userName, accountId);
      } else {
        // reset CheckingInfo to default
        info.resetValues();
      }

      info.setAccountId(accountId);

      long t1, t2, tt1, tt2;
      if (Utils.isEmptyField(account.getIncomingPassword())) {
        info.setStatusCode(CheckingInfo.RETRY_PASSWORD);
        updateCheckingMailStatusByCometd(userName, accountId, info);
        return;
      }
      logger.debug(" #### Getting mail from " + account.getIncomingHost() + " ... !");
      info.setStatusCode(CheckingInfo.START_CHECKMAIL_STATUS);
      updateCheckingMailStatusByCometd(userName, accountId, info);
      int totalNew = 0;

      String incomingFolder = account.getIncomingFolder().trim();
      URLName storeURL = new URLName(account.getProtocol(), account.getIncomingHost(), Integer.valueOf(account.getIncomingPort()), incomingFolder, account.getIncomingUser(), account.getIncomingPassword());

      Pop3Connector connector = openPOPConnection(userName, account, info);
      if (connector == null) {
        return;
      }

      if (info.isRequestStop()) {
        throw new CheckMailInteruptedException("stopped checking emails");
      }

      javax.mail.Folder folder = connector.getFolder(storeURL.getFile());

      // after get folder, check stopping request of user.
      if (info.isRequestStop()) {
        throw new CheckMailInteruptedException("stopped checking emails");
      }

      try {
        if (!folder.exists()) {
          logger.debug(" #### Folder " + incomingFolder + " is not exists !");
          info.setStatusCode(CheckingInfo.COMMON_ERROR);

          updateCheckingMailStatusByCometd(userName, accountId, info);
          return;
        } else {
          logger.debug(" #### Getting mails from folder " + incomingFolder + " !");
          info.setStatusCode(CheckingInfo.DOWNLOADING_MAIL_STATUS);
          updateCheckingMailStatusByCometd(userName, accountId, info);
        }
        folder.open(javax.mail.Folder.READ_WRITE);

        Date lastCheckedDate = account.getLastCheckedDate();
        Date lastCheckedFromDate = account.getLastStartCheckingTime();
        Date checkFromDate = account.getCheckFromDate();
        LinkedHashMap<javax.mail.Message, List<String>> msgMap = getMessageMap(userName, account, folder, lastCheckedDate, checkFromDate, lastCheckedFromDate);
        
        // after get message map, check stopping request of user.
        if (info.isRequestStop()) {
          throw new CheckMailInteruptedException("stopped checking emails");
        }

        totalNew = msgMap.size();

        logger.debug("=============================================================");
        logger.debug("=============================================================");
        logger.debug(" #### Folder contains " + totalNew + " messages !");

        tt1 = System.currentTimeMillis();
        boolean saved = false;

        if (totalNew > 0) {
          boolean leaveOnServer = (Boolean.valueOf(account.getServerProperties().get(Utils.SVR_LEAVE_ON_SERVER)));
          info.setTotalMsg(totalNew);
          int i = 0;
          javax.mail.Message msg;
          Date lastFromDate = null;
          Date receivedDate = null;
          List<javax.mail.Message> msgList = new ArrayList<javax.mail.Message>(msgMap.keySet());
          String folderId = makeStoreFolder(userName, accountId, incomingFolder, totalNew);
          updateCheckingMailStatusByCometd(userName, accountId, info);
          while (i < totalNew) {
            if (info != null && info.isRequestStop()) {
              if (logger.isDebugEnabled()) {
                logger.debug("Stop requested on checkmail for " + account.getId());
              }
              throw new CheckMailInteruptedException("Stop getting mails from folder " + folder.getName() + " !");
            }
            msg = msgList.get(i);
            info.setFetching(i + 1);
            logger.debug("Fetching message " + (i + 1) + " ...");
            t1 = System.currentTimeMillis();
            try {
              saved = saveMessage(false, folderId, msgMap, userName, accountId, msg, folder);
              if (saved) {
                msg.setFlag(Flags.Flag.SEEN, true);
                if (!leaveOnServer)
                  msg.setFlag(Flags.Flag.DELETED, true);

                info.setFetchingToFolders(folderStr);
                info.setMsgId(MimeMessageParser.getMessageId(msg));
              }

              receivedDate = MimeMessageParser.getReceivedDate(msg).getTime();
              if (i == 0)
                lastFromDate = receivedDate;
              account.setLastCheckedDate(receivedDate);
              if ((i == (totalNew - 1)))
                account.setCheckFromDate(lastFromDate);

              if (lastFromDate != null && (account.getLastStartCheckingTime() == null || account.getLastStartCheckingTime().before(lastFromDate))) {
                account.setLastStartCheckingTime(lastFromDate);
              }
            } catch (Exception e) {
              info.setStatusCode(CheckingInfo.COMMON_ERROR);
              updateCheckingMailStatusByCometd(userName, accountId, info);
              i++;
              continue;
            }
            i++;
            t2 = System.currentTimeMillis();
            logger.debug("Message " + i + " saved : " + (t2 - t1) + " ms");
          }
          tt2 = System.currentTimeMillis();
          logger.debug(" ### Check mail finished total took: " + (tt2 - tt1) + " ms");
        } else {
          if (info != null) {
            info.setStatusCode(CheckingInfo.FINISHED_CHECKMAIL_STATUS);
            updateCheckingMailStatusByCometd(userName, accountId, info);
            return;
          }
        }
      } finally {
        folder.close(true);
      }
      info.setStatusCode(CheckingInfo.FINISHED_CHECKMAIL_STATUS);
      updateCheckingMailStatusByCometd(userName, accountId, info);
      logger.debug("/////////////////////////////////////////////////////////////");
      logger.debug("/////////////////////////////////////////////////////////////");
    }
  }

  private String makeStoreFolder(String userName, String accountId, String incomingFolder, int unReadMsg) throws Exception {
    String folderId = Utils.generateFID(accountId, incomingFolder, false);
    Folder storeFolder = storage_.getFolderById(userName, accountId, folderId);
    if (storeFolder == null) {
      folderId = Utils.generateFID(accountId, incomingFolder, true);
      Folder storeUserFolder = storage_.getFolderById(userName, accountId, folderId);
      if (storeUserFolder != null) {
        storeFolder = storeUserFolder;
      } else {
        storeFolder = new Folder();
      }
      storeFolder.setNumberOfUnreadMessage(unReadMsg);
      storeFolder.setId(folderId);
      storeFolder.setName(incomingFolder);
      storeFolder.setPersonalFolder(true);
      storage_.saveFolder(userName, accountId, storeFolder);
    }else{
      if(unReadMsg > 0){
        long numUnReadMsg = 0;
        numUnReadMsg = storeFolder.getNumberOfUnreadMessage();
        numUnReadMsg = numUnReadMsg > 0 ? numUnReadMsg : 0;
        storeFolder.setNumberOfUnreadMessage(numUnReadMsg + unReadMsg);
        storage_.saveFolder(userName, accountId, storeFolder);
      }
    }
    return folderId;
  }

  private SearchTerm getSearchTerm(SearchTerm sTerm, MessageFilter filter) throws Exception {
    if (!Utils.isEmptyField(filter.getFrom())) {
      FromStringTerm fsTerm = new FromStringTerm(filter.getFrom());
      if (filter.getFromCondition() == Utils.CONDITION_CONTAIN) {
        if (sTerm == null) {
          sTerm = fsTerm;
        } else {
          sTerm = new AndTerm(sTerm, fsTerm);
        }
      } else if (filter.getFromCondition() == Utils.CONDITION_NOT_CONTAIN) {
        if (sTerm == null) {
          sTerm = new NotTerm(fsTerm);
        } else {
          sTerm = new AndTerm(sTerm, new NotTerm(fsTerm));
        }
      }
    }

    if (!Utils.isEmptyField(filter.getTo())) {
      RecipientStringTerm toTerm = new RecipientStringTerm(RecipientType.TO, filter.getTo());
      if (filter.getToCondition() == Utils.CONDITION_CONTAIN) {
        if (sTerm == null) {
          sTerm = toTerm;
        } else {
          sTerm = new AndTerm(sTerm, toTerm);
        }
      } else if (filter.getToCondition() == Utils.CONDITION_NOT_CONTAIN) {
        if (sTerm == null) {
          sTerm = new NotTerm(toTerm);
        } else {
          sTerm = new AndTerm(sTerm, new NotTerm(toTerm));
        }
      }
    }

    if (!Utils.isEmptyField(filter.getSubject())) {
      SubjectTerm subjectTerm = new SubjectTerm(filter.getSubject());
      if (filter.getSubjectCondition() == Utils.CONDITION_CONTAIN) {
        if (sTerm == null) {
          sTerm = subjectTerm;
        } else {
          sTerm = new AndTerm(sTerm, subjectTerm);
        }
      } else if (filter.getSubjectCondition() == Utils.CONDITION_NOT_CONTAIN) {
        if (sTerm == null) {
          sTerm = new NotTerm(subjectTerm);
        } else {
          sTerm = new AndTerm(sTerm, new NotTerm(subjectTerm));
        }
      }
    }

    if (!Utils.isEmptyField(filter.getBody())) {
      BodyTerm bodyTerm = new BodyTerm(filter.getBody());
      if (filter.getBodyCondition() == Utils.CONDITION_CONTAIN) {
        if (sTerm == null) {
          sTerm = bodyTerm;
        } else {
          sTerm = new AndTerm(sTerm, bodyTerm);
        }
      } else if (filter.getBodyCondition() == Utils.CONDITION_NOT_CONTAIN) {
        if (sTerm == null) {
          sTerm = new NotTerm(bodyTerm);
        } else {
          sTerm = new AndTerm(sTerm, new NotTerm(bodyTerm));
        }
      }
    }

    return sTerm;
  }

  public void createAccount(String userName, Account account) throws Exception {
    saveAccount(userName, account, true);
  }

  public List<Folder> getFolders(String userName, String accountId) throws Exception {
    List<Folder> gottenFolderList = storage_.getFolders(userName, accountId);
    CheckingInfo info = getCheckingInfo(userName, accountId);
    
    if (info != null) {
      Connector connector = null;
      for (Folder folder : gottenFolderList) {
        String urlName = folder.getURLName();
        if (!folder.isPersonalFolder() && Utils.isEmptyField(urlName)) {
          if (connector == null) {
            Account account = getAccountById(userName, accountId);
            if (Utils.POP3.equalsIgnoreCase(account.getProtocol())) {
              connector = openPOPConnection(userName, account, info);
            } 
            
            if (Utils.IMAP.equalsIgnoreCase(account.getProtocol())) {
              connector = openIMAPConnection(userName, account, info);
            }
          }

          if ((connector != null) && connector.isConnected()) {
            javax.mail.Folder fd = connector.getFolder(folder.getName());
            folder.setURLName(fd.getURLName().toString());
            storage_.saveFolder(userName, accountId, folder);
          }
        }
      }
    }
    return gottenFolderList;
  }

  public List<Folder> getFolders(String userName, String accountId, boolean isPersonal) throws Exception {
    List<Folder> folders = getFolders(userName, accountId);
    List<Folder> resultList = new ArrayList<Folder>();
    
    for (Folder folder : folders) {
      if (folder.isPersonalFolder() == isPersonal) {
        resultList.add(folder);
      }
    }
    return resultList;
  }
  
  public void addTag(String userName, String accountId, Tag tag) throws Exception {
    if (!Utils.isEmptyField(getDelegatorUserName(userName, accountId)))
      userName = getDelegatorUserName(userName, accountId);
    storage_.addTag(userName, accountId, tag);
  }

  public void addTag(String userName, String accountId, List<Message> messages, List<Tag> tag) throws Exception {
    if (isDelegatedAccount(userName, accountId))
      userName = getDelegatedAccount(userName, accountId).getDelegateFrom();
    storage_.addTag(userName, accountId, messages, tag);
  }

  public List<Tag> getTags(String userName, String accountId) throws Exception {
    if (isDelegatedAccount(userName, accountId))
      userName = getDelegatedAccount(userName, accountId).getDelegateFrom();
    return storage_.getTags(userName, accountId);
  }

  public Tag getTag(String userName, String accountId, String tagId) throws Exception {
    if (!Utils.isEmptyField(getDelegatorUserName(userName, accountId)))
      userName = getDelegatorUserName(userName, accountId);
    return storage_.getTag(userName, accountId, tagId);
  }

  public void removeTagsInMessages(String userName, String accountId, List<Message> msgList, List<String> tagIdList) throws Exception {
    if (!Utils.isEmptyField(getDelegatorUserName(userName, accountId)))
      userName = getDelegatorUserName(userName, accountId);
    storage_.removeTagsInMessages(userName, accountId, msgList, tagIdList);
  }

  public void removeTag(String userName, String accountId, String tag) throws Exception {
    if (!Utils.isEmptyField(getDelegatorUserName(userName, accountId)))
      userName = getDelegatorUserName(userName, accountId);
    storage_.removeTag(userName, accountId, tag);
  }

  public void updateTag(String userName, String accountId, Tag tag) throws Exception {
    if (!Utils.isEmptyField(getDelegatorUserName(userName, accountId)))
      userName = getDelegatorUserName(userName, accountId);
    storage_.updateTag(userName, accountId, tag);
  }

  public List<Message> getMessageByTag(String userName, String accountId, String tagName) throws Exception {
    if (!Utils.isEmptyField(getDelegatorUserName(userName, accountId)))
      userName = getDelegatorUserName(userName, accountId);
    return storage_.getMessageByTag(userName, accountId, tagName);
  }

  public MessagePageList getMessagePagelistByTag(String userName, String accountId, String tagId) throws Exception {
    MessageFilter filter = new MessageFilter("Filter By Tag");
    filter.setAccountId(accountId);
    filter.setTag(new String[] { tagId });
    if (!Utils.isEmptyField(getDelegatorUserName(userName, accountId)))
      userName = getDelegatorUserName(userName, accountId);
    return getMessagePageList(userName, filter);
  }

  public MessagePageList getMessagePageListByFolder(String userName, String accountId, String folderId) throws Exception {
    MessageFilter filter = new MessageFilter("Filter By Folder");
    filter.setAccountId(accountId);
    filter.setFolder(new String[] { folderId });
    return getMessagePageList(userName, filter);
  }

  public MailSetting getMailSetting(String userName) throws Exception {
    return storage_.getMailSetting(userName);
  }

  public void saveMailSetting(String userName, MailSetting newSetting) throws Exception {
    storage_.saveMailSetting(userName, newSetting);
  }

  public void importMessage(String userName, String accountId, String folderId, InputStream inputStream, String type) throws Exception {
    Properties props = System.getProperties();
    Session session = Session.getDefaultInstance(props, null);
    MimeMessage mimeMessage = new MimeMessage(session, inputStream);
    Account account = getAccountById(userName, accountId);
    Folder folder = getFolderById(userName, accountId, folderId);
    if (isDelegatedAccount(userName, accountId)) {
      userName = getDelegatedAccount(userName, accountId).getDelegateFrom();
    }
    
    ImapConnector connector = openIMAPConnection(userName, account, null);
    long[] msgUID = connector.importMessageIntoServerMail(folder.getURLName(), mimeMessage);
    emlImportExport_.importMessage(userName, accountId, folderId, mimeMessage, msgUID);
  }

  public OutputStream exportMessage(String userName, String accountId, Message message) throws Exception {
    return emlImportExport_.exportMessage(userName, accountId, message);
  }

  public SpamFilter getSpamFilter(String userName, String accountId) throws Exception {
    return storage_.getSpamFilter(userName, accountId);
  }

  public void saveSpamFilter(String userName, String accountId, SpamFilter spamFilter) throws Exception {
    storage_.saveSpamFilter(userName, accountId, spamFilter);
  }

  public void toggleMessageProperty(String userName, String accountId, List<Message> msgList, String folderId, String property, boolean value) throws Exception {
    Account account = getAccountById(userName, accountId);
    Folder folder = getFolderById(userName, accountId, folderId);
    boolean success = true;
    if (account.getProtocol().equalsIgnoreCase(Utils.IMAP)) {
      try {
        ImapConnector connector = openIMAPConnection(userName, account, null);
        if (property.equals(Utils.EXO_STAR)) {
          if (folder != null && !Utils.isEmptyField(folder.getName())) {
            success = connector.markIsReadStared(msgList, folder, null, value);
          } else {
            List<Message> l = new ArrayList<Message>();
            for (Message m : msgList) {
              folder = getFolderById(userName, accountId, m.getFolders()[0]);
              if (folder != null) {
                l.add(m);
                success = connector.markIsReadStared(l, folder, null, value);
              }
            }
          }
        } else if (property.equals(Utils.EXO_ISUNREAD)) {
          if (folder != null && !Utils.isEmptyField(folder.getName())) {
            if (value) {
              success = connector.markIsReadStared(msgList, folder, false, null);
            } else {
              success = connector.markIsReadStared(msgList, folder, true, null);
            }
          } else {
            List<Message> l;
            for (Message m : msgList) {
              folder = getFolderById(userName, accountId, m.getFolders()[0]);
              if (folder != null) {
                l = new ArrayList<Message>();
                l.add(m);
                if (value) {
                  success = connector.markIsReadStared(msgList, folder, false, null);
                } else {
                  success = connector.markIsReadStared(msgList, folder, true, null);
                }
              }
            }
          }
        }

      } catch (Exception e) {
        return;
      }
    }
    if (success)
      storage_.toggleMessageProperty(userName, accountId, msgList, property, value);
  }

  public String getFolderHomePath(String userName, String accountId) throws Exception {
    return storage_.getFolderHomePath(userName, accountId);
  }

  public List<Folder> getSubFolders(String userName, String accountId, String parentPath) throws Exception {
    if (isDelegatedAccount(userName, accountId))
      userName = getDelegatedAccount(userName, accountId).getDelegateFrom();
    return storage_.getSubFolders(userName, accountId, parentPath);
  }

  public List<Message> getReferencedMessages(String userName, String accountId, String msgPath) throws Exception {
    return storage_.getReferencedMessages(userName, accountId, msgPath);
  }

  public Account getDefaultAccount(String userName) throws Exception {
    MailSetting mailSetting = storage_.getMailSetting(userName);
    String defaultAccount = mailSetting.getDefaultAccount();
    Account account = null;
    if (defaultAccount != null) {
      account = getAccountById(userName, defaultAccount);
    } else {
      List<Account> accList = getAccounts(userName);
      if (accList.size() > 0)
        account = getAccounts(userName).get(0);
    }
    return account;
  }

  public Message loadTotalMessage(String userName, String accountId, Message msg) throws Exception {
    try {
      Account account = getAccountById(userName, accountId);
      if (account.getProtocol().equals(Utils.IMAP)) {
        if (msg.isLoaded()) {
          msg = storage_.loadTotalMessage(userName, accountId, msg);
        } else {
          ImapConnector connector = openIMAPConnection(userName, account, null);
          if (connector != null) {
            String url = getFolderById(userName, accountId, msg.getFolders()[0]).getURLName();
            javax.mail.Message message = connector.getMessageByUID(msg.getUID(), url);
            if (message == null) {
              throw new Exception("Selected message does not exist");
            }
            msg = storage_.loadTotalMessage(userName, accountId, msg, message);
          }
        }
      } else if (account.getProtocol().equals(Utils.POP3)) {
        msg = storage_.loadTotalMessage(userName, accountId, msg);
      }
    } catch (Exception e) {
      try {
        msg = storage_.loadTotalMessage(userName, accountId, msg, null);
      } catch (Exception ex) {
      }
      logger.info("Download content failure");
    }
    return msg;
  }

  public void start() {
    for (MailUpdateStorageEventListener updateListener : listeners_) {
      updateListener.preUpdate();
    }
  }

  public void stop() {
  }

  public synchronized void addListenerPlugin(ComponentPlugin listener) throws Exception {
    if (listener instanceof MailUpdateStorageEventListener) {
      listeners_.add((MailUpdateStorageEventListener) listener);
    }
  }

  private Properties getAccountProperties(Account acc) {
    Properties props = System.getProperties();
    String smtpUser = acc.getIncomingUser();
    String outgoingHost = acc.getOutgoingHost();
    String outgoingPort = acc.getOutgoingPort();
    String isSSl = acc.getServerProperties().get(Utils.SVR_OUTGOING_SSL);
    props.put(Utils.SVR_SMTP_USER, smtpUser);
    props.put(Utils.SVR_SMTP_HOST, outgoingHost);
    props.put(Utils.SVR_SMTP_PORT, outgoingPort);
    props.put("mail.smtp.dsn.notify", "SUCCESS,FAILURE ORCPT=rfc822;" + acc.getEmailAddress());
    props.put("mail.smtp.dsn.ret", "FULL");
    props.put("mail.smtp.socketFactory.port", outgoingPort);
    props.put(Utils.SVR_SMTP_AUTH, "true");
    props.put(Utils.SVR_SMTP_SOCKET_FACTORY_FALLBACK, "true");
    props.put("mail.smtp.connectiontimeout", "0");
    props.put("mail.smtp.timeout", "0");
    String socketFactoryClass = "javax.net.SocketFactory";
    if (Boolean.valueOf(isSSl)) {
      socketFactoryClass = Utils.SSL_FACTORY;
      props.put(Utils.SVR_SMTP_STARTTLS_ENABLE, "true");
      props.put("mail.smtp.ssl.protocols", "SSLv3 TLSv1");
    }
    props.put(Utils.SVR_SMTP_SOCKET_FACTORY_CLASS, socketFactoryClass);

    return props;
  }

  public boolean sendReturnReceipt(String userName, String accId, String msgId, ResourceBundle res) throws Exception {
    Account acc = getAccountById(userName, accId);
    Message msg = getMessageById(userName, accId, msgId);

    String subject = new String("Disposition notification");
    String text = new String("The message sent on {0} to {1} with subject \"{2}\" has been displayed. This is no guarantee that the message has been read or understood.");
    if (res != null) {
      try {
        subject = res.getString("UIMessagePreview.msg.return-receipt-subject");
      } catch (MissingResourceException e) {
        subject = new String("Disposition notification");
      }
      try {
        text = res.getString("UIMessagePreview.msg.return-receipt-text");
      } catch (MissingResourceException e) {
        text = new String("The message sent on {0} to {1} with subject \"{2}\" has been displayed. This is no guarantee that the message has been read or understood.");
      }
    }
    text = text.replace("{0}", msg.getSendDate().toString());
    text = text.replace("{1}", msg.getMessageTo());
    text = text.replace("{2}", msg.getSubject());

    Message receiptMsg = new Message();
    receiptMsg.setMessageTo(msg.getFrom());
    receiptMsg.setSubject(subject);
    receiptMsg.setSendDate(new Date());

    DispositionNotification disNotification = new DispositionNotification();
    disNotification.getNotifications().setHeader("Reporting-UA", "cs.exoplatform.com;" + " CS-Mail");
    disNotification.getNotifications().setHeader("MDN-Gateway", "smtp;" + " " + acc.getOutgoingHost());
    disNotification.getNotifications().setHeader("Original-Recipient", "rfc822;" + " " + msg.getFrom());
    disNotification.getNotifications().setHeader("Final-Recipient", "rfc822;" + " " + acc.getUserDisplayName() + "<" + acc.getEmailAddress() + ">");
    disNotification.getNotifications().setHeader("Original-Message-ID", msg.getId());
    disNotification.getNotifications().setHeader("Disposition", "manual-action/MDN-sent-automatically;" + " displayed");

    MultipartReport report = new MultipartReport(text, disNotification);

    Properties props = getAccountProperties(acc);
    Session session = Session.getDefaultInstance(props, null);
    logger.debug(" #### Sending email ... ");
    SMTPTransport transport = (SMTPTransport) session.getTransport(Utils.SVR_SMTP);
    try {
      if (!acc.isOutgoingAuthentication()) {
        transport.connect();
      } else if (acc.useIncomingSettingForOutgoingAuthent()) {
        transport.connect(acc.getOutgoingHost(), Integer.parseInt(acc.getOutgoingPort()), acc.getIncomingUser(), acc.getIncomingPassword());
      } else {
        transport.connect(acc.getOutgoingHost(), Integer.parseInt(acc.getOutgoingPort()), acc.getOutgoingUserName(), acc.getOutgoingPassword());
      }
    } catch (Exception ex) {
      logger.debug("#### Can not connect to smtp server ...");
      throw ex;
    }

    MailcapCommandMap mailcap = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
    mailcap.addMailcap("message/disposition-notification;; x-java-content-handler=org.exoplatform.mail.service.impl.Message_DispositionNotification");
    CommandMap.setDefaultCommandMap(mailcap);

    sendReturnReceipt(session, transport, receiptMsg, report);
    transport.close();

    return true;
  }

  private void sendReturnReceipt(Session session, Transport transport, Message message, MultipartReport report) throws Exception {
    MimeMessage mimeMessage = new MimeMessage(session);
    String status = "";
    InternetAddress addressFrom;
    mimeMessage.setHeader("Message-ID", message.getId());
    mimeMessage.setHeader("Content-Transfer-Encoding", "utf-8");

    if (message.getFrom() != null)
      addressFrom = new InternetAddress(message.getFrom());
    else
      addressFrom = new InternetAddress(session.getProperties().getProperty(Utils.SVR_SMTP_USER));

    mimeMessage.setFrom(addressFrom);
    if (message.getMessageTo() != null)
      mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(message.getMessageTo()));

    mimeMessage.setSubject(message.getSubject(), "UTF-8");
    mimeMessage.setSentDate(message.getSendDate());

    mimeMessage.setContent(report);

    mimeMessage.saveChanges();
    try {
      transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
      status = "Mail Delivered !";
    } catch (AddressException e) {
      status = "There was an error parsing the addresses. Sending Failed !" + e.getMessage();
      throw e;
    } catch (AuthenticationFailedException e) {
      status = "The Username or Password may be wrong. Sending Failed !" + e.getMessage();
      throw e;
    } catch (SMTPSendFailedException e) {
      status = "Sorry, There was an error sending the message. Sending Failed !" + e.getMessage();
      throw e;
    } catch (MessagingException e) {
      status = "There was an unexpected error. Sending Failed ! " + e.getMessage();
      throw e;
    } catch (Exception e) {
      status = "There was an unexpected error. Sending Falied !" + e.getMessage();
      throw e;
    }
    logger.debug(" #### Info : " + status);
  }

  public ContinuationService getContinuationService() {
    return continuationService_;
  }

  public void setContinuationService(ContinuationService continuationService) {
    continuationService_ = continuationService;
  }

  public void updateCheckingMailStatusByCometd(String userName, String accountId, CheckingInfo info) {
    if (info != null && info.hasChanged()) {
      try {
        JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
        JsonValue json = generatorImpl.createJsonObject(info.getStatus());
        continuationService_.sendMessage(userName, "/eXo/Application/mail/ckmailsts/" + accountId, json);
        info.setHasChanged(false);
      } catch (JsonException je) {
        logger.warn("can not send cometd message to client [ " + userName + " ]!", je);
      } catch (Exception e) {
        if (logger.isDebugEnabled())
          logger.debug("n\n can not send update message to UI " + e.getMessage());
      }
    }
  }

  class CheckMailInteruptedException extends Exception {
    private static final long serialVersionUID = 1L;
    private String message;

    public CheckMailInteruptedException(String msg) {
      super();
      this.message = msg;
    }

    public String getMessage() {
      return message;
    }
  }

  public BufferAttachment getAttachmentFromDMS(String userName, String relPath) throws Exception {
    return storage_.getAttachmentFromDMS(userName, relPath);
  }

  public String[] getDMSDataInfo(String userName) throws Exception {
    return storage_.getDMSDataInfo(userName);
  }

  @Override
  public Node getDMSSelectedNode(String userName, String relPath) throws Exception {
    return storage_.getDMSSelectedNode(userName, relPath);
  }

  @Override
  public void delegateAccount(String sender, String revieve, String accountId, String permisison) throws Exception {
    Account acc = storage_.getAccountById(sender, accountId);
    storage_.delegateAccount(sender, revieve, accountId);
    Map<String, String> oldPermission = acc.getPermissions();
    if (oldPermission == null)
      oldPermission = new HashMap<String, String>();
    oldPermission.put(revieve, permisison);
    acc.setPermissions(oldPermission);
    storage_.saveAccount(sender, acc, false);
  }

  @Override
  public List<Account> getDelegatedAccounts(String userId) throws Exception {
    return storage_.getDelegateAccounts(userId);
  }

  public void removeDelegateAccount(String userId, String receiver, String accountId) throws Exception {
    Account acc = storage_.getAccountById(userId, accountId);
    storage_.removeDelegateAccount(receiver, accountId);
    Map<String, String> oldPermission = acc.getPermissions();
    oldPermission.remove(receiver);
    acc.setPermissions(oldPermission);
    storage_.saveAccount(userId, acc, false);
  }

  @Override
  public Account getDelegatedAccount(String userId, String accountId) throws Exception {
    for (Account acc : storage_.getDelegateAccounts(userId)) {
      if (accountId.equalsIgnoreCase(acc.getId())) {
        return acc;
      }
    }
    return null;
  }

  private boolean isDelegatedAccount(String id, String accId) {
    try {
      return (getDelegatedAccount(id, accId) != null);
    } catch (Exception e) {
      return false;
    }
  }

  public String getDelegatorUserName(String currentUserName, String accountId) throws Exception {
    if (isDelegatedAccount(currentUserName, accountId))
      return getDelegatedAccount(currentUserName, accountId).getDelegateFrom();
    return null;
  }

  private boolean saveMessage(boolean isImap, String folderId, LinkedHashMap<javax.mail.Message, List<String>> msgMap, String username, String accountId, javax.mail.Message msg, javax.mail.Folder folder) {
    String[] folderIds = { folderId };
    List<String> folderList = new ArrayList<String>();
    List<String> tagList = new ArrayList<String>();
    List<String> filterList = msgMap.get(msg);
    SpamFilter spamFilter;
    boolean saved = false;
    if (filterList != null && filterList.size() > 0) {
      String tagId;
      for (int j = 0; j < filterList.size(); j++) {
        MessageFilter filter = null;
        try {
          filter = getFilterById(username, accountId, filterList.get(j));
        } catch (Exception e) {
          if (logger.isDebugEnabled())
            logger.debug("Cannot get Filter. ", e);
          return false;
        }
        folderList.add(filter.getApplyFolder());
        tagId = filter.getApplyTag();
        if (tagId != null && tagId.trim().length() > 0)
          tagList.add(tagId);
      }
      folderIds = folderList.toArray(new String[] {});
    }
    for (int k = 0; k < folderIds.length; k++) {
      if (folderStr.indexOf(folderIds[k] + COMMA) == -1) {
        folderStr += folderIds[k] + COMMA;
      }
    }

    Info infoObj = new Info();
    infoObj.setFolders(folderStr);

    try {
      spamFilter = getSpamFilter(username, getAccountById(username, accountId).getId());
      if (isImap) {
        long msgUID = ((IMAPFolder) folder).getUID(msg);
        saved = storage_.saveMessage(username, accountId, msgUID, msg, folderIds, tagList, spamFilter, infoObj, this.continuationService_, false, username);
      } else {
        saved = storage_.savePOP3Message(username, accountId, msg, folderIds, tagList, spamFilter, infoObj, continuationService_, username);
      }
    } catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("Cannot save message. ", e);
      return false;
    }

    return saved;
  }

  public void addPlugin(ComponentPlugin plugin) {
    if (plugin instanceof MailSettingConfigPlugin) {
      MailSettingConfigPlugin mailConfigPlugin = (MailSettingConfigPlugin) plugin;
      settingPlugins.put(mailConfigPlugin.getName(), mailConfigPlugin);
    }
  }

  public Map<String, MailSettingConfigPlugin> getSettingConfig() {
    return settingPlugins;
  }

  @Override
  public List<String> getListOfMessageIds(String username, MessageFilter filter) throws Exception {
    return storage_.getListOfMessageIds(username, filter);
  }
  
  public void closeAllMailConnectionByUser(String username) throws Exception {
    Map<String, Object> connectionCache = connectionStore.get(username);
    if (connectionCache != null) {
      for (Object obj : connectionCache.values()) {
         if (obj instanceof BaseConnector) {
           BaseConnector connector = (BaseConnector) obj;
           connector.close();
         }
      }
      connectionStore.remove(username);
    }
  }
}
