/**
 * @author Uoc Nguyen
 */
function MailServiceHandler() {
  this.START_CHECKMAIL_STATUS = 101;
  this.CONNECTION_FAILURE = 102 ;
  this.RETRY_PASSWORD = 103
  this.DOWNLOADING_MAIL_STATUS = 150;
  this.NO_UPDATE_STATUS = 201;
  this.FINISHED_CHECKMAIL_STATUS = 200;
  this.REQUEST_STOP_STATUS = 202;
  
  this.SERVICE_BASED_URL = '/portal/rest/cs/mail';
  this.CHECK_MAIL_ACTION = 'check mail action';
  this.GET_CHECK_MAIL_INFO_ACTION = 'get check mail info action';
  this.STOP_CHECK_MAIL_ACTION = 'stop check mail action';
  this.MAX_TRY = 3;
  this.tryCount = 0;
  this.isUpdateUI_ = true ;
}

MailServiceHandler.prototype = new eXo.cs.webservice.core.WebserviceHandler();

MailServiceHandler.prototype.initService = function(uiId, userName, accountId, checkMailInterval) {
  this.checkMailInterval = checkMailInterval;
  this.uiId = uiId;
  this.accountId = accountId;
  this.userName = userName;
  if (this.checkMailInterval &&
      !isNaN(this.checkMailInterval) && (parseInt(this.checkMailInterval) > 0) ) {
    this.checkMailInterval = parseInt(this.checkMailInterval);
    window.setInterval(eXo.mail.MailServiceHandler.checkMailWrapper, this.checkMailInterval);
  }
  if (eXo.core.Browser.getCookie('cs.mail.checkingmail' + this.accountId) == 'true') {
    this.checkMail();
  }
};

/**
 * 
 * @param {Integer} state
 * @param {eXo.portal.AjaxRequest}} requestObj
 * @param {String} action
 */
MailServiceHandler.prototype.update = function(state, requestObj, action) {
  if (state == this.SUCCESS_STATE && requestObj.responseXML) {
    try {
      eval ("this.serverData = " + eXo.cs.Utils.xml2json(requestObj.responseXML, ''));
    } catch(e) {
      this.updateUI(this.ERROR_STATE);
    }
    if (!this.serverData.info ||
    		!this.serverData.info.checkingmail ||
        !this.serverData.info.checkingmail.status) {
      return;
    }
    var status = parseInt(this.serverData.info.checkingmail.status);
    var url = false;
    if (status == this.START_CHECKMAIL_STATUS ||
        status == this.DOWNLOADING_MAIL_STATUS ||
        status == this.NO_UPDATE_STATUS) {
      url = this.SERVICE_BASED_URL + '/checkmailjobinfo/' + this.userName + '/' + this.accountId + '/';
    }
    if (url && url != '') {
      this.activeAction = this.GET_CHECK_MAIL_INFO_ACTION;
      this.makeRequest(url, this.HTTP_GET, '', this.GET_CHECK_MAIL_INFO_ACTION);
    }
    if (status != this.NO_UPDATE_STATUS) {
      this.updateUI(status);
    }
    if (status == this.FINISHED_CHECKMAIL_STATUS || status == this.CONNECTION_FAILURE || status == this.RETRY_PASSWORD ||
        status == this.REQUEST_STOP_STATUS) {
      this.destroy();    
    }
  } else if(state == this.ERROR_STATE) {
    if (this.activeAction == this.GET_CHECK_MAIL_INFO_ACTION &&
    	  this.tryCount < this.MAX_TRY) {
			this.activeAction = this.GET_CHECK_MAIL_INFO_ACTION;
			this.tryCount ++;
    	url = this.SERVICE_BASED_URL + '/checkmailjobinfo/' + this.userName + '/' + this.accountId + '/';
    	this.makeRequest(url, this.HTTP_GET, '', this.GET_CHECK_MAIL_INFO_ACTION);
    }
    this.updateUI(state);
    this.destroy();    
  }
};

MailServiceHandler.prototype.checkMailWrapper = function() {
  eXo.mail.MailServiceHandler.checkMail();
};

MailServiceHandler.prototype.checkMail = function(isUpdateUI) {
  if (!this.accountId ||
      !this.userName) {
    return;
  }
  this.isUpdateUI_ = isUpdateUI ;
  this.activeAction = this.CHECK_MAIL_ACTION;
  this.tryCount = 0;
  eXo.core.Browser.setCookie('cs.mail.checkingmail' + this.accountId, 'true');
  var url = this.SERVICE_BASED_URL + '/checkmail/' + this.userName + '/' + this.accountId + '/';
  this.makeRequest(url, this.HTTP_GET, '', this.CHECK_MAIL_ACTION);
};

MailServiceHandler.prototype.stopCheckMail = function() {
  if (this.accountId) {
  	this.activeAction = this.STOP_CHECK_MAIL_ACTION;
    var url = this.SERVICE_BASED_URL + '/stopcheckmail/' + this.userName + '/' + this.accountId + '/';
    this.makeRequest(url, this.HTTP_GET, '', this.STOP_CHECK_MAIL_ACTION);
  }
};

/**
 * 
 * @param {Object} status
 */
MailServiceHandler.prototype.updateUI = function(status) {
  this.checkMailInfobarNode = document.getElementById(this.uiId);
  var statusTxt = '';
  if (this.serverData.info.checkingmail.statusmsg) {
    statusTxt = this.serverData.info.checkingmail.statusmsg;
  }
  var statusTextNode = eXo.core.DOMUtil.findFirstDescendantByClass(this.checkMailInfobarNode, 'div', 'StatusText');
  if (this.checkMailInfobarNode.style.display == 'none') {
    if (this.isUpdateUI_) {
    	this.checkMailInfobarNode.style.display = 'block';
    } 
  }
  if (statusTxt != '') {
    statusTextNode.innerHTML = statusTxt;
  }
};

MailServiceHandler.prototype.destroy = function() {
	var st = this.serverData.info.checkingmail.status ;
  if (st == this.FINISHED_CHECKMAIL_STATUS || st == this.CONNECTION_FAILURE || st == this.RETRY_PASSWORD) {
    eXo.core.Browser.setCookie('cs.mail.checkingmail' + this.accountId, 'false');
    var stopLabel = eXo.core.DOMUtil.findFirstDescendantByClass(this.checkMailInfobarNode, 'div', 'StopCheckMail') ;
    stopLabel.style.display = 'none' ;
    if (st == this.RETRY_PASSWORD) {
      eXo.webui.UIForm.submitForm('UIMessageList','ComfirmPassword', true) ;
    } else if (st != this.CONNECTION_FAILURE){
      var refeshLabel = eXo.core.DOMUtil.findFirstDescendantByClass(this.checkMailInfobarNode, 'div', 'Here');
    	eval(eXo.core.DOMUtil.findDescendantsByTagName(refeshLabel, 'a')[0].href)
    } else {
    	var hideLabel = eXo.core.DOMUtil.findFirstDescendantByClass(this.checkMailInfobarNode, 'div', 'Hide') ;
      hideLabel.style.display = 'block' ;
      return ;
    }
  }
};

eXo.mail.MailServiceHandler = new MailServiceHandler() ;