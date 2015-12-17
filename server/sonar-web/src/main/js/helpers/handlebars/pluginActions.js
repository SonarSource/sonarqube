const DEFAULT_ACTIONS = ['comment', 'assign', 'assign_to_me', 'plan', 'set_severity', 'set_tags'];

module.exports = function (actions, options) {
  var pluginActions = _.difference(actions, DEFAULT_ACTIONS);
  return pluginActions.reduce(function (prev, current) {
    return prev + options.fn(current);
  }, '');
};
