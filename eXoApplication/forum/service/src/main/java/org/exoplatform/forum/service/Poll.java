/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL				 All rights reserved.	*
 * Please look at license.txt in info directory for more license detail.	 *
 **************************************************************************/
package org.exoplatform.forum.service ;

import java.util.Date;

import org.exoplatform.services.jcr.util.IdGenerator;
/**
 * Created by The eXo Platform SARL
 * Author : Vu Duy Tu
 * 				tu.duy@exoplatform.com
 * Octo 25, 2007
 */
public class Poll { 
	private String id;
	private String owner;
	private Date createdDate;
	private String modifiedBy;
	private Date modifiedDate;
	private long timeOut = 0;
	private String question;
	private String[] option;
	private String[] vote;
	private String[] userVote;
	private boolean isMultiCheck = false ;
	
	
	public Poll() {
		id = "poll" + IdGenerator.generate() ;
		createdDate = new Date() ;
		option = new String[] {};
		vote = new String[] {};
		userVote = new String[] {};
	}
	
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	/**
	 * This method should calculate the id of the topic base on the id of the post
	 * @return
	 */
	public String getTopicId() { return null; }
	/**
	 * This method should calculate the id of the forum base on the id of the post
	 * @return
	 */
	public String getForumId() { return null ;}

	public String getOwner(){return owner;}
	public void setOwner(String owner){this.owner = owner;}
	
	public Date getCreatedDate(){return createdDate;}
	public void setCreatedDate(Date createdDate){this.createdDate = createdDate;}
	
	public String getModifiedBy(){return modifiedBy;}
	public void setModifiedBy(String modifiedBy){this.modifiedBy = modifiedBy;}
	
	public Date getModifiedDate(){return modifiedDate;}
	public void setModifiedDate(Date modifiedDate){this.modifiedDate = modifiedDate;}
	
	public String getQuestion(){return question;}
	public void setQuestion(String question){this.question = question;}

	public long getTimeOut(){return timeOut;}
	public void setTimeOut(long timeOut){this.timeOut = timeOut;}
	
	public String[] getOption() { return this.option ; }
	public void setOption( String[] option) {this.option = option ; }
	
	public String[] getVote() { return vote; }
	public void setVote(String[] vote) { this.vote = vote; }
	
	public String[] getUserVote() {return userVote; }
	public void setUserVote( String[] userVote) { this.userVote = userVote;}
	
	public boolean getIsMultiCheck() { return isMultiCheck;}
	public void setIsMultiCheck(boolean isMultiCheck) { this.isMultiCheck = isMultiCheck;}
}
