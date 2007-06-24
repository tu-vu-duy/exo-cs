/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.mail.service;

import java.io.InputStream;

/**
 * Created by The eXo Platform SARL
 * Author : Tuan Nguyen
 *          tuan.nguyen@exoplatform.com
 * Jun 23, 2007  
 */
abstract public class Attachment {
  public String id_ ;
  
  public String getId() { return id_ ; }
  public void setId(String id) { id_ = id ; }
  
  abstract InputStream getInputStream() throws Exception ;
  
  public String getMessageId() { return null ; }
  
}
