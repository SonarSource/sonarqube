(function($) {

  var autocomplete = function(options) {

    var el = $(this),
        resultsEl = $(options.results),
        spinnerEl = $(options.spinner);

    var index, total, selected, items, symbol = false;


    var select = function() {
          if (selected) {
            selected.removeClass('selected');
          }

          selected = items.eq(index);
          selected.addClass('selected');
        },

        selectPrev = function() {
          if (index > 0) {
            index--;
          }
          select();
        },

        selectNext = function() {
           if (index < total - 1) {
             index++;
           }
          select();
        },

        choose = function() {
          if (selected) {
            var id = selected.prop('id');
            window.location = baseUrl + '/dashboard/index/' + id;
          }
        },

        show = function(content) {
          resultsEl.html(content).show();
          items = resultsEl.find('li');
          index = -1;
          total = items.length;
          selectNext();

          items
              .on('mouseover', function() {
                index = items.index($(this));
                select();
              })
              .on('click', function() {
                index = items.index($(this));
                select();
                choose();
              });
        },

        hide = function() {
          resultsEl.fadeOut();
        };


    el
        .on('keydown', function(e) {
          function prevent(e) {
            e.preventDefault();
            symbol = false;
          }


          switch (e.keyCode) {
            case 13: // return
              prevent(e);
              choose();
              return;
            case 38: // up
              prevent(e);
              selectPrev();
              return;
            case 40: // down
              prevent(e);
              selectNext();
              return;
            case 37: // left
            case 39: // right
              symbol = false;
              return;
            default:
              symbol = true;
          }
        })
        .on('keyup', function() {
          if (symbol) {
            if (el.val().length >= options.minLength) {
              var data = {};
              data[options.searchTerm] = el.val();

              spinnerEl.show();
              $.ajax({
                    url: options.searchUrl,
                    data: data
                  })
                  .done(function(r) {
                    show(r);
                  })
                  .fail(hide)
                  .always(function() {
                    spinnerEl.hide();
                  });
            } else {
              hide();
            }
          }
        })
        .on('focus', function() {
          el.data('placeholder', el.val());
          el.val('');
        })
        .on('focusout', function() {
          if (el.val().length === 0) {
            el.val(el.data('placeholder') || '');
          }
          hide();
        });

    $('body').on('mousedown', function() {
      hide();
    });
  };

  $.extend($.fn, {
    autocomplete: autocomplete
  });

})(jQuery);
