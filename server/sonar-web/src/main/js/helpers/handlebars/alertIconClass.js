import Handlebars from 'handlebars/runtime';

module.exports = function (alert) {
  return new Handlebars.default.SafeString(
      'icon-alert-' + alert.toLowerCase()
  );
};
