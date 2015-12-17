import moment from 'moment';

module.exports = function (date) {
  return moment(date).format('LL');
};
