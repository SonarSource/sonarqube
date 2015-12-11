import { expect } from 'chai';

import { formatMeasure, formatMeasureVariation } from '../../src/main/js/helpers/measures';


describe('Measures', function () {
  var HOURS_IN_DAY = 8,
      ONE_MINUTE = 1,
      ONE_HOUR = ONE_MINUTE * 60,
      ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

  before(function () {
    window.messages = {
      'work_duration.x_days': '{0}d',
      'work_duration.x_hours': '{0}h',
      'work_duration.x_minutes': '{0}min',
      'work_duration.about': '~ {0}',
      'metric.level.ERROR': 'Error',
      'metric.level.WARN': 'Warning',
      'metric.level.OK': 'Ok'
    };
    window.t = function() {
      if (!window.messages) {
        return window.translate.apply(this, arguments);
      }
      var args = Array.prototype.slice.call(arguments, 0),
          key = args.join('.');
      return window.messages[key] != null ? window.messages[key] : key;
    };
    window.tp = function () {
      var args = Array.prototype.slice.call(arguments, 0),
          key = args.shift(),
          message = window.messages[key];
      if (message) {
        args.forEach(function (p, i) {
          message = message.replace('{' + i + '}', p);
        });
      }
      return message || (key + ' ' + args.join(' '));
    };
    window.SS = { hoursInDay: HOURS_IN_DAY };
  });

  describe('#formatMeasure()', function () {
    it('should format INT', function () {
      expect(formatMeasure(0, 'INT')).to.equal('0');
      expect(formatMeasure(1, 'INT')).to.equal('1');
      expect(formatMeasure(-5, 'INT')).to.equal('-5');
      expect(formatMeasure(999, 'INT')).to.equal('999');
      expect(formatMeasure(1000, 'INT')).to.equal('1,000');
      expect(formatMeasure(1529, 'INT')).to.equal('1,529');
      expect(formatMeasure(10000, 'INT')).to.equal('10,000');
      expect(formatMeasure(1234567890, 'INT')).to.equal('1,234,567,890');
    });

    it('should format SHORT_INT', function () {
      expect(formatMeasure(0, 'SHORT_INT')).to.equal('0');
      expect(formatMeasure(1, 'SHORT_INT')).to.equal('1');
      expect(formatMeasure(999, 'SHORT_INT')).to.equal('999');
      expect(formatMeasure(1000, 'SHORT_INT')).to.equal('1k');
      expect(formatMeasure(1529, 'SHORT_INT')).to.equal('1.5k');
      expect(formatMeasure(10000, 'SHORT_INT')).to.equal('10k');
      expect(formatMeasure(10678, 'SHORT_INT')).to.equal('11k');
      expect(formatMeasure(1234567890, 'SHORT_INT')).to.equal('1b');
    });

    it('should format FLOAT', function () {
      expect(formatMeasure(0.0, 'FLOAT')).to.equal('0.0');
      expect(formatMeasure(1.0, 'FLOAT')).to.equal('1.0');
      expect(formatMeasure(1.3, 'FLOAT')).to.equal('1.3');
      expect(formatMeasure(1.34, 'FLOAT')).to.equal('1.34');
      expect(formatMeasure(50.89, 'FLOAT')).to.equal('50.89');
      expect(formatMeasure(100.0, 'FLOAT')).to.equal('100.0');
      expect(formatMeasure(123.456, 'FLOAT')).to.equal('123.456');
      expect(formatMeasure(123456.7, 'FLOAT')).to.equal('123,456.7');
      expect(formatMeasure(1234567890.0, 'FLOAT')).to.equal('1,234,567,890.0');
    });

    it('should respect FLOAT precision', function () {
      expect(formatMeasure(0.1, 'FLOAT')).to.equal('0.1');
      expect(formatMeasure(0.12, 'FLOAT')).to.equal('0.12');
      expect(formatMeasure(0.12345, 'FLOAT')).to.equal('0.12345');
      expect(formatMeasure(0.123456, 'FLOAT')).to.equal('0.12346');
    });

    it('should format PERCENT', function () {
      expect(formatMeasure(0.0, 'PERCENT')).to.equal('0.0%');
      expect(formatMeasure(1.0, 'PERCENT')).to.equal('1.0%');
      expect(formatMeasure(1.3, 'PERCENT')).to.equal('1.3%');
      expect(formatMeasure(1.34, 'PERCENT')).to.equal('1.3%');
      expect(formatMeasure(50.89, 'PERCENT')).to.equal('50.9%');
      expect(formatMeasure(100.0, 'PERCENT')).to.equal('100.0%');
    });

    it('should format WORK_DUR', function () {
      expect(formatMeasure(0, 'WORK_DUR')).to.equal('0');
      expect(formatMeasure(5 * ONE_DAY, 'WORK_DUR')).to.equal('5d');
      expect(formatMeasure(2 * ONE_HOUR, 'WORK_DUR')).to.equal('2h');
      expect(formatMeasure(40 * ONE_MINUTE, 'WORK_DUR')).to.equal('40min');
      expect(formatMeasure(ONE_MINUTE, 'WORK_DUR')).to.equal('1min');
      expect(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR, 'WORK_DUR')).to.equal('5d 2h');
      expect(formatMeasure(2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR')).to.equal('2h 1min');
      expect(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR')).to.equal('5d 2h');
      expect(formatMeasure(15 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR')).to.equal('15d');
      expect(formatMeasure(-5 * ONE_DAY, 'WORK_DUR')).to.equal('-5d');
      expect(formatMeasure(-2 * ONE_HOUR, 'WORK_DUR')).to.equal('-2h');
      expect(formatMeasure(-1 * ONE_MINUTE, 'WORK_DUR')).to.equal('-1min');
    });

    it('should format SHORT_WORK_DUR', function () {
      expect(formatMeasure(0, 'SHORT_WORK_DUR')).to.equal('0');
      expect(formatMeasure(5 * ONE_DAY, 'SHORT_WORK_DUR')).to.equal('5d');
      expect(formatMeasure(2 * ONE_HOUR, 'SHORT_WORK_DUR')).to.equal('2h');
      expect(formatMeasure(40 * ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('40min');
      expect(formatMeasure(ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('1min');
      expect(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR, 'SHORT_WORK_DUR')).to.equal('5d');
      expect(formatMeasure(2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('2h');
      expect(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('5d');
      expect(formatMeasure(15 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('15d');
      expect(formatMeasure(7 * ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('7min');
      expect(formatMeasure(-5 * ONE_DAY, 'SHORT_WORK_DUR')).to.equal('-5d');
      expect(formatMeasure(-2 * ONE_HOUR, 'SHORT_WORK_DUR')).to.equal('-2h');
      expect(formatMeasure(-1 * ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('-1min');

      expect(formatMeasure(1529 * ONE_DAY, 'SHORT_WORK_DUR')).to.equal('1.5kd');
      expect(formatMeasure(1234567 * ONE_DAY, 'SHORT_WORK_DUR')).to.equal('1md');
      expect(formatMeasure(1234567 * ONE_DAY + 2 * ONE_HOUR, 'SHORT_WORK_DUR')).to.equal('1md');
    });

    it('should format RATING', function () {
      expect(formatMeasure(1, 'RATING')).to.equal('A');
      expect(formatMeasure(2, 'RATING')).to.equal('B');
      expect(formatMeasure(3, 'RATING')).to.equal('C');
      expect(formatMeasure(4, 'RATING')).to.equal('D');
      expect(formatMeasure(5, 'RATING')).to.equal('E');
    });

    it('should format LEVEL', function () {
      expect(formatMeasure('ERROR', 'LEVEL')).to.equal('Error');
      expect(formatMeasure('WARN', 'LEVEL')).to.equal('Warning');
      expect(formatMeasure('OK', 'LEVEL')).to.equal('Ok');
      expect(formatMeasure('UNKNOWN', 'LEVEL')).to.equal('UNKNOWN');
    });

    it('should format MILLISEC', function () {
      expect(formatMeasure(0, 'MILLISEC')).to.equal('0ms');
      expect(formatMeasure(1, 'MILLISEC')).to.equal('1ms');
      expect(formatMeasure(173, 'MILLISEC')).to.equal('173ms');
      expect(formatMeasure(3649, 'MILLISEC')).to.equal('4s');
      expect(formatMeasure(893481, 'MILLISEC')).to.equal('15min');
      expect(formatMeasure(17862325, 'MILLISEC')).to.equal('298min');
    });

    it('should not format unknown type', function () {
      expect(formatMeasure('random value', 'RANDOM_TYPE')).to.equal('random value');
    });

    it('should return null if value is empty string', function () {
      expect(formatMeasure('', 'PERCENT')).to.be.null;
    });

    it('should not fail without parameters', function () {
      expect(formatMeasure()).to.be.null;
    });
  });

  describe('#formatMeasureVariation()', function () {
    it('should format INT', function () {
      expect(formatMeasureVariation(0, 'INT')).to.equal('+0');
      expect(formatMeasureVariation(1, 'INT')).to.equal('+1');
      expect(formatMeasureVariation(-1, 'INT')).to.equal('-1');
      expect(formatMeasureVariation(1529, 'INT')).to.equal('+1,529');
      expect(formatMeasureVariation(-1529, 'INT')).to.equal('-1,529');
    });

    it('should format SHORT_INT', function () {
      expect(formatMeasureVariation(0, 'SHORT_INT')).to.equal('+0');
      expect(formatMeasureVariation(1, 'SHORT_INT')).to.equal('+1');
      expect(formatMeasureVariation(-1, 'SHORT_INT')).to.equal('-1');
      expect(formatMeasureVariation(1529, 'SHORT_INT')).to.equal('+1.5k');
      expect(formatMeasureVariation(-1529, 'SHORT_INT')).to.equal('-1.5k');
      expect(formatMeasureVariation(10678, 'SHORT_INT')).to.equal('+11k');
      expect(formatMeasureVariation(-10678, 'SHORT_INT')).to.equal('-11k');
    });

    it('should format FLOAT', function () {
      expect(formatMeasureVariation(0.0, 'FLOAT')).to.equal('+0.0');
      expect(formatMeasureVariation(1.0, 'FLOAT')).to.equal('+1.0');
      expect(formatMeasureVariation(-1.0, 'FLOAT')).to.equal('-1.0');
      expect(formatMeasureVariation(50.89, 'FLOAT')).to.equal('+50.89');
      expect(formatMeasureVariation(-50.89, 'FLOAT')).to.equal('-50.89');
    });

    it('should respect FLOAT precision', function () {
      expect(formatMeasureVariation(0.1, 'FLOAT')).to.equal('+0.1');
      expect(formatMeasureVariation(0.12, 'FLOAT')).to.equal('+0.12');
      expect(formatMeasureVariation(0.12345, 'FLOAT')).to.equal('+0.12345');
      expect(formatMeasureVariation(0.123456, 'FLOAT')).to.equal('+0.12346');
    });

    it('should format PERCENT', function () {
      expect(formatMeasureVariation(0.0, 'PERCENT')).to.equal('+0.0%');
      expect(formatMeasureVariation(1.0, 'PERCENT')).to.equal('+1.0%');
      expect(formatMeasureVariation(-1.0, 'PERCENT')).to.equal('-1.0%');
      expect(formatMeasureVariation(50.89, 'PERCENT')).to.equal('+50.9%');
      expect(formatMeasureVariation(-50.89, 'PERCENT')).to.equal('-50.9%');
    });

    it('should format WORK_DUR', function () {
      expect(formatMeasureVariation(0, 'WORK_DUR')).to.equal('0');
      expect(formatMeasureVariation(5 * ONE_DAY, 'WORK_DUR')).to.equal('+5d');
      expect(formatMeasureVariation(2 * ONE_HOUR, 'WORK_DUR')).to.equal('+2h');
      expect(formatMeasureVariation(ONE_MINUTE, 'WORK_DUR')).to.equal('+1min');
      expect(formatMeasureVariation(-5 * ONE_DAY, 'WORK_DUR')).to.equal('-5d');
      expect(formatMeasureVariation(-2 * ONE_HOUR, 'WORK_DUR')).to.equal('-2h');
      expect(formatMeasureVariation(-1 * ONE_MINUTE, 'WORK_DUR')).to.equal('-1min');
    });

    it('should format SHORT_WORK_DUR', function () {
      expect(formatMeasureVariation(0, 'SHORT_WORK_DUR')).to.equal('+0');
      expect(formatMeasureVariation(5 * ONE_DAY, 'SHORT_WORK_DUR')).to.equal('+5d');
      expect(formatMeasureVariation(2 * ONE_HOUR, 'SHORT_WORK_DUR')).to.equal('+2h');
      expect(formatMeasureVariation(ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('+1min');
      expect(formatMeasureVariation(5 * ONE_DAY + 2 * ONE_HOUR, 'SHORT_WORK_DUR')).to.equal('+5d');
      expect(formatMeasureVariation(2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('+2h');
      expect(formatMeasureVariation(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('+5d');
      expect(formatMeasureVariation(15 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('+15d');
      expect(formatMeasureVariation(7 * ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('+7min');
      expect(formatMeasureVariation(-5 * ONE_DAY, 'SHORT_WORK_DUR')).to.equal('-5d');
      expect(formatMeasureVariation(-2 * ONE_HOUR, 'SHORT_WORK_DUR')).to.equal('-2h');
      expect(formatMeasureVariation(-1 * ONE_MINUTE, 'SHORT_WORK_DUR')).to.equal('-1min');

      expect(formatMeasureVariation(1529 * ONE_DAY, 'SHORT_WORK_DUR')).to.equal('+1.5kd');
      expect(formatMeasureVariation(1234567 * ONE_DAY, 'SHORT_WORK_DUR')).to.equal('+1md');
      expect(formatMeasureVariation(1234567 * ONE_DAY + 2 * ONE_HOUR, 'SHORT_WORK_DUR')).to.equal('+1md');
    });

    it('should not format unknown type', function () {
      expect(formatMeasureVariation('random value', 'RANDOM_TYPE')).to.equal('random value');
    });

    it('should not fail without parameters', function () {
      expect(formatMeasureVariation()).to.be.null;
    });
  });
});
