function UIWeekView() {
	
}

UIWeekView.prototype.mousePos = function(evt){
	return {
		"x" : eXo.core.Browser.findMouseXInPage(evt) ,
		"y" : eXo.core.Browser.findMouseYInPage(evt)
	} ;
} ;

UIWeekView.prototype.onLoad = function(){
	if(eXo.core.Browser.browserType != "ie") {
		window.setTimeout("eXo.calendar.UICalendarPortlet.checkFilter() ;", 1500) ;
		return ;
	}
	eXo.calendar.UICalendarPortlet.checkFilter() ;	
} ;

UIWeekView.prototype.init = function() {
	var UICalendarPortlet = eXo.calendar.UICalendarPortlet ;
	var DOMUtil = eXo.core.DOMUtil ;
	var UIWeekView = eXo.calendar.UIWeekView ;
	var uiCalendarViewContainer = document.getElementById("UICalendarViewContainer") ;
	var allEvents = DOMUtil.findDescendantsByClass(uiCalendarViewContainer, "div", "EventContainerBorder") ;
	this.container = document.getElementById("UIWeekViewGrid") ;
  var EventWeekContent = DOMUtil.findAncestorByClass(this.container,"EventWeekContent") ;
	this.items = new Array() ;
  eXo.calendar.UICalendarPortlet.viewType = "UIWeekView" ;
	for(var i = 0 ; i < allEvents.length ; i ++) {
		if(allEvents[i].style.display != "none") this.items.push(allEvents[i]) ;
	}
	var len = UIWeekView.items.length ;
  UICalendarPortlet.setFocus() ;
	if (len <= 0) {
  	this.initAllday() ;
		return;
  }	
	var marker = null ;
	for(var i = 0 ; i < len ; i ++){		
		var height = parseInt(this.items[i].getAttribute("endtime")) - parseInt(this.items[i].getAttribute("starttime")) ;
		this.items[i].onmousedown = UIWeekView.dragStart ;
    this.items[i].ondblclick = eXo.calendar.UICalendarPortlet.ondblclickCallback ;
		eXo.calendar.UICalendarPortlet.setSize(this.items[i]) ;
		marker = DOMUtil.findFirstDescendantByClass(this.items[i], "div", "ResizeEventContainer") ;
		marker.onmousedown = UIWeekView.initResize ;
	}
	var tr = DOMUtil.findDescendantsByTagName(this.container, "tr") ;
	var firstTr = null ;
	for(var i = 0 ; i < tr.length ; i ++) {
		if (tr[i].style.display != "none") {
			firstTr = tr[i] ;
			break ;
		}
	}
	this.cols = DOMUtil.findDescendantsByTagName(firstTr, "td") ;
	this.distributeEvent() ;
	this.setSize() ;
	this.initAllday() ;
	UICalendarPortlet.setFocus() ;
} ;

UIWeekView.prototype.distributeEvent = function() {
	var UIWeekView = eXo.calendar.UIWeekView ;
	var len = UIWeekView.cols.length ;
	for(var i = 1 ; i < len ; i ++) {
		if (!eXo.core.DOMUtil.findChildrenByClass(UIWeekView.cols[i], "div", "EventContainerBorder")) return ;
		var colIndex = parseInt(UIWeekView.cols[i].getAttribute("eventindex")) ;
		var eventIndex = null ;
		for(var j = 0 ; j < UIWeekView.items.length ; j ++){		
			eventIndex = parseInt(UIWeekView.items[j].getAttribute("eventindex")) ;
			if (colIndex == eventIndex) UIWeekView.cols[i].appendChild(UIWeekView.items[j]) ;
		}			
	}
} ;

UIWeekView.prototype.onResize = function() {
	//if(eXo.core.Browser.browserType == "ie") {		
		eXo.calendar.UIWeekView.setSize() ;
	//}
} ;

UIWeekView.prototype.setSize = function() {
	var UIWeekView = eXo.calendar.UIWeekView ;
	if(!UIWeekView.cols) return ;
	var len = UIWeekView.cols.length ;
	for(var i = 1 ; i < len ; i ++) {
		UIWeekView.showInCol(UIWeekView.cols[i]) ;
	}	
} ;
UIWeekView.prototype.showHideEvent = function(el,isVisible){
	var i = el.length ;
	if(isVisible){
		while(i--){
			el[i].style.visibility = "visible";
		}
	}else{
		while(i--){
			el[i].style.visibility = "hidden";
		}
	}
}
UIWeekView.prototype.adjustWidth = function(el) {
	var UICalendarPortlet = eXo.calendar.UICalendarPortlet ;
	var inter = UICalendarPortlet.getInterval(el) ;
	if (el.length <= 0) return ;
	var width = "" ;
	for(var i = 0 ; i < inter.length ; i ++) {
		var totalWidth = (arguments.length > 1) ? arguments[1] : parseFloat(100) ;
    totalWidth -= 10 ;
		var offsetLeft = parseFloat(0) ;
		var left = parseFloat(0) ;
		if(arguments.length > 2) {
			offsetLeft = parseFloat(arguments[2]) ;
			left = arguments[2] ;
		} 
		var len = (inter[i+1] - inter[i]) ;
		if(isNaN(len)) continue ;
		var mark = null ;
		if (i > 0){
			for(var l = 0 ; l < inter[i] ; l ++) {
				if((el[inter[i]].offsetTop > el[l].offsetTop) && (el[inter[i]].offsetTop < (el[l].offsetTop + el[l].offsetHeight))) {
					mark = l ;					
				}
			}			
			if (mark != null) {
				offsetLeft = parseFloat(el[mark].style.left) + parseFloat(el[mark].style.width) ;
			}
		}
		var n = 0 ;
		for(var j = inter[i]; j < inter[i+1] ; j++) {
			if(mark != null) {				
				width = parseFloat((totalWidth + left - parseFloat(el[mark].style.left) - parseFloat(el[mark].style.width))/len) ;
			} else {
				width = parseFloat(totalWidth/len) ;
			}
			el[j].style.width = width + "px" ;
			if (el[j-1]&&(len > 1)) el[j].style.left = offsetLeft + parseFloat(el[j-1].style.width)*n +  "px" ;
			else {
				el[j].style.left = offsetLeft +  "px" ;
			}
			n++ ;
		}
	}
} ;

UIWeekView.prototype.showInCol = function(obj) {
	var items = eXo.calendar.UICalendarPortlet.getElements(obj) ;
	var len = items.length ;
	if (len <= 0) return ;
	//this.showHideEvent(items,false);
	var UIWeekView = eXo.calendar.UIWeekView ;
	var container = (eXo.core.Browser.isFF()) ? UIWeekView.container : items[0].offsetParent ;
	var left = parseFloat((eXo.core.Browser.findPosXInContainer(obj, container) - 1)/container.offsetWidth)*100 ;
	var width = parseFloat((obj.offsetWidth - 2)/container.offsetWidth)*100 ;
	items = eXo.calendar.UICalendarPortlet.sortByAttribute(items, "starttime") ;
	UIWeekView.adjustWidth(items, obj.offsetWidth, eXo.core.Browser.findPosXInContainer(obj, container)) ;
	this.showHideEvent(items,true);
} ;

UIWeekView.prototype.dragStart = function(evt) {
	var _e = window.event || evt ;
	_e.cancelBubble = true ;
	if (_e.button == 2) return ;
	var UIWeekView = eXo.calendar.UIWeekView ;
	UIWeekView.dragElement = this ;
	eXo.calendar.UICalendarPortlet.resetZIndex(UIWeekView.dragElement) ;
	UIWeekView.objectOffsetLeft = eXo.core.Browser.findPosX(UIWeekView.dragElement) ;
	UIWeekView.offset = UIWeekView.getOffset(UIWeekView.dragElement, _e) ;
	UIWeekView.mouseY = _e.clientY ;
	UIWeekView.eventY = UIWeekView.dragElement.offsetTop ;
	UIWeekView.containerOffset = {
		"x" : eXo.core.Browser.findPosX(UIWeekView.container.parentNode),
		"y" : eXo.core.Browser.findPosY(UIWeekView.container.parentNode)
	}
  UIWeekView.title = eXo.core.DOMUtil.findDescendantsByTagName(UIWeekView.dragElement,"p")[0].innerHTML ;
	document.onmousemove = UIWeekView.drag ;
	document.onmouseup = UIWeekView.drop ;
} ;

UIWeekView.prototype.drag = function(evt) {
	var _e = window.event || evt ;
	var src = _e.srcElement || _e.target ;
	var UIWeekView = eXo.calendar.UIWeekView ;
	var mouseY = eXo.core.Browser.findMouseRelativeY(UIWeekView.container,_e) - UIWeekView.container.scrollTop ;
	var posY = UIWeekView.dragElement.offsetTop ;
	var height =  UIWeekView.dragElement.offsetHeight ;
	var deltaY = null ;	
	deltaY = _e.clientY - UIWeekView.mouseY ;
	if (deltaY % eXo.calendar.UICalendarPortlet.interval == 0) {
    UIWeekView.dragElement.style.top = UIWeekView.mousePos(_e).y - UIWeekView.offset.y - UIWeekView.containerOffset.y + "px" ;
    eXo.calendar.UICalendarPortlet.updateTitle(UIWeekView.dragElement, posY) ;
  }
	if (UIWeekView.isCol(_e)) {
		var posX = eXo.core.Browser.findPosXInContainer(UIWeekView.currentCol, UIWeekView.dragElement.offsetParent) ;
//		var uiControlWorkspace = document.getElementById("UIControlWorkspace") ;
//		if(uiControlWorkspace) posX += uiControlWorkspace.offsetWidth ;
		UIWeekView.dragElement.style.left = posX + "px" ;
	}
} ;

UIWeekView.prototype.dropCallback = function() {
	var dragElement = eXo.calendar.UIWeekView.dragElement ;
	var start = parseInt(dragElement.getAttribute("startTime")) ;
	var end = parseInt(dragElement.getAttribute("endTime")) ;
	var calType = parseInt(dragElement.getAttribute("calType")) ;
	var workingStart = 0 ;
	if (end == 0) end = 1440 ;
	var delta = end - start  ;
	var currentStart = dragElement.offsetTop + workingStart ;
	var currentEnd = currentStart + delta ;
	var actionLink =	eXo.calendar.UICalendarPortlet.adjustTime(currentStart, currentEnd, dragElement) ;
	var currentDate = eXo.calendar.UIWeekView.currentCol.getAttribute("starttime").toString() ;
	actionLink = actionLink.toString().replace(/'\s*\)/,"&currentDate=" + currentDate + "&calType=" + calType + "')") ;
	eval(actionLink) ;	
} ;

UIWeekView.prototype.drop = function(evt) {
	document.onmousemove = null ;
	var _e = window.event || evt ;
	var UIWeekView = eXo.calendar.UIWeekView ;
	if (!UIWeekView.isCol(_e)) return ;
	var currentCol = UIWeekView.currentCol ;
	var sourceCol = UIWeekView.dragElement.parentNode ;
  var eventY = UIWeekView.eventY ;
	currentCol.appendChild(UIWeekView.dragElement) ;
	UIWeekView.showInCol(sourceCol) ;	
	UIWeekView.showInCol(currentCol) ;
  UIWeekView.title = null ;
  UIWeekView.offset = null ;
  UIWeekView.mouseY = null ;
  UIWeekView.eventY = null ;
  UIWeekView.objectOffsetLeft = null ;
  UIWeekView.containerOffset = null ;
  UIWeekView.title = null ;
	if ((currentCol.cellIndex != sourceCol.cellIndex) || (UIWeekView.dragElement.offsetTop != eventY)) UIWeekView.dropCallback() ;
	UIWeekView.dragElement = null ;
	return null ;
} ;

UIWeekView.prototype.getOffset = function(object, evt) {	
	return {
		"x": (eXo.calendar.UIWeekView.mousePos(evt).x - eXo.core.Browser.findPosX(object)) ,
		"y": (eXo.calendar.UIWeekView.mousePos(evt).y - eXo.core.Browser.findPosY(object))
	} ;
} ;

UIWeekView.prototype.isCol = function(evt) {
	var UIWeekView = eXo.calendar.UIWeekView ;
	if (!UIWeekView.dragElement) return false;
	var Browser = eXo.core.Browser ;
	var isIE = (Browser.browserType == "ie")?true:false ;
	var isDesktop = (document.getElementById("UIPageDesktop"))?true:false ;
	var mouseX = Browser.findMouseXInPage(evt) ;
	var len = UIWeekView.cols.length ;
	var colX = 0 ;
	var uiControlWorkspace = document.getElementById("UIControlWorkspace") ;
	for(var i = 1 ; i < len ; i ++) {
		colX = Browser.findPosX(UIWeekView.cols[i]) ;
		if(uiControlWorkspace && isIE && (!isDesktop || Browser.isIE7())) colX -= uiControlWorkspace.offsetWidth ;
		if ((mouseX > colX) && (mouseX < colX + UIWeekView.cols[i].offsetWidth)){
			return UIWeekView.currentCol = UIWeekView.cols[i] ;
		}
	}
	
	return false ;
} ;

// for resize

UIWeekView.prototype.initResize = function(evt) {
	var _e = window.event || evt ;
	_e.cancelBubble = true ;
	if(_e.button == 2) return ;
	var UIResizeEvent = eXo.calendar.UIResizeEvent ;
	var outerElement = eXo.core.DOMUtil.findAncestorByClass(this,'EventContainerBorder') ;
	var innerElement = eXo.core.DOMUtil.findPreviousElementByTagName(this, "div") ;
	var container = eXo.core.DOMUtil.findAncestorByClass(document.getElementById("UIWeekViewGrid"), "EventWeekContent") ;
	var minHeight = 15 ;
	var interval = eXo.calendar.UICalendarPortlet.interval ;
	UIResizeEvent.start(_e, innerElement, outerElement, container, minHeight, interval) ;
	UIResizeEvent.callback = eXo.calendar.UIWeekView.resizeCallback ;
} ;

UIWeekView.prototype.resizeCallback = function(evt) {
	var UIResizeEvent = eXo.calendar.UIResizeEvent ;
	var eventBox = UIResizeEvent.outerElement ;
	var start =  parseInt(eventBox.getAttribute("startTime")) ;
	var end =  start + eventBox.offsetHeight ;
	var calType = parseInt(eventBox.getAttribute("calType")) ;
	if (eventBox.offsetHeight != UIResizeEvent.beforeHeight) {
		var actionLink = eXo.calendar.UICalendarPortlet.adjustTime(start, end, eventBox) ;
		var currentDate = eventBox.parentNode.getAttribute("starttime").toString() ;
		actionLink = actionLink.toString().replace(/'\s*\)/,"&currentDate=" + currentDate + "&calType=" + calType + "')") ;
		eval(actionLink) ;
	}	
} ;

UIWeekView.prototype.initAllDayRightResize = function(evt) {
	var _e = window.event || evt ;
	_e.cancelBubble = true ;
	if (_e.button == 2) return ;
	var UIHorizontalResize = eXo.calendar.UIHorizontalResize ;
	var outerElement = eXo.core.DOMUtil.findAncestorByClass(this,'WeekViewEventBoxes') ;
	var innerElement = eXo.core.DOMUtil.findFirstDescendantByClass(outerElement, "div", "EventAlldayContent") ;
	UIHorizontalResize.start(_e, outerElement, innerElement) ;
	UIHorizontalResize.dragCallback = eXo.calendar.UIWeekView.rightDragResizeCallback ;
	UIHorizontalResize.callback = eXo.calendar.UIWeekView.rightResizeCallback ;
} ;

UIWeekView.prototype.initAllDayLeftResize = function(evt) {
	var _e = window.event || evt ;
	_e.cancelBubble = true ;
	if (_e.button == 2) return ;	
	var UIHorizontalResize = eXo.calendar.UIHorizontalResize ;
	var outerElement = eXo.core.DOMUtil.findAncestorByClass(this,'WeekViewEventBoxes') ;
	var innerElement = eXo.core.DOMUtil.findFirstDescendantByClass(outerElement, "div", "EventAlldayContent") ;
	UIHorizontalResize.start(_e, outerElement, innerElement, true) ;
	UIHorizontalResize.dragCallback = eXo.calendar.UIWeekView.leftDragResizeCallback ;
	UIHorizontalResize.callback = eXo.calendar.UIWeekView.leftResizeCallback ;
} ;

UIWeekView.prototype.rightDragResizeCallback = function() {
	var outer = eXo.calendar.UIHorizontalResize.outerElement ;
	var totalWidth = eXo.core.DOMUtil.findAncestorByClass(outer, "EventAllday") ;
	totalWidth = totalWidth.offsetWidth ;
	var posX = outer.offsetLeft ;
	var width = parseInt(outer.style.width) ;
	var maxX = posX + width ;
	if (maxX >= totalWidth) outer.style.width = (totalWidth - posX - 2) + "px" ;	
} ;

UIWeekView.prototype.leftDragResizeCallback = function() {
	var outer = eXo.calendar.UIHorizontalResize.outerElement ;
	var left = parseInt(outer.style.left) ;
	if (left < 0 ) {		
		outer.style.left = "0px" ;
	}
} ;

UIWeekView.prototype.rightResizeCallback = function() {
	var UIWeekView = eXo.calendar.UIWeekView ;
	var UIHorizontalResize = eXo.calendar.UIHorizontalResize ;	
	var outer = UIHorizontalResize.outerElement ;
	var totalWidth = outer.parentNode.offsetWidth;
	var delta = outer.offsetWidth - UIHorizontalResize.beforeWidth ;
	if (delta != 0) {
		var weekdays = parseInt(document.getElementById("UIWeekViewGridAllDay").getAttribute("numberofdays"));
		var UICalendarPortlet = eXo.calendar.UICalendarPortlet
		var delta = Math.round(delta*(24*weekdays*60*60*1000)/totalWidth) ;
		var start =  parseInt(outer.getAttribute("startTime")) ;
		var end = parseInt(outer.getAttribute("endTime")) + delta;
		var calType = parseInt(outer.getAttribute("calType")) ;
		var actionLink = UICalendarPortlet.adjustTime(start, end, outer) ;
		actionLink = actionLink.toString().replace(/'\s*\)/,"&calType=" + calType + "')") ;
		eval(actionLink) ;
	} else{
		outer.style.left = parseFloat(outer.offsetLeft/outer.offsetParent.offsetWidth)*100 + "%" ;
		outer.style.width = parseFloat(outer.offsetWidth/outer.offsetParent.offsetWidth)*100 + "%" ;
	}
} ;

UIWeekView.prototype.leftResizeCallback = function() {
	var UIWeekView = eXo.calendar.UIWeekView ;
	var UIHorizontalResize = eXo.calendar.UIHorizontalResize ;
	var outer = UIHorizontalResize.outerElement ;
	var totalWidth = outer.parentNode.offsetWidth;
	var delta = UIHorizontalResize.beforeWidth - outer.offsetWidth ;
	if (delta != 0) {
		var weekdays = parseInt(document.getElementById("UIWeekViewGridAllDay").getAttribute("numberofdays"));
		var UICalendarPortlet = eXo.calendar.UICalendarPortlet
		var delta = Math.round(delta*(24*weekdays*60*60*1000)/totalWidth) ;
		var start =  parseInt(outer.getAttribute("startTime")) + delta ;
		var end = parseInt(outer.getAttribute("endTime")) ;
		var calType = parseInt(outer.getAttribute("calType")) ;
		var actionLink = UICalendarPortlet.adjustTime(start, end, outer) ;
		actionLink = actionLink.toString().replace(/'\s*\)/,"&calType=" + calType + "')") ;
		eval(actionLink) ;
	} else{
		outer.style.left = parseFloat(outer.offsetLeft/outer.offsetParent.offsetWidth)*100 + "%" ;
		outer.style.width = parseFloat(outer.offsetWidth/outer.offsetParent.offsetWidth)*100 + "%" ;
	}	
} ;

// For all day event

UIWeekView.prototype.initAlldayDND = function(evt) {
	var _e = window.event || evt ;
	if (_e.button == 2) return ;
	var UIWeekView = eXo.calendar.UIWeekView ;
	var DragDrop = eXo.core.DragDrop ;
	var EventAllday = eXo.core.DOMUtil.findAncestorByClass(this, "EventAllday") ;
	dragObject = this ;
	UIWeekView.totalWidth = EventAllday.offsetWidth ;
	UIWeekView.elementTop = dragObject.offsetTop ;
	UIWeekView.elementLeft = dragObject.offsetLeft ;
	DragDrop.initCallback = UIWeekView.allDayInitCallback ;
  DragDrop.dragCallback = UIWeekView.allDayDragCallback ;
  DragDrop.dropCallback = UIWeekView.allDayDropCallback ;
	DragDrop.init(null, dragObject, dragObject, _e) ;	
} ;

UIWeekView.prototype.allDayInitCallback = function(evt) {
	var UIWeekView = eXo.calendar.UIWeekView ;
	var dragObject = evt.dragObject ;
	UIWeekView.beforeStart = dragObject.offsetLeft ;
	dragObject.style.left = UIWeekView.elementLeft + "px" ;
} ;

UIWeekView.prototype.allDayDragCallback = function(evt) {
	var UIWeekView = eXo.calendar.UIWeekView ;
	var dragObject = evt.dragObject ;
	dragObject.style.top = UIWeekView.elementTop + "px" ;
	var posX = parseInt(dragObject.style.left) ;
	var is55 = document.getElementById("UIPageDesktop") || !eXo.core.Browser.isIE6() ;
	var min = 0 ;
	var max = UIWeekView.totalWidth - dragObject.offsetWidth ;	
	if(is55) 
		min += 55 ;
	else
		max -= 55 ;
		
	if (posX <= min) {
		dragObject.style.left = min + "px" ;
	}
	if (posX >= max) {		
		dragObject.style.left = max + "px" ;
	}
} ;

UIWeekView.prototype.allDayDropCallback = function(evt) {
	var dragObject = evt.dragObject ;	
	var UIWeekView = eXo.calendar.UIWeekView ;
	var totalWidth = dragObject.parentNode.offsetWidth ;
	var delta = dragObject.offsetLeft - UIWeekView.beforeStart ;
	if (delta == 0) dragObject.style.left = parseFloat(parseInt(dragObject.style.left)/dragObject.offsetParent.offsetWidth)*100 + "%" ;
	UIWeekView.elementLeft = null ;
	UIWeekView.elementTop = null ;
	UIWeekView.beforeStart = null ;	
	if (delta != 0) {
		var weekdays = parseInt(document.getElementById("UIWeekViewGridAllDay").getAttribute("numberofdays"));
		var UICalendarPortlet = eXo.calendar.UICalendarPortlet
		var delta = Math.round(delta*(24*weekdays*60*60*1000)/totalWidth) ;
		var start =  parseInt(dragObject.getAttribute("startTime")) + delta ;
		var end = parseInt(dragObject.getAttribute("endTime")) + delta ;
		var calType = parseInt(dragObject.getAttribute("calType")) ;
		var actionLink = UICalendarPortlet.adjustTime(start, end, dragObject) ;		
		actionLink = actionLink.toString().replace(/'\s*\)/,"&calType=" + calType + "')") ;
		eval(actionLink) ;
	}	
} ;

UIWeekView.prototype.initAllday = function() {
	var UIWeekView = eXo.calendar.UIWeekView ;
	var uiWeekView = document.getElementById("UIWeekView") ;
	var uiWeekViewGridAllDay = eXo.core.DOMUtil.findFirstDescendantByClass(uiWeekView,"table","UIGrid") ;
	this.eventAlldayContainer = eXo.core.DOMUtil.findDescendantsByClass(uiWeekView, "div", "EventAlldayContainer") ;
	var eventAllday = new Array() ;
	for(var i = 0 ; i < this.eventAlldayContainer.length ; i ++) {
		if (this.eventAlldayContainer[i].style.display != "none") eventAllday.push(this.eventAlldayContainer[i]) ;
	}
	var len = eventAllday.length ;
	if (len <= 0) return ;
	var resizeMark = null ;
	for(var i = 0 ; i < len ; i ++) {
		resizeMark = eXo.core.DOMUtil.getChildrenByTagName(eventAllday[i], "div") ;
		if (eXo.core.DOMUtil.hasClass(resizeMark[0], "ResizableSign")) resizeMark[0].onmousedown = UIWeekView.initAllDayLeftResize ;
		if (eXo.core.DOMUtil.hasClass(resizeMark[2], "ResizableSign")) resizeMark[2].onmousedown = UIWeekView.initAllDayRightResize ; 
    eventAllday[i].onmousedown = eXo.calendar.UIWeekView.initAlldayDND ;
	}
	var EventAlldayContainer = eXo.core.DOMUtil.findFirstDescendantByClass(uiWeekViewGridAllDay,"td","EventAllday") ;
//	EventAlldayContainer.style.height = eventAllday.length * eventAllday[0].offsetHeight + "px" ;
	this.weekdays = eXo.core.DOMUtil.findDescendantsByTagName(uiWeekViewGridAllDay, "th") ;
	this.startWeek = 	UIWeekView.weekdays[1] ;
	this.endWeek = 	UIWeekView.weekdays[UIWeekView.weekdays.length-1] ;
//	this.setPosition(eventAllday) ;
} ;

UIWeekView.prototype.sortByWidth = function(obj) {
	var len = obj.length ;
	var tmp = null ;
	var attribute1 = null ;
	var attribute2 = null ;
	for(var i = 0 ; i < len ; i ++){
		attribute1 = obj[i].offsetWidth ;
		for(var j = i + 1 ; j < len ; j ++){
			attribute2 = obj[j].offsetWidth ;
			if(attribute2 > attribute1) {
				tmp = obj[i] ;
				obj[i] = obj[j] ;
				obj[j] = tmp ;
			}
		}
	}
	return obj ;
} ;

UIWeekView.prototype.getMinutes = function(millisecond) {
	return eXo.calendar.UICalendarPortlet.timeToMin(millisecond) ;
} ;

UIWeekView.prototype.sortEventsInCol = function(events) {
	var index = this.getStartEvent(events) ;
	//events = eXo.calendar.UICalendarPortlet.sortByAttribute(events, "startTime", "dsc") ;
	var len = index.length ;// alert(len) ;
	var tmp = new Array() ;
	for(var i = 0 ; i < len ; i ++) {
		tmp.pushAll(this.setGroup(events, index[i])) ;
	}
	eXo.calendar.UICalendarPortlet.sortByAttribute(tmp, "startTime") ;
	return tmp ;
} ;

UIWeekView.prototype.setPosition = function(events) {
	events = this.setWidth(events) ;
	events = this.setLeft(events) ;
	events = this.sortEventsInCol(events) ;
	this.setTop(events) ;
} ;

UIWeekView.prototype.setLeft = function(events) {
	var len = events.length ;
	if (len <= 0) return ;
	var start = 0 ;
	var left = 0 ;
	var startWeek = parseInt(this.startWeek.getAttribute("startTime")) ;
	var totalWidth = parseFloat(eXo.core.Browser.findPosXInContainer(events[0].parentNode, events[0].offsetParent)/events[0].offsetParent.offsetWidth)*100 ;
	for(var i = 0 ; i < len ; i ++) {
		start = parseInt(events[i].getAttribute("startTime")) ;
		if (start < startWeek) start = startWeek ;
		diff = start - startWeek ;
		left = parseFloat((diff/(24*7*60*60*1000))*(100*events[0].parentNode.offsetWidth)/(events[0].offsetParent.offsetWidth)) ;
		events[i].style.left = left + totalWidth + "%" ;
	}
	return events ;
} ;

UIWeekView.prototype.arrayUnique = function(arr) {
	var tmp = new Array() ;
	arr.sort() ;
	for(var i = 0 ; i < arr.length ; i ++) {
		if(arr[i] !== arr[i+1]) {
			tmp[tmp.length] = arr[i] ;
		}
	}
	return tmp ;
} ;

UIWeekView.prototype.getStartEvent = function(events) {
	var start = new Array() ;
	var len = events.length ;
	for(var i = 0 ; i < len ; i ++) {
		start.push(parseInt(events[i].offsetLeft)) ;
	}
	return this.arrayUnique(start) ;
} ;

UIWeekView.prototype.setGroup = function(events, value) {
	var len = events.length ;
	var tmp = new Array() ;
	for(var i = 0 ; i < len ; i ++) {
		if (events[i].offsetLeft == value) {
			tmp.push(events[i]) ;
		}
	}
	return this.sortByWidth(tmp) ;
} ;
UIWeekView.prototype.setTop = function (events) {
	var len = events.length ;
	for(var i = 0 ; i < len ; i ++) {		
		events[i].style.top = "0px" ;
		events[i].style.top = eXo.core.Browser.findPosYInContainer(events[i],events[i].offsetParent) +  i*events[i].offsetHeight + "px" ;
	}
	this.resort(events) ;
	return events ;
} ;

UIWeekView.prototype.resort = function (events) {
	var len = events.length ;
	for(var i = 0 ; i < len ; i ++) {
		var beforeLeft = events[i].offsetLeft + events[i].offsetWidth - 1 ;
		for(var j = i + 1 ; j < len ; j ++) {
			var afterLeft = events[j].offsetLeft ;
			if (afterLeft > beforeLeft) {
				events[j].style.top = events[i].style.top ;
				break ;
			}
		}
	}	
} ;

UIWeekView.prototype.setIndex = function (events) {

} ;

UIWeekView.prototype.setWidth = function(events) {
	var len = events.length ;
	var start = 0 ;
	var end = 0 ;
	var diff = 0 ;
	var uiWeekViewGridAllDay = document.getElementById("UIWeekViewGridAllDay") ;
	var startWeek = this.startWeek ;
	var endWeek = this.endWeek ;
	startWeek = parseInt(startWeek.getAttribute("startTime")) ;
	endWeek = parseInt(endWeek.getAttribute("startTime")) ;
	var totalWidth = parseFloat(events[0].parentNode.offsetWidth/events[0].offsetParent.offsetWidth) ;
	for(var i = 0 ; i < len ; i ++) {
		start = parseInt(events[i].getAttribute("startTime")) ;
		end = parseInt(events[i].getAttribute("endTime")) ;
		if (start < startWeek) start = startWeek ;
		if (end > (endWeek + 24*60*60*1000)) end = endWeek + 24*60*60*1000 ;
		diff = end - start ;
		events[i].style.width = parseFloat(diff/(24*7*60*60*1000))*100*totalWidth - 0.2 + "%" ;
		events[i].onmousedown = eXo.calendar.UIWeekView.initAlldayDND ;
	}
	return events ;
} ;
// Resize horizontal

function UIHorizontalResize() {
	
}

UIHorizontalResize.prototype.start = function(evt, outer, inner) {
	var _e = window.event || evt ;
	this.outerElement = outer ;
	this.innerElement = inner ;
	this.outerElement.style.width = this.outerElement.offsetWidth - 2 + "px" ;
	this.innerElement.style.width = this.innerElement.offsetWidth - 2 + "px" ;
	if(arguments.length > 3) {
		this.outerElement.style.left = this.outerElement.offsetLeft + "px" ;
		this.isLeft = true ;
		this.beforeLeft = this.outerElement.offsetLeft ;
	} else {
		this.isLeft = false ;
	}
	this.mouseX = _e.clientX ;
	this.outerBeforeWidth = this.outerElement.offsetWidth - 2 ;
	this.innerBeforeWidth = this.innerElement.offsetWidth - 2 ;
	this.beforeWidth = this.outerElement.offsetWidth ;
	document.onmousemove = eXo.calendar.UIHorizontalResize.execute ;
	document.onmouseup = eXo.calendar.UIHorizontalResize.end ;
} ;

UIHorizontalResize.prototype.execute = function(evt) {
	var _e = window.event || evt ;
	var	UIHorizontalResize = eXo.calendar.UIHorizontalResize ;
	var delta = _e.clientX - UIHorizontalResize.mouseX ;
	//window.status = "Delta : " + delta + " Event : " + eXoP ;
	if(UIHorizontalResize.isLeft == true) {
		UIHorizontalResize.outerElement.style.left = UIHorizontalResize.beforeLeft + delta + "px" ;
		if (parseInt(UIHorizontalResize.outerElement.style.left) > 0){
			UIHorizontalResize.outerElement.style.width = UIHorizontalResize.outerBeforeWidth - delta + "px" ;
			UIHorizontalResize.innerElement.style.width = UIHorizontalResize.innerBeforeWidth - delta + "px" ;			
		}
	} else {
		UIHorizontalResize.outerElement.style.width = UIHorizontalResize.outerBeforeWidth + delta + "px" ;
		UIHorizontalResize.innerElement.style.width = UIHorizontalResize.innerBeforeWidth + delta + "px" ;		
	}
	if(typeof(UIHorizontalResize.dragCallback) == "function") {
		UIHorizontalResize.dragCallback() ;
	}
} ;

UIHorizontalResize.prototype.end = function(evt) {
	var	UIHorizontalResize = eXo.calendar.UIHorizontalResize ;
	if (typeof(UIHorizontalResize.callback) == "function") UIHorizontalResize.callback() ;
	delete UIHorizontalResize.outerElement ;
	delete UIHorizontalResize.innerElement ;
	delete UIHorizontalResize.outerBeforeWidth ;
	delete UIHorizontalResize.innerBeforeWidth ;
	delete UIHorizontalResize.beforeWidth ;
	delete UIHorizontalResize.callback ;
	delete UIHorizontalResize.mouseX ;
	delete UIHorizontalResize.isLeft ;
	delete UIHorizontalResize.beforeLeft ;
	document.onmousemove = null ;
	document.onmouseup = null ;
} ;

// For user selection 

UIWeekView.prototype.initSelection = function() {	
	var UISelection = eXo.calendar.UISelection ;
	var container = document.getElementById("UIWeekViewGrid") ;
	UISelection.step = 30 ;	
	UISelection.block = document.createElement("div")
	UISelection.block.className = "UserSelectionBlock" ;
	UISelection.container = container ;
	eXo.core.DOMUtil.findPreviousElementByTagName(container, "div").appendChild(UISelection.block) ;
	UISelection.container.onmousedown = UISelection.start ;
	UISelection.relativeObject = eXo.core.DOMUtil.findAncestorByClass(UISelection.container, "EventWeekContent") ;
	UISelection.viewType = "UIWeekView" ;
} ;

UIWeekView.prototype.initSelectionX = function() {
	var Highlighter = eXo.calendar.Highlighter ;
	var table = document.getElementById("UIWeekViewGridAllDay") ;
	var cell = eXo.core.DOMUtil.findDescendantsByTagName(table, "th");	
	var len = cell.length ;
	var link = null ;
	for(var i = 0 ; i < len ; i ++) {
		link = eXo.core.DOMUtil.getChildrenByTagName(cell[i],"a")[0] ;		
		if (link) link.onmousedown = eXo.calendar.UIWeekView.cancelBubble ;
		cell[i].onmousedown = Highlighter.start ;
	}
} ;

UIWeekView.prototype.cancelBubble = function(evt) {
	var _e = evt || window.event ;
	_e.cancelBubble = true ;
} ;

UIWeekView.prototype.callbackSelectionX = function() {
	var UIHSelection = eXo.calendar.UIHSelection ;
	var startTime = parseInt(UIHSelection.firstCell.getAttribute("startTime")) ;
	var endTime = parseInt(UIHSelection.lastCell.getAttribute("startTime")) + 24*60*60*1000 - 1 ;
	eXo.webui.UIForm.submitEvent("UIWeekView" ,'QuickAdd','&objectId=Event&startTime=' + startTime + '&finishTime=' + endTime) ;
} ;
eXo.calendar.UIHorizontalResize = new UIHorizontalResize() ;
eXo.calendar.UIWeekView = new UIWeekView() ;
