import Handlebars from 'handlebars/runtime';

module.exports = function (value, parameter) {
  if (parameter) {
    return new Handlebars.default.SafeString(
        window.tp('quality_profiles.parameter_set_to_x', value, parameter)
    );
  } else {
    return new Handlebars.default.SafeString(
        window.tp('quality_profiles.changelog.parameter_reset_to_default_value_x', parameter)
    );
  }
};
