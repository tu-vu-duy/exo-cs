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

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.mail.connection.Connector;
import org.exoplatform.mail.connection.impl.ImapConnector;
import org.exoplatform.mail.service.Account;
import org.exoplatform.mail.service.Folder;
import org.exoplatform.mail.service.Message;
import org.exoplatform.mail.service.Utils;

import com.sun.mail.imap.IMAPFolder;

public class TestConnectorService extends BaseMailTestCase {
  public TestConnectorService() throws Exception {
    super();
  }

  public static final String TEXT_PLAIN = "text/plain".intern();

  public static final String TEXT_HTML  = "text/html".intern();

  public void setUp() throws Exception {
    super.setUp();
  }

  public Account createAccountObj(String protocol) {
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

    if (Utils.POP3.equals(protocol)) {
      account.setDescription("Create " + protocol + " account");
      account.setEmailAddress("exomailtest@gmail.com");
      account.setEmailReplyAddress("exomailtest@gmail.com");
      account.setIncomingHost("pop.gmail.com");
      account.setIncomingPassword("exoadmin");
      account.setIncomingPort("995");
      account.setIncomingSsl(true);
      account.setIncomingUser("exomailtest@gmail.com");
      account.setIsSavePassword(true);
      account.setLabel("exomail test account");
      account.setOutgoingHost("smtp.gmail.com");
      account.setOutgoingPort("465");
      account.setPlaceSignature("exomailtest pop");
    } else if (Utils.IMAP.equals(protocol)) {
      account.setDescription("Create " + protocol + " account");
      account.setEmailAddress("exomailtest@gmail.com");
      account.setEmailReplyAddress("exomailtest@gmail.com");
      account.setIncomingHost("imap.gmail.com");
      account.setIncomingPassword("exoadmin");
      account.setIncomingPort("993");
      account.setIncomingSsl(true);
      account.setIncomingUser("exomailtest@gmail.com");
      account.setIsSavePassword(true);
      account.setLabel("exomailtest test account");
      account.setOutgoingHost("smtp.gmail.com");
      account.setOutgoingPort("465");
      account.setOutgoingSsl(true);
      account.setPlaceSignature("exosevice imap");
    }
    account.setIsOutgoingAuthentication(true);
    account.setUseIncomingForAuthentication(true);
    return account;
  }

  public void testCreateFolder() throws Exception {
    Account account = createAccountObj(Utils.IMAP);
    Folder folder = new Folder();
    folder.setId("testID");
    folder.setName("testFolder");
    Connector connector = new ImapConnector(account);
    IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(folder);
    folder.setURLName(imapFolder.getURLName().toString());

    assertNotNull(imapFolder);
    assertEquals(folder.getName(), imapFolder.getName());
    assertEquals(true, connector.deleteFolder(folder));
  }

  public void testCreateFolderInParent() throws Exception {
    Account account = createAccountObj(Utils.IMAP);

    Folder parentFolder = new Folder();
    parentFolder.setName("parentFolder2");
    Connector connector = new ImapConnector(account);

    IMAPFolder imapParentFolder = (IMAPFolder) connector.createFolder(parentFolder);
    parentFolder.setURLName(imapParentFolder.getURLName().toString());

    assertEquals(parentFolder.getName(), imapParentFolder.getName());
    assertNotNull(imapParentFolder);

    Folder childFolder = new Folder();
    childFolder.setName("testFolder12");
    IMAPFolder imapChildFolder = (IMAPFolder) connector.createFolder(parentFolder, childFolder);
    childFolder.setURLName(imapChildFolder.getURLName().toString());

    assertNotNull("Child folder is NUL", imapChildFolder);
    assertEquals("Parent and child folder is NOT SAME",
                 childFolder.getName(),
                 imapChildFolder.getName());

    assertEquals(true, connector.deleteFolder(childFolder));
    assertEquals(true, connector.deleteFolder(parentFolder));
  }

  public void testRenameFolder() throws Exception {
    List<Message> messageList = new ArrayList<Message>();
    for (int i = 0; i < 4; i++) {
      Message msg = new Message();
      msg.setFrom("mail" + i + "@gmail.com");
      msg.setContentType(TEXT_HTML);
      msg.setMessageCc("nguyenngocduy1981@gmail.com");
      msg.setMessageBcc("nguyenngocduy1981@gmail.com");
      msg.setMessageBody("This is test in item " + i);

      messageList.add(msg);

    }

    Account account = createAccountObj(Utils.IMAP);
    Folder folder = new Folder();
    folder.setName("rootFolder");
    Connector connector = new ImapConnector(account);
    IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(folder);
    folder.setURLName(imapFolder.getURLName().toString());

    List<Message> createdMessageList = connector.createMessage(messageList, folder);
    assertNotNull("Can not create MESSAGE in FOLDER", createdMessageList);

    assertNotNull("Can not create folder", imapFolder);
    assertEquals(folder.getName(), imapFolder.getName());

    Folder renamedFolder = connector.renameFolder("newName", folder);
    assertNotNull("Can not rename folder", renamedFolder);
    assertEquals("newName", renamedFolder.getName());

    assertEquals(true, connector.deleteFolder(renamedFolder));
  }

  // Error
  // public void testEmptyFolder() throws Exception {
  // List<Message> messageList = new ArrayList<Message>();
  // for (int i = 0; i < 4; i++) {
  // Message msg = new Message();
  // msg.setFrom("exomailtest@gmail.com");
  // msg.setContentType(TEXT_HTML);
  // msg.setMessageCc("nguyenngocduy1981@gmail.com");
  // msg.setMessageBcc("nguyenngocduy1981@gmail.com");
  // msg.setMessageBody("This is test in item " + i);
  //
  // messageList.add(msg);
  //
  // }
  //
  // Account account = createAccountObj(Utils.IMAP);
  // Folder folder = new Folder();
  // folder.setName("emptyFolderName");
  // Connector connector = new ImapConnector(account);
  // IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(folder);
  // folder.setURLName(imapFolder.getURLName().toString());
  //
  // List<Message> createdMessageList = connector.createMessage(messageList,
  // folder);
  // assertNotNull("Can not create MESSAGE in FOLDER", createdMessageList);
  //
  // int empty = connector.emptyFolder(folder);
  // assertEquals(messageList.size(), empty);
  //
  // // assertEquals(true, connector.deleteFolder(folder));
  // }

  // Error
  // public void testDeleteMessage() throws Exception {
  // List<Message> messageList = new ArrayList<Message>();
  // for (int i = 0; i < 3; i++) {
  // Message msg = new Message();
  // msg.setFrom("javaMail" + i + "@gmail.com");
  // msg.setContentType(TEXT_HTML);
  // msg.setSubject("SUBJECT thu " + i);
  // msg.setMessageTo("exomailtest@gmail.com");
  // msg.setMessageCc("exomailtest@gmail.com");
  // msg.setMessageBcc("exomailtest@gmail.com");
  // msg.setMessageBody("Day la phan TESt voi item: " + i);
  // messageList.add(msg);
  // }
  //    
  // Account account = createAccountObj(Utils.IMAP);
  // Folder folder = new Folder();
  // folder.setName("TAM");
  // Connector connector = new ImapConnector(account);
  // IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(folder);
  // folder.setURLName(imapFolder.getURLName().toString());
  //    
  // List<Message> messageListForDelete =
  // connector.createMessageDuy(messageList, folder);
  // assertNotNull("Null roi", messageListForDelete);
  // assertEquals(3, messageListForDelete.size());
  // //
  // // // assertTrue("Not open", imapFolder.isOpen());
  // //
  // // for(Message msg : messageList){
  // // System.out.println("CLIENT: " +msg.getUID() +"--" + msg.getFrom() +"--"
  // + msg.getContentType() + "--" + msg.getMessageCc() + "--" +
  // msg.getMessageBcc() + "--" + msg.getMessageBody());
  // // }
  //
  // boolean deleted = connector.deleteMessage(messageListForDelete, folder);
  // assertTrue("Chua duoc xoa", deleted);
  // // assertEquals(true, connector.deleteFolder(folder));
  // }

  // Error
  // public void testMoveMessage() throws Exception {
  // List<Message> messageList = new ArrayList<Message>();
  // for (int i = 0; i < 4; i++) {
  // Message msg = new Message();
  // msg.setFrom("exomailtest"+ i +"@gmail.com");
  // msg.setContentType(TEXT_HTML);
  // msg.setMessageCc("nguyenngocduy1981@gmail.com");
  // msg.setMessageBcc("nguyenngocduy1981@gmail.com");
  // msg.setMessageBody("This is test " + i);
  // msg.setMessageBody("This is test in item " + i);
  //
  // messageList.add(msg);
  //
  // }
  //
  // Account account = createAccountObj(Utils.IMAP);
  // Folder currentFolder = new Folder();
  // currentFolder.setId("currentFolderID121");
  // currentFolder.setName("currentFolderName121");
  // Connector connector = new ImapConnector(account);
  // IMAPFolder imapCurrentFolder = (IMAPFolder)
  // connector.createFolder(currentFolder);
  // currentFolder.setURLName(imapCurrentFolder.getURLName().toString());
  // List<Message> createdMessageList = connector.createMessage(messageList,
  // currentFolder);
  //
  // if(!imapCurrentFolder.isOpen())imapCurrentFolder.open(javax.mail.Folder.READ_WRITE);
  // assertNotNull("Created message is NULL", createdMessageList);
  // assertTrue("Not open", imapCurrentFolder.isOpen());
  // assertEquals(4, imapCurrentFolder.getMessages().length);
  //
  // Folder destinationFolder = new Folder();
  // destinationFolder.setId("destinationFolderID121");
  // destinationFolder.setName("destinationFolderName121");
  // IMAPFolder imapDestinationFolder = (IMAPFolder)
  // connector.createFolder(destinationFolder);
  // destinationFolder.setURLName(imapDestinationFolder.getURLName().toString());
  //
  // List<Message> resultMessageList = connector.moveMessage(createdMessageList,
  // currentFolder,
  // destinationFolder);
  // assertNull("Reuslt list is NUL", resultMessageList);
  //
  // assertEquals(0, imapCurrentFolder.getMessages().length);
  // // assertEquals(4, imapDestinationFolder.getMessages().length);
  // assertEquals(4, resultMessageList.size());
  //
  // System.out.println("Cur: " + imapCurrentFolder.getMessageCount());
  // System.out.println("Des: " + imapDestinationFolder.getMessageCount());
  //
  // // assertNotNull(resultMessageList);
  // //
  // assertEquals(true, connector.deleteFolder(currentFolder));
  // assertEquals(true, connector.deleteFolder(destinationFolder));
  // }

  public void testMarkAsRead() throws Exception {
    List<Message> messageList = new ArrayList<Message>();
    for (int i = 0; i < 4; i++) {
      Message msg = new Message();
      msg.setFrom("exomailtest@gmail.com");
      msg.setContentType(TEXT_HTML);
      msg.setMessageCc("nguyenngocduy1981@gmail.com");
      msg.setMessageBcc("nguyenngocduy1981@gmail.com");
      msg.setMessageBody("This is test");
      msg.setSubject("This is subject " + i);

      messageList.add(msg);

    }

    Account account = createAccountObj(Utils.IMAP);
    Folder folder = new Folder();
    folder.setId("folderReadID");
    folder.setName("foldeReadrName");
    Connector connector = new ImapConnector(account);
    IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(folder);
    folder.setURLName(imapFolder.getURLName().toString());
    List<Message> list = connector.createMessage(messageList, folder);

    assertNotNull("Created Message list is NULL", list);
    assertEquals(4, list.size());
    boolean asRead = connector.markAsRead(list, folder);
    assertTrue("Can not mark message as READ", asRead);
    assertEquals(true, connector.deleteFolder(folder));
  }

  public void testMarkAsUnRead() throws Exception {
    List<Message> messageList = new ArrayList<Message>();
    for (int i = 0; i < 4; i++) {
      Message msg = new Message();
      msg.setFrom("exomailtest@gmail.com");
      msg.setContentType(TEXT_HTML);
      msg.setMessageCc("nguyenngocduy1981@gmail.com");
      msg.setMessageBcc("nguyenngocduy1981@gmail.com");
      msg.setMessageBody("This is test");
      msg.setSubject("This is subject " + i);

      messageList.add(msg);

    }

    Account account = createAccountObj(Utils.IMAP);
    Folder folder = new Folder();
    folder.setId("folderUnReadID");
    folder.setName("foldeUnReadrName");
    Connector connector = new ImapConnector(account);
    IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(folder);
    folder.setURLName(imapFolder.getURLName().toString());
    List<Message> list = connector.createMessage(messageList, folder);

    assertNotNull("Created Message list is NULL", list);
    assertEquals(4, list.size());
    boolean asUnRead = connector.markAsUnread(list, folder);
    assertTrue("Can not mark message as UnREAD", asUnRead);
    assertEquals(true, connector.deleteFolder(folder));
  }

  public void testSetIsStared() throws Exception {
    List<Message> messageList = new ArrayList<Message>();
    for (int i = 0; i < 4; i++) {
      Message msg = new Message();
      msg.setFrom("exomailtest@gmail.com");
      msg.setContentType(TEXT_HTML);
      msg.setMessageCc("nguyenngocduy1981@gmail.com");
      msg.setMessageBcc("nguyenngocduy1981@gmail.com");
      msg.setMessageBody("This is test");
      msg.setSubject("This is subject " + i);

      messageList.add(msg);

    }

    Account account = createAccountObj(Utils.IMAP);
    Folder folder = new Folder();
    folder.setId("setIsStaredID");
    folder.setName("setIsStaredName");
    Connector connector = new ImapConnector(account);
    IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(folder);
    folder.setURLName(imapFolder.getURLName().toString());
    List<Message> list = connector.createMessage(messageList, folder);

    assertNotNull("Created Message list is NULL", list);
    assertEquals(4, list.size());
    boolean isStared = connector.setIsStared(list, true, folder);
    assertTrue("Can not set star", isStared);
    assertEquals(true, connector.deleteFolder(folder));
  }

  public void testSetIsNotStared() throws Exception {
    List<Message> messageList = new ArrayList<Message>();
    for (int i = 0; i < 4; i++) {
      Message msg = new Message();
      msg.setFrom("exomailtest@gmail.com");
      msg.setContentType(TEXT_HTML);
      msg.setMessageCc("nguyenngocduy1981@gmail.com");
      msg.setMessageBcc("nguyenngocduy1981@gmail.com");
      msg.setMessageBody("This is test");
      msg.setSubject("This is subject " + i);

      messageList.add(msg);

    }

    Account account = createAccountObj(Utils.IMAP);
    Folder folder = new Folder();
    folder.setId("setIsNotStaredID");
    folder.setName("setIsNotStaredName");
    Connector connector = new ImapConnector(account);
    IMAPFolder imapFolder = (IMAPFolder) connector.createFolder(folder);
    folder.setURLName(imapFolder.getURLName().toString());
    List<Message> list = connector.createMessage(messageList, folder);

    assertNotNull("Created Message list is NULL", list);
    assertEquals(4, list.size());
    boolean isNotStared = connector.setIsStared(list, false, folder);
    assertTrue("Message is Stared(unexpected)", isNotStared);
    assertEquals(true, connector.deleteFolder(folder));
  }
}
