import _ from 'underscore';
import moment from 'moment';


export function getPeriodLabel (periods, periodIndex) {
  let period = _.findWhere(periods, { index: periodIndex });
  if (!period) {
    return null;
  }
  if (period.mode === 'previous_version' && !period.modeParam) {
    return window.t('overview.period.previous_version_only_date');
  }
  return window.tp(`overview.period.${period.mode}`, period.modeParam);
}


export function getPeriodDate (periods, periodIndex) {
  let period = _.findWhere(periods, { index: periodIndex });
  if (!period) {
    return null;
  }
  return period.date ? moment(period.date).toDate() : null;
}
