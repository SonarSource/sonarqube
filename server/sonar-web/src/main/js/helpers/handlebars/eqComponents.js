module.exports = function (a, b, options) {
  var notEq = a && b && ((a.project !== b.project) || (a.subProject !== b.subProject));
  return notEq ? options.inverse(this) : options.fn(this);
};
