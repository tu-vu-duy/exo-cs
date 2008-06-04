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
package org.exoplatform.contact.webui.popup;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.contact.ContactUtils;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.EmailAddressValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Tuan Nguyen
 *          tuan.nguyen@exoplatform.com
 * Aug 24, 2007  
 */
@ComponentConfig(
    template = "app:/templates/contact/webui/popup/UIProfileInputSet.gtmpl"
)
public class UIProfileInputSet extends UIFormInputWithActions {
  private static final String FIELD_FIRSTNAME_INPUT = "firstName";
  private static final String FIELD_LASTNAME_INPUT = "lastName";
  private static final String FIELD_NICKNAME_INPUT = "nickName";
  private static final String FIELD_GENDER_BOX = "gender" ;
  private static final String INFO_BIRTHDAY= "birthday" ;
  private static final String FIELD_DAY = "day" ;
  private static final String FIELD_MONTH = "month" ;
  private static final String FIELD_YEAR = "year" ; 
//public static final String[] months = { "January", "February", "March", "April", "May", "June",
//"July", "August", "September", "October", "November", "December" } ;
  private static final String FIELD_JOBTITLE_INPUT = "jobTitle";
  private static final String FIELD_EMAIL_INPUT = "email" ;
  private static final String MALE = "male" ;
  private static final String FEMALE = "female" ;
  @SuppressWarnings("unused")
  private String gender = "male" ;
  private byte[] imageBytes = null;
  private String fileName = null ;
  private String imageMimeType = null ;
  
  public UIProfileInputSet(String id) throws Exception {
    super(id) ;  
    setComponentConfig(getClass(), null) ;  
    addUIFormInput(new UIFormStringInput(FIELD_FIRSTNAME_INPUT, FIELD_FIRSTNAME_INPUT, null));
    addUIFormInput(new UIFormStringInput(FIELD_LASTNAME_INPUT, FIELD_LASTNAME_INPUT, null));
    addUIFormInput(new UIFormStringInput(FIELD_NICKNAME_INPUT, FIELD_NICKNAME_INPUT, null));
    List<SelectItemOption<String>> genderOptions = new ArrayList<SelectItemOption<String>>() ;
    genderOptions.add(new SelectItemOption<String>(MALE, MALE));
    genderOptions.add(new SelectItemOption<String>(FEMALE, FEMALE));
    addUIFormInput(new UIFormRadioBoxInput(FIELD_GENDER_BOX, FIELD_GENDER_BOX, genderOptions));    
    addUIFormInput(new UIFormInputInfo(INFO_BIRTHDAY, INFO_BIRTHDAY, null)) ;
    
    List<SelectItemOption<String>> datesOptions = new ArrayList<SelectItemOption<String>>() ;
    datesOptions.add(new SelectItemOption<String>("- "+FIELD_DAY+" -", FIELD_DAY)) ;
    for (int i = 1; i < 32; i ++) {
      String date = i + "" ;
      datesOptions.add(new SelectItemOption<String>(date, date)) ;
    }
    addUIFormInput(new UIFormSelectBox(FIELD_DAY, FIELD_DAY, datesOptions)) ;
    
    List<SelectItemOption<String>> monthOptions = new ArrayList<SelectItemOption<String>>() ;
    monthOptions.add(new SelectItemOption<String>("-"+FIELD_MONTH+"-", FIELD_MONTH)) ;
    for (int i = 1; i < 13; i ++) {
      String month = i + "" ;
      monthOptions.add(new SelectItemOption<String>(month, month)) ;
    }
    addUIFormInput(new UIFormSelectBox(FIELD_MONTH, FIELD_MONTH, monthOptions)) ;

    String date = ContactUtils.formatDate("dd/MM/yyyy", new Date()) ;
    String strDate = date.substring(date.lastIndexOf("/") + 1, date.length()) ; 
    int thisYear = Integer.parseInt(strDate) ;
    List<SelectItemOption<String>> yearOptions = new ArrayList<SelectItemOption<String>>() ;
    yearOptions.add(new SelectItemOption<String>("- "+FIELD_YEAR+" -", FIELD_YEAR)) ;
    for (int i = thisYear; i >= 1900; i --) {
      String year = i + "" ;
      yearOptions.add(new SelectItemOption<String>(year, year)) ;
    }
    addUIFormInput(new UIFormSelectBox(FIELD_YEAR, FIELD_YEAR, yearOptions)) ;

    addUIFormInput(new UIFormStringInput(FIELD_JOBTITLE_INPUT, FIELD_JOBTITLE_INPUT, null));
    addUIFormInput(new UIFormStringInput(FIELD_EMAIL_INPUT, FIELD_EMAIL_INPUT, null)
    .addValidator(EmailAddressValidator.class));
  }

  protected String getFieldFirstName() { return getUIStringInput(FIELD_FIRSTNAME_INPUT).getValue() ; }
  protected void setFieldFirstName(String s) { getUIStringInput(FIELD_FIRSTNAME_INPUT).setValue(s); }

  protected String getFieldLastName() { return getUIStringInput(FIELD_LASTNAME_INPUT).getValue() ; }
  protected void setFieldLastName(String s) { getUIStringInput(FIELD_LASTNAME_INPUT).setValue(s); }

  protected String getFieldNickName() { return getUIStringInput(FIELD_NICKNAME_INPUT).getValue() ; }
  protected void setFieldNickName(String s) { getUIStringInput(FIELD_NICKNAME_INPUT).setValue(s); }

  protected String getFieldGender() {
    String value = null ; //= getChild(UIFormRadioBoxInput.class).getValue() ;
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    ResourceBundle res = context.getApplicationResourceBundle() ;
    try {
      if  (getChild(UIFormRadioBoxInput.class).getValue().equalsIgnoreCase(MALE)) {
        value =  res.getString(((UIContactForm)getParent()).getId() + ".label." + MALE);
      } else {
        value = res.getString(((UIContactForm)getParent()).getId() + ".label." + FEMALE) ;
      }
    } catch (MissingResourceException e) {      
      e.printStackTrace() ;
    }
    return value ;
  }
  protected void setFieldGender(String s) {
    if (ContactUtils.isEmpty(s)) return ;
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    ResourceBundle res = context.getApplicationResourceBundle() ;
    try {
      if (s.equals(res.getString(((UIContactForm)getParent()).getId() + ".label." + MALE))) {
        gender = MALE ;
      } else gender = FEMALE ;      
    } catch (Exception e) { }
    
  }

  protected Date getFieldBirthday(){
    int day, month, year ;
    day = month = year = 0 ;
    boolean emptyDay, emptyMonth, emptyYear ;
    emptyDay = emptyMonth = emptyYear = false ;
    try {
      day = Integer.parseInt(getUIFormSelectBox(FIELD_DAY).getValue()) ;
    } catch (NumberFormatException e) {
      emptyDay = true ;
    }
    try {
      month = Integer.parseInt(getUIFormSelectBox(FIELD_MONTH).getValue()) ;
    } catch (NumberFormatException e) {
      emptyMonth = true ;
    }
    try {
      year = Integer.parseInt(getUIFormSelectBox(FIELD_YEAR).getValue()) ;
    } catch (NumberFormatException e) {
      emptyYear = true ;
    }
    if (emptyDay && emptyMonth && emptyYear) return null ;
    else {
      Calendar cal = GregorianCalendar.getInstance() ;
      cal.setLenient(false) ;
      cal.set(Calendar.DATE, day) ;
      cal.set(Calendar.MONTH, month - 1) ;
      cal.set(Calendar.YEAR, year) ;
      return cal.getTime() ;
    }    
  }
  protected void setFieldBirthday(Date date) throws Exception {
    if (date != null) {
      Calendar cal = GregorianCalendar.getInstance() ;
      //cal.setLenient(false) ;
      cal.setTime(date) ;
      getUIFormSelectBox(FIELD_MONTH).setValue(String.valueOf(cal.get(Calendar.MONTH) + 1)) ;
      getUIFormSelectBox(FIELD_DAY).setValue(String.valueOf(cal.get(Calendar.DATE))) ;
      getUIFormSelectBox(FIELD_YEAR).setValue(String.valueOf(cal.get(Calendar.YEAR))) ;
    }
  }

  protected String getFieldJobName() { return getUIStringInput(FIELD_JOBTITLE_INPUT).getValue() ; }
  protected void setFieldJobName(String s) { getUIStringInput(FIELD_JOBTITLE_INPUT).setValue(s); }
  protected UIForm getParentFrom() { return (UIForm)getParent() ; }
  protected String getFieldEmail() { return getUIStringInput(FIELD_EMAIL_INPUT).getValue(); }
  protected void setFieldEmail(String s) { getUIStringInput(FIELD_EMAIL_INPUT).setValue(s); }

  protected void setImage(InputStream input) throws Exception{
    if (input != null) {
      imageBytes = new byte[input.available()] ; 
      input.read(imageBytes) ;
    }
    else imageBytes = null ;
  }
  protected byte[] getImage() {return imageBytes ;}

  protected String getMimeType() { return imageMimeType ;} ;
  protected void setMimeType(String mimeType) {imageMimeType = mimeType ;} 

  protected void setFileName(String name) { fileName = name ; }
  protected String getFileName() {return fileName ;}

  protected String getImageSource() throws Exception {    
    if(imageBytes == null || imageBytes.length == 0) return null;
    ByteArrayInputStream byteImage = new ByteArrayInputStream(imageBytes) ;    
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource = new InputStreamDownloadResource(byteImage, "image") ;
    dresource.setDownloadName(fileName) ;
    return  dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

}

