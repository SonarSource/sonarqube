module.exports = function (value, total) {
  if (total > 0) {
    return '' + ((value || 0) / total * 100) + '%';
  } else {
    return '0%';
  }
};
