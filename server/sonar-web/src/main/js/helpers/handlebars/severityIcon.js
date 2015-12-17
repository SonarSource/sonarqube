import Handlebars from 'handlebars/runtime';

module.exports = function (severity) {
  return new Handlebars.default.SafeString(
      '<i class="icon-severity-' + severity.toLowerCase() + '"></i>'
  );
};
