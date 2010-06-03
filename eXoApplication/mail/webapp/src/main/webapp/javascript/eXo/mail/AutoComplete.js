/**
 * @author Hoang Manh Dung
 */
eXo.require("eXo.core.JSON");
eXo.require("eXo.core.Keyboard");

function AutoComplete(){
	this.REST_URL = eXo.env.portal.context + "/rest/cs/mail/searchemail/";
	this.elm = null;
};
/**
 * Register handler for input
 * @param {Array} ids is the array of input ids
 */
AutoComplete.prototype.init = function(ids){
	if(eXo.cs.restContext) this.REST_URL = eXo.env.portal.context + "/" + eXo.cs.restContext + "/cs/mail/searchemail/";  
	var i = 0;
	var elm;
	while(i < ids.length){
		elm = document.getElementById(ids[i]);
		if(elm) {
			elm.onkeydown = function(evt){
				eXo.mail.AutoComplete.elm = this;
				eXo.mail.AutoComplete.pressHandler(evt, this);
			};
		}
		i = i + 1;
	}
};

AutoComplete.prototype.startHandler = function() {
	window.setTimeout(eXo.mail.AutoComplete.timeOutHandler, 20);
};

AutoComplete.prototype.timeOutHandler = function() {
	if(eXo.mail.AutoComplete.elm) {
		eXo.mail.AutoComplete.typeHandler(eXo.mail.AutoComplete.elm);
		eXo.mail.AutoComplete.elm = null;
	}
};

AutoComplete.prototype.pressHandler = function(evt, textbox){
	if(!evt) var evt = window.event;
	var me = eXo.mail.AutoComplete;
	var keyNum = eXo.core.Keyboard.getKeynum(evt);
	switch(keyNum){
		case 13: me.enterHandler(evt, textbox); break;
		case 27: me.escapeHandler(evt, textbox); break;
		case 38: me.arrowUpHandler(evt, textbox); break;
		case 40: me.arrowDownHandler(evt, textbox); break;
		default: {
			if(keyNum > 40 || keyNum === 8) { me.startHandler();}
			break;
		}
	}
	return; 
};

AutoComplete.prototype.makeRequest = function(url,callback){
	if(this.xhr){
		this.xhr.abort();
		this.xhr = null;
		delete this.xhr;
	}	
	this.xhr =  eXo.core.Browser.createHttpRequest() ;
	this.xhr.open('GET', url, false) ;
	this.xhr.setRequestHeader("Cache-Control", "max-age=86400") ;
	this.xhr.send(null) ;
	if(callback) callback(this.xhr.responseText) 
};
/**
 * @return unique array
 * @param {Array} a is original array
 */
AutoComplete.prototype.uniqueArray = function(a){
	a.sort();
	for(var i = 1;i < a.length;){
		if(a[i-1] == a[i]){
			a.splice(i, 1);
		}
		else{
			i++;
		}
	}
	return a;
}
///**
// * @return {Array} list contact
// * @param {Object} data
// */
//AutoComplete.prototype.processData = function(data){
//	var tmpList = [];
//	var a = this.uniqueArray(data.info);
//	var l = a.length;
//	if(!l || l == 0) return;
//	a = this.reArrange(a,this.typingWords);
//	var tmpArr = [];
//	for(var i=0; i < l; i++){
//		tmpArr = a[i].split("::"); 
//		if(tmpArr[1].indexOf(";")){
//			this.splitEmail(tmpArr[0],tmpArr[1],tmpList);
//			continue;
//		}
//		tmpList.push(this.addBiTag(tmpArr[0]) + " &lt;" + this.addBiTag(tmpArr[1]) + "&gt;");
//	}
//	return tmpList;
//};

//AutoComplete.prototype.addBiTag = function(bodyText){
//	var newText = "";
//  var i = -1;
//	var searchTerm = this.typingWords;
//  var lcSearchTerm = searchTerm.toLowerCase();
//  var lcBodyText = bodyText.toLowerCase();
//  var highlightStartTag="<b>", highlightEndTag = "</b>";
//  while (bodyText.length > 0) {
//    i = lcBodyText.indexOf(lcSearchTerm, i+1);
//    if (i < 0) {
//      newText += bodyText;
//      bodyText = "";
//    } else {      
//      newText += bodyText.substring(0, i) + highlightStartTag + bodyText.substr(i, searchTerm.length) + highlightEndTag;
//      bodyText = bodyText.substr(i + searchTerm.length);
//      lcBodyText = bodyText.toLowerCase();
//      i = -1;
//    }
//  }
//  
//  return newText;
//}

//AutoComplete.prototype.splitEmail = function(value,a, data){	
//	var arr = a.split(";");
//	var l = arr.length;
//	for(var i=0; i < l; i++){
//		data.push(this.addBiTag(value) + " &lt;" + this.addBiTag(arr[i]) + "&gt;");
//	}	
//};

AutoComplete.prototype.reArrange = function(data,str){
	if(!data) return;	
	var l = data.length;
	var tmp = [];
	var tmp2 = [];
	for (var i = 0; i < l; i++) {
  	if(data[i].toLowerCase().indexOf(str) == 0) {
  		tmp.push(data[i]);
  		continue;
  	}
  	tmp2.push(data[i]);
  }
	return tmp.concat(tmp2);
};

/**
 * render drop down menu
 * @param {Object} data
 */
AutoComplete.prototype.renderMenu = function(data){
	var a = (data.info);
	var len = a.length;
	if(!len || len == 0) {
		this.hideMenu();
		return;
	}
	
	var menu = this.createMenuContainer("AutoCompleteMail");
	var html = '';
	for(i=0; i<len; i++){
		html += a[i];
	}
	menu.innerHTML = html ;
	this.currentItem = menu.firstChild;
	if(html != '') menu.style.display = "block";
	else  menu.style.display = "none";
};

AutoComplete.prototype.typeHandler = function(textbox){
	var me = eXo.mail.AutoComplete ;
	var keyword = eXo.mail.AutoComplete.createKeyword(textbox.value);
	me.activeInput = textbox;
	me.typingWords = keyword;
	if(keyword == '') {
		eXo.mail.AutoComplete.hideMenu();
		return;
	}
	var url = me.REST_URL + keyword;
	me.makeRequest(url,me.typeCallback);
	if(me.menu) me.setPosition(me.menu,me.activeInput);
};

AutoComplete.prototype.typeCallback = function(data){
	if(!data) return;
	var me = eXo.mail.AutoComplete ;
	eval("var data = " + data.trim());
	if(typeof(data) != "object") return ;
	me.renderMenu(data);
};
/**
 * Capture key is pressed by users
 * @param {Object} data
 */
AutoComplete.prototype.captureKey = function(e){
	var code;
	if (!e) var e = window.event;
	if (e.keyCode) code = e.keyCode;
	else if (e.which) code = e.which;
	return code;
};
/**
 * Move pointer among items
 */
AutoComplete.prototype.arrowDownHandler = function(){
	var me = eXo.mail.AutoComplete;
	if(!me.currentItem) {
		me.currentItem = me.menu.firstChild;
		eXo.core.DOMUtil.addClass(me.currentItem,"AutoCompleteOver");
		return ;
	}
	eXo.core.DOMUtil.replaceClass(me.currentItem,"AutoCompleteOver","");
	if(me.currentItem.nextSibling) me.currentItem = me.currentItem.nextSibling;
	else me.currentItem = me.menu.firstChild;
	eXo.core.DOMUtil.addClass(me.currentItem,"AutoCompleteOver");
};

AutoComplete.prototype.arrowUpHandler = function(){
	var me = eXo.mail.AutoComplete;
	if(!me.currentItem) {
		return ;
	}
	eXo.core.DOMUtil.replaceClass(me.currentItem,"AutoCompleteOver","");
	if(me.currentItem.previousSibling) me.currentItem = me.currentItem.previousSibling;
	else me.currentItem = me.menu.lastChild;
	eXo.core.DOMUtil.addClass(me.currentItem,"AutoCompleteOver");
};

AutoComplete.prototype.enterHandler = function(){
	var me = eXo.mail.AutoComplete;
	if(me.currentItem) me.addValue(me.currentItem);
	me.hideMenu();
};

AutoComplete.prototype.escapeHandler = function(){
	eXo.mail.AutoComplete.hideMenu();
};

AutoComplete.prototype.createMenuContainer = function(id){
	if (document.getElementById(id)) {
		eXo.core.DOMUtil.listHideElements(document.getElementById(id));
		document.getElementById(id).innerHTML = "<span></span>";
		return (this.menu = document.getElementById(id));
	}
	var div = document.createElement("div");
	div.id = id;
	div.onmouseover = this.overItem;
	div.onmouseout = this.cleanUp;
	div.onclick = this.setValue;
	div.className = "AutoCompleteMenu";
	document.body.appendChild(div);
	this.menu = div ;
	eXo.core.DOMUtil.listHideElements(div);
	return div;
};

AutoComplete.prototype.setValue = function(evt){
	var target = eXo.core.EventManager.getEventTargetByClass(evt,"AutoCompleteItem");
	if(!target) return ;
	eXo.mail.AutoComplete.addValue(target);
};

AutoComplete.prototype.addValue = function(obj){
	var value = "";
	if(String(eXo.mail.AutoComplete.activeInput.value).indexOf(",") != -1){
		value += eXo.mail.AutoComplete.activeInput.value;
	}
	value = value.substr(0,value.lastIndexOf(",")+1) + " ";
	value += obj.innerHTML + ", ";
	value = value.replace(/\&lt;/gi,"<").replace(/\&gt;/gi,">");
	eXo.mail.AutoComplete.activeInput.value = this.stripHTML(value);
	eXo.mail.AutoComplete.hideMenu();
	if(eXo.mail.AutoComplete.currentItem) delete eXo.mail.AutoComplete.currentItem;
	eXo.mail.AutoComplete.activeInput.focus();
};

AutoComplete.prototype.overItem = function(evt){
	var target = eXo.core.EventManager.getEventTargetByClass(evt,"AutoCompleteItem");
	var me = eXo.mail.AutoComplete;
	if(!target) return ;
	if(me.currentItem) eXo.core.DOMUtil.replaceClass(me.currentItem,"AutoCompleteOver","");
	eXo.core.DOMUtil.addClass(target,"AutoCompleteOver");
	me.currentItem = target;
};

AutoComplete.prototype.setPosition = function(menu,anchor){
	var x = eXo.core.Browser.findPosX(anchor);
	var y = eXo.core.Browser.findPosY(anchor);
	if(eXo.core.DOMUtil.findAncestorByClass(anchor,"PopupContent")) y -= eXo.core.DOMUtil.findAncestorByClass(anchor,"PopupContent").scrollTop ;
	menu.style.left = x + "px";
	menu.style.top = (y + anchor.offsetHeight) + "px";
};

AutoComplete.prototype.cleanUp = function(evt){
	var me = eXo.mail.AutoComplete;
	if (me.currentItem) {
  	eXo.core.DOMUtil.replaceClass(me.currentItem, "AutoCompleteOver", "");
		delete me.currentItem;
  }
};

AutoComplete.prototype.hideMenu = function(){
	if(eXo.mail.AutoComplete.menu) eXo.mail.AutoComplete.menu.style.display = "none";
};

AutoComplete.prototype.createKeyword = function(str){
	if(str.indexOf(",") != -1) {
		str = str.substr(str.lastIndexOf(",") + 1, str.length);
	}
	str = str.replace(/^\s*/,"");
	return str.toLowerCase();	
};

AutoComplete.prototype.stripHTML = function(str){
	return str.replace(/<b>/ig,"").replace(/<\/b>/ig,"");
}

eXo.mail.AutoComplete = new AutoComplete();