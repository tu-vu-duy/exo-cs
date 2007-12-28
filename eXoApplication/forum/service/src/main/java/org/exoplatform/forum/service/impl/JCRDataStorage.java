/***************************************************************************
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
 ***************************************************************************/
package org.exoplatform.forum.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.forum.service.BufferAttachment;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumAttachment;
import org.exoplatform.forum.service.ForumLinkData;
import org.exoplatform.forum.service.ForumOption;
import org.exoplatform.forum.service.ForumPageList;
import org.exoplatform.forum.service.JCRForumAttachment;
import org.exoplatform.forum.service.JCRPageList;
import org.exoplatform.forum.service.Poll;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Tag;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.TopicView;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen Quang
 *					hung.nguyen@exoplatform.com
 * Jul 10, 2007	
 * Edited by Vu Duy Tu
 *					tu.duy@exoplatform.com
 * July 16, 2007 
 */
public class JCRDataStorage{
	private final static String FORUM_SERVICE = "ForumService" ;
	private final static String NT_UNSTRUCTURED = "nt:unstructured".intern() ;
	private NodeHierarchyCreator nodeHierarchyCreator_ ;
	public JCRDataStorage(NodeHierarchyCreator nodeHierarchyCreator)throws Exception {
		nodeHierarchyCreator_ = nodeHierarchyCreator ;
	}
	
	protected Node getForumHomeNode(SessionProvider sProvider) throws Exception {
		Node appNode = nodeHierarchyCreator_.getPublicApplicationNode(sProvider) ;
		if(appNode.hasNode(FORUM_SERVICE)) return appNode.getNode(FORUM_SERVICE) ;
		return appNode.addNode(FORUM_SERVICE, NT_UNSTRUCTURED) ;
	}
	
	public List<Category> getCategories(SessionProvider sProvider) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		QueryManager qm = forumHomeNode.getSession().getWorkspace().getQueryManager() ;
		StringBuffer queryString = new StringBuffer("/jcr:root" + forumHomeNode.getPath() +"//element(*,exo:forumCategory) order by @exo:categoryOrder ascending") ;
		Query query = qm.createQuery(queryString.toString(), Query.XPATH) ;
		QueryResult result = query.execute() ;
		NodeIterator iter = result.getNodes() ;
		List<Category> categories = new ArrayList<Category>() ;
		while(iter.hasNext()) {
			Node cateNode = iter.nextNode() ;
			categories.add(getCategory(cateNode)) ;
		}
		return categories ;
	}
	
	public Category getCategory(SessionProvider sProvider, String categoryId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(!forumHomeNode.hasNode(categoryId)) return null;
		Node cateNode = forumHomeNode.getNode(categoryId) ;
		Category cat = new Category() ;
		cat = getCategory(cateNode) ;
		return cat ;
	}

	private Category getCategory(Node cateNode) throws Exception {
		Category cat = new Category() ;
		if(cateNode.hasProperty("exo:id"))cat.setId(cateNode.getProperty("exo:id").getString()) ;
		if(cateNode.hasProperty("exo:owner"))cat.setOwner(cateNode.getProperty("exo:owner").getString()) ;
		if(cateNode.hasProperty("exo:path"))cat.setPath(cateNode.getProperty("exo:path").getString()) ;
		if(cateNode.hasProperty("exo:name"))cat.setCategoryName(cateNode.getProperty("exo:name").getString()) ;
		if(cateNode.hasProperty("exo:categoryOrder"))cat.setCategoryOrder(cateNode.getProperty("exo:categoryOrder").getLong()) ;
		if(cateNode.hasProperty("exo:createdDate"))cat.setCreatedDate(cateNode.getProperty("exo:createdDate").getDate().getTime()) ;
		if(cateNode.hasProperty("exo:description"))cat.setDescription(cateNode.getProperty("exo:description").getString()) ;
		if(cateNode.hasProperty("exo:modifiedBy"))cat.setModifiedBy(cateNode.getProperty("exo:modifiedBy").getString()) ;
		if(cateNode.hasProperty("exo:modifiedDate"))cat.setModifiedDate(cateNode.getProperty("exo:modifiedDate").getDate().getTime()) ;
		if(cateNode.hasProperty("exo:userPrivate"))cat.setUserPrivate(cateNode.getProperty("exo:userPrivate").getString()) ;
		return cat;
	}
	
	public void saveCategory(SessionProvider sProvider, Category category, boolean isNew) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Node catNode;
		if(isNew) {
			catNode = forumHomeNode.addNode(category.getId(), "exo:forumCategory") ;
			catNode.setProperty("exo:id", category.getId()) ;
			catNode.setProperty("exo:owner", category.getOwner()) ;
			catNode.setProperty("exo:path", catNode.getPath()) ;
			catNode.setProperty("exo:createdDate", getGreenwichMeanTime()) ;
		} else {
			catNode = forumHomeNode.getNode(category.getId()) ;
		}
		catNode.setProperty("exo:name", category.getCategoryName()) ;
		catNode.setProperty("exo:categoryOrder", category.getCategoryOrder()) ;
		catNode.setProperty("exo:description", category.getDescription()) ;
		catNode.setProperty("exo:modifiedBy", category.getModifiedBy()) ;
		catNode.setProperty("exo:modifiedDate", getGreenwichMeanTime()) ;
		catNode.setProperty("exo:userPrivate", category.getUserPrivate()) ;
		
		//forumHomeNode.save() ;
		forumHomeNode.getSession().save() ;		
	}
	
	public Category removeCategory(SessionProvider sProvider, String categoryId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Category category = new Category () ;
		if(forumHomeNode.hasNode(categoryId)){
			category = getCategory(sProvider, categoryId) ;
			forumHomeNode.getNode(categoryId).remove() ;
			//forumHomeNode.save() ;
			forumHomeNode.getSession().save() ;
			return category;
		}
		return null;
	}
	
	
	public List<Forum> getForums(SessionProvider sProvider, String categoryId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node catNode = forumHomeNode.getNode(categoryId) ;
			QueryManager qm = forumHomeNode.getSession().getWorkspace().getQueryManager() ;
			String queryString = "/jcr:root" + catNode.getPath() + "//element(*,exo:forum) order by @exo:forumOrder ascending,@exo:createdDate ascending";
			Query query = qm.createQuery(queryString , Query.XPATH) ;
			QueryResult result = query.execute() ;
			NodeIterator iter = result.getNodes() ;
			List<Forum> forums = new ArrayList<Forum>() ;
			while (iter.hasNext()) {
				Node forumNode = iter.nextNode() ;
				forums.add(getForum(forumNode)) ;
			}
			return forums;
		}
		return null; 
	}

	public Forum getForum(SessionProvider sProvider, String categoryId, String forumId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node catNode = forumHomeNode.getNode(categoryId) ;
			Node forumNode = catNode.getNode(forumId) ;
			Forum forum = new Forum() ;
			forum = getForum(forumNode) ;
			return forum;
		}
		return null;
	}

	public void saveForum(SessionProvider sProvider, String categoryId, Forum forum, boolean isNew) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node catNode = forumHomeNode.getNode(categoryId) ;
			Node forumNode;
			if(isNew) {
				forumNode = catNode.addNode(forum.getId(), "exo:forum") ;
				forumNode.setProperty("exo:id", forum.getId()) ;
				forumNode.setProperty("exo:owner", forum.getOwner()) ;
				forumNode.setProperty("exo:path", forumNode.getPath()) ;
				forumNode.setProperty("exo:createdDate", getGreenwichMeanTime()) ;
				forumNode.setProperty("exo:lastTopicPath", forum.getLastTopicPath()) ;
				forumNode.setProperty("exo:postCount", 0) ;
				forumNode.setProperty("exo:topicCount", 0) ;
			} else {
				forumNode = catNode.getNode(forum.getId()) ;
			}
			forumNode.setProperty("exo:name", forum.getForumName()) ;
			forumNode.setProperty("exo:forumOrder", forum.getForumOrder()) ;
			forumNode.setProperty("exo:modifiedBy", forum.getModifiedBy()) ;
			forumNode.setProperty("exo:modifiedDate", getGreenwichMeanTime()) ;
			forumNode.setProperty("exo:description", forum.getDescription()) ;
			
			forumNode.setProperty("exo:notifyWhenAddPost", forum.getNotifyWhenAddPost()) ;
			forumNode.setProperty("exo:notifyWhenAddTopic", forum.getNotifyWhenAddTopic()) ;
			forumNode.setProperty("exo:isModerateTopic", forum.getIsModerateTopic()) ;
			forumNode.setProperty("exo:isModeratePost", forum.getIsModeratePost()) ;
			forumNode.setProperty("exo:isClosed", forum.getIsClosed()) ;
			forumNode.setProperty("exo:isLock", forum.getIsLock()) ;
			
			forumNode.setProperty("exo:viewForumRole", forum.getViewForumRole()) ;
			forumNode.setProperty("exo:createTopicRole", forum.getCreateTopicRole()) ;
			forumNode.setProperty("exo:replyTopicRole", forum.getReplyTopicRole()) ;
			forumNode.setProperty("exo:moderators", forum.getModerators()) ;
			
			//forumHomeNode.save() ;
			forumHomeNode.getSession().save() ;
		}
	}
	
	private Forum getForum(Node forumNode) throws Exception {
		Forum forum = new Forum() ;
		if(forumNode.hasProperty("exo:id")) forum.setId(forumNode.getProperty("exo:id").getString()) ;
		if(forumNode.hasProperty("exo:owner")) forum.setOwner(forumNode.getProperty("exo:owner").getString()) ;
		if(forumNode.hasProperty("exo:path")) forum.setPath(forumNode.getPath()) ;
		if(forumNode.hasProperty("exo:name")) forum.setForumName(forumNode.getProperty("exo:name").getString()) ;
		if(forumNode.hasProperty("exo:forumOrder")) forum.setForumOrder(Integer.valueOf(forumNode.getProperty("exo:forumOrder").getString())) ;
		if(forumNode.hasProperty("exo:createdDate")) forum.setCreatedDate(forumNode.getProperty("exo:createdDate").getDate().getTime()) ;
		if(forumNode.hasProperty("exo:modifiedBy")) forum.setModifiedBy(forumNode.getProperty("exo:modifiedBy").getString()) ;
		if(forumNode.hasProperty("exo:modifiedDate")) forum.setModifiedDate(forumNode.getProperty("exo:modifiedDate").getDate().getTime()) ;
		if(forumNode.hasProperty("exo:lastTopicPath"))forum.setLastTopicPath(forumNode.getProperty("exo:lastTopicPath").getString()) ; 
		if(forumNode.hasProperty("exo:description")) forum.setDescription(forumNode.getProperty("exo:description").getString()) ;
		if(forumNode.hasProperty("exo:postCount")) forum.setPostCount(forumNode.getProperty("exo:postCount").getLong()) ;
		if(forumNode.hasProperty("exo:topicCount")) forum.setTopicCount(forumNode.getProperty("exo:topicCount").getLong()) ;

		if(forumNode.hasProperty("exo:isModerateTopic")) forum.setIsModerateTopic(forumNode.getProperty("exo:isModerateTopic").getBoolean()) ;
		if(forumNode.hasProperty("exo:isModeratePost")) forum.setIsModeratePost(forumNode.getProperty("exo:isModeratePost").getBoolean()) ;
		if(forumNode.hasProperty("exo:isClosed")) forum.setIsClosed(forumNode.getProperty("exo:isClosed").getBoolean()) ;
		if(forumNode.hasProperty("exo:isLock")) forum.setIsLock(forumNode.getProperty("exo:isLock").getBoolean()) ;
		
		if(forumNode.hasProperty("exo:notifyWhenAddPost")) forum.setNotifyWhenAddPost(ValuesToStrings(forumNode.getProperty("exo:notifyWhenAddPost").getValues())) ;
		if(forumNode.hasProperty("exo:notifyWhenAddTopic")) forum.setNotifyWhenAddTopic(ValuesToStrings(forumNode.getProperty("exo:notifyWhenAddTopic").getValues())) ;
		if(forumNode.hasProperty("exo:viewForumRole")) forum.setViewForumRole(ValuesToStrings(forumNode.getProperty("exo:viewForumRole").getValues())) ;
		if(forumNode.hasProperty("exo:createTopicRole")) forum.setCreateTopicRole(ValuesToStrings(forumNode.getProperty("exo:createTopicRole").getValues())) ;
		if(forumNode.hasProperty("exo:replyTopicRole")) forum.setReplyTopicRole(ValuesToStrings(forumNode.getProperty("exo:replyTopicRole").getValues())) ;
		if(forumNode.hasProperty("exo:moderators")) forum.setModerators(ValuesToStrings(forumNode.getProperty("exo:moderators").getValues())) ;
		return forum;
	}

	public Forum removeForum(SessionProvider sProvider, String categoryId, String forumId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Forum forum = new Forum() ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node catNode = forumHomeNode.getNode(categoryId) ;
			forum = getForum(sProvider, categoryId, forumId) ;
			catNode.getNode(forumId).remove() ;
			//forumHomeNode.save() ;
			forumHomeNode.getSession().save() ;
			return forum;
		}
		return null ;
	}

	public void moveForum(SessionProvider sProvider, String forumId, String forumPath, String destCategoryPath)throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		String newForumPath = destCategoryPath + "/" + forumId;
		forumHomeNode.getSession().getWorkspace().move(forumPath, newForumPath) ;
		Node forumNode = (Node)forumHomeNode.getSession().getItem(newForumPath) ;
		forumNode.setProperty("exo:path", newForumPath) ;
		//orumHomeNode.save() ;
		forumHomeNode.getSession().save() ;
	}
	
	
	public JCRPageList getPageTopic(SessionProvider sProvider, String categoryId, String forumId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			Node forumNode = CategoryNode.getNode(forumId) ;
			QueryManager qm = forumHomeNode.getSession().getWorkspace().getQueryManager() ;
			String pathQuery = "/jcr:root" + forumNode.getPath() + "//element(*,exo:topic) order by @exo:isSticky descending,@exo:createdDate descending";
			Query query = qm.createQuery(pathQuery , Query.XPATH) ;
			QueryResult result = query.execute() ;
			NodeIterator iter = result.getNodes(); 
			JCRPageList pagelist = new ForumPageList(iter, 10, forumNode.getPath(), false) ;
			return pagelist ;
		}
		return null ;
	}
	
	public List<Topic> getTopics(SessionProvider sProvider, String categoryId, String forumId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			Node forumNode = CategoryNode.getNode(forumId) ;
			NodeIterator iter = forumNode.getNodes() ;
			List<Topic> topics = new ArrayList<Topic>() ;
			while (iter.hasNext()) {
				Node topicNode = iter.nextNode() ;
				topics.add(getTopicNode(topicNode)) ;
			}
			return topics ;
		}
		return null ;
	}
	
	public Topic getTopic(SessionProvider sProvider, String categoryId, String forumId, String topicId, boolean viewTopic) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			Node forumNode = CategoryNode.getNode(forumId) ;
			Node topicNode = forumNode.getNode(topicId) ;
			Topic topicNew = new Topic() ;
			topicNew = getTopicNode(topicNode) ;
			// setViewCount for Topic
			if(viewTopic) {
				long newViewCount = topicNode.getProperty("exo:viewCount").getLong() + 1 ;
				topicNode.setProperty("exo:viewCount", newViewCount) ;
			}
			//forumHomeNode.save() ;
			forumHomeNode.getSession().save() ;
			return topicNew ;
		}
		return null ;
	}
	
	public Topic getTopicByPath(SessionProvider sProvider, String topicPath)throws Exception {
		try {
			//TODO: Need to review this way to get Topic node
			return getTopicNode((Node)getForumHomeNode(sProvider).getSession().getItem(topicPath)) ;
		}catch(Exception e) {
			if(topicPath != null && topicPath.length() > 0) {
				String forumPath = topicPath.substring(0, topicPath.lastIndexOf("/")) ;
				return getTopicNode(queryLastTopic(sProvider, forumPath)) ;
			} else {
				return null ;
			}
		}
	}
	
	private Node queryLastTopic(SessionProvider sProvider, String forumPath) throws Exception {
		QueryManager qm = getForumHomeNode(sProvider).getSession().getWorkspace().getQueryManager() ;
		String queryString = "/jcr:root" + forumPath + "//element(*,exo:topic) order by @exo:lastPostDate descending";
		Query query = qm.createQuery(queryString , Query.XPATH) ;
		QueryResult result = query.execute() ;
		NodeIterator iter = result.getNodes() ;
		if(iter.getSize() < 1) return null ;
		return iter.nextNode() ;
	}
	
	private Topic getTopicNode(Node topicNode) throws Exception {
		if(topicNode == null ) return null ;
		Topic topicNew = new Topic() ;		
		if(topicNode.hasProperty("exo:id")) topicNew.setId(topicNode.getProperty("exo:id").getString()) ;
		if(topicNode.hasProperty("exo:owner")) topicNew.setOwner(topicNode.getProperty("exo:owner").getString()) ;
		if(topicNode.hasProperty("exo:path")) topicNew.setPath(topicNode.getPath()) ;
		if(topicNode.hasProperty("exo:name")) topicNew.setTopicName(topicNode.getProperty("exo:name").getString()) ;
		if(topicNode.hasProperty("exo:createdDate")) topicNew.setCreatedDate(topicNode.getProperty("exo:createdDate").getDate().getTime()) ;
		if(topicNode.hasProperty("exo:modifiedBy")) topicNew.setModifiedBy(topicNode.getProperty("exo:modifiedBy").getString()) ;
		if(topicNode.hasProperty("exo:modifiedDate")) topicNew.setModifiedDate(topicNode.getProperty("exo:modifiedDate").getDate().getTime()) ;
		if(topicNode.hasProperty("exo:lastPostBy")) topicNew.setLastPostBy(topicNode.getProperty("exo:lastPostBy").getString()) ;
		if(topicNode.hasProperty("exo:lastPostDate")) topicNew.setLastPostDate(topicNode.getProperty("exo:lastPostDate").getDate().getTime()) ;
		if(topicNode.hasProperty("exo:description")) topicNew.setDescription(topicNode.getProperty("exo:description").getString()) ;
		if(topicNode.hasProperty("exo:postCount")) topicNew.setPostCount(topicNode.getProperty("exo:postCount").getLong()) ;
		if(topicNode.hasProperty("exo:viewCount")) topicNew.setViewCount(topicNode.getProperty("exo:viewCount").getLong()) ;
		if(topicNode.hasProperty("exo:numberAttachments")) topicNew.setNumberAttachment(topicNode.getProperty("exo:numberAttachments").getLong()) ;
		if(topicNode.hasProperty("exo:icon")) topicNew.setIcon(topicNode.getProperty("exo:icon").getString()) ;
		
		if(topicNode.hasProperty("exo:isNotifyWhenAddPost")) topicNew.setIsNotifyWhenAddPost(topicNode.getProperty("exo:isNotifyWhenAddPost").getBoolean()) ;
		if(topicNode.hasProperty("exo:isModeratePost")) topicNew.setIsModeratePost(topicNode.getProperty("exo:isModeratePost").getBoolean()) ;
		if(topicNode.hasProperty("exo:isClosed")) topicNew.setIsClosed(topicNode.getProperty("exo:isClosed").getBoolean()) ;
		if(topicNode.hasProperty("exo:isLock")) {
			if(topicNode.getParent().getProperty("exo:isLock").getBoolean()) topicNew.setIsLock(true);
			else topicNew.setIsLock(topicNode.getProperty("exo:isLock").getBoolean()) ;
		}
		if(topicNode.hasProperty("exo:isApproved")) topicNew.setIsApproved(topicNode.getProperty("exo:isApproved").getBoolean()) ;
		if(topicNode.hasProperty("exo:isSticky")) topicNew.setIsSticky(topicNode.getProperty("exo:isSticky").getBoolean()) ;
		if(topicNode.hasProperty("exo:canView")) topicNew.setCanView(ValuesToStrings(topicNode.getProperty("exo:canView").getValues())) ;
		if(topicNode.hasProperty("exo:canPost")) topicNew.setCanPost(ValuesToStrings(topicNode.getProperty("exo:canPost").getValues())) ;
		if(topicNode.hasProperty("exo:isPoll")) topicNew.setIsPoll(topicNode.getProperty("exo:isPoll").getBoolean()) ;
		if(topicNode.hasProperty("exo:userVoteRating")) topicNew.setUserVoteRating(ValuesToStrings(topicNode.getProperty("exo:userVoteRating").getValues())) ;
		if(topicNode.hasProperty("exo:tagId")) topicNew.setTagId(ValuesToStrings(topicNode.getProperty("exo:tagId").getValues())) ;
		if(topicNode.hasProperty("exo:voteRating")) topicNew.setVoteRating(topicNode.getProperty("exo:voteRating").getDouble()) ;
		String idFirstPost = topicNode.getName().replaceFirst("topic", "post") ;
		if(topicNode.hasNode(idFirstPost)) {
			Node FirstPostNode	= topicNode.getNode(idFirstPost) ;
			if(FirstPostNode.hasProperty("exo:numberAttachments")) {
				if(FirstPostNode.getProperty("exo:numberAttachments").getLong() > 0) {
					NodeIterator postAttachments = FirstPostNode.getNodes();
					List<ForumAttachment> attachments = new ArrayList<ForumAttachment>();
					Node nodeFile ;
					while (postAttachments.hasNext()) {
						Node node = postAttachments.nextNode();
						if (node.isNodeType("nt:file")) {
							JCRForumAttachment attachment = new JCRForumAttachment() ;
							nodeFile = node.getNode("jcr:content") ;
							attachment.setId(node.getPath());
							attachment.setMimeType(nodeFile.getProperty("jcr:mimeType").getString());
							attachment.setName(node.getName());
							attachment.setWorkspace(node.getSession().getWorkspace().getName()) ;
							attachment.setSize(nodeFile.getProperty("jcr:data").getStream().available());
							attachments.add(attachment);
						}
					}
					topicNew.setAttachments(attachments);
				}
			}
		}
		return topicNew;
	}

	public TopicView getTopicView(SessionProvider sProvider, String categoryId, String forumId, String topicId) throws Exception {
		TopicView topicview = new TopicView() ;
		topicview.setTopicView(getTopic(sProvider, categoryId, forumId, topicId, true)) ;
		topicview.setPageList(getPosts(sProvider, categoryId, forumId, topicId)) ;
		return topicview;
	}

	public void saveTopic(SessionProvider sProvider, String categoryId, String forumId, Topic topic, boolean isNew) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			if(CategoryNode.hasNode(forumId)) {
				Node forumNode = CategoryNode.getNode(forumId) ;
				Node topicNode;
				if(isNew) {
					topicNode = forumNode.addNode(topic.getId(), "exo:topic") ;
					topicNode.setProperty("exo:id", topic.getId()) ;
					topicNode.setProperty("exo:path", topicNode.getPath()) ;
					topicNode.setProperty("exo:createdDate", getGreenwichMeanTime()) ;
					topicNode.setProperty("exo:lastPostBy", topic.getLastPostBy()) ;
					topicNode.setProperty("exo:lastPostDate", getGreenwichMeanTime()) ;
					topicNode.setProperty("exo:postCount", -1) ;
					topicNode.setProperty("exo:viewCount", 0) ;
					topicNode.setProperty("exo:tagId", topic.getTagId());
					// setTopicCount for Forum
					long newTopicCount = forumNode.getProperty("exo:topicCount").getLong() + 1 ;
					forumNode.setProperty("exo:topicCount", newTopicCount ) ;
				} else {
					topicNode = forumNode.getNode(topic.getId()) ;
				}
				topicNode.setProperty("exo:owner", topic.getOwner()) ;
				topicNode.setProperty("exo:name", topic.getTopicName()) ;
				topicNode.setProperty("exo:modifiedBy", topic.getModifiedBy()) ;
				topicNode.setProperty("exo:modifiedDate", getGreenwichMeanTime()) ;
				topicNode.setProperty("exo:description", topic.getDescription()) ;
				topicNode.setProperty("exo:icon", topic.getIcon()) ;
				
				topicNode.setProperty("exo:isModeratePost", topic.getIsModeratePost()) ;
				topicNode.setProperty("exo:isNotifyWhenAddPost", topic.getIsNotifyWhenAddPost()) ;
				topicNode.setProperty("exo:isClosed", topic.getIsClosed()) ;
				topicNode.setProperty("exo:isLock", topic.getIsLock()) ;
				topicNode.setProperty("exo:isApproved", topic.getIsApproved()) ;
				topicNode.setProperty("exo:isSticky", topic.getIsSticky()) ;
				topicNode.setProperty("exo:canView", topic.getCanView()) ;
				topicNode.setProperty("exo:canPost", topic.getCanPost()) ;
				topicNode.setProperty("exo:userVoteRating", topic.getUserVoteRating()) ;
				topicNode.setProperty("exo:voteRating", topic.getVoteRating()) ;
				topicNode.setProperty("exo:numberAttachments", 0) ;
				//forumHomeNode.save() ;
				forumHomeNode.getSession().save() ;
				if(isNew) {
					// createPost first
					String id = topic.getId().replaceFirst("topic", "post") ;
					Post post = new Post() ;
					post.setId(id.toUpperCase()) ;
					post.setOwner(topic.getOwner()) ;
					post.setCreatedDate(new Date()) ;
					post.setModifiedBy(topic.getModifiedBy()) ;
					post.setModifiedDate(new Date()) ;
					post.setSubject(topic.getTopicName()) ;
					post.setMessage(topic.getDescription()) ;
					post.setRemoteAddr("") ;
					post.setIcon(topic.getIcon()) ;
					post.setIsApproved(false) ;
					post.setAttachments(topic.getAttachments()) ;
					
					savePost(sProvider, categoryId, forumId, topic.getId(), post, true) ;
				} else {
					String id = topic.getId().replaceFirst("topic", "post") ;
					if(topicNode.hasNode(id)) {
						Node fistPostNode = topicNode.getNode(id) ;
						Post post = getPost(fistPostNode) ;
						post.setModifiedBy(topic.getModifiedBy()) ;
						post.setModifiedDate(new Date()) ;
						post.setSubject(topic.getTopicName()) ;
						post.setMessage(topic.getDescription()) ;
						post.setIcon(topic.getIcon()) ;
						post.setAttachments(topic.getAttachments()) ;
						savePost(sProvider, categoryId, forumId, topic.getId(), post, false) ;
					}
				}
			}
		}
	}
	
	public Topic removeTopic(SessionProvider sProvider, String categoryId, String forumId, String topicId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Topic topic = new Topic() ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			Node forumNode = CategoryNode.getNode(forumId) ;
			topic = getTopic(sProvider, categoryId, forumId, topicId, false) ;
			// setTopicCount for Forum
			long newTopicCount = forumNode.getProperty("exo:topicCount").getLong() - 1 ;
			forumNode.setProperty("exo:topicCount", newTopicCount ) ;
			// setPostCount for Forum
			long newPostCount = forumNode.getProperty("exo:postCount").getLong() - topic.getPostCount() - 1;
			forumNode.setProperty("exo:postCount", newPostCount ) ;
			
			forumNode.getNode(topicId).remove() ;
			//forumHomeNode.save() ;
			forumHomeNode.getSession().save() ;
			return topic ;
		}
		return null ;
	}
	
	public void moveTopic(SessionProvider sProvider, String topicId, String topicPath, String destForumPath) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		String newTopicPath = destForumPath + "/" + topicId;
		//Forum remove Topic(srcForum)
		Node srcForumNode = (Node)forumHomeNode.getSession().getItem(topicPath).getParent() ;
		//Move Topic
		forumHomeNode.getSession().getWorkspace().move(topicPath, newTopicPath) ;
		//Set TopicCount srcForum
		srcForumNode.setProperty("exo:topicCount", srcForumNode.getProperty("exo:topicCount").getLong() - 1) ;
		//Set NewPath for srcForum
		Node lastTopicSrcForum = queryLastTopic(sProvider, srcForumNode.getPath()) ;
		if(lastTopicSrcForum != null) srcForumNode.setProperty("exo:lastTopicPath", lastTopicSrcForum.getPath()) ;
		//Topic Move
		Node topicNode = (Node)forumHomeNode.getSession().getItem(newTopicPath) ;
		topicNode.setProperty("exo:path", newTopicPath) ;
		long topicPostCount = topicNode.getProperty("exo:postCount").getLong() + 1 ;
		//Forum add Topic (destForum)
		Node destForumNode = (Node)forumHomeNode.getSession().getItem(destForumPath) ;
		destForumNode.setProperty("exo:topicCount", destForumNode.getProperty("exo:topicCount").getLong() + 1) ;
		Node lastTopicNewForum = queryLastTopic(sProvider, destForumNode.getPath()) ;
		if(lastTopicNewForum != null) destForumNode.setProperty("exo:lastTopicPath", lastTopicNewForum.getPath()) ;
		//Set PostCount
		srcForumNode.setProperty("exo:postCount", srcForumNode.getProperty("exo:postCount").getLong() - topicPostCount) ;
		destForumNode.setProperty("exo:postCount", destForumNode.getProperty("exo:postCount").getLong() + topicPostCount) ;
		
		//forumHomeNode.save() ;
		forumHomeNode.getSession().save() ;
	}
	

	public JCRPageList getPosts(SessionProvider sProvider, String categoryId, String forumId, String topicId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			if(CategoryNode.hasNode(forumId)) {
				Node forumNode = CategoryNode.getNode(forumId) ;
				if(forumNode.hasNode(topicId)) {
					Node topicNode = forumNode.getNode(topicId) ;
					NodeIterator iter = topicNode.getNodes() ; 
					JCRPageList pagelist = new ForumPageList(iter, 10, topicNode.getPath(), false) ;
					return pagelist ;
				}
			}
		}
		return null ;
	}
	
	public Post getPost(SessionProvider sProvider, String categoryId, String forumId, String topicId, String postId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			if(CategoryNode.hasNode(forumId)) {
				Node forumNode = CategoryNode.getNode(forumId) ;
				Node topicNode = forumNode.getNode(topicId) ;
				if(!topicNode.hasNode(postId)) return null;
				Node postNode = topicNode.getNode(postId) ;
				Post postNew = new Post() ;
				postNew = getPost(postNode) ;
				return postNew ;
			}
		}
		return null ;
	}

	private Post getPost(Node postNode) throws Exception {
		Post postNew = new Post() ;
		if(postNode.hasProperty("exo:id")) postNew.setId(postNode.getProperty("exo:id").getString()) ;
		if(postNode.hasProperty("exo:owner")) postNew.setOwner(postNode.getProperty("exo:owner").getString()) ;
		if(postNode.hasProperty("exo:path")) postNew.setPath(postNode.getProperty("exo:path").getString()) ;
		if(postNode.hasProperty("exo:createdDate")) postNew.setCreatedDate(postNode.getProperty("exo:createdDate").getDate().getTime()) ;
		if(postNode.hasProperty("exo:modifiedBy")) postNew.setModifiedBy(postNode.getProperty("exo:modifiedBy").getString()) ;
		if(postNode.hasProperty("exo:modifiedDate")) postNew.setModifiedDate(postNode.getProperty("exo:modifiedDate").getDate().getTime()) ;
		if(postNode.hasProperty("exo:subject")) postNew.setSubject(postNode.getProperty("exo:subject").getString()) ;
		if(postNode.hasProperty("exo:message")) postNew.setMessage(postNode.getProperty("exo:message").getString()) ;
		if(postNode.hasProperty("exo:remoteAddr")) postNew.setRemoteAddr(postNode.getProperty("exo:remoteAddr").getString()) ;
		if(postNode.hasProperty("exo:icon")) postNew.setIcon(postNode.getProperty("exo:icon").getString()) ;
		if(postNode.hasProperty("exo:isApproved")) postNew.setIsApproved(postNode.getProperty("exo:isApproved").getBoolean()) ;
		if(postNode.hasProperty("exo:numberAttach")) {
			if(postNode.getProperty("exo:numberAttach").getLong() > 0) {
				NodeIterator postAttachments = postNode.getNodes();
				List<ForumAttachment> attachments = new ArrayList<ForumAttachment>();
				Node nodeFile ;
				while (postAttachments.hasNext()) {
					Node node = postAttachments.nextNode();
					if (node.isNodeType("nt:file")) {
						JCRForumAttachment attachment = new JCRForumAttachment() ;
						nodeFile = node.getNode("jcr:content") ;
						attachment.setId(node.getPath());
						attachment.setMimeType(nodeFile.getProperty("jcr:mimeType").getString());
						attachment.setName(node.getName());
						attachment.setWorkspace(node.getSession().getWorkspace().getName()) ;
						attachment.setSize(nodeFile.getProperty("jcr:data").getStream().available());
						attachments.add(attachment);
					}
				}
				postNew.setAttachments(attachments);
			}
		}
		return postNew;
	}
	
	public void savePost(SessionProvider sProvider, String categoryId, String forumId, String topicId, Post post, boolean isNew) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			if(CategoryNode.hasNode(forumId)) {
				Node forumNode = CategoryNode.getNode(forumId) ;
				Node topicNode = forumNode.getNode(topicId) ;
				Node postNode;
				if(isNew) {
					postNode = topicNode.addNode(post.getId(), "exo:post") ;
					postNode.setProperty("exo:id", post.getId()) ;
					postNode.setProperty("exo:owner", post.getOwner()) ;
					postNode.setProperty("exo:path", postNode.getPath()) ;
					postNode.setProperty("exo:createdDate", getGreenwichMeanTime()) ;
				} else {
					postNode = topicNode.getNode(post.getId()) ;
				}
				postNode.setProperty("exo:modifiedBy", post.getModifiedBy()) ;
				postNode.setProperty("exo:modifiedDate", getGreenwichMeanTime()) ;
				postNode.setProperty("exo:subject", post.getSubject()) ;
				postNode.setProperty("exo:message", post.getMessage()) ;
				postNode.setProperty("exo:remoteAddr", post.getRemoteAddr()) ;
				postNode.setProperty("exo:icon", post.getIcon()) ;
				postNode.setProperty("exo:isApproved", post.getIsApproved()) ;
				long numberAttach = 0 ;
				List<ForumAttachment> attachments = post.getAttachments();
				if(attachments != null) { 
					Iterator<ForumAttachment> it = attachments.iterator();
					while (it.hasNext()) {
						BufferAttachment file = (BufferAttachment)it.next();
						Node nodeFile = null;
						if (!postNode.hasNode(file.getName())) nodeFile = postNode.addNode(file.getName(), "nt:file");
						else nodeFile = postNode.getNode(file.getName());
						Node nodeContent = null;
						if (!nodeFile.hasNode("jcr:content")) nodeContent = nodeFile.addNode("jcr:content", "nt:resource");
						else nodeContent = nodeFile.getNode("jcr:content");
						nodeContent.setProperty("jcr:mimeType", file.getMimeType());
						nodeContent.setProperty("jcr:data", file.getInputStream());
						nodeContent.setProperty("jcr:lastModified", Calendar.getInstance().getTimeInMillis());
						++ numberAttach ;
					}
				}				
				if(isNew) {
					// set InfoPost for Topic
					long topicPostCount = topicNode.getProperty("exo:postCount").getLong() + 1 ;
					topicNode.setProperty("exo:postCount", topicPostCount ) ;
					topicNode.setProperty("exo:lastPostDate", getGreenwichMeanTime()) ;
					long newNumberAttach =	topicNode.getProperty("exo:numberAttachments").getLong() + numberAttach ;
					topicNode.setProperty("exo:numberAttachments", newNumberAttach);
					// set InfoPost for Forum
					long forumPostCount = forumNode.getProperty("exo:postCount").getLong() + 1 ;
					forumNode.setProperty("exo:postCount", forumPostCount ) ;
					forumNode.setProperty("exo:lastTopicPath", topicNode.getPath()) ;
				} else {
					long temp = topicNode.getProperty("exo:numberAttachments").getLong() -	postNode.getProperty("exo:numberAttach").getLong() ;
					topicNode.setProperty("exo:numberAttachments", (temp + numberAttach));
				}
				postNode.setProperty("exo:numberAttach", numberAttach) ;
				//forumHomeNode.save() ;
				forumHomeNode.getSession().save() ;
			}
		}
	}
	
	public Post removePost(SessionProvider sProvider, String categoryId, String forumId, String topicId, String postId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Post post = new Post() ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			if(CategoryNode.hasNode(forumId)) {
				post = getPost(sProvider, categoryId, forumId, topicId, postId) ;
				Node forumNode = CategoryNode.getNode(forumId) ;
				Node topicNode = forumNode.getNode(topicId) ;
				long numberAttachs = topicNode.getNode(postId).getProperty("exo:numberAttach").getLong() ; 
				topicNode.getNode(postId).remove() ;
				// setPostCount for Topic
				long topicPostCount = topicNode.getProperty("exo:postCount").getLong() - 1 ;
				topicNode.setProperty("exo:postCount", topicPostCount ) ;
				long newNumberAttachs = topicNode.getProperty("exo:numberAttachments").getLong() - numberAttachs ;
				topicNode.setProperty("exo:numberAttachments", newNumberAttachs) ;
				// setPostCount for Forum
				long forumPostCount = forumNode.getProperty("exo:postCount").getLong() - 1 ;
				forumNode.setProperty("exo:postCount", forumPostCount ) ;

				//forumHomeNode.save() ;
				forumHomeNode.getSession().save() ;
				return post;
			}
		}
		return null;
	}
	
	public void movePost(SessionProvider sProvider, String postId, String postPath, String destTopicPath) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		String newPostPath = destTopicPath + "/" + postId;
		//Node Topic move Post
		Node srcTopicNode = (Node)forumHomeNode.getSession().getItem(postPath).getParent() ;
		Node srcForumNode = (Node)srcTopicNode.getParent() ;
		srcForumNode.setProperty("exo:postCount", srcForumNode.getProperty("exo:postCount").getLong() - 1 ) ;
		srcTopicNode.setProperty("exo:postCount", srcTopicNode.getProperty("exo:postCount").getLong() - 1 ) ;
		forumHomeNode.getSession().getWorkspace().move(postPath, newPostPath) ;
		//Node Post move
		Node postNode = (Node)forumHomeNode.getSession().getItem(newPostPath) ;
		long numberAttach = postNode.getProperty("exo:numberAttach").getLong() ;
		srcTopicNode.setProperty("exo:numberAttachments", srcTopicNode.getProperty("exo:numberAttachments").getLong() - numberAttach);
		postNode.setProperty("exo:path", newPostPath) ;
		//Node Topic add Post
		Node destTopicNode = (Node)forumHomeNode.getSession().getItem(destTopicPath) ;
		Node destForumNode = (Node)destTopicNode.getParent() ;
		destTopicNode.setProperty("exo:postCount", destTopicNode.getProperty("exo:postCount").getLong() + 1 ) ;
		destTopicNode.setProperty("exo:numberAttachments", destTopicNode.getProperty("exo:numberAttachments").getLong() + numberAttach);
		destForumNode.setProperty("exo:postCount", destForumNode.getProperty("exo:postCount").getLong() + 1 ) ;
		forumHomeNode.save() ;
		forumHomeNode.getSession().save() ;
	}
	
	public Poll getPoll(SessionProvider sProvider, String categoryId, String forumId, String topicId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			if(CategoryNode.hasNode(forumId)) {
				Node forumNode = CategoryNode.getNode(forumId) ;
				Node topicNode = forumNode.getNode(topicId) ;
				String pollId = topicId.replaceFirst("topic", "poll") ;
				if(!topicNode.hasNode(pollId)) return null;
				Node pollNode = topicNode.getNode(pollId) ;
				Poll pollNew = new Poll() ;
				pollNew.setId(pollId) ;
				if(pollNode.hasProperty("exo:owner")) pollNew.setOwner(pollNode.getProperty("exo:owner").getString()) ;
				if(pollNode.hasProperty("exo:createdDate")) pollNew.setCreatedDate(pollNode.getProperty("exo:createdDate").getDate().getTime()) ;
				if(pollNode.hasProperty("exo:modifiedBy")) pollNew.setModifiedBy(pollNode.getProperty("exo:modifiedBy").getString()) ;
				if(pollNode.hasProperty("exo:modifiedDate")) pollNew.setModifiedDate(pollNode.getProperty("exo:modifiedDate").getDate().getTime()) ;
				if(pollNode.hasProperty("exo:timeOut")) pollNew.setTimeOut(pollNode.getProperty("exo:timeOut").getLong()) ;
				if(pollNode.hasProperty("exo:question")) pollNew.setQuestion(pollNode.getProperty("exo:question").getString()) ;
				
				if(pollNode.hasProperty("exo:option")) pollNew.setOption(ValuesToStrings(pollNode.getProperty("exo:option").getValues())) ;
				if(pollNode.hasProperty("exo:vote")) pollNew.setVote(ValuesToStrings(pollNode.getProperty("exo:vote").getValues())) ;
				
				if(pollNode.hasProperty("exo:userVote")) pollNew.setUserVote(ValuesToStrings(pollNode.getProperty("exo:userVote").getValues())) ;
				if(pollNode.hasProperty("exo:isMultiCheck")) pollNew.setIsMultiCheck(pollNode.getProperty("exo:isMultiCheck").getBoolean()) ;
				return pollNew ;
			}
		}
		return null ;
	}
	
	public Poll removePoll(SessionProvider sProvider, String categoryId, String forumId, String topicId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Poll poll = new Poll() ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			if(CategoryNode.hasNode(forumId)) {
				poll = getPoll(sProvider, categoryId, forumId, topicId) ;
				Node forumNode = CategoryNode.getNode(forumId) ;
				Node topicNode = forumNode.getNode(topicId) ;
				String pollId = topicId.replaceFirst("topic", "poll") ;
				topicNode.getNode(pollId).remove() ;
				topicNode.setProperty("exo:isPoll", false) ;
				//forumHomeNode.save() ;
				forumHomeNode.getSession().save() ;
				return poll;
			}
		}
		return null;
	}
	
	public void savePoll(SessionProvider sProvider, String categoryId, String forumId, String topicId, Poll poll, boolean isNew, boolean isVote) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(categoryId)) {
			Node CategoryNode = forumHomeNode.getNode(categoryId) ;
			if(CategoryNode.hasNode(forumId)) {
				Node forumNode = CategoryNode.getNode(forumId) ;
				Node topicNode = forumNode.getNode(topicId) ;
				Node pollNode;
				String pollId = topicId.replaceFirst("topic", "poll") ;
				if(isVote) {
					pollNode = topicNode.getNode(pollId) ;
					pollNode.setProperty("exo:vote", poll.getVote()) ;
					pollNode.setProperty("exo:userVote", poll.getUserVote()) ;
				} else {
					if(isNew) {
						pollNode = topicNode.addNode(pollId, "exo:poll") ;
						pollNode.setProperty("exo:id", pollId) ;
						pollNode.setProperty("exo:owner", poll.getOwner()) ;
						pollNode.setProperty("exo:userVote", new String[] {}) ;
						pollNode.setProperty("exo:createdDate", getGreenwichMeanTime()) ;
						topicNode.setProperty("exo:isPoll", true);
					} else {
						pollNode = topicNode.getNode(pollId) ;
					}
					if(poll.getUserVote().length > 0) {
						pollNode.setProperty("exo:userVote", poll.getUserVote()) ;
					}
					pollNode.setProperty("exo:vote", poll.getVote()) ;
					pollNode.setProperty("exo:modifiedBy", poll.getModifiedBy()) ;
					pollNode.setProperty("exo:modifiedDate", getGreenwichMeanTime()) ;
					pollNode.setProperty("exo:timeOut", poll.getTimeOut()) ;
					pollNode.setProperty("exo:question", poll.getQuestion()) ;
					pollNode.setProperty("exo:option", poll.getOption()) ;
					pollNode.setProperty("exo:isMultiCheck", poll.getIsMultiCheck()) ;
				}
				//forumHomeNode.save() ;
				forumHomeNode.getSession().save() ;
			}
		}
	}

	
	
	public void addTopicInTag(SessionProvider sProvider, String tagId, String topicPath) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Node topicNode = (Node)getForumHomeNode(sProvider).getSession().getItem(topicPath);
		if(topicNode.hasProperty("exo:tagId")) {
			String []oldTagsId = ValuesToStrings(topicNode.getProperty("exo:tagId").getValues()) ;
			int t = oldTagsId.length ;
			String []newTagsId = new String[t+1];
			for (int i = 0; i < t; i++) {
				newTagsId[i] = oldTagsId[i];
			}
			newTagsId[t] = tagId ;
			topicNode.setProperty("exo:tagId", newTagsId);
			forumHomeNode.save() ;
			forumHomeNode.getSession().save() ;
		}
	}
	
	public void removeTopicInTag(SessionProvider sProvider, String tagId, String topicPath) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Node topicNode = (Node)getForumHomeNode(sProvider).getSession().getItem(topicPath);
		String []oldTagsId = ValuesToStrings(topicNode.getProperty("exo:tagId").getValues()) ;
		int t = oldTagsId.length, j = 0 ;
		String []newTagsId = new String[t-1];
		for (int i = 0; i < t; i++) {
			if(!oldTagsId[i].equals(tagId)){
				newTagsId[j] = oldTagsId[i];
				++j ;
			}
		}
		topicNode.setProperty("exo:tagId", newTagsId);
		forumHomeNode.save() ;
		forumHomeNode.getSession().save() ;
	}
	
	public Tag getTag(SessionProvider sProvider, String tagId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		if(forumHomeNode.hasNode(tagId)) {
			Node tagNode ;
			tagNode = forumHomeNode.getNode(tagId) ;
			return getTagNode(tagNode) ;
		}
		return null ;
	}
	
	public List<Tag> getTags(SessionProvider sProvider) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		QueryManager qm = forumHomeNode.getSession().getWorkspace().getQueryManager() ;
		StringBuffer queryString = new StringBuffer("/jcr:root" + forumHomeNode.getPath() +"//element(*,exo:forumTag)") ;
		Query query = qm.createQuery(queryString.toString(), Query.XPATH) ;
		QueryResult result = query.execute() ;
		NodeIterator iter = result.getNodes() ;
		List<Tag>tags = new ArrayList<Tag>() ;
		while (iter.hasNext()) {
			Node tagNode = iter.nextNode() ;
			tags.add(getTagNode(tagNode)) ;
		}
		return tags;
	}
	
	private Tag getTagNode(Node tagNode) throws Exception {
		Tag newTag = new Tag() ;
		if(tagNode.hasProperty("exo:id"))newTag.setId(tagNode.getProperty("exo:id").getString());
		if(tagNode.hasProperty("exo:owner"))newTag.setOwner(tagNode.getProperty("exo:owner").getString());
		if(tagNode.hasProperty("exo:name"))newTag.setName(tagNode.getProperty("exo:name").getString());
		if(tagNode.hasProperty("exo:description"))newTag.setDescription(tagNode.getProperty("exo:description").getString());
		if(tagNode.hasProperty("exo:color"))newTag.setColor(tagNode.getProperty("exo:color").getString());
		return newTag;
	}
	
	public List<Tag> getTagsByUser(SessionProvider sProvider, String userName) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		QueryManager qm = forumHomeNode.getSession().getWorkspace().getQueryManager() ;
		String pathQuery = "/jcr:root" + forumHomeNode.getPath() + "//element(*,exo:forumTag)[@exo:owner='"+userName+"']";
		Query query = qm.createQuery(pathQuery , Query.XPATH) ;
		QueryResult result = query.execute() ;
		NodeIterator iter = result.getNodes(); 
		List<Tag>tags = new ArrayList<Tag>() ;
		while (iter.hasNext()) {
			Node tagNode = iter.nextNode() ;
			tags.add(getTagNode(tagNode)) ;
		}
		return tags;
	}
	
	public List<Tag> getTagsByTopic(SessionProvider sProvider, String[] tagIds) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		QueryManager qm = forumHomeNode.getSession().getWorkspace().getQueryManager() ;
		StringBuffer queryString = new StringBuffer("/jcr:root" + forumHomeNode.getPath() +"//element(*,exo:forumTag)") ;
		Query query = qm.createQuery(queryString.toString(), Query.XPATH) ;
		QueryResult result = query.execute() ;
		NodeIterator iter = result.getNodes() ;
		List<Tag>tags = new ArrayList<Tag>() ;
		while (iter.hasNext()) {
			Node tagNode = iter.nextNode() ;
			String nodeId = tagNode.getName() ;
			for(String tagId : tagIds) {
				if(nodeId.equals(tagId)){ 
					tags.add(getTagNode(tagNode)) ;
					break ;
				}
			}
		}
		return tags;
	}
	
	public JCRPageList getTopicsByTag(SessionProvider sProvider, String tagId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		QueryManager qm = forumHomeNode.getSession().getWorkspace().getQueryManager() ;
		String pathQuery = "/jcr:root" + forumHomeNode.getPath() + "//element(*,exo:topic)[@exo:tagId='"+tagId+"']";
		Query query = qm.createQuery(pathQuery , Query.XPATH) ;
		QueryResult result = query.execute() ;
		NodeIterator iter = result.getNodes(); 
		JCRPageList pagelist = new ForumPageList(iter, 10, "", false) ;
		return pagelist ;
	}

	public void saveTag(SessionProvider sProvider, Tag newTag, boolean isNew) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Node newTagNode ;
		if(isNew) {
			newTagNode = forumHomeNode.addNode(newTag.getId(), "exo:forumTag") ;
			newTagNode.setProperty("exo:id", newTag.getId()) ;
			newTagNode.setProperty("exo:owner", newTag.getOwner()) ;
		} else {
			newTagNode = forumHomeNode.getNode(newTag.getId()) ;
		}
		newTagNode.setProperty("exo:name", newTag.getName()) ;
		newTagNode.setProperty("exo:description", newTag.getDescription()) ;
		newTagNode.setProperty("exo:color", newTag.getColor()) ;
		forumHomeNode.save() ;
		forumHomeNode.getSession().save() ;
	}
	
	public void removeTag(SessionProvider sProvider, String tagId) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		forumHomeNode.getNode(tagId).remove() ;
		forumHomeNode.save() ;
		forumHomeNode.getSession().save() ;
	}
	
	
	
	public ForumOption getOption(SessionProvider sProvider, String userName) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Node newOptionNode ;
		ForumOption forumOption = new ForumOption();
		userName = userName.trim().replaceAll(" ", "0") ;
		if(forumHomeNode.hasNode(userName)) {
			newOptionNode = forumHomeNode.getNode(userName) ;
			if(newOptionNode.hasProperty("exo:userName"))forumOption.setUserName(userName);
			if(newOptionNode.hasProperty("exo:timeZone"))forumOption.setTimeZone(newOptionNode.getProperty("exo:timeZone").getDouble());
			if(newOptionNode.hasProperty("exo:shortDateformat"))forumOption.setShortDateFormat(newOptionNode.getProperty("exo:shortDateformat").toString());
			if(newOptionNode.hasProperty("exo:longDateformat"))forumOption.setLongDateFormat(newOptionNode.getProperty("exo:longDateformat").toString());
			if(newOptionNode.hasProperty("exo:timeFormat"))forumOption.setTimeFormat(newOptionNode.getProperty("exo:timeFormat").toString());
			if(newOptionNode.hasProperty("exo:maxPost"))forumOption.setMaxPostInPage(newOptionNode.getProperty("exo:maxPost").getLong());
			if(newOptionNode.hasProperty("exo:maxTopic"))forumOption.setMaxTopicInPage(newOptionNode.getProperty("exo:maxTopic").getLong());
			if(newOptionNode.hasProperty("exo:isShowForumJump"))forumOption.setIsShowForumJump(newOptionNode.getProperty("exo:isShowForumJump").getBoolean());
			return forumOption;
		}
		return null ;
  }

	public void saveOption(SessionProvider sProvider, ForumOption newOption, boolean isNew) throws Exception {
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		Node newOptionNode ;
		if(isNew) {
			String optionId = newOption.getUserName().trim().replaceAll(" ", "0") ;
			newOptionNode = forumHomeNode.addNode(optionId, "exo:forumOption") ;
			newOptionNode.setProperty("exo:userName", optionId);
		} else {
			newOptionNode = forumHomeNode.getNode(newOption.getUserName()) ;
		}
		newOptionNode.setProperty("exo:timeZone", newOption.getTimeZone());
		newOptionNode.setProperty("exo:shortDateformat", newOption.getTimeZone());
		newOptionNode.setProperty("exo:longDateformat", newOption.getTimeZone());
		newOptionNode.setProperty("exo:timeFormat", newOption.getTimeZone());
		newOptionNode.setProperty("exo:maxPost", newOption.getTimeZone());
		newOptionNode.setProperty("exo:maxTopic", newOption.getTimeZone());
		newOptionNode.setProperty("exo:isShowForumJump", newOption.getTimeZone());
		forumHomeNode.save() ;
		forumHomeNode.getSession().save() ;
  }
	
	
	@SuppressWarnings("unchecked")
	public List getPage(long page, JCRPageList pageList, SessionProvider sProvider) throws Exception {
		try {
			return pageList.getPage(page, getForumHomeNode(sProvider).getSession()) ;
		} catch (Exception e) {
			return null ;
		}
	}

	private String [] ValuesToStrings(Value[] Val) throws Exception {
		if(Val.length == 1) return new String[]{Val[0].getString()} ;
		String[] Str = new String[Val.length] ;
		for(int i = 0; i < Val.length; ++i) {
			Str[i] = Val[i].getString() ;
		}
		return Str;
	}
	
	@SuppressWarnings("deprecation")
  private Calendar getGreenwichMeanTime() {
		Date date = new Date() ;
		int hostZone = date.getTimezoneOffset()/60 ;
		date.setHours(date.getHours() + hostZone);
		TimeZone timeZone2 = TimeZone.getTimeZone("GMT+00:00") ;
		Calendar calendar  = GregorianCalendar.getInstance(timeZone2) ;
		calendar.setTimeZone(timeZone2);
		calendar.setTime(date);
		return calendar ;
	}
	
	public Object getObjectNameByPath(SessionProvider sProvider, String path) throws Exception {
		Object object = new Object() ;
		Node myNode = (Node)getForumHomeNode(sProvider).getSession().getItem(path) ;
		if(path.indexOf("post") > 0) {
			Post post = new Post() ;
			post.setId(myNode.getName()) ;
			post.setPath(path);
			post.setSubject(myNode.getProperty("exo:subject").getString()) ;
			object = (Object)post ;
		}else if(path.indexOf("topic") > 0) {
			Topic topic = new Topic() ;
			topic.setId(myNode.getName()) ;
			topic.setPath(path);
			topic.setTopicName(myNode.getProperty("exo:name").getString()) ;
			object = (Object)topic;
		}else if(path.indexOf("forum") > 0) {
			Forum forum = new Forum() ;
			forum.setId(myNode.getName()) ;
			forum.setPath(path);
			forum.setForumName(myNode.getProperty("exo:name").getString());
			object = (Object)forum ;
		}else if(path.indexOf("category") > 0) {
			Category category = new Category() ;
			category.setId(myNode.getName()) ;
			category.setPath(path);
			category.setCategoryName(myNode.getProperty("exo:name").getString()) ;
			object = (Object)category ;
		} else if(path.indexOf("tag") > 0){
			Tag tag = new Tag() ;
			tag.setId(myNode.getName()) ;
			tag.setName(myNode.getProperty("exo:name").getString()) ;
			object = (Object)tag ;
		} else return null ;

		return object;
	}
	
	//TODO Need to review
	public List<ForumLinkData> getAllLink(SessionProvider sProvider) throws Exception {
		List<ForumLinkData> forumLinks = new ArrayList<ForumLinkData>() ;
		Node forumHomeNode = getForumHomeNode(sProvider) ;
		QueryManager qm = forumHomeNode.getSession().getWorkspace().getQueryManager() ;
		StringBuffer queryString = new StringBuffer("/jcr:root" + forumHomeNode.getPath() +"//element(*,exo:forumCategory) order by @exo:categoryOrder ascending") ;
		Query query = qm.createQuery(queryString.toString(), Query.XPATH) ;
		QueryResult result = query.execute() ;
		NodeIterator iter = result.getNodes() ;
		ForumLinkData linkData = new ForumLinkData() ;
		while(iter.hasNext()) {
			linkData = new ForumLinkData() ;
			Node cateNode = iter.nextNode() ;
			linkData.setId(cateNode.getName());
			linkData.setName("&nbsp; &nbsp; " + cateNode.getProperty("exo:name").getString() + "/categoryLink");
			linkData.setType("category");
			linkData.setPath(cateNode.getName());
			forumLinks.add(linkData) ;
			{
				queryString = new StringBuffer("/jcr:root" + cateNode.getPath() + "//element(*,exo:forum) order by @exo:forumOrder ascending,@exo:createdDate ascending");
				query = qm.createQuery(queryString.toString(), Query.XPATH) ;
				result = query.execute() ;
				NodeIterator iterForum = result.getNodes() ;
				while (iterForum.hasNext()) {
					linkData = new ForumLinkData() ;
					Node forumNode = (Node) iterForum.nextNode();
					linkData.setId(forumNode.getName());
					linkData.setName("&nbsp; &nbsp; &nbsp; &nbsp; " + forumNode.getProperty("exo:name").getString() + "/forumLink");
					linkData.setType("forum");
					linkData.setPath(cateNode.getName() + "/" + forumNode.getName());
					forumLinks.add(linkData) ;
				}
			}
		}
		return forumLinks ;
	}

}
