import Handlebars from 'handlebars/runtime';

module.exports = function (status) {
  return new Handlebars.default.SafeString(
      '<i class="icon-test-status-' + status.toLowerCase() + '"></i>'
  );
};
