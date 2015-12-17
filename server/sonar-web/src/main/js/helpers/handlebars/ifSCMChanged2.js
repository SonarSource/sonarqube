import _ from 'underscore';

module.exports = function (source, line, options) {
  var currentLine = _.findWhere(source, { line: line }),
      prevLine = _.findWhere(source, { line: line - 1 }),
      changed = true;
  if (currentLine && prevLine && currentLine.scmAuthor && prevLine.scmAuthor) {
    changed = (currentLine.scmAuthor !== prevLine.scmAuthor) || (currentLine.scmDate !== prevLine.scmDate);
  }
  return changed ? options.fn(this) : options.inverse(this);
};
