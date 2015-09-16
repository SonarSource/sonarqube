(function() {

  window.csvEscape = function(value) {
    var escaped = value.replace(/"/g, '\\"');
    return '"' + escaped + '"';
  };

})();
