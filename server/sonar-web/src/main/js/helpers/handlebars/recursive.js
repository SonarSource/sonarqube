let audaciousFn;

module.exports = function (children, options) {
  var out = '';

  if (options.fn !== undefined) {
    audaciousFn = options.fn;
  }

  children.forEach(function (child) {
    out = out + audaciousFn(child);
  });

  return out;
};
