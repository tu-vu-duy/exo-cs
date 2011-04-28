/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.mail.service.test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLSocketFactory;
import java.util.Random;

import javax.mail.AuthenticationFailedException;

import org.exoplatform.mail.connection.Connector;
import org.exoplatform.mail.connection.impl.ImapConnector;
import org.exoplatform.mail.service.Account;
import org.exoplatform.mail.service.Folder;
import org.exoplatform.mail.service.Message;
import org.exoplatform.mail.service.Utils;
import org.exoplatform.mail.service.impl.ExoMailSSLSocketFactory;
import com.sun.mail.imap.IMAPFolder;


public class TestConnectorService extends BaseMailTestCase {
  public TestConnectorService() throws Exception {
    super();
  }

  private MailProvider prv_ = new MailProvider(MailProvider.GMAIL);

  public static final String TEXT_PLAIN = "text/plain".intern();

  public static final String TEXT_HTML  = "text/html".intern();

  public void setUp() throws Exception {
    super.setUp();
  }
  public SSLSocketFactory getSSLSocketFactory(String protocol){
	      ExoMailSSLSocketFactory sslSocketFact = new ExoMailSSLSocketFactory(protocol);
	      return sslSocketFact.getSSLSocketFactory();
  }

  /**
   * Simple provider class for easy test 
   * @author tuan_pham
   *
   */
  public class MailProvider {
    public String TYPE = null;
    public final static String GMAIL = "GMAIL";
    public final static String GMX = "GMX";
    public final static String EXO = "EXO";

    /**
     * Simple constructor for provider 
     * @param name : the provider name now just support 3 providers by configuration
     */
    public MailProvider(String name){
      if(GMAIL.equalsIgnoreCase(name)) TYPE = GMAIL;
      else if (GMX.equalsIgnoreCase(name)) TYPE = GMX;
      else if(EXO.equalsIgnoreCase(name)) TYPE = EXO;
    }

  }

  private Message createMessage() {
    Message msg = new Message();
    msg.setFrom("exomailtest@gmail.com");
    msg.setContentType(TEXT_HTML);
    msg.setMessageCc("exomailtest@gmx.com");
    msg.setMessageBcc("exomailtest@gmx.com");
    msg.setMessageBody("This is test");
    msg.setSubject("This is subject test number " + new Random().nextLong());
    return msg ;
  }

  public Account createAccountObj(String protocol, MailProvider prv) {
    Account account = new Account();
    Folder folder = new Folder();
    folder.setName("inbox");
    folder.setPersonalFolder(false);
    List<Folder> folders = new ArrayList<Folder>();
    folders.add(folder);
    account.setCheckedAuto(false);
    account.setEmptyTrashWhenExit(false);
    account.setIncomingFolder("inbox");
    account.setProtocol(protocol);
    if( prv.TYPE.equalsIgnoreCase(MailProvider.GMX)) {
      if (Utils.POP3.equals(protocol)) {
        account.setDescription("Create " + protocol + " account to " + prv.TYPE);
        account.setEmailAddress("exomailtest@gmx.com");
        account.setEmailReplyAddress("exomailtest@gmx.com");
        account.setIncomingHost("pop.gmx.com");
        account.setIncomingPassword("exoadmin");
        account.setIncomingPort("110");
        account.setIncomingSsl(false);
        account.setIncomingUser("exomailtest@gmx.com");
        account.setIsSavePassword(true);
        account.setLabel("exomail test account");
        account.setOutgoingHost("mail.gmx.com");
        account.setOutgoingSsl(false);
        account.setOutgoingPort("25");
        account.setPlaceSignature("exomailtest pop");
      } else if (Utils.IMAP.equals(protocol)) {
        account.setDescription("Create " + protocol + " account");
        account.setEmailAddress("exomailtest@gmx.com");
        account.setEmailReplyAddress("exomailtest@gmx.com");
        account.setIncomingHost("imap.gmx.com");
        account.setIncomingPassword("exoadmin");
        account.setIncomingPort("143");
        account.setIncomingSsl(false);
        account.setIncomingUser("exomailtest@gmx.com");
        account.setIsSavePassword(true);
        account.setLabel("exomailtest test account");
        account.setOutgoingHost("mail.gmx.com");
        account.setOutgoingPort("25");
        account.setOutgoingSsl(false);
        account.setPlaceSignature("exosevice imap");
      }
    } else  if(prv.TYPE.equalsIgnoreCase(MailProvider.GMAIL)) {
      if (Utils.POP3.equals(protocol)) {
        account.setDescription("Create " + protocol + " account to " + prv.TYPE);
        account.setEmailAddress("exomailtest@gmail.com");
        account.setEmailReplyAddress("exomailtest@gmail.com");
        account.setIncomingHost("pop.gmail.com");
        account.setIncomingPassword("exoadmin");
        account.setIncomingPort("995");
        account.setIncomingSsl(true);
        account.setIncomingUser("exomailtest");
        account.setIsSavePassword(true);
        account.setLabel("exomail test account");
        account.setOutgoingHost("smtp.gmail.com");
        account.setOutgoingPort("993");
        account.setIncomingSsl(true);
        account.setPlaceSignature("exomailtest pop");
      } else if (Utils.IMAP.equals(protocol)) {
        account.setDescription("Create " + protocol + " account to " + prv.TYPE);
        account.setEmailAddress("exomailtest@gmail.com");
        account.setEmailReplyAddress("exomailtest@gmail.com");
        account.setIncomingHost("imap.gmail.com");
        account.setIncomingPassword("exoadmin");
        account.setIncomingPort("993");
        account.setIncomingSsl(true);
        account.setIncomingUser("exomailtest");
        account.setIsSavePassword(true);
        account.setLabel("exomailtest test account");
        account.setOutgoingHost("smtp.gmail.com");
        account.setOutgoingPort("993");
        account.setOutgoingSsl(true);
        account.setPlaceSignature("exosevice imap");
      }
    } else if(prv.TYPE.equalsIgnoreCase(MailProvider.EXO) ) {
      if (Utils.POP3.equals(protocol)) {
        account.setDescription("Create " + protocol + " account to " + prv.TYPE);
        account.setEmailAddress("demo@exoplatform.vn");
        account.setEmailReplyAddress("demo@exoplatform.vn");
        account.setIncomingHost("pop.exoplatform.vn");
        account.setIncomingPassword("exoadmin");
        account.setIncomingPort("110");
        account.setIncomingSsl(false);
        account.setIncomingUser("demo@exoplatform.vn");
        account.setIsSavePassword(true);
        account.setLabel("exo demo mail test account");
        account.setOutgoingHost("smtp.exoplatform.vn");
        account.setOutgoingPort("25");
        account.setIncomingSsl(false);
        account.setPlaceSignature("exomailtest pop");
      } else if (Utils.IMAP.equals(protocol)) {
        account.setDescription("Create " + protocol + " account to " + prv.TYPE);
        account.setEmailAddress("demo@exoplatform.vn");
        account.setEmailReplyAddress("demo@exoplatform.vn");
        account.setIncomingHost("imap.exoplatform.vn");
        account.setIncomingPassword("exoadmin");
        account.setIncomingPort("143");
        account.setIncomingSsl(false);
        account.setIncomingUser("demo");
        account.setIsSavePassword(true);
        account.setLabel("exomailtest test account");
        account.setOutgoingHost("smtp.exoplatform.vn");
        account.setOutgoingPort("25");
        account.setOutgoingSsl(false);
        account.setPlaceSignature("exosevice imap");
      }
    }

    account.setIsOutgoingAuthentication(true);
    account.setUseIncomingForAuthentication(true);
    return account;
  }

  public void testCreateFolder() throws Exception {
    Account account = createAccountObj(Utils.IMAP, prv_);
    Folder folder = new Folder();
    folder.setId("testID");
    folder.setName("testFolder");
    Connector connector = new ImapConnector(account, this.getSSLSocketFactory(null));
    if(connector != null){
      javax.mail.Folder imapFolder = connector.createFolder(folder);
      folder.setURLName(imapFolder.getURLName().toString());

      assertNotNull(imapFolder);     
    } else {
      System.out.println("\n\n connector is null, check configuration !");
    }
  }

  public void testCreateFolderInParent() throws Exception {
    Account account = createAccountObj(Utils.IMAP, prv_);

    Folder parentFolder = new Folder();
    parentFolder.setName("parentFolder2");
    Connector connector = new ImapConnector(account, this.getSSLSocketFactory(null));
    if(connector != null) {

      javax.mail.Folder imapParentFolder = connector.createFolder(parentFolder);
      parentFolder.setURLName(imapParentFolder.getURLName().toString());

      assertNotNull(imapParentFolder);

      Folder childFolder = new Folder();
      childFolder.setName("testFolder12");
      javax.mail.Folder imapChildFolder = connector.createFolder(parentFolder, childFolder);
      childFolder.setURLName(imapChildFolder.getURLName().toString());

      assertNotNull("Child folder is NUL", imapChildFolder);
    } else {
      System.out.println("\n\n connector is null, check configuration !");
    }
  }

  //TODO problem with this function from gmail and gmx, this one only pass when use local mail server in vietnam
  // have to check more
  public void testRenameFolder() throws Exception {
    Account account = createAccountObj(Utils.IMAP, prv_);
    Folder folder = new Folder();
    folder.setName("rootFolder");
    Connector connector = new ImapConnector(account, this.getSSLSocketFactory(null));
    if (connector != null) {
      IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(folder);
      folder.setURLName(imapFolder.getURLName().toString());

      assertNotNull("Can not create folder", imapFolder);

      Folder renamedFolder = connector.renameFolder("newName", folder);
      assertNotNull("Can not rename folder", renamedFolder);
      assertEquals("newName", renamedFolder.getName());

    } else {
      System.out.println("\n\n connector is null, check configuration !");
    }
  }



  private Connector getConnector(Account acc) {
    try {  
      return new ImapConnector(acc, this.getSSLSocketFactory(null));
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.out.println("\n\n check your net work connection or account configuration");
    }
    catch (AuthenticationFailedException e){
      e.printStackTrace();
      System.out.println("\n\n check your account configuration or server mail setting");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void testMarkAsRead() throws Exception {
    List<Message> messageList = new ArrayList<Message>();
    messageList.add(createMessage());

    Account account = createAccountObj(Utils.IMAP, prv_);
    Folder folder = new Folder();
    folder.setId("folderReadID");
    folder.setName("foldeReadrName");
    Connector connector = new ImapConnector(account, this.getSSLSocketFactory(null));
    if(connector != null) {
      javax.mail.Folder imapFolder = connector.createFolder(folder);
      folder.setURLName(imapFolder.getURLName().toString());
      List<Message> list = connector.createMessage(messageList, folder);

      assertNotNull("Created Message list is NULL", list);
      assertEquals(1, list.size());
      boolean asRead = connector.markAsRead(list, folder);
      assertTrue("Can not mark message as READ", asRead);
    } else {
      System.out.println("\n\n connector is null, check configuration !");
    }
  }

  public void testMarkAsUnRead() throws Exception {
    List<Message> messageList = new ArrayList<Message>();
    messageList.add(createMessage());

    Account account = createAccountObj(Utils.IMAP, prv_);
    Folder folder = new Folder();
    folder.setId("folderUnReadID");
    folder.setName("foldeUnReadrName");
    Connector connector = new ImapConnector(account, this.getSSLSocketFactory(null));
    if(connector != null) {
      javax.mail.Folder imapFolder = connector.createFolder(folder);
      folder.setURLName(imapFolder.getURLName().toString());
      List<Message> list = connector.createMessage(messageList, folder);

      assertNotNull("Created Message list is NULL", list);
      assertEquals(1, list.size());
      boolean asUnRead = connector.markAsUnread(list, folder);
      assertTrue("Can not mark message as UnREAD", asUnRead);

    } else {
      System.out.println("\n\n connector is null, check configuration !");
    }
  }

  public void testSetIsStared() throws Exception {
    List<Message> messageList = new ArrayList<Message>();
    messageList.add(createMessage());

    Account account = createAccountObj(Utils.IMAP, prv_);
    Folder folder = new Folder();
    folder.setId("setIsStaredID");
    folder.setName("setIsStaredName");
    Connector connector = new ImapConnector(account, this.getSSLSocketFactory(null));
    if(connector != null){
      javax.mail.Folder imapFolder = connector.createFolder(folder);
      folder.setURLName(imapFolder.getURLName().toString());
      List<Message> list = connector.createMessage(messageList, folder);

      assertNotNull("Created Message list is NULL", list);
      assertEquals(1, list.size());
      boolean isStared = connector.setIsStared(list, true, folder);
      assertTrue("Can not set star", isStared);
    } else {
      System.out.println("\n\n connector is null, check configuration !");
    }
  }

  public void testSetIsNotStared() throws Exception {
    List<Message> messageList = new ArrayList<Message>();
    messageList.add(createMessage());

    Account account = createAccountObj(Utils.IMAP, prv_);
    Folder folder = new Folder();
    folder.setId("setIsNotStaredID");
    folder.setName("setIsNotStaredName");
    Connector connector = new ImapConnector(account, this.getSSLSocketFactory(null));
    if(connector != null){
      javax.mail.Folder imapFolder = connector.createFolder(folder);
      folder.setURLName(imapFolder.getURLName().toString());
      List<Message> list = connector.createMessage(messageList, folder);

      assertNotNull("Created Message list is NULL", list);
      assertEquals(1, list.size());
      boolean isNotStared = connector.setIsStared(list, false, folder);
      assertTrue("Message is Stared(unexpected)", isNotStared);
    } else {
      System.out.println("\n\n connector is null, check configuration !");
    }
  }

}
