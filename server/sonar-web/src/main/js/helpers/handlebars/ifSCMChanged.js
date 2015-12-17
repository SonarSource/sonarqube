import _ from 'underscore';

module.exports = function (source, line, options) {
  var currentLine = _.findWhere(source, { lineNumber: line }),
      prevLine = _.findWhere(source, { lineNumber: line - 1 }),
      changed = true;
  if (currentLine && prevLine && currentLine.scm && prevLine.scm) {
    changed = (currentLine.scm.author !== prevLine.scm.author) ||
        (currentLine.scm.date !== prevLine.scm.date) || (!prevLine.show);
  }
  return changed ? options.fn(this) : options.inverse(this);
};
