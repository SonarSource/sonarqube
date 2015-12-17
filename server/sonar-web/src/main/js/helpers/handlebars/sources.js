module.exports = function (source, scm, options) {
  if (options == null) {
    options = scm;
    scm = null;
  }

  var sources = _.map(source, function (code, line) {
    return {
      lineNumber: line,
      code: code,
      scm: (scm && scm[line]) ? { author: scm[line][0], date: scm[line][1] } : undefined
    };
  });

  return sources.reduce(function (prev, current, index) {
    return prev + options.fn(_.extend({ first: index === 0 }, current));
  }, '');
};
