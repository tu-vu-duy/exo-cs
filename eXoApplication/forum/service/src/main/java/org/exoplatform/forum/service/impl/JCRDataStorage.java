/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.forum.service.impl;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.TopicView;
import org.exoplatform.registry.JCRRegistryService;
import org.exoplatform.registry.ServiceRegistry;
import org.exoplatform.services.jcr.RepositoryService;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen Quang
 *          hung.nguyen@exoplatform.com
 * Jul 10, 2007  
 */
public class JCRDataStorage implements DataStorage{
  private RepositoryService  repositoryService_ ; 
  private JCRRegistryService jcrRegistryService_ ;
  
  public JCRDataStorage(RepositoryService  repositoryService, 
                        JCRRegistryService jcrRegistryService)throws Exception {
    repositoryService_ = repositoryService ;
    jcrRegistryService_ = jcrRegistryService ;
  }
  
  public void createCategory(Category category) throws Exception {
    Node forumHomeNode = getForumHomeNode() ;
    Node newCategory = forumHomeNode.addNode(category.getId(), "exo:forumCategory") ;
    GregorianCalendar calendar = new GregorianCalendar() ;
    newCategory.setProperty("exo:id", String.valueOf(calendar.getTimeInMillis())) ;
    newCategory.setProperty("exo:owner", category.getOwner()) ;
    newCategory.setProperty("exo:createdDate", GregorianCalendar.getInstance()) ;
    newCategory.setProperty("exo:modifiedBy", category.getModifiedBy()) ;
    newCategory.setProperty("exo:modifiedDate", GregorianCalendar.getInstance()) ;
    newCategory.setProperty("exo:name", category.getCategoryName()) ;
    newCategory.setProperty("exo:description", category.getDescription()) ;
    newCategory.setProperty("exo:categoryOrder", category.getCategoryOrder()) ;
    forumHomeNode.save() ;
    forumHomeNode.getSession().save() ;    
  }
  
  public List<Category> getCategories() throws Exception {
    Node forumHomeNode = getForumHomeNode() ;
    NodeIterator iter = forumHomeNode.getNodes() ;
    List<Category> categories = new ArrayList<Category>() ;
    Category cat ;
    while(iter.hasNext()) {
      Node cateNode = iter.nextNode() ;
      cat = new Category() ;
      cat.setId(cateNode.getProperty("exo:id").getString()) ;
      cat.setOwner(cateNode.getProperty("exo:owner").getString()) ;
      cat.setCategoryName(cateNode.getProperty("exo:name").getString()) ;
      cat.setCategoryOrder(cateNode.getProperty("exo:categoryOrder").getLong()) ;
      cat.setCreatedDate(cateNode.getProperty("exo:createdDate").getDate().getTime()) ;
      cat.setDescription(cateNode.getProperty("exo:description").getString()) ;
      cat.setModifiedBy(cateNode.getProperty("exo:modifiedBy").getString()) ;
      cat.setModifiedDate(cateNode.getProperty("exo:modifiedDate").getDate().getTime()) ;
      categories.add(cat) ;
    }
    return categories ;
  }
  
  public Category getCategory(String categoryId) throws Exception {
    Node forumHomeNode = getForumHomeNode() ;
    if(!forumHomeNode.hasNode(categoryId)) return null;
    Node catNode = forumHomeNode.getNode(categoryId) ;
    Category cat = new Category() ;
    cat.setId(categoryId) ;
    cat.setCategoryName(catNode.getProperty("exo:name").getString()) ;
    cat.setCategoryOrder(catNode.getProperty("exo:categoryOrder").getLong()) ;
    cat.setCreatedDate(catNode.getProperty("exo:createdDate").getDate().getTime()) ;
    cat.setDescription(catNode.getProperty("exo:description").getString()) ;
    cat.setModifiedBy(catNode.getProperty("exo:modifiedBy").getString()) ;
    cat.setModifiedDate(catNode.getProperty("exo:modifiedDate").getDate().getTime()) ;
    cat.setOwner(catNode.getProperty("exo:owner").getString()) ;
    return cat ;
  }

  public void removeCategory(String categoryId) throws Exception {
    Node forumHomeNode = getForumHomeNode() ;
    if(forumHomeNode.hasNode(categoryId)){
      forumHomeNode.getNode(categoryId).remove() ;
    }
    forumHomeNode.save() ;
    forumHomeNode.getSession().save() ;
  }
  
  public void updateCategory(Category category) throws Exception {
    Node forumHomeNode = getForumHomeNode() ;
    if(forumHomeNode.hasNode(category.getId())){
      Node catNode = forumHomeNode.getNode(category.getId()) ;
      catNode.setProperty("exo:name", category.getCategoryName()) ;
      catNode.setProperty("exo:categoryOrder", category.getCategoryOrder()) ;
      GregorianCalendar cal = new GregorianCalendar() ;
      cal.setTime(category.getCreatedDate()) ;
      catNode.setProperty("exo:createdDate", cal.getInstance()) ;
      catNode.setProperty("exo:description", category.getDescription()) ;
      catNode.setProperty("exo:modifiedBy", category.getModifiedBy()) ;
      cal.setTime(category.getModifiedDate()) ;
      catNode.setProperty("exo:modifiedDate", cal.getInstance()) ;
      catNode.setProperty("exo:owner", category.getOwner()) ;
    }
    forumHomeNode.save() ;
    forumHomeNode.getSession().save() ;
  }
  
  public void createForum(String categoryId, Forum forum) throws Exception {
    // TODO Auto-generated method stub
  }
  
  public Forum getForum(String categoryId, String forumId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }
  
  public List<Forum> getForums(String categoryId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }
  
  public void removeForum(String categoryId, String forumId) throws Exception {
    // TODO Auto-generated method stub
  }
  
  public void updateForum(String categoryId, Forum newForum) throws Exception {
    // TODO Auto-generated method stub
  }
  
  
  public void createPost(String categoryId, String forumId, String topicId, Post post) throws Exception {
    // TODO Auto-generated method stub
  }
  
  public Post getPost(String categoryId, String forumId, String topicId, String postId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }
  
  public List<Post> getPosts(String categoryId, String forumId, String topicId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }
  
  public void removePost(String categoryId, String forumId, String topicId, String postId) throws Exception {
    // TODO Auto-generated method stub
  }
  
  public void updatePost(String categoryId, String forumId, String topicId, Post newPost) throws Exception {
    // TODO Auto-generated method stub
  }
  
  public void createTopic(String categoryId, String forumId, Topic topic) throws Exception {
    // TODO Auto-generated method stub
  }

  public Topic getTopic(String categoryId, String forumId, String topicId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  public TopicView getTopicView(String categoryId, String forumId, String topicId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  public PageList getTopics(String categoryId, String forumId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  public void removeTopic(String categoryId, String forumId, String topicId) throws Exception {
    // TODO Auto-generated method stub
  }

  public void updateTopic(String categoryId, String forumId, Topic newTopic) throws Exception {
    // TODO Auto-generated method stub
  } 
  
  private Node getForumHomeNode() throws Exception {
    ServiceRegistry serviceRegistry = new ServiceRegistry("ForumService") ;
    Session session = getJCRSession() ;
    jcrRegistryService_.createServiceRegistry(serviceRegistry, false) ;    
    return jcrRegistryService_.getServiceRegistryNode(session, serviceRegistry.getName()) ;
  }
  
  private Session getJCRSession() throws Exception {
    String defaultWS = 
      repositoryService_.getDefaultRepository().getConfiguration().getDefaultWorkspaceName() ;
    return repositoryService_.getDefaultRepository().getSystemSession(defaultWS) ;
  }
  
}
