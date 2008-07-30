/**
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
 **/
package org.exoplatform.mail.service;

import org.apache.commons.logging.Log;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.scheduler.JobInfo;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class CheckMailJob extends Thread implements Job, Runnable  {
  private Thread thread ;
  
	public CheckMailJob() throws Exception {
		setDaemon(true) ;	
		start() ;
		
	}
	public void start() { 
		if ( thread == null ) { 
    	thread = new Thread(this); 
    	thread.start(); 
    }
	} 
	
	@SuppressWarnings("deprecation")
  public void destroy() {
		thread.stop() ;
		thread = null ;
	} 
	
	private static Log log_ = ExoLogger.getLogger("job.RecordsJob");

  @SuppressWarnings("deprecation")
  public void execute(JobExecutionContext context) throws JobExecutionException {
	  try {
		  //TODO : khdung
		  // getting service references from ExoContainer is risky ... this one is another thread.
		  // it's better (and of course correct) if we get static references.
		  MailService mailService = Utils.getMailService();
		  JobSchedulerService schedulerService = Utils.getJobSchedulerService();
		  String name = context.getJobDetail().getName();
		  JobInfo info = new JobInfo(context.getJobDetail().getName(), "CollaborationSuite-webmail",
				  context.getJobDetail().getJobClass());
		  if (name != null && name.indexOf(":") > 0) {
			  String[] array = name.split(":") ;
			  mailService.checkNewMessage(SessionProvider.createSystemProvider(), array[0].trim(), array[1].trim()) ;
		  }
		  schedulerService.removeJob(info) ;
		  System.out.println("\n\n####  Checking mail of " + context.getJobDetail().getName()+ " finished ");

	  } catch (Exception e) {
		  e.printStackTrace();			
	  }
	  if (log_.isDebugEnabled()) {
		log_.debug("File plan job done");
	  }
  }
}
