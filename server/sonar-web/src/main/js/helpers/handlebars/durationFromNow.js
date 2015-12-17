import moment from 'moment';

module.exports = function (date, units) {
  return moment(new Date()).diff(date, units);
};
