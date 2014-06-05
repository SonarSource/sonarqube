(function() {
  var warn = function(message) {
    if (console != null && typeof console.warn === 'function') {
      console.warn(message);
    }
  };

  window.t2 = function() {
    if (!window.messages) {
      return window.translate.apply(this, arguments);
    }

    var args = Array.prototype.slice.call(arguments, 0),
        key = args.join('.');
    if (!window.messages[key]) {
      warn('No translation for "' + key + '"');
    }
    return (window.messages && window.messages[key]) || key;
  };

  window.t = function() {
    var args = Array.prototype.slice.call(arguments, 0),
        key = args.join('.'),
        storageKey = 'l10n.' + key,
        message = localStorage.getItem(storageKey);
    if (!message) {
      return window.t2.apply(this, arguments);
    }
    return message;
  };


  window.tp = function() {
    var args = Array.prototype.slice.call(arguments, 0),
        key = args.shift(),
        storageKey = 'l10n.' + key,
        message = localStorage.getItem(storageKey);
    if (!message) {
      message = window.messages[key];
    }
    if (message) {
      args.forEach(function(p, i) {
        message = message.replace('{' + i + '}', p);
      });
    } else {
      warn('No translation for "' + key + '"');
    }
    return message || '';
  };


  window.translate = function() {
    var args = Array.prototype.slice.call(arguments, 0),
        tokens = args.reduce(function(prev, current) {
          return prev.concat(current.split('.'));
        }, []),
        key = tokens.join('.'),
        start = window.SS && window.SS.phrases,
        found = !!start;

    if (found) {
      var result = tokens.reduce(function(prev, current) {
        if (!current || !prev[current]) {
          warn('No translation for "' + key + '"');
          found = false;
        }
        return current ? prev[current] : prev;
      }, start);
    } else {
      warn('No translation for "' + key + '"');
    }

    return found ? result : key;
  };


  window.requestMessages = function() {
    var currentLocale = navigator.language.replace('-', '_');
    var cachedLocale = localStorage.getItem('l10n.locale');
    if (cachedLocale !== currentLocale) {
      localStorage.removeItem('l10n.timestamp');
    }

    var bundleTimestamp = localStorage.getItem('l10n.timestamp');
    var params = {};
    if (bundleTimestamp !== null) {
      params['ts'] = bundleTimestamp;
    }

    var apiUrl = baseUrl + '/api/l10n/index';
    return jQuery.ajax({
      'url': apiUrl,
      'data': params,
      'statusCode': {
        304: function() {
          // NOP, use cached messages
        }
      }
    }).done(function(bundle) {
      bundleTimestamp = new Date().toISOString();
      bundleTimestamp = bundleTimestamp.substr(0, bundleTimestamp.indexOf('.')) + '+0000';
      localStorage.setItem('l10n.timestamp', bundleTimestamp);
      localStorage.setItem('l10n.locale', currentLocale);

      for (var message in bundle) {
        if (bundle.hasOwnProperty(message)) {
          var storageKey = 'l10n.' + message;
          localStorage.setItem(storageKey, bundle[message]);
        }
      }
    });
  };

})();
