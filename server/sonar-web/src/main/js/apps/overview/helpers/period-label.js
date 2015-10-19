import _ from 'underscore';
import moment from 'moment';

export let periodLabel = (periods, periodIndex) => {
  let period = _.findWhere(periods, { index: periodIndex });
  if (!period) {
    return null;
  }
  return window.tp(`overview.period.${period.mode}`, period.modeParam);
};

export let getPeriodDate = (periods, periodIndex) => {
  let period = _.findWhere(periods, { index: periodIndex });
  if (!period) {
    return null;
  }
  return moment(period.date).toDate();
};
