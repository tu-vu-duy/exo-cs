/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.forum.service ;

import java.util.Date;

/**
 * Created by The eXo Platform SARL
 * March 2, 2007  
 */
public class Topic { 
  private String id;
  private String owner;
  private String path ;
  private Date createdDate;
  private String modifiedBy;
  private Date modifiedDate;
  private String lastPostBy;
  private Date lastPostDate;
  private String name;
  private String description;
  private long postCount = 0;  
	private long viewCount = 0;

  private boolean isModeratePost = false ;
  private boolean isNotifyWhenAddPost = false ;  
  private boolean isClosed = false ;
  private boolean isLock = false ;
  
  public Topic(){ }
  
  public String getOwner(){return owner;} 
  public void setOwner(String owner){this.owner = owner;}
  
  public String getPath() {return path; }
  public void setPath( String path) { this.path = path;}
  
  public Date getCreatedDate(){return createdDate;}
  public void setCreatedDate(Date createdDate){this.createdDate = createdDate;}
  
  public String getModifiedBy(){return modifiedBy;}
  public void setModifiedBy(String modifiedBy){this.modifiedBy = modifiedBy;}
    
  public Date getModifiedDate(){return modifiedDate;}
  public void setModifiedDate(Date modifiedDate){this.modifiedDate = modifiedDate;}
  
  public String getLastPostBy(){return lastPostBy;}
  public void setLastPostBy(String lastPostBy){this.lastPostBy = lastPostBy;}
    
  public Date getLastPostDate(){return lastPostDate;}
  public void setLastPostDate(Date lastPostDate){this.lastPostDate = lastPostDate;}
  
  public String getTopicName(){return name;}
  public void setTopicName(String topic){this.name = topic;}
  
  public String getDescription(){return description;}
  public void setDescription(String description){this.description = description;}
  
  public long getPostCount(){return postCount;}
  public void setPostCount(long postCount){this.postCount = postCount;}

  public long getViewCount(){return viewCount;}
  public void setViewCount(long viewCount){this.viewCount = viewCount;}

  public boolean getIsModeratePost() { return isModeratePost;}
  public void setIsModeratePost(boolean isModeratePost) { this.isModeratePost = isModeratePost;}

  public boolean getIsNotifyWhenAddPost() { return isNotifyWhenAddPost;}
  public void setIsNotifyWhenAddPost(boolean isNotifyWhenAddPost) { this.isNotifyWhenAddPost = isNotifyWhenAddPost;}
  
  public boolean getIsClosed() { return isClosed;}
  public void setIsClosed(boolean isClosed) { this.isClosed = isClosed;}
  
  public boolean getIsLock() { return isLock;}
  public void setIsLock(boolean isLock) { this.isLock = isLock;}
  
  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  /**
   * This method should calculate the forum id base on the topic id
   * @return
   */
  public String getForumId() { return null ; }

}
