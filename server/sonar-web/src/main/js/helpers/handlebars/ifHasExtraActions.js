const DEFAULT_ACTIONS = ['comment', 'assign', 'assign_to_me', 'plan', 'set_severity', 'set_tags'];

module.exports = function (actions, options) {
  var actionsLeft = _.difference(actions, DEFAULT_ACTIONS);
  if (actionsLeft.length > 0) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
};
