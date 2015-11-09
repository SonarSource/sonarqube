import _ from 'underscore';
import moment from 'moment';

export let getPeriodLabel = (periods, periodIndex) => {
  let period = _.findWhere(periods, { index: periodIndex });
  if (!period) {
    return null;
  }
  if (period.mode === 'previous_version' && !period.modeParam) {
    return window.t('overview.period.previous_version_only_date');
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
