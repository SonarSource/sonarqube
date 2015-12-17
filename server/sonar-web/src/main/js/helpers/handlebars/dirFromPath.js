import _ from 'underscore';

module.exports = function (path) {
  if (typeof path === 'string') {
    var tokens = path.split('/');
    return tokens.length > 1 ? _.initial(tokens).join('/') + '/' : '';
  } else {
    return null;
  }
};
