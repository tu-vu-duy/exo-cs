/**
 * @author uocnb
 */
function UIMailDragDrop() {
  this.scKey = 'border' ;
  this.scValue = 'solid 1px #000' ;
  this.DOMUtil = eXo.core.DOMUtil ;
  this.DragDrop = eXo.core.DragDrop ;
  this.dropableSets = [] ;
  this.msgItemClass = 'MessageItem' ;
} ;

UIMailDragDrop.prototype.onLoad = function() {
  eXo.mail.UIMailDragDrop.init() ;
} ;

UIMailDragDrop.prototype.init = function() {
  this.uiMailPortletNode = document.getElementById('UIMailPortlet') ;
  this.getAllDropableSets() ;
  this.regDnDItem() ;
} ;

UIMailDragDrop.prototype.getAllDropableSets = function() {
  var uiFolderContainerNode = document.getElementById('UIFolderContainer') ;
  var folderList = this.DOMUtil.findDescendantsByClass(uiFolderContainerNode, 'a', 'Folder') ;
  for (var i=0; i<folderList.length; i++) {
    this.dropableSets[this.dropableSets.length] = folderList[i] ;
  }
  folderList = this.DOMUtil.findDescendantsByClass(uiFolderContainerNode, 'a', 'FolderLink') ;
  for (var i=0; i<folderList.length; i++) {
    this.dropableSets[this.dropableSets.length] = folderList[i] ;
  }
  var uiTagContainerNode = document.getElementById('UITagContainer') ;
  var tagLists = this.DOMUtil.findDescendantsByClass(uiTagContainerNode, 'a', 'IconTagHolder') ;
  for (var i=0; i<tagLists.length; i++) {
    this.dropableSets[this.dropableSets.length] = tagLists[i] ;
  }
  var tagContainer = document.getElementById('UITagContainer') ;
//  if (tagContainer &&  tagLists.length <= 0) {
  if (tagContainer) {
    this.dropableSets[this.dropableSets.length] = tagContainer ;
  }
} ;

UIMailDragDrop.prototype.regDnDItem = function() {
  var uiListUsersNode = document.getElementById('UIListUsers') ;
  var mailList = this.DOMUtil.findDescendantsByClass(uiListUsersNode, 'tr', this.msgItemClass) ;
  for (var i=0; i<mailList.length; i++) {
    mailList[i].onmousedown = this.mailMDTrigger ;
  }
} ;

/**
 * 
 * @param {Event} e
 */
UIMailDragDrop.prototype.mailMDTrigger = function(e) {
  e = e ? e : window.event ;
  if (e.button == 0 || e.which == 1) {
    return eXo.mail.UIMailDragDrop.initDnD(eXo.mail.UIMailDragDrop.dropableSets, this, this, e) ;
  }
  return true ;
} ;

UIMailDragDrop.prototype.initDnD = function(dropableObjs, clickObj, dragObj, e) {
  var clickBlock = (clickObj && clickObj.tagName) ? clickObj : document.getElementById(clickObj) ;
  var dragBlock = (dragObj && dragObj.tagName) ? dragObj : document.getElementById(dragObj) ;
  
  var blockWidth = clickBlock.offsetWidth ;
  var blockHeight = clickBlock.offsetHeight ;
  
  var tmpNode = document.createElement('div') ;
  var uiGridNode = document.createElement('table') ;
  uiGridNode.className = 'UIGrid' ;
  with(tmpNode.style) {
    background = '#ffe98f;' ;
    border = 'solid 1px #A5A5A5' ;
    position = 'absolute' ;
    width = blockWidth + 'px' ;
    display = 'none' ;
  }
  eXo.core.Browser.setOpacity(tmpNode, 80) ;
  var selectedItems = eXo.cs.FormHelper.getSelectedElementByClass(
                        document.getElementById('UIListUsers'), this.msgItemClass, dragBlock) ;
  if (selectedItems.length > 0) {
    for (var i=0; i<selectedItems.length; i++) {
      if (selectedItems[i] && selectedItems[i].cloneNode) {
        uiGridNode.appendChild(selectedItems[i].cloneNode(true)) ;
      }
    }
  } else {
    uiGridNode.appendChild(dragBlock.cloneNode(true)) ;
  }
  
  tmpNode.appendChild(uiGridNode) ;
  document.body.appendChild(tmpNode) ;
  
  this.DragDrop.initCallback = this.initCallback ;
  this.DragDrop.dragCallback = this.dragCallback ;
  this.DragDrop.dropCallback = this.dropCallback ;
  this.DragDrop.init(dropableObjs, clickBlock, tmpNode, e) ;
  return false ;
} ;

UIMailDragDrop.prototype.synDragObjectPos = function(dndEvent) {
  if (!dndEvent.backupMouseEvent) {
    dndEvent.backupMouseEvent = window.event ;
    if (!dndEvent.backupMouseEvent) {
      return ;
    }
  }
  var dragObject = dndEvent.dragObject ;
  var mouseX = eXo.core.Browser.findMouseXInPage(dndEvent.backupMouseEvent) ;
  var mouseY = eXo.core.Browser.findMouseYInPage(dndEvent.backupMouseEvent)
  dragObject.style.top = mouseY + 'px' ;
  dragObject.style.left = mouseX + 'px' ;
} ;

UIMailDragDrop.prototype.initCallback = function(dndEvent) {
  eXo.mail.UIMailDragDrop.synDragObjectPos(dndEvent) ;
} ;

UIMailDragDrop.prototype.dragCallback = function(dndEvent) {
  var dragObject = dndEvent.dragObject ;
  if (!dragObject.style.display ||
      dragObject.style.display == 'none') {
    dragObject.style.display = 'block' ;
  }
  eXo.mail.UIMailDragDrop.synDragObjectPos(dndEvent) ;
  
  if (dndEvent.foundTargetObject) {
    if (this.foundTargetObjectCatch != dndEvent.foundTargetObject) {
      if(this.foundTargetObjectCatch) {
        this.foundTargetObjectCatch.style[eXo.mail.UIMailDragDrop.scKey] = this.foundTargetObjectCatchStyle ;
      }
      this.foundTargetObjectCatch = dndEvent.foundTargetObject ;
      this.foundTargetObjectCatchStyle = this.foundTargetObjectCatch.style[eXo.mail.UIMailDragDrop.scKey] ;
      this.foundTargetObjectCatch.style[eXo.mail.UIMailDragDrop.scKey] = eXo.mail.UIMailDragDrop.scValue ;
    }
  } else {
    if (this.foundTargetObjectCatch) {
      this.foundTargetObjectCatch.style[eXo.mail.UIMailDragDrop.scKey] = this.foundTargetObjectCatchStyle ;
    }
    this.foundTargetObjectCatch = null ;
  }
} ;

UIMailDragDrop.prototype.dropCallback = function(dndEvent) {
  document.body.removeChild(dndEvent.dragObject) ;
  if (this.foundTargetObjectCatch) {
    this.foundTargetObjectCatch.style[eXo.mail.UIMailDragDrop.scKey] = this.foundTargetObjectCatchStyle ;
  }
  this.foundTargetObjectCatch = dndEvent.foundTargetObject ;
  if (this.foundTargetObjectCatch) {
    eXo.core.DOMUtil.findDescendantsByClass(dndEvent.clickObject, 'input', 'checkbox')[0].checked = true ;
    var place2MoveId = false ;
    var formOp = false ;
    if (this.foundTargetObjectCatch.className == 'UITagContainer') {
      eXo.webui.UIForm.submitForm('UIMessageList','AddTag', true) ;
      return ;
    } else if (eXo.core.DOMUtil.findAncestorByClass(this.foundTargetObjectCatch, 'UITagContainer')) {
      place2MoveId = this.foundTargetObjectCatch.getAttribute('tagid') ;
      formOp = 'AddTagDnD' ;
    } else {
      place2MoveId = this.foundTargetObjectCatch.getAttribute('folder') ;
      formOp = 'MoveDirectMessages' ;
    }
    var uiMsgList = document.getElementById('UIMessageList') ;
    uiMsgList.action = uiMsgList.action + '&objectId=' + place2MoveId ;
    eXo.webui.UIForm.submitForm('UIMessageList', formOp, true) ;
  }
} ;

eXo.mail.UIMailDragDrop = new UIMailDragDrop();