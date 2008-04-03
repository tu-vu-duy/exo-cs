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
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.scheduler.JobInfo;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class CheckMailJob extends Thread implements Job, Runnable  {
  private Thread thread ;
  private CheckingInfo checkingInfo_ ;
  
	public CheckMailJob() throws Exception {
		setDaemon(true) ;	
		start() ;
		
	}
	public void start() { 
		//System.out.println("\n\n\n\n >>>>>>>> Started \n\n\n") ;
		if ( thread == null ) { 
    	thread = new Thread(this); 
    	thread.start(); 
    }
    checkingInfo_ = new CheckingInfo() ;
	} 
	
	public void destroy() {
		//System.out.println("\n\n\n\n >>>>>>>> detroy \n\n\n") ;
		thread.stop() ;
		thread = null ;
	}
	/*public void stop() { 
	    if ( thread != null ) { 
	    	thread.stop(); 
	    	thread = null; 
	    } 
	}*/
	
	private static Log log_ = ExoLogger.getLogger("job.RecordsJob");
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			ExoContainer container = ExoContainerContext.getCurrentContainer();
			MailService mailService = 
				(MailService) container.getComponentInstanceOfType(MailService.class);
			JobSchedulerService schedulerService = 
				(JobSchedulerService) container.getComponentInstanceOfType(JobSchedulerService.class);
			String name = context.getJobDetail().getName() ;
			JobInfo info = new JobInfo(context.getJobDetail().getName(), "CollaborationSuite-webmail", context.getJobDetail().getJobClass()) ;
			if(name != null && name.indexOf(":") > 0) {
				String[] array = name.split(":") ;
        String username = array[0].trim() ;
        String accId = array[1].trim() ;
        checkingInfo_.setAccountId(accId) ;
			  mailService.checkNewMessage(SessionProvider.createSystemProvider(), username, accId, checkingInfo_) ;
			}
			schedulerService.removeJob(info) ;
			System.out.println("\n\n####  Checking mail of " + context.getJobDetail().getName()+ " finished ");
			
		} catch (Exception e) {
			e.printStackTrace();			
		}
		if (log_.isDebugEnabled())
			log_.debug("File plan job done");
	}
	
}
