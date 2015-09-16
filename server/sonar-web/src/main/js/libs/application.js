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
 * Measures
 */

(function () {

  function shortIntFormatter (value) {
    var format = '0,0';
    if (value >= 1000) {
      format = '0.[0]a';
    }
    if (value >= 10000) {
      format = '0a';
    }
    return numeral(value).format(format);
  }

  function shortIntVariationFormatter (value) {
    if (value === 0) {
      return '0';
    }
    var format = '+0,0';
    if (Math.abs(value) >= 1000) {
      format = '+0.[0]a';
    }
    if (Math.abs(value) >= 10000) {
      format = '+0a';
    }
    return numeral(value).format(format);
  }

  /**
   * Check if days should be displayed for a work duration
   * @param {number} days
   * @returns {boolean}
   */
  function shouldDisplayDays (days) {
    return days > 0;
  }

  /**
   * Check if hours should be displayed for a work duration
   * @param {number} days
   * @param {number} hours
   * @returns {boolean}
   */
  function shouldDisplayHours (days, hours) {
    return hours > 0 && days < 10;
  }

  /**
   * Check if hours should be displayed for a work duration
   * @param {number} days
   * @param {number} hours
   * @returns {boolean}
   */
  function shouldDisplayHoursInShortFormat (days, hours) {
    return hours > 0 && days === 0;
  }

  /**
   * Check if minutes should be displayed for a work duration
   * @param {number} days
   * @param {number} hours
   * @param {number} minutes
   * @returns {boolean}
   */
  function shouldDisplayMinutes (days, hours, minutes) {
    return minutes > 0 && hours < 10 && days === 0;
  }

  /**
   * Check if minutes should be displayed for a work duration
   * @param {number} days
   * @param {number} hours
   * @param {number} minutes
   * @returns {boolean}
   */
  function shouldDisplayMinutesInShortFormat (days, hours, minutes) {
    return minutes > 0 && hours === 0 && days === 0;
  }

  /**
   * Add a space between units if needed
   * @param {string} value
   * @returns {string}
   */
  function addSpaceIfNeeded (value) {
    return value.length > 0 ? value + ' ' : value;
  }

  /**
   * Check if about sign be displayed for a work duration
   * @param {number} days
   * @param {number} hours
   * @param {number} minutes
   * @returns {boolean}
   */
  function shouldDisplayAbout (days, hours, minutes) {
    var hasDays = days > 0,
        fewDays = days < 1000,
        hasHours = hours > 0,
        hasMinutes = minutes > 0;
    return (hasDays && fewDays && hasHours) || (!hasDays && hasHours && hasMinutes);
  }

  /**
   * Format a work duration based on parameters
   * @param {bool} isNegative
   * @param {number} days
   * @param {number} hours
   * @param {number} minutes
   * @returns {string}
   */
  function formatDuration (isNegative, days, hours, minutes) {
    var formatted = '';
    if (shouldDisplayDays(days)) {
      formatted += tp('work_duration.x_days', isNegative ? -1 * days : days);
    }
    if (shouldDisplayHours(days, hours)) {
      formatted = addSpaceIfNeeded(formatted);
      formatted += tp('work_duration.x_hours', isNegative && formatted.length === 0 ? -1 * hours : hours);
    }
    if (shouldDisplayMinutes(days, hours, minutes)) {
      formatted = addSpaceIfNeeded(formatted);
      formatted += tp('work_duration.x_minutes', isNegative && formatted.length === 0 ? -1 * minutes : minutes);
    }
    return formatted;
  }

  /**
   * Format a work duration based on parameters
   * @param {bool} isNegative
   * @param {number} days
   * @param {number} hours
   * @param {number} minutes
   * @returns {string}
   */
  function formatDurationShort (isNegative, days, hours, minutes) {
    var formatted = '';
    if (shouldDisplayDays(days)) {
      var formattedDays = window.formatMeasure(isNegative ? -1 * days : days, 'SHORT_INT');
      formatted += tp('work_duration.x_days', formattedDays);
    }
    if (shouldDisplayHoursInShortFormat(days, hours)) {
      formatted = addSpaceIfNeeded(formatted);
      formatted += tp('work_duration.x_hours', isNegative && formatted.length === 0 ? -1 * hours : hours);
    }
    if (shouldDisplayMinutesInShortFormat(days, hours, minutes)) {
      formatted = addSpaceIfNeeded(formatted);
      formatted += tp('work_duration.x_minutes', isNegative && formatted.length === 0 ? -1 * minutes : minutes);
    }
    if (shouldDisplayAbout(days, hours, minutes)) {
      formatted = tp('work_duration.about', formatted);
    }
    return formatted;
  }

  /**
   * Format a work duration measure
   * @param {number} value
   * @returns {string}
   */
  var durationFormatter = function (value) {
    if (value === 0) {
      return '0';
    }
    var hoursInDay = window.SS.hoursInDay || 8,
        isNegative = value < 0,
        absValue = Math.abs(value);
    var days = Math.floor(absValue / hoursInDay / 60);
    var remainingValue = absValue - days * hoursInDay * 60;
    var hours = Math.floor(remainingValue / 60);
    remainingValue -= hours * 60;
    return formatDuration(isNegative, days, hours, remainingValue);
  };

  /**
   * Format a work duration measure
   * @param {number} value
   * @returns {string}
   */
  var shortDurationFormatter = function (value) {
    value = parseInt(value, 10);
    if (value === 0) {
      return '0';
    }
    var hoursInDay = window.SS.hoursInDay || 8,
        isNegative = value < 0,
        absValue = Math.abs(value);
    var days = Math.floor(absValue / hoursInDay / 60);
    var remainingValue = absValue - days * hoursInDay * 60;
    var hours = Math.floor(remainingValue / 60);
    remainingValue -= hours * 60;
    return formatDurationShort(isNegative, days, hours, remainingValue);
  };

  /**
   * Format a work duration variation
   * @param {number} value
   */
  var durationVariationFormatter = function (value) {
    if (value === 0) {
      return '0';
    }
    var formatted = durationFormatter(value);
    return formatted[0] !== '-' ? '+' + formatted : formatted;
  };

  /**
   * Format a rating measure
   * @param {number} value
   */
  var ratingFormatter = function (value) {
    value = parseInt(value, 10);
    return String.fromCharCode(97 + value - 1).toUpperCase();
  };


  /**
   * Format a level measure
   * @param {number} value
   */
  var levelFormatter = function (value) {
    var l10nKey = 'metric.level.' + value,
        result = window.t(l10nKey);
    // if couldn't translate, return the initial value
    return l10nKey !== result ? result : value;
  };

  /**
   * Format a measure according to its type
   * @param measure
   * @param {string} type
   * @returns {string|null}
   */
  window.formatMeasure = function (measure, type) {
    var formatted = null,
        formatters = {
          'INT': function (value) {
            return numeral(value).format('0,0');
          },
          'SHORT_INT': shortIntFormatter,
          'FLOAT': function (value) {
            return numeral(value).format('0,0.0');
          },
          'PERCENT': function (value) {
            return numeral(+value / 100).format('0,0.0%');
          },
          'SHORT_PERCENT': function (value) {
            return numeral(+value / 100).format('0,0%');
          },
          'WORK_DUR': durationFormatter,
          'SHORT_WORK_DUR': shortDurationFormatter,
          'RATING': ratingFormatter,
          'LEVEL': levelFormatter
        };
    if (measure != null && type != null) {
      formatted = formatters[type] != null ? formatters[type](measure) : measure;
    }
    return formatted;
  };

  /**
   * Format a measure variation according to its type
   * @param measure
   * @param {string} type
   * @returns {string|null}
   */
  window.formatMeasureVariation = function (measure, type) {
    var formatted = null,
        formatters = {
          'INT': function (value) {
            return value === 0 ? '0' : numeral(value).format('+0,0');
          },
          'SHORT_INT': shortIntVariationFormatter,
          'FLOAT': function (value) {
            return value === 0 ? '0' : numeral(value).format('+0,0.0');
          },
          'PERCENT': function (value) {
            return value === 0 ? '0%' : numeral(+value / 100).format('+0,0.0%');
          },
          'WORK_DUR': durationVariationFormatter
        };
    if (measure != null && type != null) {
      formatted = formatters[type] != null ? formatters[type](measure) : measure;
    }
    return formatted;
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


  /**
   * Return an md5 hash of a string
   * @param s
   * @returns {*}
   */
  window.getMD5Hash = function (s) {
    if (typeof s === 'string') {
      return window.md5(s.trim());
    } else {
      return null;
    }
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
