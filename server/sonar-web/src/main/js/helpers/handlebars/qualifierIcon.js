import Handlebars from 'handlebars/runtime';

module.exports = function (qualifier) {
  return new Handlebars.default.SafeString(
      qualifier ? '<i class="icon-qualifier-' + qualifier.toLowerCase() + '"></i>' : ''
  );
};
