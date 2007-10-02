/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.forum.webui.popup;

import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Vu Duy Tu
 *          tu.duy@exoplatform.com
 * October 2, 2007  
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/popup/UIViewTopic.gtmpl",
    events = {
      @EventConfig(listeners = UIViewTopic.CloseActionListener.class)
    }
)
public class UIViewTopic extends UIForm implements UIPopupComponent {
  private Post post;
  
  public UIViewTopic() {
    
  }
  
  public void setPostView(Post post) throws Exception {
    this.post = post ;
  }

  private Post getPostView() throws Exception {
    return post ;
  }
  public void activate() throws Exception {
    // TODO Auto-generated method stub
    
  }
  public void deActivate() throws Exception {
    // TODO Auto-generated method stub
  }
  
  static  public class CloseActionListener extends EventListener<UIViewTopic> {
    public void execute(Event<UIViewTopic> event) throws Exception {
      UIViewTopic uiForm = event.getSource() ;
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class) ;
      forumPortlet.cancelAction() ;
    }
  }
}
