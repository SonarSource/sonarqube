(function($) {

  $.fn.topSearch = function(options) {

    var el = $(this),
        resultsEl = $(options.results),
        spinnerEl = $(options.spinner);

    var index, total, selected, items, term, symbol = false;


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
            var key = selected.data('key');
            window.location = baseUrl + '/dashboard/index/' + key + dashboardParameters();
          }
        },

        show = function(r) {
          resultsEl.empty();

          var ul = $('<ul></ul>').appendTo(resultsEl);

          r.results.forEach(function(qualifier) {
            qualifier.items.forEach(function(item, idx) {
              var itemEl = $('<li></li>')
                  .data('key', item.id),

                  q = $('<div></div>')
                      .addClass('q')
                      .appendTo(itemEl),

                  highlightRegexp = new RegExp(term, 'gi'),
                  highlightedName = item.name.replace(highlightRegexp, '<strong>$&</strong>'),

                  label = $('<span></span>')
                      .html(' ' + highlightedName)
                      .appendTo(itemEl);

              $('<i>')
                  .addClass('icon-qualifier-' + qualifier.q.toLowerCase())
                  .prependTo(label);

              if (idx === 0) {
                q.text(qualifier.name);
              }

              itemEl.appendTo(ul);
            });
          });

          resultsEl.append('<div class="autocompleteNote">' + r.total + ' ' + resultsEl.data('results') + '</div>');

          resultsEl.show();

          if (r.total === 0) {
            ul.append('<li>' + resultsEl.data('no-results') + '</li>');
          } else {
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
          }
        },

        hide = function() {
          resultsEl.fadeOut();
        },

        onKeyup = function() {
          if (symbol) {
            if (el.val().length >= options.minLength) {
              term = el.val();

              spinnerEl.show();
              $.ajax({
                url: baseUrl + '/api/components/suggestions',
                data: { s: term }
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
        },

        debouncedKeyup = _.debounce(onKeyup, 250),

        onKeyDown = function(e) {
          if ([13, 38, 40, 37, 39, 16, 17, 18, 91, 20, 21].indexOf(e.keyCode) !== -1) {
            symbol = false;
          }

          switch (e.keyCode) {
            case 13: // return
              e.preventDefault();
              choose();
              return;
            case 38: // up
              e.preventDefault();
              selectPrev();
              return;
            case 40: // down
              e.preventDefault();
              selectNext();
              return;
            default:
              symbol = true;
          }
        };


    el
        .on('keydown', onKeyDown)
        .on('keyup', debouncedKeyup)
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

})(jQuery);
