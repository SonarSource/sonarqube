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
            console.log(baseUrl + '/dashboard/index/' + key + dashboardParameters());
          }
        },

        show = function(r) {
          resultsEl.empty();

          var ul = $('<ul></ul>').appendTo(resultsEl);

          r.results.forEach(function(qualifier) {
            qualifier.items.forEach(function(item, index) {
              var el = $('<li></li>')
                  .data('key', item.id),

                  q = $('<div></div>')
                      .addClass('q')
                      .appendTo(el),

                  highlightRegexp = new RegExp(term, 'gi'),
                  highlightedName = item.name.replace(highlightRegexp, '<strong>$&</strong>'),

                  label = $('<span></span>')
                      .html(' ' + highlightedName)
                      .appendTo(el);

              $('<img>')
                  .prop('src', baseUrl + qualifier.icon)
                  .prop('width', 16)
                  .prop('height', 16)
                  .prependTo(label);

              if (index === 0) {
                q.text(qualifier.name);
              }

              el.appendTo(ul);
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

        debouncedKeyup = _.debounce(onKeyup, 250);


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
            case 16: // shift
            case 17: // ctrl
            case 18: // alt
            case 91: // cmd
            case 20: // caps
            case 27: // esc
              symbol = false;
              return;
            default:
              symbol = true;
          }
        })
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
