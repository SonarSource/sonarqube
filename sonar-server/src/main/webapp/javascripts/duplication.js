// JS scripts used in the duplication tab of the resource viewer

function updateDuplicationLines(url, groupId, itemId, linesCount, fromLine, toLine) {
  $$('#duplGroup_' + groupId + ' p.selected').invoke('removeClassName', 'selected');
  $('duplCount-' + groupId + '-' + itemId).addClassName('selected');
  $('duplFrom-' + groupId + '-' + itemId).addClassName('selected');
  $('duplName-' + groupId + '-' + itemId).addClassName('selected');
  $('duplLoading-' + groupId).addClassName('loading');

  if ($('source-' + groupId).childElements()[0].hasClassName('expanded')) {
    toLine = fromLine + linesCount - 1;
  }

  new Ajax.Updater('source-' + groupId, url + "&to_line=" + toLine + "&from_line=" + fromLine + "&lines_count=" + linesCount + "&group_index=" + groupId, {asynchronous:true, evalScripts:true});
  return false;
}