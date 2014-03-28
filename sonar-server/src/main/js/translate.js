(function() {
  var warn = function(message) {
    if (console != null && typeof console.warn === 'function') {
      console.warn(message);
    }
  };

  window.t = function() {
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


  window.tp = function() {
    var args = Array.prototype.slice.call(arguments, 0),
        key = args.shift(),
        message = window.messages[key];
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

})();
