module.exports = function (list, options) {
  if (list && list.length > 0) {
    return options.fn(list[list.length - 1]);
  } else {
    return '';
  }
};
