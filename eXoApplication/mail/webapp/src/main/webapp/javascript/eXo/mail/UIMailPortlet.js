eXo.require('eXo.cs.UIContextMenu','/csResources/javascript/') ;
function UIMailPortlet(){
};

UIMailPortlet.prototype.showContextMenu = function(compid) {	
	var UIContextMenu = eXo.webui.UIContextMenu ; //eXo.contact.ContextMenu ;
	UIContextMenu.portletName = compid ;
	var config = {
		'preventDefault':false, 
		'preventForms':false
	} ;	
	UIContextMenu.init(config) ;
	UIContextMenu.attach('MessageItem', 'UIMessagePopupMenu') ;
	UIContextMenu.attach('FolderLink', 'UIFolderListPopupMenu') ;
	UIContextMenu.attach('IconTagHolder', 'UITagListPopupMenu') ;
	UIContextMenu.attach('InboxIcon', 'UIDefaultFolderPopupMenu') ;
	UIContextMenu.attach('DraftsIcon', 'UIDefaultFolderPopupMenu') ;
	UIContextMenu.attach('SentIcon', 'UIDefaultFolderPopupMenu') ;
	UIContextMenu.attach('SpamIcon', 'UIDefaultFolderPopupMenu') ;
	UIContextMenu.attach('TrashIcon', 'UITrashFolderPopupMenu') ;
} ;

//UIMailPortlet.prototype.init = function(container) {	
//	if(typeof(container) == "string") container = document.getElementById(container) ;
//	var checks = eXo.core.DOMUtil.findDescendantsByClass(container, "input", "checkbox") ;
//	var len = checks.length ;
//	this.datatable = eXo.core.DOMUtil.getChildrenByTagName(container, "tbody")[0] ;
//	this.checkall = checks[0] ;
//	this.checkbox = checks.slice(1) ;
//	this.rows = new Array() ;
//	checks[0].onclick = eXo.mail.UIMailPortlet.checkAll ;
//	for(var i = 1 ; i < len ; i++) {
//		checks[i].onclick = eXo.mail.UIMailPortlet.selectItem ;
//		this.rows.push(eXo.core.DOMUtil.findAncestorByTagName(checks[i], "tr")) ;
//	}
//} ;;

UIMailPortlet.prototype.msgPopupMenuCallback = function(evt) {
	var UIContextMenu = eXo.webui.UIContextMenu ;
	var DOMUtil = eXo.core.DOMUtil ;
	var _e = window.event || evt ;
	//_e.cancelBubble = true ;
	var src = null ;
	if (UIContextMenu.IE) {
		src = _e.srcElement;
	} else {
		src = _e.target;
	}
	if (src.nodeName != "tr")
		src = eXo.core.DOMUtil.findAncestorByTagName(src, "tr");
		
	var check = DOMUtil.findDescendantsByClass(src, "input", "checkbox") ;
	if (check[0].checked == false) {
		var tbody = DOMUtil.findAncestorByTagName(src, "tbody") ;
		var checkboxes = DOMUtil.findDescendantsByClass(tbody, "input", "checkbox") ;
		var len = checkboxes.length ;
		for(var i = 0 ; i < len ; i++) {
			if (checkboxes[i].checked) {
				var tr = DOMUtil.findAncestorByTagName(checkboxes[i] , "tr");
				if (tr.className.indexOf("SelectedItem") > -1) {
					tr.className  = tr.className.replace("SelectedItem", "");
				}
				checkboxes[i].checked = false;
			}
		}
		check[0].checked = true ;
		var str = DOMUtil.findAncestorByTagName(check[0] , "tr");
		str.className += " SelectedItem";
	}
	id = src.getAttribute("msgId");
	eXo.webui.UIContextMenu.changeAction(UIContextMenu.menuElement, id) ;
} ;

UIMailPortlet.prototype.defaultFolderPopupMenuCallback = function(evt) {
	var UIContextMenu = eXo.webui.UIContextMenu ;
	var _e = window.event || evt ;
	//_e.cancelBubble = true ;
	var src = null ;
	if (UIContextMenu.IE) {
		src = _e.srcElement;
	} else {
		src = _e.target;
	}
	if (src.nodeName != "A")
		src = src.parentNode;
		
	folder = src.getAttribute("folder");
	eXo.webui.UIContextMenu.changeAction(UIContextMenu.menuElement, folder) ;
} ;

UIMailPortlet.prototype.tagListPopupMenuCallback = function(evt) {
	var UIContextMenu = eXo.webui.UIContextMenu ;
	var _e = window.event || evt ;
	//_e.cancelBubble = true ;
	var src = null ;
	if (UIContextMenu.IE) {
		src = _e.srcElement;
	} else {
		src = _e.target;
	}
	if (src.nodeName != "A")
		src = src.parentNode;
		
	tagName = src.getAttribute("tagId");
	eXo.webui.UIContextMenu.changeAction(UIContextMenu.menuElement, tagName) ;
} 

//UIMailPortlet.prototype.selectItem = function() {
//	var UIMailPortlet = eXo.mail.UIMailPortlet ;
//	var isCheck = this.checked ;
//	var tr = eXo.core.DOMUtil.findAncestorByTagName(this,"tr") ;
//	if(!isCheck) {
//		tr.className = tr.className.replace("SelectedItem", "") ;
//		UIMailPortlet.checkall.checked = false ;
//	}
//	else {
//		tr.className += "SelectedItem" ;	
//		UIMailPortlet.checkall.checked = UIMailPortlet.isAll() ;
//	}
//} ;
//UIMailPortlet.prototype.isAll = function() {
//	var items = eXo.mail.UIMailPortlet.checkbox ;
//	var len = items.length ;
//	for(var i = 0 ; i < len ; i ++) {
//		if(!items[i].checked) return false ;
//	}
//	return true ;
//} ;
//UIMailPortlet.prototype.checkAll = function() {
//	var DOMUtil = eXo.core.DOMUtil ;
//	var ck = eXo.mail.UIMailPortlet.checkbox ;
//	var rows = eXo.mail.UIMailPortlet.rows ;
//	var len = ck.length ;
//	var isCheck = this.checked ;
//	if(isCheck) eXo.mail.UIMailPortlet.datatable.setAttribute("class","MessageContainerAll") ;
//	else eXo.mail.UIMailPortlet.datatable.setAttribute("class","MessageContainer") ;
//	for(var i = 0 ; i < len ; i ++) {
//		ck[i].checked = isCheck ;
//		if(ck[i].checked) rows[i].className += "SelectedItem" ;
//		else rows[i].className = rows[i].className.replace("SelectedItem", "") ;
//	}
//} ;

//UIMailPortlet.prototype.setClass = function(obj, isCheck) {
//	try{
//		if(isCheck) obj.className = obj.className + " SelectedItem" ;
//		else obj.className = obj.className.replace("SelectedItem", "");		
//	} catch(e) {alert(e.message) ;}
//} ;
UIMailPortlet.prototype.readMessage = function() {} ;

UIMailPortlet.prototype.showPrintPreview = function(obj1) {
	document.getElementById("UIPortalApplication").style.display = "none";
	var uiMailPortletNode = document.createElement('div') ;
	uiMailPortletNode.className = 'UIMailPortlet' ;
	var mailWorkingWorkspaceNode = document.createElement('div') ;
	mailWorkingWorkspaceNode.className = 'MailWorkingWorkspace' ;
	var uiMessagePreviewNode = document.createElement('div') ;
	uiMessagePreviewNode.className = 'UIMessagePreview' ;
	uiMessagePreviewNode.appendChild(obj1.cloneNode(true)) ;
	mailWorkingWorkspaceNode.appendChild(uiMessagePreviewNode) ;
	uiMailPortletNode.appendChild(mailWorkingWorkspaceNode) ;

  // Fit UIMailPortlet to window.
  with(uiMessagePreviewNode.style) {
    width = '100%' ;
    height = '100%' ;
    position = 'absolute' ;
    top = '0px' ;
    left = '0px' ;
  }
	document.body.appendChild(uiMailPortletNode) ;
} ;

UIMailPortlet.prototype.printMessage = function() {
	window.print()
} ;

UIMailPortlet.prototype.closePrint = function() {
	document.getElementById("UIPortalApplication").style.display = "block";
	document.getElementById("printWrapper").style.display = "none";
} ;

UIMailPortlet.prototype.switchLayout = function(layout) {	
	var Browser = eXo.core.Browser ;
	layout = parseInt(layout) ;
	var	layout1 = document.getElementById("UINavigationContainer") ;
	var	layout2 = document.getElementById("uiMessageListResizableArea") ;
	var	layout3 = document.getElementById("SpliterResizableArea") ;
	var resizePane = document.getElementById("ResizeReadingPane");
	var workingarea = document.getElementById("UIMessageArea");
		
	switch(layout) {
		case 0 :
			if (layout1.style.display == "none") {
				layout1.style.display = "block" ;
				workingarea.style.marginLeft = "225px"	;			
			}
			
			if (layout2.style.display == "none") {
				layout2.style.display = "block" ;
			}
			
			if (layout3.style.display == "none") {
				layout3.style.display = "block" ;
			}
			
			if (resizePane.style.display == "none") {
				resizePane.style.display = "block";
			}
			Browser.setCookie("UINavigationContainer", "block", 30);
			Browser.setCookie("SpliterResizableArea", "block", 30);
			layout2.style.height = "220px" ;
			Browser.setCookie("ResizeReadingPane", "block", 30);
			Browser.setCookie("uiMessageListResizableArea", "block", 30)	
			break ;
		case 1 :
			if (layout1.style.display == "none") {
				layout1.style.display = "block" ;
				Browser.setCookie("UINavigationContainer", "block", 30)
				 workingarea.style.marginLeft = "225px"	;			
			} else {
				layout1.style.display = "none" ;
				Browser.setCookie("UINavigationContainer", "none", 30)
				if(layout1.style.display == "none") {
					workingarea.style.marginLeft = "0px"	;
				}
			}
			break ;
		case 2 :
			if (layout2.style.display == "none") {
				layout2.style.display = "block" ;
				Browser.setCookie("uiMessageListResizableArea", "block", 30)
				if (layout3.style.display != "none" && layout2.style.display != "none") {
					resizePane.style.display = "block";
					Browser.setCookie("ResizeReadingPane", "block", 30)
				}
			} else {				
				layout2.style.display = "none" ;
				resizePane.style.display = "none";
				Browser.setCookie("ResizeReadingPane", "none", 30)
				Browser.setCookie("uiMessageListResizableArea", "none", 30)
			}
			break ;
		case 3 :
			if (layout3.style.display == "none") {
				layout3.style.display = "block" ;
				layout2.style.height = "220px" ;
				Browser.setCookie("SpliterResizableArea", "block", 30)
				if (layout3.style.display != "none" && layout2.style.display != "none") {
					resizePane.style.display = "block";
					Browser.setCookie("ResizeReadingPane", "block", 30)
				}

			} else {				
				layout3.style.display = "none" ;	
				layout2.style.height = "100%" ;			
			    resizePane.style.display = "none";
			    Browser.setCookie("ResizeReadingPane", "none", 30)
			    Browser.setCookie("SpliterResizableArea", "none", 30)	
			}
			break ;
	}
} ;

UIMailPortlet.prototype.checkLayout = function() {
	var Browser = eXo.core.Browser ;
	var	layout1 = document.getElementById("UINavigationContainer") ;
	var	layout2 = document.getElementById("uiMessageListResizableArea") ;
	var	layout3 = document.getElementById("SpliterResizableArea") ;
	var resizePane = document.getElementById("ResizeReadingPane");
	var workingarea = document.getElementById("UIMessageArea");
	var	uiMessageList = Browser.getCookie("uiMessageListResizableArea") ;
	var	uiNavigationContainer = Browser.getCookie("UINavigationContainer") ;
	var	SpliterResizableArea = Browser.getCookie("SpliterResizableArea") ;
	var	ResizeReadingPane = Browser.getCookie("ResizeReadingPane") ;
	if (uiMessageList != null) {
		layout1.style.display = uiNavigationContainer;
		layout2.style.display = uiMessageList;
		layout3.style.display = SpliterResizableArea;
		resizePane.style.display = ResizeReadingPane;
	} else {
		layout1.style.display = "block";
		layout2.style.display = "block";
		layout3.style.display = "block";
		resizePane.style.display = "block";
	}
	if(layout1.style.display == "none") {
		workingarea.style.marginLeft = "0px"	;
	}
	
	if(layout3.style.display == "block") {
		layout2.style.height = "220px" ;
	} else {
		layout2.style.height = "100%" ;
	}
} ;

UIMailPortlet.prototype.showHide = function(add) {	
	var elm = document.getElementById(add);
	if (elm.style.display == "none") {
		elm.style.display = "" ;
	}
} ; 

UIMailPortlet.prototype.showHidePreviewPane = function(layout) {	
	var Browser = eXo.core.Browser ;
    var	uiMessageList = document.getElementById("uiMessageListResizableArea") ;
	var	previewPane = document.getElementById("SpliterResizableArea") ;
	var resizePane = document.getElementById("ResizeReadingPane");
	var actionReadingPane = document.getElementById("ActionReadingPane");
	if (uiMessageList.style.display == "block") {
		uiMessageList.style.display = "none";
		resizePane.style.display = "none";
		actionReadingPane.className = "MinimumReadingPane";
		Browser.setCookie("uiMessageListResizableArea", "none", 30)
		Browser.setCookie("ResizeReadingPane", "none", 30)
	} else {
		uiMessageList.style.display = "block";
		resizePane.style.display = "block";
		actionReadingPane.className = "MaximizeReadingPane";
		Browser.setCookie("uiMessageListResizableArea", "block", 30)
		Browser.setCookie("ResizeReadingPane", "block", 30)
	}
}

UIMailPortlet.prototype.showHideMessageHeader = function(obj) {
	var DOMUtil = eXo.core.DOMUtil ;
	var decorator = DOMUtil.findAncestorByClass(obj, "DecoratorBox");
	var colapse = DOMUtil.findDescendantById(decorator, "CollapseMessageAddressPreview");
	var expand = DOMUtil.findDescendantById(decorator, "MessageAddressPreview");
	if (colapse.style.display == "none") {
		expand.style.display = "none";
		colapse.style.display = "block"
		obj.innerHTML = "Show details";
	} else {
		colapse.style.display = "none"
		expand.style.display = "block";
		obj.innerHTML = "Hide details";
	}
  var icons = eXo.core.DOMUtil.findDescendantsByClass(obj.parentNode, 'div', 'DownArrow1Icon') ;
  if (icons.length > 0) {
    icons[0].className = 'NextArrow1Icon' ;
  } else {
    icons = eXo.core.DOMUtil.findDescendantsByClass(obj.parentNode, 'div', 'NextArrow1Icon') ;
    if (icons.length > 0) {
      icons[0].className = 'DownArrow1Icon' ;
    }
  }
} ;

UIMailPortlet.prototype.showHideMessageDetails = function(obj) {
	var DOMUtil = eXo.core.DOMUtil;
	var paneDetails = DOMUtil.findAncestorByClass(obj, "ReadingPaneDetails");
    var expands = DOMUtil.findDescendantsByClass(paneDetails, "div", "ExpandMessage");
    var numberExpand = 0;
    for (var i = 0; i < expands.length; i++) {
    	if (expands[i].style.display == "block") numberExpand++;
    }
	var decorator = DOMUtil.findAncestorByClass(obj, "DecoratorBox");
	if ((obj.id == "CollapseMessageAddressPreview") && numberExpand > 1){
		var expand = DOMUtil.findFirstDescendantByClass(decorator, "div", "ExpandMessage");
		var collapse = DOMUtil.findFirstDescendantByClass(decorator, "div", "CollapseMessage");
	    expand.style.display = "none";
	    collapse.style.display = "block";
	} else if (obj.id == "CollapseMessage") {
	    var expand = DOMUtil.findNextElementByTagName(obj, "div");
	    obj.style.display = "none";
	    expand.style.display= "block";
	}
}

UIMailPortlet.prototype.isAllday = function(form) {
	try{
		if (typeof(form) == "string") form = document.getElementById(form) ;		
		if (form.tagName.toLowerCase() != "form") {
			form = eXo.core.DOMUtil.findDescendantsByTagName(form, "form") ;
		}
		for(var i = 0 ; i < form.elements.length ; i ++) {
			if(form.elements[i].getAttribute("name") == "allDay") {
				eXo.mail.UIMailPortlet.showHideTime(form.elements[i]) ;
				break ;
			}
		}
	}catch(e){
		
	}
} ;
UIMailPortlet.prototype.showHideTime = function(chk) {
	var DOMUtil = eXo.core.DOMUtil ;
	if(chk.tagName.toLowerCase() != "input") {
		chk = DOMUtil.findFirstDescendantByClass(chk, "input", "checkbox") ;
	}
	var selectboxes = DOMUtil.findDescendantsByTagName(chk.form, "select") ;
	var fields = new Array() ;
	var len = selectboxes.length ;
	for(var i = 0 ; i < len ; i ++) {
		if((selectboxes[i].getAttribute("name") == "toTime") || (selectboxes[i].getAttribute("name") == "fromTime")) {
			fields.push(selectboxes[i]) ;
		}
	}
	eXo.mail.UIMailPortlet.showHideField(chk, fields) ;
} ;

UIMailPortlet.prototype.showHideField = function(chk,fields) {
	var display = "" ;
	if (typeof(chk) == "string") chk = document.getElementById(chk) ;
	display = (chk.checked) ? "hidden" : "visible" ;
	var len = fields.length ;
	for(var i = 0 ; i < len ; i ++) {
		fields[i].style.visibility = display ;i
	}
} ;

UIMailPortlet.prototype.collapseExpandFolder = function(obj) {
	var DOMUtil = eXo.core.DOMUtil;
	var divChild = DOMUtil.findNextElementByTagName(obj, "div");
	
	var objClass = obj.className;
	if (objClass.indexOf(" OpenFolder") > -1) { 
		objClass = objClass.replace(" OpenFolder", "");
		objClass = objClass + " CloseFolder" ;
		obj.className = objClass ;
    } else if (objClass.indexOf(" CloseFolder") > -1) { 
		objClass = objClass.replace(" CloseFolder", "");
		objClass = objClass + " OpenFolder" ;
		obj.className = objClass ;
    }	
    
    if (divChild != null) {
		var childClass = divChild.className ;
		if (childClass.indexOf("Expand") > -1) {
			divChild.className = "Collapse" ;
 		} else if (childClass.indexOf("Collapse") > -1){
			divChild.className = "Expand" ;
		} 
	} 
}
// Check all
function CheckBox() {
}

CheckBox.prototype.init = function(cont) {
	if (typeof(cont) == "string") cont =document.getElementById(cont) ;
	this.table = eXo.core.DOMUtil.findDescendantsByTagName(cont, "tbody")[0] ;
	var checkboxes = eXo.core.DOMUtil.findDescendantsByClass(cont, "input", "checkbox") ;
	var len = checkboxes.length ;
	if (len <= 0) return ;
	this.checkall = checkboxes[0] ;
	this.checkboxes = checkboxes.slice(1) ;
	this.rows = new Array() ;
	checkboxes[0].onclick = eXo.mail.CheckBox.checkAll ;
	for(var i = len - 1 ; i >= 1 ; i--) {
		checkboxes[i].onclick = eXo.mail.CheckBox.check ;
		this.rows.push(checkboxes[i].parentNode.parentNode) ;
	}
} ;

CheckBox.prototype.checkAll = function() {
	var CheckBox = eXo.mail.CheckBox ;
	var isChecked = CheckBox.checkall.checked ;
	var items = CheckBox.checkboxes ;
	var rows = CheckBox.rows ;
	var len = items.length - 1 ;
	for(var i = len ; i >= 0 ; i--) {
		rows[i].className = rows[i].className.replace("SelectedItem","") ;
		rows[i].style.background = "#eceffc" ;
		if(!isChecked) rows[i].removeAttribute("style") ;
		if(items[i].checked == isChecked) continue ;
		items[i].checked = isChecked ;
	}
} ;

CheckBox.prototype.check = function() {
	var isChecked = this.checked ;
	var tr = this.parentNode.parentNode ;
	if(!isChecked) {
		tr.removeAttribute("style") ;
		tr.className = tr.className.replace("SelectedItem","") ;
		eXo.mail.CheckBox.checkall.checked = false ;
	}
	else {
		tr.style.background = "#eceffc" ;
		eXo.mail.CheckBox.checkall.checked = eXo.mail.CheckBox.isAll() ;
	}
}

CheckBox.prototype.isAll = function() {
	var items = eXo.mail.CheckBox.checkboxes ;
	var len = items.length ;
	for(var i = 0 ; i < len ; i ++) {
		if(!items[i].checked) return false ;
	}
	return true ;
} ;
eXo.mail.CheckBox = new CheckBox() ;
eXo.mail.UIMailPortlet = new UIMailPortlet();
