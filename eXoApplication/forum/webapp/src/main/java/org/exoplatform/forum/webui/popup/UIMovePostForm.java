/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.forum.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.forum.webui.UITopicContainer;
import org.exoplatform.forum.webui.UITopicDetail;
import org.exoplatform.forum.webui.UITopicDetailContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Vu Duy Tu
 *          tu.duy@exoplatform.com
 * Aus 15, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/popup/UIMovePostForm.gtmpl",
    events = {
      @EventConfig(listeners = UIMovePostForm.SaveActionListener.class), 
      @EventConfig(listeners = UIMovePostForm.CancelActionListener.class)
    }
)
public class UIMovePostForm extends UIForm implements UIPopupComponent {
  private ForumService forumService = (ForumService)PortalContainer.getInstance().getComponentInstanceOfType(ForumService.class) ;
  private String topicId ;
  private List<Post> posts ;
  
  public UIMovePostForm() throws Exception {
    
  }
  
  public void activate() throws Exception {
    // TODO Auto-generated method stub
  }
  public void deActivate() throws Exception {
    // TODO Auto-generated method stub
  }
  
  public void updatePost(String topicId, List<Post> posts) {
    this.topicId = topicId ;
    this.posts = posts ;
  }
  
  private List<Category> getCategories() throws Exception {
    return this.forumService.getCategories() ;
  }
  
  private boolean getSelectForum(String forumId) throws Exception {
    if(this.posts.get(0).getPath().contains(forumId)) return true ;
    else return false ;
  }
  
  private List<Forum> getForums(String categoryId) throws Exception {
    return this.forumService.getForums(categoryId) ;
  }

  private List<Topic> getTopics(String categoryId, String forumId) throws Exception {
    List<Topic> topics = new ArrayList<Topic>() ;
    for(Topic topic : this.forumService.getTopics(categoryId, forumId)) {
      if(topic.getId().equalsIgnoreCase(this.topicId)) continue ;
      topics.add(topic) ;
    }
    return topics ;
  }
  
  static  public class SaveActionListener extends EventListener<UIMovePostForm> {
    public void execute(Event<UIMovePostForm> event) throws Exception {
      UIMovePostForm uiForm = event.getSource() ;
      String topicPath = event.getRequestContext().getRequestParameter(OBJECTID) ;
      if(topicPath != null && topicPath.length() > 0) {
        List<Post> posts = uiForm.posts ;
        for (Post post : posts) {
          uiForm.forumService.movePost(post.getId(), post.getPath(), topicPath) ;
        }
        UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class) ;
        forumPortlet.cancelAction() ;
        String[] temp = topicPath.split("/") ;
        for (String string : temp) {
          System.out.println("\n ====>:  " + string);
        }
        UITopicDetailContainer topicDetailContainer = forumPortlet.findFirstComponentOfType(UITopicDetailContainer.class) ;
        topicDetailContainer.getChild(UITopicDetail.class).setUpdateTopic(temp[temp.length - 3], temp[temp.length - 2], temp[temp.length - 1], false) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(topicDetailContainer) ;
      }
    }
  }
  
  static  public class CancelActionListener extends EventListener<UIMovePostForm> {
    public void execute(Event<UIMovePostForm> event) throws Exception {
      UIMovePostForm uiForm = event.getSource() ;
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class) ;
      forumPortlet.cancelAction() ;
    }
  }
  
}
