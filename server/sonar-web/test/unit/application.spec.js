define(function (require) {
  var bdd = require('intern!bdd');
  var assert = require('intern/chai!assert');

  require('intern/order!build/js/libs/translate.js');
  require('intern/order!build/js/libs/third-party/jquery.js');
  require('intern/order!build/js/libs/third-party/underscore.js');
  require('intern/order!build/js/libs/third-party/keymaster.js');
  require('intern/order!build/js/libs/third-party/numeral.js');
  require('intern/order!build/js/libs/third-party/numeral-languages.js');
  require('intern/order!build/js/libs/application.js');

  bdd.describe('Application', function () {
    bdd.describe('#collapsedDirFromPath()', function () {
      bdd.it('should return null when pass null', function () {
        assert.isNull(window.collapsedDirFromPath(null));
      });

      bdd.it('should return "/" when pass "/"', function () {
        assert.equal(window.collapsedDirFromPath('/'), '/');
      });

      bdd.it('should not cut short path', function () {
        assert.equal(window.collapsedDirFromPath('src/main/js/components/state.js'), 'src/main/js/components/');
      });

      bdd.it('should cut long path', function () {
        assert.equal(window.collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js'),
            'src/.../js/components/navigator/app/models/');
      });

      bdd.it('should cut very long path', function () {
        assert.equal(window.collapsedDirFromPath('src/main/another/js/components/navigator/app/models/state.js'),
            'src/.../js/components/navigator/app/models/');
      });
    });

    bdd.describe('#fileFromPath()', function () {
      bdd.it('should return null when pass null', function () {
        assert.isNull(window.fileFromPath(null));
      });

      bdd.it('should return empty string when pass "/"', function () {
        assert.equal(window.fileFromPath('/'), '');
      });

      bdd.it('should return file name when pass only file name', function () {
        assert.equal(window.fileFromPath('file.js'), 'file.js');
      });

      bdd.it('should return file name when pass file path', function () {
        assert.equal(window.fileFromPath('src/main/js/file.js'), 'file.js');
      });

      bdd.it('should return file name when pass file name without extension', function () {
        assert.equal(window.fileFromPath('src/main/file'), 'file');
      });
    });

    bdd.describe('Measures', function () {
      var HOURS_IN_DAY = 8,
          ONE_MINUTE = 1,
          ONE_HOUR = ONE_MINUTE * 60,
          ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

      bdd.before(function () {
        window.messages = {
          'work_duration.x_days': '{0}d',
          'work_duration.x_hours': '{0}h',
          'work_duration.x_minutes': '{0}min',
          'work_duration.about': '~ {0}'
        };
        window.SS = { hoursInDay: HOURS_IN_DAY };
      });

      bdd.describe('#formatMeasure()', function () {
        bdd.it('should format INT', function () {
          assert.equal(window.formatMeasure(0, 'INT'), '0');
          assert.equal(window.formatMeasure(1, 'INT'), '1');
          assert.equal(window.formatMeasure(-5, 'INT'), '-5');
          assert.equal(window.formatMeasure(999, 'INT'), '999');
          assert.equal(window.formatMeasure(1000, 'INT'), '1,000');
          assert.equal(window.formatMeasure(1529, 'INT'), '1,529');
          assert.equal(window.formatMeasure(10000, 'INT'), '10,000');
          assert.equal(window.formatMeasure(1234567890, 'INT'), '1,234,567,890');
        });

        bdd.it('should format SHORT_INT', function () {
          assert.equal(window.formatMeasure(0, 'SHORT_INT'), '0');
          assert.equal(window.formatMeasure(1, 'SHORT_INT'), '1');
          assert.equal(window.formatMeasure(999, 'SHORT_INT'), '999');
          assert.equal(window.formatMeasure(1000, 'SHORT_INT'), '1k');
          assert.equal(window.formatMeasure(1529, 'SHORT_INT'), '1.5k');
          assert.equal(window.formatMeasure(10000, 'SHORT_INT'), '10k');
          assert.equal(window.formatMeasure(10678, 'SHORT_INT'), '11k');
          assert.equal(window.formatMeasure(1234567890, 'SHORT_INT'), '1b');
        });

        bdd.it('should format FLOAT', function () {
          assert.equal(window.formatMeasure(0.0, 'FLOAT'), '0.0');
          assert.equal(window.formatMeasure(1.0, 'FLOAT'), '1.0');
          assert.equal(window.formatMeasure(1.3, 'FLOAT'), '1.3');
          assert.equal(window.formatMeasure(1.34, 'FLOAT'), '1.3');
          assert.equal(window.formatMeasure(50.89, 'FLOAT'), '50.9');
          assert.equal(window.formatMeasure(100.0, 'FLOAT'), '100.0');
          assert.equal(window.formatMeasure(123.456, 'FLOAT'), '123.5');
          assert.equal(window.formatMeasure(123456.7, 'FLOAT'), '123,456.7');
          assert.equal(window.formatMeasure(1234567890.0, 'FLOAT'), '1,234,567,890.0');
        });

        bdd.it('should format PERCENT', function () {
          assert.equal(window.formatMeasure(0.0, 'PERCENT'), '0.0%');
          assert.equal(window.formatMeasure(1.0, 'PERCENT'), '1.0%');
          assert.equal(window.formatMeasure(1.3, 'PERCENT'), '1.3%');
          assert.equal(window.formatMeasure(1.34, 'PERCENT'), '1.3%');
          assert.equal(window.formatMeasure(50.89, 'PERCENT'), '50.9%');
          assert.equal(window.formatMeasure(100.0, 'PERCENT'), '100.0%');
        });

        bdd.it('should format WORK_DUR', function () {
          assert.equal(window.formatMeasure(0, 'WORK_DUR'), '0');
          assert.equal(window.formatMeasure(5 * ONE_DAY, 'WORK_DUR'), '5d');
          assert.equal(window.formatMeasure(2 * ONE_HOUR, 'WORK_DUR'), '2h');
          assert.equal(window.formatMeasure(ONE_MINUTE, 'WORK_DUR'), '1min');
          assert.equal(window.formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR, 'WORK_DUR'), '5d 2h');
          assert.equal(window.formatMeasure(2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR'), '2h 1min');
          assert.equal(window.formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR'), '5d 2h');
          assert.equal(window.formatMeasure(15 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR'), '15d');
          assert.equal(window.formatMeasure(-5 * ONE_DAY, 'WORK_DUR'), '-5d');
          assert.equal(window.formatMeasure(-2 * ONE_HOUR, 'WORK_DUR'), '-2h');
          assert.equal(window.formatMeasure(-1 * ONE_MINUTE, 'WORK_DUR'), '-1min');
        });

        bdd.it('should format SHORT_WORK_DUR', function () {
          assert.equal(window.formatMeasure(0, 'SHORT_WORK_DUR'), '0');
          assert.equal(window.formatMeasure(5 * ONE_DAY, 'SHORT_WORK_DUR'), '5d');
          assert.equal(window.formatMeasure(2 * ONE_HOUR, 'SHORT_WORK_DUR'), '2h');
          assert.equal(window.formatMeasure(ONE_MINUTE, 'SHORT_WORK_DUR'), '1min');
          assert.equal(window.formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR, 'SHORT_WORK_DUR'), '~ 5d');
          assert.equal(window.formatMeasure(2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR'), '~ 2h');
          assert.equal(window.formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR'), '~ 5d');
          assert.equal(window.formatMeasure(15 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR'), '~ 15d');
          assert.equal(window.formatMeasure(7 * ONE_MINUTE, 'SHORT_WORK_DUR'), '7min');
          assert.equal(window.formatMeasure(-5 * ONE_DAY, 'SHORT_WORK_DUR'), '-5d');
          assert.equal(window.formatMeasure(-2 * ONE_HOUR, 'SHORT_WORK_DUR'), '-2h');
          assert.equal(window.formatMeasure(-1 * ONE_MINUTE, 'SHORT_WORK_DUR'), '-1min');

          assert.equal(window.formatMeasure(1529 * ONE_DAY, 'SHORT_WORK_DUR'), '1.5kd');
          assert.equal(window.formatMeasure(1234567 * ONE_DAY, 'SHORT_WORK_DUR'), '1md');
          assert.equal(window.formatMeasure(1234567 * ONE_DAY + 2 * ONE_HOUR, 'SHORT_WORK_DUR'), '1md');
        });

        bdd.it('should format RATING', function () {
          assert.equal(window.formatMeasure(1, 'RATING'), 'A');
          assert.equal(window.formatMeasure(2, 'RATING'), 'B');
          assert.equal(window.formatMeasure(3, 'RATING'), 'C');
          assert.equal(window.formatMeasure(4, 'RATING'), 'D');
          assert.equal(window.formatMeasure(5, 'RATING'), 'E');
        });

        bdd.it('should not format unknown type', function () {
          assert.equal(window.formatMeasure('random value', 'RANDOM_TYPE'), 'random value');
        });

        bdd.it('should not fail without parameters', function () {
          assert.isNull(window.formatMeasure());
        });
      });

      bdd.describe('#formatMeasureVariation()', function () {
        bdd.it('should format INT', function () {
          assert.equal(window.formatMeasureVariation(0, 'INT'), '0');
          assert.equal(window.formatMeasureVariation(1, 'INT'), '+1');
          assert.equal(window.formatMeasureVariation(-1, 'INT'), '-1');
          assert.equal(window.formatMeasureVariation(1529, 'INT'), '+1,529');
          assert.equal(window.formatMeasureVariation(-1529, 'INT'), '-1,529');
        });

        bdd.it('should format SHORT_INT', function () {
          assert.equal(window.formatMeasureVariation(0, 'SHORT_INT'), '0');
          assert.equal(window.formatMeasureVariation(1, 'SHORT_INT'), '+1');
          assert.equal(window.formatMeasureVariation(-1, 'SHORT_INT'), '-1');
          assert.equal(window.formatMeasureVariation(1529, 'SHORT_INT'), '+1.5k');
          assert.equal(window.formatMeasureVariation(-1529, 'SHORT_INT'), '-1.5k');
          assert.equal(window.formatMeasureVariation(10678, 'SHORT_INT'), '+11k');
          assert.equal(window.formatMeasureVariation(-10678, 'SHORT_INT'), '-11k');
        });

        bdd.it('should format FLOAT', function () {
          assert.equal(window.formatMeasureVariation(0.0, 'FLOAT'), '0');
          assert.equal(window.formatMeasureVariation(1.0, 'FLOAT'), '+1.0');
          assert.equal(window.formatMeasureVariation(-1.0, 'FLOAT'), '-1.0');
          assert.equal(window.formatMeasureVariation(50.89, 'FLOAT'), '+50.9');
          assert.equal(window.formatMeasureVariation(-50.89, 'FLOAT'), '-50.9');
        });

        bdd.it('should format PERCENT', function () {
          assert.equal(window.formatMeasureVariation(0.0, 'PERCENT'), '0%');
          assert.equal(window.formatMeasureVariation(1.0, 'PERCENT'), '+1.0%');
          assert.equal(window.formatMeasureVariation(-1.0, 'PERCENT'), '-1.0%');
          assert.equal(window.formatMeasureVariation(50.89, 'PERCENT'), '+50.9%');
          assert.equal(window.formatMeasureVariation(-50.89, 'PERCENT'), '-50.9%');
        });

        bdd.it('should format WORK_DUR', function () {
          assert.equal(window.formatMeasureVariation(0, 'WORK_DUR'), '0');
          assert.equal(window.formatMeasureVariation(5 * ONE_DAY, 'WORK_DUR'), '+5d');
          assert.equal(window.formatMeasureVariation(2 * ONE_HOUR, 'WORK_DUR'), '+2h');
          assert.equal(window.formatMeasureVariation(ONE_MINUTE, 'WORK_DUR'), '+1min');
          assert.equal(window.formatMeasureVariation(-5 * ONE_DAY, 'WORK_DUR'), '-5d');
          assert.equal(window.formatMeasureVariation(-2 * ONE_HOUR, 'WORK_DUR'), '-2h');
          assert.equal(window.formatMeasureVariation(-1 * ONE_MINUTE, 'WORK_DUR'), '-1min');
        });

        bdd.it('should not format unknown type', function () {
          assert.equal(window.formatMeasureVariation('random value', 'RANDOM_TYPE'), 'random value');
        });

        bdd.it('should not fail without parameters', function () {
          assert.isNull(window.formatMeasureVariation());
        });
      });
    });

    bdd.describe('Severity Comparators', function () {
      bdd.describe('#severityComparator', function () {
        bdd.it('should have correct order', function () {
          assert.equal(window.severityComparator('BLOCKER'), 0);
          assert.equal(window.severityComparator('CRITICAL'), 1);
          assert.equal(window.severityComparator('MAJOR'), 2);
          assert.equal(window.severityComparator('MINOR'), 3);
          assert.equal(window.severityComparator('INFO'), 4);
        });
      });

      bdd.describe('#severityColumnsComparator', function () {
        bdd.it('should have correct order', function () {
          assert.equal(window.severityColumnsComparator('BLOCKER'), 0);
          assert.equal(window.severityColumnsComparator('CRITICAL'), 2);
          assert.equal(window.severityColumnsComparator('MAJOR'), 4);
          assert.equal(window.severityColumnsComparator('MINOR'), 1);
          assert.equal(window.severityColumnsComparator('INFO'), 3);
        });
      });
    });
  });
});
