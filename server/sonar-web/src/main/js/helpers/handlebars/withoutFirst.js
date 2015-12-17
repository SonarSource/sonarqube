module.exports = function (list, options) {
  if (list && list.length > 1) {
    return list.slice(1).reduce(function (prev, current) {
      return prev + options.fn(current);
    }, '');
  } else {
    return '';
  }
};
