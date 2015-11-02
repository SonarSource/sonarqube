/*
 * Global Messages
 */

(function () {
  /**
   * Show a global message
   * @param {string} id
   * @param {string} message
   */
  window.showMessage = function (id, message) {
    jQuery('#' + id + 'msg').html(message);
    jQuery('#' + id).removeClass('hidden');
    jQuery('#messages-panel').removeClass('hidden');
  };

  /**
   * Hide a global message
   * @param {string} id
   * @returns {boolean} always false
   */
  window.hideMessage = function (id) {
    jQuery('#' + id).addClass('hidden');
    var messagePanel = jQuery('#messages-panel'),
        isEmpty = messagePanel.children('*:not(.hidden)').length === 0;
    messagePanel.toggleClass('hidden', isEmpty);
    return false;
  };

  /**
   * Show a global error message
   * @param {string} message
   */
  window.error = function (message) {
    window.showMessage('error', message);
  };

  /**
   * Show a global warning message
   * @param {string} message
   */
  window.warning = function (message) {
    window.showMessage('warning', message);
  };

  /**
   * Show a global info message
   * @param {string} message
   */
  window.info = function (message) {
    window.showMessage('info', message);
  };

  /**
   * Hide a global error message
   * @returns {boolean} always false
   */
  window.hideError = function () {
    return window.hideMessage('error');
  };

  /**
   * Hide a global warning message
   * @returns {boolean} always false
   */
  window.hideWarning = function () {
    return window.hideMessage('warning');
  };

  /**
   * Hide a global info message
   * @returns {boolean} always false
   */
  window.hideInfo = function () {
    return window.hideMessage('info');
  };
})();



function toggleFav (resourceId, elt) {
  jQuery.ajax({
    type: 'POST', dataType: 'json', url: baseUrl + '/favourites/toggle/' + resourceId,
    success: function (data) {
      var star = jQuery(elt);
      star.removeClass('icon-favorite icon-not-favorite');
      star.addClass(data.css);
      star.attr('title', data.title);
    }
  });
}

function dashboardParameters (urlHasSomething) {
  var queryString = window.location.search;
  var parameters = [];

  var matchDashboard = queryString.match(/did=\d+/);
  if (matchDashboard && jQuery('#is-project-dashboard').length === 1) {
    parameters.push(matchDashboard[0]);
  }

  var matchPeriod = queryString.match(/period=\d+/);
  if (matchPeriod) {
    // If we have a match for period, check that it is not project-specific
    var period = parseInt(/period=(\d+)/.exec(queryString)[1], 10);
    if (period <= 3) {
      parameters.push(matchPeriod[0]);
    }
  }

  var query = parameters.join('&');
  if (query !== '') {
    query = (urlHasSomething ? '&' : '?') + query;
  }
  return query;
}

function openModalWindow (url, options) {
  var width = (options && options.width) || 540;
  var $dialog = jQuery('#modal');
  if (!$dialog.length) {
    $dialog = jQuery('<div id="modal" class="ui-widget-overlay ui-front"></div>').appendTo('body');
  }
  jQuery.get(url, function (html) {
    $dialog.removeClass('ui-widget-overlay');
    $dialog.html(html);
    $dialog
        .dialog({
          dialogClass: 'no-close',
          width: width,
          draggable: false,
          autoOpen: false,
          modal: true,
          minHeight: 50,
          resizable: false,
          title: null,
          close: function () {
            jQuery('#modal').remove();
          }
        });
    $dialog.dialog('open');
  }).always(function () {
    $dialog.removeClass('ui-widget-overlay');
  });
  return false;
}

(function (jQuery) {
  jQuery.fn.extend({
    openModal: function () {
      return this.each(function () {
        var obj = jQuery(this);
        var url = obj.attr('modal-url') || obj.attr('href');
        return openModalWindow(url, { 'width': obj.attr('modal-width') });
      });
    },
    modal: function () {
      return this.each(function () {
        var obj = jQuery(this);
        obj.unbind('click');
        var $link = obj.bind('click', function () {
          $link.openModal();
          return false;
        });
      });
    },
    modalForm: function (ajax_options) {
      return this.each(function () {
        var obj = jQuery(this);
        obj.submit(function () {
          jQuery('input[type=submit]', this).attr('disabled', 'disabled');
          jQuery.ajax(jQuery.extend({
            type: 'POST',
            url: obj.attr('action'),
            data: obj.serialize(),
            success: function () {
              window.location.reload();
            },
            error: function (xhr) {
              // If the modal window has defined a modal-error element, then returned text must be displayed in it
              var errorElt = obj.find('.modal-error');
              if (errorElt.length) {
                // Hide all loading images
                jQuery('.loading-image').addClass('hidden');
                // Re activate submit button
                jQuery('input[type=submit]', obj).removeAttr('disabled');
                errorElt.show();
                errorElt.html(jQuery('<div/>').html(xhr.responseText).text());
              } else {
                // otherwise replace modal window by the returned text
                jQuery('#modal').html(xhr.responseText);
              }
            }
          }, ajax_options));
          return false;
        });
      });
    }
  });
})(jQuery);

function closeModalWindow () {
  jQuery('#modal').dialog('close');
  return false;
}



/*
 * File Path
 */

(function () {
  /**
   * Return a collapsed path without a file name
   * @example
   * // returns 'src/.../js/components/navigator/app/models/'
   * collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js')
   * @param {string} path
   * @returns {string|null}
   */
  window.collapsedDirFromPath = function (path) {
    var limit = 30;
    if (typeof path === 'string') {
      var tokens = _.initial(path.split('/'));
      if (tokens.length > 2) {
        var head = _.first(tokens),
            tail = _.last(tokens),
            middle = _.initial(_.rest(tokens)),
            cut = false;
        while (middle.join().length > limit && middle.length > 0) {
          middle.shift();
          cut = true;
        }
        var body = [].concat(head, cut ? ['...'] : [], middle, tail);
        return body.join('/') + '/';
      } else {
        return tokens.join('/') + '/';
      }
    } else {
      return null;
    }
  };

  /**
   * Return a file name for a given file path
   * * @example
   * // returns 'state.js'
   * collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js')
   * @param {string} path
   * @returns {string|null}
   */
  window.fileFromPath = function (path) {
    if (typeof path === 'string') {
      var tokens = path.split('/');
      return _.last(tokens);
    } else {
      return null;
    }
  };
})();



/*
 * Users
 */

(function() {

  /**
   * Convert the result of api/users/search to select2 format
   */
  window.usersToSelect2 = function (response) {
    return {
      more: false,
      results: _.map(response.users, function(user) {
        return {
          id: user.login,
          text: user.name + ' (' + user.login + ')'
        };
      })
    };
  };

})();



/*
 * Misc
 */

(function () {

  /**
   * Comparator for _.sortBy()-like functions
   *
   * Fit for natural severities order
   * @param {string} severity
   * @returns {number}
   */
  window.severityComparator = function (severity) {
    var SEVERITIES_ORDER = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];
    return SEVERITIES_ORDER.indexOf(severity);
  };


  /**
   * Comparator for _.sortBy()-like functions
   *
   * Fit for facet-like display:
   * BLOCKER   MINOR
   * CRITICAL  INFO
   * MAJOR
   * @param {string} severity
   * @returns {number}
   */
  window.severityColumnsComparator = function (severity) {
    var SEVERITIES_ORDER = ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'];
    return SEVERITIES_ORDER.indexOf(severity);
  };


  /**
   * Return a hash of GET parameters
   * @returns {object}
   */
  window.getQueryParams = function () {
    var qs = window.location.search.split('+').join(' '),
        params = {},
        re = /[?&]?([^=]+)=([^&]*)/g,
        tokens = re.exec(qs);
    while (tokens) {
      params[decodeURIComponent(tokens[1])] = decodeURIComponent(tokens[2]);
      tokens = re.exec(qs);
    }
    return params;
  };

})();

(function () {
  jQuery(function () {

    // Process login link in order to add the anchor
    jQuery('#login-link').on('click', function (e) {
      e.preventDefault();
      var href = jQuery(this).prop('href'),
          hash = window.location.hash;
      if (hash.length > 0) {
        href += decodeURIComponent(hash);
      }
      window.location = href;
    });
  });
})();
