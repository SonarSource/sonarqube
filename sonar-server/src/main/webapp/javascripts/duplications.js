// JS scripts used in the duplication tab of the resource viewer

function updateDuplicationLines(url, groupClass, groupRowClass, sourceDivId, linesCount, fromLine, toLine) {
  // handle first the style of the selectable rows
  divs = $$('.'+groupClass);
  for ( i = 0; i < divs.size(); i++) {
	  divs[i].removeClassName('selected');
  }
  divs = $$('.'+groupRowClass);
  for ( i = 0; i < divs.size(); i++) {
	  divs[i].addClassName('selected');
  }
  
  // then show that a request is pending
  $(sourceDivId).addClassName('loading');
  
  // and send the Ajax request
  if ($(sourceDivId).childElements()[0].hasClassName('expanded')) {
	toLine = fromLine + linesCount;
  }
  
  new Ajax.Updater(sourceDivId, url + "&to_line=" + toLine, {asynchronous:true, evalScripts:true, onComplete:function(request){$(sourceDivId).removeClassName('loading');}});
  
  return false;
}