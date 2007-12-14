function UICombobox() {
}

UICombobox.prototype.init = function(textbox) {
	if(typeof(textbox) == "string") textbox = document.getElementById(textbox) ;
	var UICombobox = eXo.calendar.UICombobox ;
	var onfocus = textbox.getAttribute("onfocus") ;
	var onclick = textbox.getAttribute("onclick") ;
	var onblur = textbox.getAttribute("onblur") ;
	if(!onfocus) textbox.onfocus = UICombobox.show ;
	if(!onclick) textbox.onclick = UICombobox.show ;
	if(!onblur)  textbox.onblur = UICombobox.correct ;
} ;

UICombobox.prototype.show = function(evt) {
	var UICombobox = eXo.calendar.UICombobox ;	
	var _e = window.event || evt ;
	_e.cancelBubble = true ;
	UICombobox.defaultValue = this.value ;
	var src = _e.target || _e.srcElement ;
	if(UICombobox.list) UICombobox.list.style.display = "none" ;
	UICombobox.list = eXo.core.DOMUtil.findPreviousElementByTagName(src, "div") ;
	UICombobox.items = eXo.core.DOMUtil.findDescendantsByTagName(UICombobox.list, "a") ;		
	var len = UICombobox.items.length ;
	
	for(var i = 0 ; i < len ; i ++ ) {
		UICombobox.items[i].onclick = UICombobox.getValue ; 
	}
	if (len <= 0) return ;
	UICombobox.list.onmousedown = UICombobox.cancelBubbe ;
	UICombobox.list.style.width = (this.offsetWidth - 2) + "px" ;	
	UICombobox.list.style.overflowX = "hidden" ;
	UICombobox.list.style.display = "block" ;
	var top = eXo.core.Browser.findPosYInContainer(this, UICombobox.list.offsetParent) + this.offsetHeight ;
	var left = eXo.core.Browser.findPosXInContainer(this, UICombobox.list.offsetParent) ;
	UICombobox.list.style.top = top + "px" ;	
	UICombobox.list.style.left = left + "px" ;
	document.onmousedown = eXo.calendar.UICombobox.hide ;
} ;

UICombobox.prototype.cancelBubbe = function(evt) {
	var _e = window.event || evt ;
	_e.cancelBubble = true ;
} ;

UICombobox.prototype.hide = function() {
	eXo.calendar.UICombobox.list.style.display = "none" ;
} ;

UICombobox.prototype.getValue = function(evt) {
	var _e = window.event || evt ;
	_e.cancelBubble = true ;
	var UICombobox = eXo.calendar.UICombobox ;
	var val = this.getAttribute("value") ;
	var textbox = eXo.core.DOMUtil.findNextElementByTagName(UICombobox.list,"input") ;
	val = eXo.calendar.UICombobox.setValue(val) ;
	textbox.value = val ;
	var len = UICombobox.items.length ;
	var icon = null ;
	var selectedIcon = null ;
	for(var i = 0 ; i < len ; i ++ ) {
		icon = eXo.core.DOMUtil.findFirstDescendantByClass(UICombobox.items[i],"div", "UIComboboxIcon") ;
		icon.className = "UIComboboxIcon" ;
	}
	selectedIcon = eXo.core.DOMUtil.findFirstDescendantByClass(this,"div", "UIComboboxIcon") ;
	eXo.core.DOMUtil.addClass(selectedIcon, "UIComboboxSelectedIcon") ;
	UICombobox.list.style.display = "none" ;
	UICombobox.synchronize(textbox) ;
} ;

// For validating

UICombobox.prototype.correct = function() {
	var value = this.value ;
	this.value = eXo.calendar.UICombobox.setValue(value) ;
} ;

UICombobox.prototype.setValue = function(value) {
	var am = new RegExp("a","i") ;
	var pm = new RegExp("p","i") ;
	var setting = ["hh:mm a", "HH:mm"] ;
	var timeFormat = eXo.calendar.UICalendarPortlet.timeFormat ;
	var defaultValue = eXo.calendar.UICombobox.defaultValue ;
	var value = String(value).trim().toLowerCase() ;
	var time = eXo.calendar.UICombobox.digitToTime(value) ;
	var hour = parseInt(time.hour) ;
	var min = parseInt(time.minutes) ;
	if (min > 60) min = "00" ;
	else min = time.minutes ;
	if (timeFormat == setting[0]) {
		if (!time) {
			return "12:00 AM" ;
		}
		if (hour > 12) hour = "12" ;
		else if(hour == 0) hour = "00" ;
		else hour = time.hour ;
		if (am.test(value) && !pm.test(value)) {
			min += " AM" ;
		} else if (!am.test(value) && pm.test(value)) {
			min += " PM" ;
		} else if (am.test(value) && pm.test(value)) {
			if (value.indexOf("p") < value.indexOf("a")) min += " PM" ;
			if (value.indexOf("p") > value.indexOf("a")) min += " AM" ;
		} else {
			min += " AM" ;
		}		
	} else {
		if (!time) {
			return "12:00" ;
		}
		if (hour > 23) hour = "23" ;
		else hour = time.hour ;
	}
	return hour + ":" + min ;
} ;

UICombobox.prototype.digitToTime = function(stringNo) {
	stringNo = eXo.calendar.UICombobox.getDigit(stringNo) ;
	var len = stringNo.length ;
	if (len <= 0) return false ;
	switch(len) {
		case 1 : 
			stringNo += "0" ;
			return {"hour": stringNo,"minutes":"00" } ;
			break ;
		case 2 :			
			return {"hour": stringNo,"minutes": "00" } ;
			break ;
		case 3 :
			return {"hour": "0" + stringNo[0],"minutes": stringNo[1] + stringNo[2] } ;
			break ;
		case 4 : 
			return {"hour": stringNo[0] + stringNo[1],"minutes": stringNo[2] + stringNo[3] } ;
			break ;
		default: 
			var newString = stringNo.substring(0,4) ;
			return eXo.calendar.UICombobox.digitToTime(newString) ;
	}
} ;

UICombobox.prototype.getDigit = function(stringNo) {
	var parsedNo = "";
	for(var n=0; n<stringNo.length; n++) {
		var i = stringNo.substring(n,n+1);
		if(i=="1"||i=="2"||i=="3"||i=="4"||i=="5"||i=="6"||i=="7"||i=="8"||i=="9"||i=="0")
			parsedNo += i;
	}
	return parsedNo ;
} ;

UICombobox.prototype.synchronize = function(obj) {
	var DOMUtil = eXo.core.DOMUtil ;
	var UICombobox = eXo.calendar.UICombobox ;
	var value = obj.value ;
	obj.value = UICombobox.setValue(value) ;
	var uiTabContentContainer = DOMUtil.findAncestorByClass(obj, "UITabContentContainer") ;
	var UIComboboxInputs = DOMUtil.findDescendantsByClass(uiTabContentContainer, "input","UIComboboxInput") ;
	var len = UIComboboxInputs.length ;
	var name = obj.name.toLowerCase() ;
	var inputname = null ;
	var ifrom = null ;
	var ito = null ;
	var from = (name.indexOf("from") >=0) ;
	var to = (name.indexOf("to") >=0) ;
	for(var i = 0 ; i < len ; i ++) {
		inputname = UIComboboxInputs[i].name.toLowerCase() ;
		ifrom = (inputname.indexOf("from") >=0) ;
		ito = (inputname.indexOf("to") >=0) ;
		if((from && ifrom) || (to && ito)) 
			UIComboboxInputs[i].value = obj.value ;
	}
	var onfocus = obj.getAttribute("onfocus") ;
	var onclick = obj.getAttribute("onclick") ;
	if(!onfocus) obj.onfocus = UICombobox.show ;
	if(!onclick) obj.onclick = UICombobox.show ;
}
eXo.calendar.UICombobox = new UICombobox() ;