/**
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
 **/
package org.exoplatform.calendar.webui;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.calendar.Colors.Color;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputBase;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * Feb 29, 2008  
 */
public class UIFormColorPicker extends UIFormInputBase<String>  {

  /**
   * The size of the list (number of select options)
   */
  private int size_ = 1 ;

  /**
   * The list of options
   */
  private List<SelectItemOption<String>> options_ ;

  /**
   * The javascript expression executed when an onChange event fires
   */
  private String onchange_;

  /**
   * The javascript expression executed when an client onChange event fires
   */
  public static final String ON_CHANGE = "onchange".intern();

  /**
   * The javascript expression executed when an client event fires
   */
  public static final String ON_BLUR = "onblur".intern();

  /**
   * The javascript expression executed when an client event fires
   */
  public static final String ON_FOCUS = "onfocus".intern();

  /**
   * The javascript expression executed when an client event fires
   */
  public static final String ON_KEYUP = "onkeyup".intern();

  /**
   * The javascript expression executed when an client event fires
   */
  public static final String ON_KEYDOWN = "onkeydown".intern();

  /**
   * The javascript expression executed when an client event fires
   */
  public static final String ON_CLICK = "onclick".intern();

  private Map<String, String> jsActions_ = new HashMap<String, String>() ;
  private Map<String, Color> colors_ = new HashMap<String, Color>() ;

  public UIFormColorPicker(String name, String bindingExpression, String value) {
    super(name, bindingExpression, String.class);
    this.value_ = value ;
  }

  public UIFormColorPicker(String name, String bindingExpression, Color[] colors) {
    super(name, bindingExpression, null);
    setColors(colors);
  }

  public void setJsActions(Map<String, String> jsActions) {
    if(jsActions != null) jsActions_ = jsActions;
  }

  public Map<String, String> getJsActions() {
    return jsActions_;
  }
  public void addJsActions(String action, String javaScript) {
    jsActions_.put(action, javaScript) ;
  }
  public UIFormColorPicker(String name, String bindingExpression, List<SelectItemOption<String>> options, Map<String, String> jsActions) {
    super(name, bindingExpression, null);
    setOptions(options);
    setJsActions(jsActions) ;
  }

  public UIFormColorPicker(String name, String value) {
    this(name, null, value);
  }
  final public UIFormColorPicker setOptions(List<SelectItemOption<String>> options) { 
    options_ = options ; 
    if(options_ == null || options_.size() < 1) return this;
    value_ = options_.get(0).getValue();
    return this ;
  } 
  @SuppressWarnings("unused")
  public void decode(Object input, WebuiRequestContext context) throws Exception {
    value_ = (String) input;
    if(value_ != null && value_.length() == 0) value_ = null ;
  }
  public void setOnChange(String onchange){ onchange_ = onchange; } 

  protected String renderOnChangeEvent(UIForm uiForm) throws Exception {
    return uiForm.event(onchange_, (String)null);
  }
  private UIForm getUIform() {
    return getAncestorOfType(UIForm.class) ; 
  }

  private String renderJsActions() {
    StringBuffer sb = new StringBuffer() ;
    for(String k : jsActions_.keySet()){
      if(sb != null && sb.length() > 0 ) sb.append(" ") ;
      if(jsActions_.get(k) != null) {
        sb.append(k).append("=\"").append(jsActions_.get(k)).append("\"") ;
      }  
    }
    return sb.toString() ;
  }

  private String[] getColors(){
    return colors_.keySet().toArray(new String[colors_.keySet().size()]) ;
  }
  private void setColors(Color[] colors){
    for(Color c : colors) {
      colors_.put(c.getCode(), c) ;
    }
    //return colors_.keySet().toArray(new String[colors_.keySet().size()]) ;
  }
  public void processRender(WebuiRequestContext context) throws Exception {
    context.getJavascriptManager().addJavascript("eXo.calendar.UIColorPicker.init('" + getId()+ "');") ;  
    Writer w =  context.getWriter() ; 
    w.write("<div class='UIFormColorPicker'>") ;
    w.write("<div class=\"CalendarTableColor\">") ;
    int i = 0 ;
    int index = 0 ;
    int items = 5 ;
    int size = getColors().length ; 
    int rows = size/items ;
    int count = 0 ;
    while(i <= rows)  {
      w.write("<div>") ; 
      int j = 0 ;
      while(j <= items && count < size){
        String color = getColors()[count] ;
        String actionLink = "#" ;// event('ChangeColor','id&calColor='+color);  
        w.write("<a href=\"$actionLink\" class=\"ColorCell\" style=\"background:$color\"><img src=\"/eXoResources/skin/sharedImages/Blank.gif\" /></a>") ;
        count++ ;
        j++;
      }
      w.write("</div>") ;  
      i++ ;
    }
    w.write("</div>") ;
    w.write("</div>") ;
    w.write("<input class='UIColorPickerInput' name='"+getName()+"' type='hidden'" + " id='"+getId()+"' " + renderJsActions());
    if(value_ != null && value_.trim().length() > 0) {      
      w.write(" value='"+encodeValue(value_).toString()+"'");
    }
    w.write(" \\>") ;
  }

  private StringBuilder encodeValue(String value){
    char [] chars = {'\'', '"'};
    String [] refs = {"&#39;", "&#34;"};
    StringBuilder builder = new StringBuilder(value);
    int idx ;
    for(int i = 0; i < chars.length; i++){
      idx = indexOf(builder, chars[i], 0);
      while(idx > -1){
        builder = builder.replace(idx, idx+1, refs[i]);
        idx = indexOf(builder, chars[i], idx);
      }
    }    
    return builder;
  }

  private int indexOf(StringBuilder builder, char c, int from){
    int i = from;
    while(i < builder.length()){
      if(builder.charAt(i) == c) return i;
      i++;
    }
    return -1;
  }
   
}
