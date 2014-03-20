(function() {
  var $, convertRating, convertValue, convertWorkDuration, originalVal, restoreRating, restoreValue, restoreWorkDuration, transformPattern;

  $ = jQuery;

  transformPattern = function(pattern) {
    return pattern.replace(/\{0\}/g, '(\\d+)');
  };

  convertWorkDuration = function(value) {
    var days, daysPattern, hours, hoursPattern, minutes, minutesPattern;
    daysPattern = transformPattern(t('work_duration.x_days'));
    hoursPattern = transformPattern(t('work_duration.x_hours'));
    minutesPattern = transformPattern(t('work_duration.x_minutes'));
    days = value.match(daysPattern);
    hours = value.match(hoursPattern);
    minutes = value.match(minutesPattern);
    days = days ? +days[1] : 0;
    hours = hours ? +hours[1] : 0;
    minutes = minutes ? +minutes[1] : 0;
    if (!value || (value.length > 0 && days === 0 && hours === 0 && minutes === 0)) {
      return value;
    } else {
      return (days * 8 + hours) * 60 + minutes;
    }
  };

  restoreWorkDuration = function(value) {
    var days, hours, minutes, result;
    if (!/^\d+$/.test(value)) {
      return value;
    }
    days = Math.floor(value / (8 * 60));
    hours = Math.floor((value - days * 8 * 60) / 60);
    minutes = value % 60;
    result = [];
    if (days > 0) {
      result.push(t('work_duration.x_days').replace('{0}', days));
    }
    if (hours > 0) {
      result.push(t('work_duration.x_hours').replace('{0}', hours));
    }
    if (minutes > 0) {
      result.push(t('work_duration.x_minutes').replace('{0}', minutes));
    }
    return result.join(' ');
  };

  convertRating = function(value) {
    if (/^[ABCDE]$/.test(value)) {
      return value.charCodeAt(0) - 'A'.charCodeAt(0) + 1;
    } else {
      return value;
    }
  };

  convertValue = function(value, input) {
    var type;
    type = input.data('type');
    if (type == null) {
      return value;
    }
    switch (type) {
      case 'WORK_DUR':
        return convertWorkDuration(value);
      case 'RATING':
        return convertRating(value);
      default:
        return value;
    }
  };

  restoreRating = function(value) {
    if (!/^[12345]+$/.test(value)) {
      return value;
    }
    return String.fromCharCode(value - 1 + 'A'.charCodeAt(0));
  };

  restoreValue = function(value, input) {
    var type;
    type = input.data('type');
    if (type == null) {
      return value;
    }
    switch (type) {
      case 'WORK_DUR':
        return restoreWorkDuration(value);
      case 'RATING':
        return restoreRating(value);
      default:
        return value;
    }
  };

  originalVal = $.fn.val;

  $.fn.val = function(value) {
    if (arguments.length) {
      return originalVal.call(this, restoreValue(value, this));
    } else {
      return convertValue(originalVal.call(this), this);
    }
  };

  $.fn.originalVal = originalVal;

}).call(this);
