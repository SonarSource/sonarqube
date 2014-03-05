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


  window.translate = function() {
    var args = Array.prototype.slice.call(arguments, 0),
        tokens = args.reduce(function(prev, current) {
          return prev.concat(current.split('.'));
        }, []),
        key = tokens.join('.'),
        start = window.SS.phrases,
        found = true;

    var result = tokens.reduce(function(prev, current) {
      if (!current || !prev[current]) {
        warn('No translation for "' + key + '"');
        found = false;
      }
      return current ? prev[current] : prev;
    }, start);

    return found ? result : key;
  };

})();
