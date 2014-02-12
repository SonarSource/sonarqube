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
    var sortArray = rows.map(function(index) {
      var cell = $(this).find('td').eq(cellIndex);
      return { index: index, value: _getValue(cell) };
    }).get();

    Array.prototype.sort.call(sortArray, function(a, b) {
      if (isNaN(a.value) || isNaN(a.value)) {
        return order * (a.value > b.value ? 1 : -1);
      } else {
        return order * (a.value - b.value);
      }
    });

    rows.detach();
    var newRows = jQuery();
    sortArray.forEach(function(a) {
      var row = rows.eq(a.index);
      row.appendTo(container);
      newRows = newRows.add(row);
    });

    _stripe(newRows);
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
