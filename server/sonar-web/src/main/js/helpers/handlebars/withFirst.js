module.exports = function (list, options) {
  if (list && list.length > 0) {
    return options.fn(list[0]);
  } else {
    return '';
  }
};
