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
package org.exoplatform.mail.webui.popup;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PathNotFoundException;

import org.exoplatform.mail.MailUtils;
import org.exoplatform.mail.service.Account;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.MailSetting;
import org.exoplatform.mail.service.MessageFilter;
import org.exoplatform.mail.webui.UIMailPortlet;
import org.exoplatform.mail.webui.UIMessageArea;
import org.exoplatform.mail.webui.UIMessageList;
import org.exoplatform.mail.webui.UISelectAccount;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormTabPane;

/**
 * Created by The eXo Platform SARL
 * Author : Phung Nam
 *          phunghainam@gmail.com
 * Aug 10, 2007  
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/mail/webui/popup/UIMailSettings.gtmpl",
    events = {
        @EventConfig(listeners = UIMailSettings.SaveActionListener.class),
        @EventConfig(listeners = UIMailSettings.CancelActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIMailSettings.SelectTabActionListener.class, phase = Phase.DECODE)
    }
)
public class UIMailSettings extends UIFormTabPane implements UIPopupComponent {
  public static final String DEFAULT_ACCOUNT = "default-account".intern();
  public static final String NUMBER_MSG_PER_PAGE = "number-of-conversation".intern() ;
  public static final String PERIOD_CHECK_AUTO = "period-check-mail".intern() ;
  public static final String COMPOSE_MESSAGE_IN = "compose-message-in".intern();
  public static final String REPLY_WITH_ATTACH = "reply-message-with".intern();
  public static final String FORWARD_WITH_ATTACH = "forward-message-with".intern();
  public static final String SAVE_SENT_MESSAGE = "save-sent-message".intern();
  public static final String SENT_RECEIPT_ASKME = "askme".intern();
  public static final String SENT_RECEIPT_NEVER = "never".intern();
  public static final String SENT_RECEIPT_ALWAYS = "always".intern();
  public static final String RETURN_RECEIPTS="returnReceipts".intern();
  public static final String TAB_GENERAL = "general".intern();
  public static final String TAB_RETURN_RECEIPT = "return-receipt".intern();
  public static final String TAB_LAYOUT = "layout".intern();
  
  public UIMailSettings() throws Exception {    
    super("UIMailSettings");
  }
  
  public void init() throws Exception {
    UIFormInputSet  generalInputSet = new UIFormInputSet(TAB_GENERAL);
    generalInputSet.addUIFormInput(new UIFormSelectBox(DEFAULT_ACCOUNT, DEFAULT_ACCOUNT, getAccounts()));
    List<SelectItemOption<String>> numberPerPage = new ArrayList<SelectItemOption<String>>();
    for (int i = 1; i <= 7; i++) {
      numberPerPage.add(new SelectItemOption<String>(String.valueOf(10*i)));
    }
    generalInputSet.addUIFormInput(new UIFormSelectBox(NUMBER_MSG_PER_PAGE, NUMBER_MSG_PER_PAGE, numberPerPage));  
    List<SelectItemOption<String>> periodCheckAuto = new ArrayList<SelectItemOption<String>>();
    periodCheckAuto.add(new SelectItemOption<String>("Never", "period." + String.valueOf(MailSetting.NEVER_CHECK_AUTO)));
    periodCheckAuto.add(new SelectItemOption<String>("5 minutes", "period." + String.valueOf(MailSetting.FIVE_MINS)));
    periodCheckAuto.add(new SelectItemOption<String>("10 minutes", "period." + String.valueOf(MailSetting.TEN_MINS)));
    periodCheckAuto.add(new SelectItemOption<String>("20 minutes", "period." + String.valueOf(MailSetting.TWENTY_MINS)));
    periodCheckAuto.add(new SelectItemOption<String>("30 minutes", "period." + String.valueOf(MailSetting.THIRTY_MINS)));
    periodCheckAuto.add(new SelectItemOption<String>("1 hour", "period." + String.valueOf(MailSetting.ONE_HOUR)));
    generalInputSet.addUIFormInput(new UIFormSelectBox(PERIOD_CHECK_AUTO, PERIOD_CHECK_AUTO, periodCheckAuto));
    
    List<SelectItemOption<String>> useWysiwyg = new ArrayList<SelectItemOption<String>>();
    useWysiwyg.add(new SelectItemOption<String>("Rich text editor (HTML format)", "editor.true"));
    useWysiwyg.add(new SelectItemOption<String>("Plain text", "editor.false"));
    generalInputSet.addUIFormInput(new UIFormSelectBox(COMPOSE_MESSAGE_IN, COMPOSE_MESSAGE_IN, useWysiwyg));
    
    List<SelectItemOption<String>> replyWithAtt = new ArrayList<SelectItemOption<String>>();
    replyWithAtt.add(new SelectItemOption<String>("Original message included attachment", "replywith.true"));
    replyWithAtt.add(new SelectItemOption<String>("Original message", "replywith.false"));
    generalInputSet.addUIFormInput(new UIFormSelectBox(REPLY_WITH_ATTACH, REPLY_WITH_ATTACH, replyWithAtt));
    
    List<SelectItemOption<String>> forwardWithAtt = new ArrayList<SelectItemOption<String>>();
    forwardWithAtt.add(new SelectItemOption<String>("Original message included attachment", "forwardwith.true"));
    forwardWithAtt.add(new SelectItemOption<String>("Original message", "forwardwith.false"));
    generalInputSet.addUIFormInput(new UIFormSelectBox(FORWARD_WITH_ATTACH, FORWARD_WITH_ATTACH, forwardWithAtt));
    
    generalInputSet.addUIFormInput(new UIFormCheckBoxInput<Boolean>(SAVE_SENT_MESSAGE, SAVE_SENT_MESSAGE, false));
    
    UIMailLayoutTab layoutInputSet = new UIMailLayoutTab(TAB_LAYOUT);
    
    UIFormInputSet returnReceiptInputSet = new UIFormInputSet(TAB_RETURN_RECEIPT);
    List<SelectItemOption<String>> returnReceiptOptions = new ArrayList<SelectItemOption<String>>();
    returnReceiptOptions.add(new SelectItemOption<String>(SENT_RECEIPT_ASKME, SENT_RECEIPT_ASKME));
    returnReceiptOptions.add(new SelectItemOption<String>(SENT_RECEIPT_NEVER, SENT_RECEIPT_NEVER));
    returnReceiptOptions.add(new SelectItemOption<String>(SENT_RECEIPT_ALWAYS, SENT_RECEIPT_ALWAYS));
    returnReceiptInputSet.addUIFormInput((new UIFormRadioBoxInput(RETURN_RECEIPTS,RETURN_RECEIPTS, returnReceiptOptions)).setAlign(UIFormRadioBoxInput.VERTICAL_ALIGN));
    
    addUIFormInput(generalInputSet);
    addUIFormInput(returnReceiptInputSet);
    addUIFormInput(layoutInputSet);
    setSelectedTab(generalInputSet.getId()) ;
    fillData() ;
  }
  
  public List<SelectItemOption<String>> getAccounts() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    MailService mailSrv = getApplicationComponent(MailService.class);
    String username = Util.getPortalRequestContext().getRemoteUser();
    for(Account acc : mailSrv.getAccounts(username)) {
      SelectItemOption<String> itemOption = new SelectItemOption<String>(acc.getLabel() + " &lt;" + acc.getEmailAddress() + "&gt;", acc.getId());
      options.add(itemOption) ;
    }
    return options ;
  }
  
  public void fillData() throws Exception {    
    MailService mailSrv = getApplicationComponent(MailService.class);
    String username = Util.getPortalRequestContext().getRemoteUser();
    MailSetting setting = mailSrv.getMailSetting(username);
    if (setting != null) {
      long layout = setting.getLayout();
      long sendReturnReceipt = setting.getSendReturnReceipt();
      UIFormInputSet tabGeneral, returnReceipts, tabLayout;
      
      tabGeneral = (UIFormInputSet) getChildById(TAB_GENERAL);
      tabGeneral.getUIFormSelectBox(DEFAULT_ACCOUNT).setValue(setting.getDefaultAccount()) ;
      tabGeneral.getUIFormSelectBox(NUMBER_MSG_PER_PAGE).setValue(String.valueOf(setting.getNumberMsgPerPage()));
      tabGeneral.getUIFormSelectBox(PERIOD_CHECK_AUTO).setValue("period." + String.valueOf(setting.getPeriodCheckAuto()));
      tabGeneral.getUIFormSelectBox(COMPOSE_MESSAGE_IN).setValue("editor." + String.valueOf(setting.useWysiwyg()));
      tabGeneral.getUIFormSelectBox(REPLY_WITH_ATTACH).setValue("replywith." + String.valueOf(setting.replyWithAttach()));
      tabGeneral.getUIFormSelectBox(FORWARD_WITH_ATTACH).setValue("forwardwith." + String.valueOf(setting.forwardWithAtt()));
      tabGeneral.getUIFormCheckBoxInput(SAVE_SENT_MESSAGE).setChecked(setting.saveMessageInSent());
      
      returnReceipts = (UIFormInputSet) getChildById(TAB_RETURN_RECEIPT);
      if (sendReturnReceipt == MailSetting.SEND_RECEIPT_ASKSME) 
        ((UIFormRadioBoxInput) returnReceipts.getChildById(RETURN_RECEIPTS)).setValue(SENT_RECEIPT_ASKME);
      else if (sendReturnReceipt == MailSetting.SEND_RECEIPT_NEVER) 
        ((UIFormRadioBoxInput) returnReceipts.getChildById(RETURN_RECEIPTS)).setValue(SENT_RECEIPT_NEVER);
      else if (sendReturnReceipt == MailSetting.SEND_RECEIPT_ALWAYS) 
        ((UIFormRadioBoxInput) returnReceipts.getChildById(RETURN_RECEIPTS)).setValue(SENT_RECEIPT_ALWAYS);
      
      tabLayout = (UIFormInputSet) getChildById(TAB_LAYOUT);
      if (layout == MailSetting.VERTICAL_LAYOUT) {
        ((UIFormRadioBoxInput) tabLayout.getChildById(UIMailLayoutTab.VERTICAL_LAYOUT)).setValue(UIMailLayoutTab.VERTICAL_LAYOUT_VALUE);
      }
      else if (layout == MailSetting.HORIZONTAL_LAYOUT) {
        ((UIFormRadioBoxInput) tabLayout.getChildById(UIMailLayoutTab.HORIZONTAL_LAYOUT)).setValue(UIMailLayoutTab.HORIZONTAL_LAYOUT_VALUE);
      }
      else if (layout == MailSetting.NO_SPLIT_LAYOUT) {
        ((UIFormRadioBoxInput) tabLayout.getChildById(UIMailLayoutTab.NOSPLIT_LAYOUT)).setValue(UIMailLayoutTab.NO_SPLIT_LAYOUT_VALUE);
      }
    }
  }
  
  public String[] getActions() { return new String[]{"Save", "Cancel"}; }
  
  public void activate() throws Exception { }

  public void deActivate() throws Exception { }
  
  static  public class SaveActionListener extends EventListener<UIMailSettings> {
    public void execute(Event<UIMailSettings> event) throws Exception {
      
      UIMailSettings uiSetting = event.getSource();
      UIMailLayoutTab uiSettingTab = uiSetting.getChildById(TAB_LAYOUT) ;
      UIFormRadioBoxInput uiRadio = (UIFormRadioBoxInput)uiSettingTab.getChildById(UIMailLayoutTab.HORIZONTAL_LAYOUT) ;
      //TODO save to data base
       
		  UIMailPortlet uiPortlet = uiSetting.getAncestorOfType(UIMailPortlet.class);
      String username = uiPortlet.getCurrentUser();
      UISelectAccount uiSelectAccount = uiPortlet.findFirstComponentOfType(UISelectAccount.class) ;
      String accountId = uiSelectAccount.getSelectedValue();
		  
      MailService mailSrv = MailUtils.getMailService();
		  MailSetting setting = mailSrv.getMailSetting(username);
      String defaultAcc = uiSetting.getUIFormSelectBox(DEFAULT_ACCOUNT).getValue() ;
		  setting.setDefaultAccount(defaultAcc) ;
      setting.setNumberMsgPerPage(Long.valueOf(uiSetting.getUIFormSelectBox(NUMBER_MSG_PER_PAGE).getValue())) ;
      
      String period = uiSetting.getUIFormSelectBox(PERIOD_CHECK_AUTO).getValue() ;
      period = period.substring(period.indexOf(".") + 1, period.length());
		  setting.setPeriodCheckAuto(Long.valueOf(period)) ;
      
      String editor = uiSetting.getUIFormSelectBox(COMPOSE_MESSAGE_IN).getValue() ;
      setting.setUseWysiwyg(Boolean.valueOf(editor.substring(editor.indexOf(".") + 1, editor.length()))) ;
      
      String replyWith = uiSetting.getUIFormSelectBox(REPLY_WITH_ATTACH).getValue() ;
      setting.setReplyWithAttach(Boolean.valueOf(replyWith.substring(replyWith.indexOf(".") + 1, replyWith.length())));
      String forwardWith = uiSetting.getUIFormSelectBox(FORWARD_WITH_ATTACH).getValue() ;
      setting.setForwardWithAtt(Boolean.valueOf(forwardWith.substring(forwardWith.indexOf(".") + 1, forwardWith.length())));
      setting.setSaveMessageInSent(uiSetting.getUIFormCheckBoxInput(SAVE_SENT_MESSAGE).isChecked());
      
      UIFormInputSet returnReceiptLayout = (UIFormInputSet) uiSetting.getChildById(TAB_RETURN_RECEIPT);
      String value = ((UIFormRadioBoxInput) returnReceiptLayout.getChildById(UIMailSettings.RETURN_RECEIPTS)).getValue();
      if (value != null) {
        if (value.equals(UIMailSettings.SENT_RECEIPT_ASKME)) setting.setSendReturnReceipt(MailSetting.SEND_RECEIPT_ASKSME);
        else if (value.equals(UIMailSettings.SENT_RECEIPT_NEVER)) setting.setSendReturnReceipt(MailSetting.SEND_RECEIPT_NEVER);
        else if (value.equals(UIMailSettings.SENT_RECEIPT_ALWAYS)) setting.setSendReturnReceipt(MailSetting.SEND_RECEIPT_ALWAYS);
      }
      
      UIFormInputSet tabLayout = (UIFormInputSet) uiSetting.getChildById(TAB_LAYOUT);
      long oldLayout = setting.getLayout();
      value = ((UIFormRadioBoxInput) tabLayout.getChildById(UIMailLayoutTab.VERTICAL_LAYOUT)).getValue();
      if (value != null && value.equals(UIMailLayoutTab.VERTICAL_LAYOUT_VALUE)) setting.setLayout(MailSetting.VERTICAL_LAYOUT);
      else {
        value = ((UIFormRadioBoxInput) tabLayout.getChildById(UIMailLayoutTab.HORIZONTAL_LAYOUT)).getValue();
        if (value != null && value.equals(UIMailLayoutTab.HORIZONTAL_LAYOUT_VALUE)) setting.setLayout(MailSetting.HORIZONTAL_LAYOUT);
        else {
          value = ((UIFormRadioBoxInput) tabLayout.getChildById(UIMailLayoutTab.NOSPLIT_LAYOUT)).getValue();
          if (value != null && value.equals(UIMailLayoutTab.NO_SPLIT_LAYOUT_VALUE)) setting.setLayout(MailSetting.NO_SPLIT_LAYOUT);
        }
      }
      
      mailSrv.saveMailSetting(username, setting);
		  UIMessageList uiMessageList = uiPortlet.findFirstComponentOfType(UIMessageList.class);
      MessageFilter filter = uiMessageList.getMessageFilter() ;
      if (oldLayout != setting.getLayout()) {
        uiMessageList.getAncestorOfType(UIMessageArea.class).reloadMailSetting();
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet) ;
      } else if (defaultAcc != null && (!accountId.equals(setting.getDefaultAccount()) || accountId.equals(defaultAcc))){
        uiSelectAccount.updateAccount() ;
        uiSelectAccount.setSelectedValue(accountId);
        uiMessageList.setMessagePageList(mailSrv.getMessagePageList(username, filter));
      } else {
        try {
        uiMessageList.setMessagePageList(mailSrv.getMessagePageList(username, filter));
        } catch (PathNotFoundException e) {
          uiMessageList.setMessagePageList(null) ;
          uiPortlet.findFirstComponentOfType(UISelectAccount.class).refreshItems();
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet);
          UIApplication uiApp = uiMessageList.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIMessageList.msg.deleted_account", null, ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }
      uiSetting.getAncestorOfType(UIPopupAction.class).deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet);
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
      if (setting.getPeriodCheckAuto() != Long.valueOf(period)) { 
//        context.getJavascriptManager().addJavascript("eXo.mail.MailServiceHandler.initService('checkMailInfobar', '" + MailUtils.getCurrentUser() + "', '" + defaultAcc + "') ;") ;
//        context.getJavascriptManager().addJavascript("eXo.mail.MailServiceHandler.setCheckmailTimeout(" + period + ") ;") ;
      }
	  }
  }

  static  public class CancelActionListener extends EventListener<UIMailSettings> {
    public void execute(Event<UIMailSettings> event) throws Exception {
      event.getSource().getAncestorOfType(UIMailPortlet.class).cancelAction();
    }
  }
  
  static public class SelectTabActionListener extends EventListener<UIMailSettings> {
    public void execute(Event<UIMailSettings> event) throws Exception {
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource()) ;      
    }
  }
}


