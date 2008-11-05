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
package org.exoplatform.mail.service.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeMessage.RecipientType;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exoplatform.mail.service.Account;
import org.exoplatform.mail.service.AccountData;
import org.exoplatform.mail.service.Attachment;
import org.exoplatform.mail.service.CheckMailJob;
import org.exoplatform.mail.service.CheckingInfo;
import org.exoplatform.mail.service.Folder;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.MailSetting;
import org.exoplatform.mail.service.Message;
import org.exoplatform.mail.service.MessageFilter;
import org.exoplatform.mail.service.MessagePageList;
import org.exoplatform.mail.service.MimeMessageParser;
import org.exoplatform.mail.service.ServerConfiguration;
import org.exoplatform.mail.service.SpamFilter;
import org.exoplatform.mail.service.Tag;
import org.exoplatform.mail.service.Utils;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.scheduler.JobInfo;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.services.scheduler.PeriodInfo;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import com.sun.mail.smtp.SMTPSendFailedException;

/**
 * Created by The eXo Platform SARL Author : Tuan Nguyen
 * tuan.nguyen@exoplatform.com Jun 23, 2007
 */
public class MailServiceImpl implements MailService {

  private static final Log          logger = LogFactory.getLog(MailServiceImpl.class);

  private JCRDataStorage            storage_;

  // will be use map for multi import/export email type
  private EMLImportExport           emlImportExport_;

  private Map<String, CheckingInfo> checkingLog_;
  
  private JobSchedulerService schedulerService_;

  public MailServiceImpl(NodeHierarchyCreator nodeHierarchyCreator, JobSchedulerService schedulerService) throws Exception {
    storage_ = new JCRDataStorage(nodeHierarchyCreator);
    emlImportExport_ = new EMLImportExport(storage_);
    checkingLog_ = new HashMap<String, CheckingInfo>();
    this.schedulerService_ = schedulerService;
  }

  public void removeCheckingInfo(String username, String accountId) throws Exception {
    String key = username + ":" + accountId;
    checkingLog_.remove(key);
  }

  public CheckingInfo getCheckingInfo(String username, String accountId) {
    String key = username + ":" + accountId;
    return checkingLog_.get(key);
  }

  /**
   * @param username
   * @return
   * @throws Exception
   */
  public List<Account> getAccounts(SessionProvider sProvider, String username) throws Exception {
    return storage_.getAccounts(sProvider, username);
  }

  public Account getAccountById(SessionProvider sProvider, String username, String id)
  throws Exception {
    return storage_.getAccountById(sProvider, username, id);
  }

  public void saveAccount(SessionProvider sProvider, String username, Account account, boolean isNew)
  throws Exception {
    storage_.saveAccount(sProvider, username, account, isNew);
  }

  public void updateAccount(SessionProvider sProvider, String username, Account account)
  throws Exception {
    saveAccount(sProvider, username, account, false);
  }

  public void removeAccount(SessionProvider sProvider, String username, String accountId)
  throws Exception {
    stopAllJobs(sProvider, username, accountId);
    storage_.removeAccount(sProvider, username, accountId);
  }

  public Folder getFolder(SessionProvider sProvider, String username, String accountId,
      String folderId) throws Exception {
    return storage_.getFolder(sProvider, username, accountId, folderId);
  }

  public String getFolderParentId(SessionProvider sProvider, String username, String accountId,
      String folderId) throws Exception {
    return storage_.getFolderParentId(sProvider, username, accountId, folderId);
  }

  public boolean isExistFolder(SessionProvider sProvider, String username, String accountId,
      String parentId, String folderName) throws Exception {
    return storage_.isExistFolder(sProvider, username, accountId, parentId, folderName);
  }

  public void saveFolder(SessionProvider sProvider, String username, String accountId, Folder folder)
  throws Exception {
    storage_.saveFolder(sProvider, username, accountId, folder);
  }

  public void removeUserFolder(SessionProvider sProvider, String username, String accountId,
      String folderId) throws Exception {
    storage_.removeUserFolder(sProvider, username, accountId, folderId);
  }

  public List<MessageFilter> getFilters(SessionProvider sProvider, String username, String accountId)
  throws Exception {
    return storage_.getFilters(sProvider, username, accountId);
  }

  public MessageFilter getFilterById(SessionProvider sProvider, String username, String accountId,
      String filterId) throws Exception {
    return storage_.getFilterById(sProvider, username, accountId, filterId);
  }

  public void saveFilter(SessionProvider sProvider, String username, String accountId,
      MessageFilter filter, boolean applyAll) throws Exception {
    storage_.saveFilter(sProvider, username, accountId, filter, applyAll);
  }

  public void removeFilter(SessionProvider sProvider, String username, String accountId,
      String filterId) throws Exception {
    storage_.removeFilter(sProvider, username, accountId, filterId);
  }

  public Message getMessageById(SessionProvider sProvider, String username, String accountId,
      String msgId) throws Exception {
    return storage_.getMessageById(sProvider, username, accountId, msgId);
  }

  public void removeMessage(SessionProvider sProvider, String username, String accountId,
      Message message) throws Exception {
    storage_.removeMessage(sProvider, username, accountId, message);
  }

  public void removeMessages(SessionProvider sProvider, String username, String accountId,
      List<Message> messages, boolean moveReference) throws Exception {
    storage_.removeMessages(sProvider, username, accountId, messages, moveReference);
  }

  public void moveMessages(SessionProvider sProvider, String username, String accountId,
      List<Message> msgList, String currentFolderId, String destFolderId) throws Exception {
    storage_.moveMessages(sProvider, username, accountId, msgList, currentFolderId, destFolderId);
  }

  public void moveMessage(SessionProvider sProvider, String username, String accountId,
      Message msg, String currentFolderId, String destFolderId) throws Exception {
    storage_.moveMessage(sProvider, username, accountId, msg, currentFolderId, destFolderId);
  }

  public MessagePageList getMessagePageList(SessionProvider sProvider, String username,
      MessageFilter filter) throws Exception {
    return storage_.getMessagePageList(sProvider, username, filter);
  }

  public void saveMessage(SessionProvider sProvider, String username, String accountId,
      String targetMsgPath, Message message, boolean isNew) throws Exception {
    storage_.saveMessage(sProvider, username, accountId, targetMsgPath, message, isNew);
  }

  public List<Message> getMessagesByTag(SessionProvider sProvider, String username,
      String accountId, String tagId) throws Exception {
    MessageFilter filter = new MessageFilter("Tag");
    filter.setAccountId(accountId);
    filter.setFolder(new String[] { tagId });
    return getMessages(sProvider, username, filter);
  }

  public List<Message> getMessagesByFolder(SessionProvider sProvider, String username,
      String accountId, String folderId) throws Exception {
    MessageFilter filter = new MessageFilter("Folder");
    filter.setAccountId(accountId);
    filter.setFolder(new String[] { folderId });
    return getMessages(sProvider, username, filter);
  }

  public List<Message> getMessages(SessionProvider sProvider, String username, MessageFilter filter)
  throws Exception {
    return storage_.getMessages(sProvider, username, filter);
  }

  public void saveMessage(SessionProvider sProvider, String username, String accountId,
      Message message, boolean isNew) throws Exception {
    storage_.saveMessage(sProvider, username, accountId, message, isNew);
  }

  public Message sendMessage(SessionProvider sProvider, String username, String accId,
      Message message) throws Exception {
    Account acc = getAccountById(sProvider, username, accId);
    String smtpUser = acc.getIncomingUser();
    String outgoingHost = acc.getOutgoingHost();
    String outgoingPort = acc.getOutgoingPort();
    String isSSl = acc.getServerProperties().get(Utils.SVR_INCOMING_SSL);
    Properties props = new Properties();
    props.put(Utils.SVR_SMTP_HOST, outgoingHost);
    props.put(Utils.SVR_SMTP_PORT, outgoingPort);
    props.put(Utils.SVR_SMTP_AUTH, "true");
    props.put(Utils.SVR_SMTP_SOCKET_FACTORY_FALLBACK, "false");
    String socketFactoryClass = "javax.net.SocketFactory";
    if (Boolean.valueOf(isSSl))
      socketFactoryClass = Utils.SSL_FACTORY;
    props.put(Utils.SVR_SMTP_SOCKET_FACTORY_CLASS, socketFactoryClass);
    props.put(Utils.SVR_SMTP_SOCKET_FACTORY_PORT, outgoingPort);
    props.put(Utils.SVR_SMTP_USER, smtpUser);
    props.put(Utils.SVR_SMTP_STARTTLS_ENABLE, "true");
    props.put(Utils.SVR_INCOMING_SSL, isSSl);

    props.put(Utils.SVR_INCOMING_USERNAME, acc.getIncomingUser());
    props.put(Utils.SVR_INCOMING_PASSWORD, acc.getIncomingPassword());

    // TODO : add authenticator
    /*
     * Session session = Session.getInstance(props, new
     * javax.mail.Authenticator(){ protected javax.mail.PasswordAuthentication
     * getPasswordAuthentication() { return new
     * javax.mail.PasswordAuthentication(acc.getOutgoingUser(),
     * acc.getOutgoingPassword()); }});
     */
    Session session = Session.getInstance(props, null);
    logger.debug(" #### Sending email ... ");
    Transport transport = session.getTransport(Utils.SVR_SMTP);
    // khdung
    try {
      transport.connect(outgoingHost, smtpUser, acc.getIncomingPassword());
    } catch (Exception e) {
      // do nothing ... if there is an exception, keep continuing
      try {
        transport.connect() ;
      } catch(Exception ex) {
        logger.warn("#### Can not connect to smtp server ...") ;
        return null ;
      }
    }
    Message msg = send(session, transport, message);
    transport.close();

    return msg;
  }

  public Message sendMessage(SessionProvider sProvider, String username, Message message)
  throws Exception {
    return sendMessage(sProvider, username, message.getAccountId(), message);
  }

  public void sendMessage(Message message) throws Exception {
    List<Message> msgList = new ArrayList<Message>();
    msgList.add(message);
    sendMessages(msgList, message.getServerConfiguration());
  }

  public void sendMessages(List<Message> msgList, ServerConfiguration serverConfig)
  throws Exception {
    Properties props = new Properties();
    props.put(Utils.SVR_INCOMING_USERNAME, serverConfig.getUserName());
    props.put(Utils.SVR_INCOMING_PASSWORD, serverConfig.getPassword());
    props.put(Utils.SVR_SMTP_USER, serverConfig.getUserName());
    props.put(Utils.SVR_SMTP_HOST, serverConfig.getOutgoingHost());
    props.put(Utils.SVR_SMTP_PORT, serverConfig.getOutgoingPort());
    props.put(Utils.SVR_SMTP_AUTH, "true");
    props.put(Utils.SVR_SMTP_SOCKET_FACTORY_PORT, serverConfig.getOutgoingPort());
    if (serverConfig.isSsl()) {
      props.put(Utils.SVR_INCOMING_SSL, String.valueOf(serverConfig.isSsl()));
      props.put(Utils.SVR_SMTP_STARTTLS_ENABLE, "true");
      props.put(Utils.SVR_SMTP_SOCKET_FACTORY_CLASS, "javax.net.ssl.SSLSocketFactory");
    }
    props.put(Utils.SVR_SMTP_SOCKET_FACTORY_FALLBACK, "false");
    Session session = Session.getInstance(props, null);
    Transport transport = session.getTransport(Utils.SVR_SMTP);
    try {
      transport.connect(serverConfig.getOutgoingHost(), serverConfig.getUserName(), serverConfig
          .getPassword());
    } catch(Exception e) {
      try {
        transport.connect() ;
      } catch(Exception ex) {
        logger.warn("#### Can not connect to smtp server ...") ;
        return ;
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
        logger.error(" #### Info : send fail at message " + i + " \n");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        StringBuffer sb = sw.getBuffer();
        logger.error(sb.toString());
      }
    }
    logger.debug(" #### Info : Sent " + i + " email(s)");
    transport.close();
  }

  private Message send(Session session, Transport transport, Message message) throws Exception {
    javax.mail.Message mimeMessage = new MimeMessage(session);
    String status = "";
    InternetAddress addressFrom;
    mimeMessage.setHeader("Message-ID", message.getId());
    if (message.getFrom() != null)
      addressFrom = new InternetAddress(message.getFrom());
    else
      addressFrom = new InternetAddress(session.getProperties().getProperty(Utils.SVR_SMTP_USER));

    mimeMessage.setFrom(addressFrom);
    if (message.getMessageTo() != null)
      mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(message
          .getMessageTo()));

    if (message.getMessageCc() != null)
      mimeMessage.setRecipients(javax.mail.Message.RecipientType.CC, InternetAddress.parse(message
          .getMessageCc(), true));

    if (message.getMessageBcc() != null)
      mimeMessage.setRecipients(javax.mail.Message.RecipientType.BCC, InternetAddress.parse(message
          .getMessageBcc(), false));

    if (message.getReplyTo() != null)
      mimeMessage.setReplyTo(Utils.getInternetAddress(message.getReplyTo()));

    mimeMessage.setSubject(message.getSubject());
    mimeMessage.setSentDate(message.getSendDate());

    MimeMultipart multipPartRoot = new MimeMultipart("mixed");

    MimeMultipart multipPartContent = new MimeMultipart("alternative");

    List<Attachment> attachList = message.getAttachments();
    if (attachList != null && attachList.size() != 0) {
      MimeBodyPart contentPartRoot = new MimeBodyPart();
      contentPartRoot.setContent(multipPartContent);

      MimeBodyPart mimeBodyPart1 = new MimeBodyPart();
      mimeBodyPart1.setContent(message.getMessageBody(), message.getContentType());
      multipPartContent.addBodyPart(mimeBodyPart1);
      multipPartRoot.addBodyPart(contentPartRoot);

      for (Attachment att : attachList) {
        InputStream is = att.getInputStream();
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(is, att.getMimeType());
        mimeBodyPart.setDataHandler(new DataHandler(byteArrayDataSource));

        mimeBodyPart.setDisposition(Part.ATTACHMENT);
        mimeBodyPart.setFileName(att.getName());
        multipPartRoot.addBodyPart(mimeBodyPart);
      }
      mimeMessage.setContent(multipPartRoot);
    } else {
      if (message.getContentType() != null && message.getContentType().indexOf("text/plain") > -1)
        mimeMessage.setText(message.getMessageBody());
      else
        mimeMessage.setContent(message.getMessageBody(), "text/html");
    }
    mimeMessage.setHeader("X-Priority", String.valueOf(message.getPriority()));
    String priority = "Normal";
    if (message.getPriority() == Utils.PRIORITY_HIGH) {
      priority = "High";
    } else if (message.getPriority() == Utils.PRIORITY_LOW) {
      priority = "Low";
    }
    if (message.getPriority() != 0)
      mimeMessage.setHeader("Importance", priority);

    Iterator iter = message.getHeaders().keySet().iterator();
    while (iter.hasNext()) {
      String key = iter.next().toString();
      mimeMessage.setHeader(key, message.getHeaders().get(key));
    }
    mimeMessage.saveChanges();
    try {
      transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
      message.setId(MimeMessageParser.getMessageId(mimeMessage));
      Enumeration enu = mimeMessage.getAllHeaders();
      while (enu.hasMoreElements()) {
        Header header = (Header) enu.nextElement();
        message.setHeader(header.getName(), header.getValue());
      }
      status = "Mail Delivered !";
    } catch (AddressException e) {
      status = "There was an error parsing the addresses. Sending Failed !" + e.getMessage();
    } catch (AuthenticationFailedException e) {
      status = "The Username or Password may be wrong. Sending Failed !" + e.getMessage();
    } catch (SMTPSendFailedException e) {
      status = "Sorry,There was an error sending the message. Sending Failed !" + e.getMessage();
    } catch (MessagingException e) {
      status = "There was an unexpected error. Sending Failed ! " + e.getMessage();
    } catch (Exception e) {
      status = "There was an unexpected error. Sending Falied !" + e.getMessage();
    } finally {
      // logger.debug(" #### Info : " + status);
    }
    logger.debug(" #### Info : " + status);

    return message;
  }

  public void checkMail(String username, String accountId) throws Exception {

    JobDetail job = loadCheckmailJob(username, accountId);
    
    // trigger now
    if (job != null) {
      schedulerService_.executeJob(job.getName(), job.getGroup(), job.getJobDataMap());
    }
  }
  
  public void stopCheckMail(String username, String accountId)  {
    CheckingInfo checkingInfo = getCheckingInfo(username, accountId);
    if (checkingInfo != null) {
      checkingInfo.setRequestStop(true);
      System.out.println("Requested check loop to stop ");
    } 
  }
  
  private void stopAllJobs(SessionProvider sProvider, String username, String accountId) throws Exception {
    JobInfo info = CheckMailJob.getJobInfo(username, accountId);
    stopCheckMail(username, accountId);
    schedulerService_.removeJob(info);
  }

  /**
   * Load or register the CheckMailJob against scheduler
   * @return
   * @throws Exception 
   */
  private JobDetail loadCheckmailJob(String username, String accountId) throws Exception {

    JobInfo info = CheckMailJob.getJobInfo(username, accountId);
    JobDetail job = findCheckmailJob(username, accountId);

    // add job is it does not exist
    if (job == null) {
      JobDataMap jobData = new JobDataMap();
      jobData.put(CheckMailJob.USERNAME, username);
      jobData.put(CheckMailJob.ACCOUNTID, accountId);     
      
      // start now, execute once 
      // TODO :schedule as specified by account settings
      PeriodInfo periodInfo = new PeriodInfo(new GregorianCalendar().getTime(), null, 1, 24*60*60*1000);
      schedulerService_.addPeriodJob(info, periodInfo, jobData);
      
      //job = findCheckmailJob(username, accountId);
    }
    return job;
  }

  private JobDetail findCheckmailJob(String username,
                                     String accountId) throws Exception {
    // TODO current implementation is inefficient
    /// Need to upgrade to 2.0.3 and use this instead : 
    //schedulerService_.getJob(info) 
    for (Object obj : schedulerService_.getAllJobs()) {
      JobDetail tmp = (JobDetail) obj;
      if (tmp.getName().equals(username + ":" + accountId)) {
        return tmp;
      }
    }
    return null;
  }

  public List<Message> checkNewMessage(SessionProvider sProvider, String username, String accountId)
  throws Exception {
    Account account = getAccountById(sProvider, username, accountId);
    List<Message> messageList = new ArrayList<Message>();
    if(account != null) {
      CheckingInfo info = new CheckingInfo();
      String key = username + ":" + accountId;
      checkingLog_.put(key, info);
      long t1, t2, tt1, tt2;
      if (Utils.isEmptyField(account.getIncomingPassword()))
        info.setStatusCode(CheckingInfo.RETRY_PASSWORD);

      logger.warn(" #### Getting mail from " + account.getIncomingHost() + " ... !");
      info.setStatusMsg("Getting mail from " + account.getIncomingHost() + " ... !");
      int totalNew = -1;
      String protocol = account.getProtocol();
      boolean isPop3 = account.getProtocol().equals(Utils.POP3);
      boolean isImap = account.getProtocol().equals(Utils.IMAP);
      Date lastCheckedDate = account.getLastCheckedDate();
      if (!account.isCheckAll()) {
        if (lastCheckedDate == null) {
           lastCheckedDate = account.getCheckFromDate();
        }
      }
      /*ExoContainer container = RootContainer.getInstance();
    container = ((RootContainer)container).getPortalContainer("portal");
    ContinuationService continuation = (ContinuationService) container.getComponentInstanceOfType(ContinuationService.class);*/
      try {
        Properties props = System.getProperties();
        props.setProperty("mail.mime.base64.ignoreerrors", "true"); // this line fix for base64 encode problem with corrupted attachments

        String socketFactoryClass = "javax.net.SocketFactory";
        if (account.isIncomingSsl())
          socketFactoryClass = Utils.SSL_FACTORY;

        if (protocol.equals(Utils.POP3)) {
          props.setProperty("mail.pop3.socketFactory.fallback", "false");
          props.setProperty("mail.pop3.socketFactory.class", socketFactoryClass);
        } else if (protocol.equals(Utils.IMAP)) {
          props.setProperty("mail.imap.socketFactory.fallback", "false");
          props.setProperty("mail.imap.socketFactory.class", socketFactoryClass);
        }

        Session session = Session.getDefaultInstance(props);
        String[] incomingFolders = account.getIncomingFolder().split(",");

        // Later : the part inside of this loop should be in a separated method
        for (String incomingFolder : incomingFolders) {
          incomingFolder = incomingFolder.trim();
          URLName storeURL = new URLName(account.getProtocol(), account.getIncomingHost(), Integer
              .valueOf(account.getIncomingPort()), incomingFolder, account.getIncomingUser(), account
              .getIncomingPassword());
          Store store = session.getStore(storeURL);
          try {
            // Later : so for each more folder you need to connect again :-)
            store.connect();
          } catch (AuthenticationFailedException e) {
            logger.warn("Exception while connecting to server : " + e.getMessage());

            // Later : you only think about wrong password ...?
            if (!account.isSavePassword()) {
              account.setIncomingPassword("");
              updateAccount(sProvider, username, account);
            }
            info.setStatusMsg("The username or password may be wrong.");
            info.setStatusCode(CheckingInfo.RETRY_PASSWORD);
            return messageList;

          } catch (MessagingException e) {
            logger.warn("Exception while connecting to server : " + e.getMessage());

            info.setStatusMsg("Connecting failed. Please check server configuration.");
            info.setStatusCode(CheckingInfo.CONNECTION_FAILURE);
            return messageList;

          } catch (Exception e) {
            logger.warn("Exception while connecting to server : " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            StringBuffer sb = sw.getBuffer();
            logger.error(sb.toString());

            info.setStatusMsg("There was an unexpected error. Connecting failed.");
            info.setStatusCode(CheckingInfo.CONNECTION_FAILURE);
            return messageList;
          }

          javax.mail.Folder folder = store.getFolder(storeURL.getFile());
          if (!folder.exists()) {
            logger.warn(" #### Folder " + incomingFolder + " is not exists !");
            info.setStatusMsg("Folder " + incomingFolder + " is not exists");
            store.close();
            continue;
          } else {
            logger.warn(" #### Getting mails from folder " + incomingFolder + " !");
            info.setStatusMsg("Getting mails from folder " + incomingFolder + " !");
          }
          folder.open(javax.mail.Folder.READ_WRITE);

          javax.mail.Message[] messages;
          LinkedHashMap<javax.mail.Message, List<String>> msgMap = new LinkedHashMap<javax.mail.Message, List<String>>();
          SearchTerm searchTerm = null;
          if (lastCheckedDate == null) {
            messages = folder.getMessages(); // If the first time this account
            // check mail then it will fetch all
            // messages
          } else {
            searchTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            SentDateTerm dateTerm = new SentDateTerm(ComparisonTerm.GT, lastCheckedDate);
            searchTerm = new OrTerm(searchTerm, dateTerm);
            messages = folder.search(searchTerm);
          }

          // TODO : we need to improve this part ... in separated methods (but maybe later :-) )
          // to calculate correctly the number of new messages.
          // what I have done is just temporary
          // before adding a message to the map, make sure that the message is not
          // duplicated IN THE SAME FOLDER
          // (don't forget duplicate in the SAME folder
          String folderId = Utils.createFolderId(accountId, incomingFolder, false);

          javax.mail.Message[] filteredMsg;
          // Loop all filters to find the destination of new message.
          List<MessageFilter> filters = getFilters(sProvider, username, accountId);
          SearchTerm st;

          for (MessageFilter filter : filters) {
            st = getSearchTerm(searchTerm, filter);
            filteredMsg = folder.search(st);
            List<String> fl;
            boolean afterTime = false ;
            for (int k = 0; k < filteredMsg.length; k++) {
              if (msgMap.containsKey(filteredMsg[k])) {
                fl = msgMap.get(filteredMsg[k]);
                fl.add(filter.getId());
                if (lastCheckedDate == null) {
                  msgMap.put(filteredMsg[k], fl);
                } else if (afterTime || !(isImap && !MimeMessageParser.getReceivedDate(filteredMsg[k]).getTime().after(lastCheckedDate))) {
                  afterTime = true ;
                  msgMap.put(filteredMsg[k], fl);
                }
              } else {
                fl = new ArrayList<String>();
                fl.add(filter.getId());
                if (lastCheckedDate == null) {
                  msgMap.put(filteredMsg[k], fl);
                } else if (afterTime || !(isImap && !MimeMessageParser.getReceivedDate(filteredMsg[k]).getTime().after(lastCheckedDate))) {
                  msgMap.put(filteredMsg[k], fl);
                }
              }
            }
          }

          boolean afterTime = false ;
          for (int l = 0; l < messages.length; l++) {
            if (!msgMap.containsKey(messages[l])) {
              if (lastCheckedDate == null) {
                msgMap.put(messages[l], null);
              } else if (afterTime || !(isImap && !MimeMessageParser.getReceivedDate(messages[l]).getTime().after(lastCheckedDate))) {
                afterTime = true ;
                msgMap.put(messages[l], null);
              } 
            } else {
              List<String> temp = msgMap.get(messages[l]);
              msgMap.remove(messages[l]);
              msgMap.put(messages[l], temp);
            }
          }
          totalNew = msgMap.size();

          logger.warn("=============================================================");
          logger.warn("=============================================================");
          logger.warn(" #### Folder contains " + totalNew + " messages !");

          tt1 = System.currentTimeMillis();
          boolean saved = false ;

          if (totalNew > 0) {
            boolean leaveOnServer = (isPop3 && Boolean.valueOf(account.getPopServerProperties().get(
                Utils.SVR_POP_LEAVE_ON_SERVER)));
            boolean markAsDelete = (isImap && Boolean.valueOf(account.getImapServerProperties().get(
                Utils.SVR_IMAP_MARK_AS_DELETE)));

            boolean deleteOnServer = (isPop3 && !leaveOnServer) || (isImap && markAsDelete);

            info.setTotalMsg(totalNew);
            int i = 0;
            SpamFilter spamFilter = getSpamFilter(sProvider, username, account.getId());
            Folder storeFolder = storage_.getFolder(sProvider, username, account.getId(), folderId);
            if (storeFolder == null) {
              folderId = Utils.createFolderId(accountId, incomingFolder, true);
              Folder storeUserFolder = storage_.getFolder(sProvider, username, account.getId(),
                  folderId);
              if (storeUserFolder != null)
                storeFolder = storeUserFolder;
              else
                storeFolder = new Folder();
              storeFolder.setId(folderId);
              storeFolder.setName(incomingFolder);
              storeFolder.setLabel(incomingFolder);
              storeFolder.setPersonalFolder(true);
              storage_.saveFolder(sProvider, username, account.getId(), storeFolder);
            }
            javax.mail.Message msg;
            List<String> filterList;
            List<javax.mail.Message> msgList = new ArrayList<javax.mail.Message>(msgMap.keySet()) ;
            while (i < totalNew) {
              
              if(info.isRequestStop()) {
                if (logger.isDebugEnabled()) {
                  logger.debug("Stop requested on checkmail for " + account.getId());
                }
                removeCheckingInfo(username, accountId);
                break;
              }
              
              msg = msgList.get(i);
              logger.warn("Fetching message " + (i + 1) + " ...");
              /* JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
            Reminder rmdObj = new Reminder() ;   
            rmdObj.setFromDateTime(new Date()) ;
            rmdObj.setSummary("Fetching message " + (i + 1) + "/" + totalNew) ;
            JsonValue json = generatorImpl.createJsonObject(rmdObj);
            continuation.sendMessage(username, "/eXo/Application/mail/messages", json);*/
              checkingLog_.get(key).setFetching(i + 1);
              checkingLog_.get(key).setStatusMsg("Fetching message " + (i + 1) + "/" + totalNew);
              t1 = System.currentTimeMillis();
              filterList = msgMap.get(msg);
              try {
                saved = storage_.saveMessage(sProvider, username, account.getId(), msg,
                    folderId, spamFilter, filterList);
                if (saved) {
                  msg.setFlag(Flags.Flag.SEEN, true);
                  if (deleteOnServer)
                    msg.setFlag(Flags.Flag.DELETED, true);
                  
                  
                  account.setLastCheckedDate(MimeMessageParser.getReceivedDate(msg).getTime());
                }
              } catch (Exception e) {
                checkingLog_.get(key).setStatusMsg("An error occurs while fetching messsge " + i);
                e.printStackTrace();
                i++;
                continue;
              }
              i++;
              t2 = System.currentTimeMillis();
              logger.warn("Message " + i + " saved : " + (t2 - t1) + " ms");
            }

            tt2 = System.currentTimeMillis();
            logger.warn(" ### Check mail finished total took: " + (tt2 - tt1) + " ms");
          }

          if (!account.isSavePassword())
            account.setIncomingPassword("");
          updateAccount(sProvider, username, account);

          folder.close(true);
          store.close();
          if (totalNew == 0)
            info.setStatusMsg("There is no new messages !");
          else
            info.setStatusMsg("Check mail finished !");
          
        }
        info.setStatusCode(CheckingInfo.FINISHED_CHECKMAIL_STATUS);

        logger.warn("/////////////////////////////////////////////////////////////");
        logger.warn("/////////////////////////////////////////////////////////////");
      } catch (Exception e) {
        logger.error("Error while checking emails for " + username + " on account " + accountId, e);
      }
    }
    return messageList;
  }

  public SearchTerm getSearchTerm(SearchTerm sTerm, MessageFilter filter) throws Exception {
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

  public void createAccount(SessionProvider sProvider, String username, Account account)
  throws Exception {
    saveAccount(sProvider, username, account, true);
  }

  public List<Folder> getFolders(SessionProvider sProvider, String username, String accountId)
  throws Exception {
    return storage_.getFolders(sProvider, username, accountId);
  }

  public List<Folder> getFolders(SessionProvider sProvider, String username, String accountId,
      boolean isPersonal) throws Exception {
    List<Folder> folders = new ArrayList<Folder>();
    for (Folder folder : storage_.getFolders(sProvider, username, accountId))
      if (isPersonal) {
        if (folder.isPersonalFolder())
          folders.add(folder);
      } else {
        if (!folder.isPersonalFolder())
          folders.add(folder);
      }
    return folders;
  }

  public void addTag(SessionProvider sProvider, String username, String accountId, Tag tag)
  throws Exception {
    storage_.addTag(sProvider, username, accountId, tag);
  }

  public void addTag(SessionProvider sProvider, String username, String accountId,
      List<Message> messages, List<Tag> tag) throws Exception {
    storage_.addTag(sProvider, username, accountId, messages, tag);
  }

  public List<Tag> getTags(SessionProvider sProvider, String username, String accountId)
  throws Exception {
    return storage_.getTags(sProvider, username, accountId);
  }

  public Tag getTag(SessionProvider sProvider, String username, String accountId, String tagId)
  throws Exception {
    return storage_.getTag(sProvider, username, accountId, tagId);
  }

  public void removeTagsInMessages(SessionProvider sProvider, String username, String accountId,
      List<Message> msgList, List<String> tagIdList) throws Exception {
    storage_.removeTagsInMessages(sProvider, username, accountId, msgList, tagIdList);
  }

  public void removeTag(SessionProvider sProvider, String username, String accountId, String tag)
  throws Exception {
    storage_.removeTag(sProvider, username, accountId, tag);
  }

  public void updateTag(SessionProvider sProvider, String username, String accountId, Tag tag)
  throws Exception {
    storage_.updateTag(sProvider, username, accountId, tag);
  }

  public List<Message> getMessageByTag(SessionProvider sProvider, String username,
      String accountId, String tagName) throws Exception {
    return storage_.getMessageByTag(sProvider, username, accountId, tagName);
  }

  public MessagePageList getMessagePagelistByTag(SessionProvider sProvider, String username,
      String accountId, String tagId) throws Exception {
    MessageFilter filter = new MessageFilter("Filter By Tag");
    filter.setAccountId(accountId);
    filter.setTag(new String[] { tagId });
    return getMessagePageList(sProvider, username, filter);
  }

  public MessagePageList getMessagePageListByFolder(SessionProvider sProvider, String username,
      String accountId, String folderId) throws Exception {
    MessageFilter filter = new MessageFilter("Filter By Folder");
    filter.setAccountId(accountId);
    filter.setFolder(new String[] { folderId });
    return getMessagePageList(sProvider, username, filter);
  }

  public MailSetting getMailSetting(SessionProvider sProvider, String username) throws Exception {
    return storage_.getMailSetting(sProvider, username);
  }

  public void saveMailSetting(SessionProvider sProvider, String username, MailSetting newSetting)
  throws Exception {
    storage_.saveMailSetting(sProvider, username, newSetting);
  }

  public boolean importMessage(SessionProvider sProvider, String username, String accountId,
      String folderId, InputStream inputStream, String type) throws Exception {
    return emlImportExport_.importMessage(sProvider, username, accountId, folderId, inputStream,
        type);
  }

  public OutputStream exportMessage(SessionProvider sProvider, String username, String accountId,
      Message message) throws Exception {
    return emlImportExport_.exportMessage(sProvider, username, accountId, message);
  }

  public SpamFilter getSpamFilter(SessionProvider sProvider, String username, String accountId)
  throws Exception {
    return storage_.getSpamFilter(sProvider, username, accountId);
  }

  public void saveSpamFilter(SessionProvider sProvider, String username, String accountId,
      SpamFilter spamFilter) throws Exception {
    storage_.saveSpamFilter(sProvider, username, accountId, spamFilter);
  }

  public void toggleMessageProperty(SessionProvider sProvider, String username, String accountId,
      List<Message> msgList, String property) throws Exception {
    storage_.toggleMessageProperty(sProvider, username, accountId, msgList, property);
  }

  public List<AccountData> getAccountDatas(SessionProvider sProvider) throws Exception {
    return null;
  }

  public String getFolderHomePath(SessionProvider sProvider, String username, String accountId)
  throws Exception {
    return storage_.getFolderHomePath(sProvider, username, accountId);
  }

  public void saveFolder(SessionProvider sProvider, String username, String accountId,
      String parentId, Folder folder) throws Exception {
    storage_.saveFolder(sProvider, username, accountId, parentId, folder);
  }

  public List<Folder> getSubFolders(SessionProvider sProvider, String username, String accountId,
      String parentPath) throws Exception {
    return storage_.getSubFolders(sProvider, username, accountId, parentPath);
  }

  public List<Message> getReferencedMessages(SessionProvider sProvider, String username,
      String accountId, String msgPath) throws Exception {
    return storage_.getReferencedMessages(sProvider, username, accountId, msgPath);
  }

  public Account getDefaultAccount(SessionProvider sProvider, String username) throws Exception {
    MailSetting mailSetting = storage_.getMailSetting(sProvider, username);
    String defaultAccount = mailSetting.getDefaultAccount();
    Account account = null;
    if (defaultAccount != null) {
      account = getAccountById(sProvider, username, defaultAccount);
    } else {
      List<Account> accList = getAccounts(sProvider, username);
      if (accList.size() > 0)
        account = getAccounts(sProvider, username).get(0);
    }
    return account;
  }

  public Message loadAttachments(SessionProvider sProvider, String username, String accountId,
      Message msg) throws Exception {
    return storage_.loadAttachments(sProvider, username, accountId, msg);
  }


}