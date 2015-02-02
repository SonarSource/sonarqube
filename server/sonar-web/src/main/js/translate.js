(function() {
  window.suppressTranslationWarnings = false;

  window.t = function() {
    if (!window.messages) {
      return window.translate.apply(this, arguments);
    }

    var args = Array.prototype.slice.call(arguments, 0),
        key = args.join('.');
    return window.messages[key] != null ? window.messages[key] : key;
  };

  window.tp = function() {
    var args = Array.prototype.slice.call(arguments, 0),
      key = args.shift(),
      message = window.messages[key];
    if (message) {
      args.forEach(function(p, i) {
        message = message.replace('{' + i + '}', p);
      });
    }
    return message || (key + ' ' + args.join(' '));
  };


  window.translate = function() {
    var args = Array.prototype.slice.call(arguments, 0),
        tokens = args.reduce(function(prev, current) {
          return prev.concat(current.split('.'));
        }, []),
        key = tokens.join('.'),
        start = window.SS && window.SS.phrases,
        found = !!start,
        result = '';

    if (found) {
      result = tokens.reduce(function(prev, current) {
        if (!current || !prev[current]) {
          found = false;
        }
        return current ? prev[current] : prev;
      }, start);
    }

    return found ? result : key;
  };


  window.requestMessages = function() {
    var currentLocale = window.pageLang,
        cachedLocale = localStorage.getItem('l10n.locale');
    if (cachedLocale !== currentLocale) {
      localStorage.removeItem('l10n.timestamp');
    }

    var bundleTimestamp = localStorage.getItem('l10n.timestamp'),
        params = { locale: currentLocale };
    if (bundleTimestamp !== null) {
      params.ts = bundleTimestamp;
    }

    var apiUrl = baseUrl + '/api/l10n/index';
    return jQuery.ajax({
      url: apiUrl,
      data: params,
      dataType: 'json',
      statusCode: {
        304: function() {
          window.messages = JSON.parse(localStorage.getItem('l10n.bundle'));
        }
      }
    }).done(function(bundle, textStatus, jqXHR) {
      if(bundle !== undefined) {
        bundleTimestamp = new Date().toISOString();
        bundleTimestamp = bundleTimestamp.substr(0, bundleTimestamp.indexOf('.')) + '+0000';
        localStorage.setItem('l10n.timestamp', bundleTimestamp);
        localStorage.setItem('l10n.locale', currentLocale);

        window.messages = bundle;
        localStorage.setItem('l10n.bundle', JSON.stringify(bundle));
      } else if (jqXHR.status === 304) {
        window.messages = JSON.parse(localStorage.getItem('l10n.bundle'));
      }
    });
  };
})();
