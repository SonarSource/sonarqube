/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
(function ($) {

  function transformPattern (pattern) {
    return pattern.replace(/\{0\}/g, '(\\d+)');
  }

  function convertWorkDuration (value) {
    var days, daysPattern, hours, hoursPattern, minutes, minutesPattern;
    if (value === '0') {
      return 0;
    }
    daysPattern = transformPattern(t('work_duration.x_days'));
    hoursPattern = transformPattern(t('work_duration.x_hours'));
    minutesPattern = transformPattern(t('work_duration.x_minutes'));
    days = value.match(daysPattern);
    hours = value.match(hoursPattern);
    minutes = value.match(minutesPattern);
    days = days ? +days[1] : 0;
    hours = hours ? +hours[1] : 0;
    minutes = minutes ? +minutes[1] : 0;
    if (!value) {
      return value;
    } else {
      return (days * window.SS.hoursInDay + hours) * 60 + minutes;
    }
  }

  function restoreWorkDuration (value) {
    var days, hours, minutes, result;
    if (value === '0' || value === 0) {
      return '0';
    }
    if (!/^\d+$/.test(value)) {
      return value;
    }
    days = Math.floor(value / (window.SS.hoursInDay * 60));
    hours = Math.floor((value - days * window.SS.hoursInDay * 60) / 60);
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
  }

  function convertRating (value) {
    if (/^[ABCDE]$/.test(value)) {
      return value.charCodeAt(0) - 'A'.charCodeAt(0) + 1;
    } else {
      return value;
    }
  }

  function convertValue (value, input) {
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
  }

  function restoreRating (value) {
    if (!/^[12345]+$/.test(value)) {
      return value;
    }
    return String.fromCharCode(value - 1 + 'A'.charCodeAt(0));
  }

  function restoreValue (value, input) {
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
  }

  var originalVal = $.fn.val;

  $.fn.val = function (value) {
    if (arguments.length) {
      return originalVal.call(this, restoreValue(value, this));
    } else {
      return convertValue(originalVal.call(this), this);
    }
  };

  $.fn.originalVal = originalVal;

})(window.jQuery);
