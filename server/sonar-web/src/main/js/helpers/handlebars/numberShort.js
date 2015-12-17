import numeral from 'numeral';

module.exports = function (number) {
  var format = '0,0';
  if (number >= 10000) {
    format = '0.[0]a';
  }
  if (number >= 100000) {
    format = '0a';
  }
  return numeral(number).format(format);
};
