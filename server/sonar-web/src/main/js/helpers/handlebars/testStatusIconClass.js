import Handlebars from 'handlebars/runtime';

module.exports = function (status) {
  return new Handlebars.default.SafeString('' +
      'icon-test-status-' + status.toLowerCase()
  );
};
