(function($) {

  function _stripe(rows) {
    rows.each(function(index) {
      $(this).toggleClass('rowodd', index % 2 === 0);
      $(this).toggleClass('roweven', index % 2 !== 0);
    });
  }


  function _getValue(cell) {
    return cell.attr('x') || $.trim(cell.text()) || '';
  }


  function _sort(container, rows, cellIndex, order) {
    Array.prototype.sort.call(rows, function(a, b) {
      var aCell = $(a).find('td').eq(cellIndex),
          bCell = $(b).find('td').eq(cellIndex),
          aValue = _getValue(aCell),
          bValue = _getValue(bCell);

      if (isNaN(aValue) || isNaN(bValue)) {
        return order * (aValue > bValue ? 1 : -1);
      } else {
        return order * (aValue - bValue);
      }
    });
    container.html(rows);
    _stripe(rows);
  }


  function _markSorted(headCells, cell, asc) {
    headCells.removeClass('sortasc sortdesc');
    cell.toggleClass('sortasc', asc);
    cell.toggleClass('sortdesc', !asc);
  }


  $.fn.sortable = function() {
    return $(this).each(function() {
      var thead = $(this).find('thead'),
          tbody = $(this).find('tbody'),
          headCells = thead.find('tr:last th'),
          rows = tbody.find('tr');

       headCells.filter(':not(.nosort)').addClass('sortcol');
       headCells.filter(':not(.nosort)').on('click', function() {
         var asc = !$(this).is('.sortasc');
         _markSorted(headCells, $(this), asc);
         _sort(tbody, rows, headCells.index($(this)), asc ? 1 : -1);
       });

      var sortFirst = headCells.filter('[class^=sortfirst],[class*=sortfirst]');
      if (sortFirst.length > 0) {
        var asc = sortFirst.is('.sortfirstasc');
        _markSorted(headCells, sortFirst, asc);
        _sort(tbody, rows, headCells.index(sortFirst), asc ? 1 : -1);
      } else {
        _stripe(rows);
      }
    });
  };

})(jQuery);
