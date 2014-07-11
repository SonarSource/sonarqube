// JS scripts used in the duplication tab of the resource viewer

function updateDuplicationLines(url, groupId, itemId, linesCount, fromLine, toLine) {
  $j('#duplGroup_' + groupId + ' p.selected').removeClass('selected');
  $j('#duplCount-' + groupId + '-' + itemId).addClass('selected');
  $j('#duplFrom-' + groupId + '-' + itemId).addClass('selected');
  $j('#duplName-' + groupId + '-' + itemId).addClass('selected');
  $j('#duplLoading-' + groupId).addClass('loading');

  if ($j('#source-' + groupId+ ' :first-child').hasClass('expanded')) {
    toLine = fromLine + linesCount - 1;
  }
  $j.ajax({
    url: url + "&to_line=" + toLine + "&from_line=" + fromLine + "&lines_count=" + linesCount + "&group_index=" + groupId,
    success:function(response){
      $j('#source-' + groupId).html(response);
    },
    type:'get'
  });
  return false;
}