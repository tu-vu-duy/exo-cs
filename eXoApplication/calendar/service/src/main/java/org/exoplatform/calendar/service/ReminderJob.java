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
package org.exoplatform.calendar.service;

import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.core.MediaType;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ReminderJob implements Job {
  private static Log log_ = ExoLogger.getLogger("cs.calendar.job");

  public void execute(JobExecutionContext context) throws JobExecutionException {
    PortalContainer container = Utils.getPortalContainer(context);
    if (container == null)
      return;
    ExoContainer oldContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      MailService mailService = (MailService) container.getComponentInstanceOfType(MailService.class);
      if (log_.isDebugEnabled())
        log_.debug("Calendar email reminder service");
      java.util.Calendar fromCalendar = GregorianCalendar.getInstance();
      JobDataMap jdatamap = context.getJobDetail().getJobDataMap();
      Node calendarHome = Utils.getPublicServiceHome(provider);
      if (calendarHome == null)
        return;
      StringBuffer path = new StringBuffer(PopupReminderJob.getReminderPath(fromCalendar, provider));
      path.append("//element(*,exo:reminder)");
      path.append("[@exo:remindDateTime <= xs:dateTime('" + ISO8601.format(fromCalendar) + "') and @exo:isOver = 'false' and @exo:reminderType = 'email' ]");
      QueryManager queryManager = Utils.getSession(provider).getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(path.toString(), Query.XPATH);
      QueryResult results = query.execute();
      NodeIterator iter = results.getNodes();
      Message message;
      Node reminder;
      while (iter.hasNext()) {
        reminder = iter.nextNode();
        boolean isRepeat = reminder.getProperty(Utils.EXO_IS_REPEAT).getBoolean();
        long fromTime = reminder.getProperty(Utils.EXO_FROM_DATE_TIME).getDate().getTimeInMillis();
        long remindTime = reminder.getProperty(Utils.EXO_REMINDER_DATE).getDate().getTimeInMillis();
        long interval = reminder.getProperty(Utils.EXO_TIME_INTERVAL).getLong() * 60 * 1000;
        String to = reminder.getProperty(Utils.EXO_EMAIL).getString();
        if (to != null && to.length() > 0) {
          message = new Message();
          message.setMimeType(MediaType.TEXT_HTML);
          message.setTo(to);
          message.setSubject("[reminder] eXo calendar notify mail !");
          message.setBody(reminder.getProperty(Utils.EXO_DESCRIPTION).getString());
          message.setFrom(jdatamap.getString("account"));
          if (isRepeat) {
            if (fromCalendar.getTimeInMillis() >= fromTime) {
              reminder.setProperty(Utils.EXO_IS_OVER, true);
            } else {
              if ((remindTime + interval) > fromTime) {
                reminder.setProperty(Utils.EXO_IS_OVER, true);
              } else {
                java.util.Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(remindTime + interval);
                reminder.setProperty(Utils.EXO_REMINDER_DATE, cal);
                reminder.setProperty(Utils.EXO_IS_OVER, false);
              }
            }
          } else {
            reminder.setProperty(Utils.EXO_IS_OVER, true);
          }
          reminder.save();
          mailService.sendMessage(message);
        }
      }
    } catch (RepositoryException e) {
      if (log_.isDebugEnabled())
        log_.debug("Data base not ready !");
    } catch (Exception e) {
      if (log_.isDebugEnabled()) {
        log_.debug("Exception in method execute", e);
      }
    } finally {
      provider.close();
      ExoContainerContext.setCurrentContainer(oldContainer);
    }
    if (log_.isDebugEnabled())
      log_.debug("File plan job done");
  }

}
