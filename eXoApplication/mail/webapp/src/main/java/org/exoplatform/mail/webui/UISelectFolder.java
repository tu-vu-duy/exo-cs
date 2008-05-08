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
package org.exoplatform.mail.webui;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.mail.MailUtils;
import org.exoplatform.mail.service.Folder;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.webui.form.UIFormInputSet;

/**
 * Created by The eXo Platform SAS
 * Author : Phung Nam
 *          phunghainam@gmail.com
 * Jan 5, 2008  
 */
public class UISelectFolder extends UIFormInputSet {
  final public static String SELECT_FOLDER = "folder" ;
  public String level = "" ;
  public String accountId_ = "";
  
  public UISelectFolder(String accountId) throws Exception {  
    setId("UISelectFolder");
    accountId_ = accountId ;
    setId("UISelectFolder");
    accountId_ = accountId ;
    addUIFormInput(new org.exoplatform.mail.webui.UIFormSelectBox(SELECT_FOLDER, SELECT_FOLDER, getOptions()));
  }
  
  public void setSelectedValue(String s) {
    ((org.exoplatform.mail.webui.UIFormSelectBox)getChildById(SELECT_FOLDER)).setValue(s) ;
  }
  
  public String getSelectedValue() {
    return ((org.exoplatform.mail.webui.UIFormSelectBox)getChildById(SELECT_FOLDER)).getValue() ;
  }
  
  public List<Folder> getDefaultFolders() throws Exception{
    return getFolders(false);
  } 
  
  public List<Folder> getCustomizeFolders() throws Exception{
    return getFolders(true);
  }
  
  public List<Folder> getSubFolders(String parentPath) throws Exception {
    MailService mailSvr = MailUtils.getMailService();
    String username = MailUtils.getCurrentUser() ;
    List<Folder> subFolders = new ArrayList<Folder>();
    for (Folder f : mailSvr.getSubFolders(SessionProviderFactory.createSystemProvider(), username, accountId_, parentPath)) {
      subFolders.add(f);
    }
    return subFolders ;
  }

  public List<Folder> getFolders(boolean isPersonal) throws Exception{
    List<Folder> folders = new ArrayList<Folder>() ;
    MailService mailSvr = getApplicationComponent(MailService.class) ;
    String username = MailUtils.getCurrentUser() ;
    try {
      folders.addAll(mailSvr.getFolders(SessionProviderFactory.createSystemProvider(), username, accountId_, isPersonal)) ;
    } catch (Exception e){
      //e.printStackTrace() ;
    }
    return folders ;
  }
  
  public SelectItemOptionGroup addChildOption(String folderPath,  SelectItemOptionGroup optionList) throws Exception {
    level += "&nbsp;&nbsp;&nbsp;&nbsp;" ;
    for (Folder cf : getSubFolders(folderPath)) {
      if (cf != null) {
        optionList.addOption(new org.exoplatform.mail.webui.SelectItemOption<String>(level + " " + cf.getLabel(), cf.getId()));
        if (getSubFolders(cf.getPath()).size() > 0) { 
          optionList = addChildOption(cf.getPath(), optionList);
        }
      }
    }
    level = level.substring(0, level.length() - 4);
    return optionList ;
  }
  
  public List<SelectItem> getOptions() throws Exception {
    List<SelectItem> options = new ArrayList<SelectItem>() ;
    SelectItemOptionGroup defaultFolders = new SelectItemOptionGroup("default-folder");
    for(Folder df : getDefaultFolders()) {
      defaultFolders.addOption(new org.exoplatform.mail.webui.SelectItemOption<String>(df.getLabel(), df.getId())) ;
    }
    options.add(defaultFolders);
    SelectItemOptionGroup customizeFolders = new SelectItemOptionGroup("my-folder");
    for(Folder cf : getCustomizeFolders()) {
      customizeFolders.addOption(new org.exoplatform.mail.webui.SelectItemOption<String>(cf.getLabel(), cf.getId())) ;
        if (getSubFolders(cf.getPath()).size() > 0) { 
          customizeFolders = addChildOption(cf.getPath(), customizeFolders);
        }
    }
    options.add(customizeFolders);
      
    return options ;
  }
}