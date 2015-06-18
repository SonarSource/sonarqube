/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/* globals casper: false */
var lib = require('../lib'),
    testName = lib.testName('Application');

lib.initMessages();
lib.configureCasper();


casper.test.begin(testName('collapsedDirFromPath() & fileFromPath()'), function (test) {

  function collapsedDirFromPath (path) {
    return casper.evaluate(function (path) {
      return window.collapsedDirFromPath(path);
    }, path);
  }

  function fileFromPath (path) {
    return casper.evaluate(function (path) {
      return window.fileFromPath(path);
    }, path);
  }

  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        test.assert(!collapsedDirFromPath(null));
        test.assertEqual(collapsedDirFromPath('/'), '/');
        test.assertEqual(collapsedDirFromPath('src/main/js/components/state.js'), 'src/main/js/components/');
        test.assertEqual(collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js'),
            'src/.../js/components/navigator/app/models/');
        test.assertEqual(collapsedDirFromPath('src/main/another/js/components/navigator/app/models/state.js'),
            'src/.../js/components/navigator/app/models/');
      })

      .then(function () {
        test.assert(!fileFromPath(null));
        test.assertEqual(fileFromPath('/'), '');
        test.assertEqual(fileFromPath('state.js'), 'state.js');
        test.assertEqual(fileFromPath('src/main/js/components/state.js'), 'state.js');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Format Measures'), function (test) {

  function formatMeasure (measure, type) {
    return casper.evaluate(function (measure, type) {
      return formatMeasure(measure, type);
    }, measure, type);
  }

  function formatMeasureVariation (measure, type) {
    return casper.evaluate(function (measure, type) {
      return formatMeasureVariation(measure, type);
    }, measure, type);
  }

  var HOURS_IN_DAY = 8,
      ONE_MINUTE = 1,
      ONE_HOUR = ONE_MINUTE * 60,
      ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        casper.evaluate(function (hoursInDay) {
          window.messages = {
            'work_duration.x_days': '{0}d',
            'work_duration.x_hours': '{0}h',
            'work_duration.x_minutes': '{0}min'
          };
          window.hoursInDay = hoursInDay;
        }, HOURS_IN_DAY);
      })

      .then(function () {
        test.assertEqual(formatMeasure(0, 'INT'), '0');
        test.assertEqual(formatMeasure(1, 'INT'), '1');
        test.assertEqual(formatMeasure(-5, 'INT'), '-5');
        test.assertEqual(formatMeasure(999, 'INT'), '999');
        test.assertEqual(formatMeasure(1000, 'INT'), '1,000');
        test.assertEqual(formatMeasure(1529, 'INT'), '1,529');
        test.assertEqual(formatMeasure(10000, 'INT'), '10,000');
        test.assertEqual(formatMeasure(1234567890, 'INT'), '1,234,567,890');

        test.assertEqual(formatMeasure(0, 'SHORT_INT'), '0');
        test.assertEqual(formatMeasure(1, 'SHORT_INT'), '1');
        test.assertEqual(formatMeasure(999, 'SHORT_INT'), '999');
        test.assertEqual(formatMeasure(1000, 'SHORT_INT'), '1k');
        test.assertEqual(formatMeasure(1529, 'SHORT_INT'), '1.5k');
        test.assertEqual(formatMeasure(10000, 'SHORT_INT'), '10k');
        test.assertEqual(formatMeasure(10678, 'SHORT_INT'), '11k');
        test.assertEqual(formatMeasure(1234567890, 'SHORT_INT'), '1b');

        test.assertEqual(formatMeasure(0.0, 'FLOAT'), '0.0');
        test.assertEqual(formatMeasure(1.0, 'FLOAT'), '1.0');
        test.assertEqual(formatMeasure(1.3, 'FLOAT'), '1.3');
        test.assertEqual(formatMeasure(1.34, 'FLOAT'), '1.3');
        test.assertEqual(formatMeasure(50.89, 'FLOAT'), '50.9');
        test.assertEqual(formatMeasure(100.0, 'FLOAT'), '100.0');
        test.assertEqual(formatMeasure(123.456, 'FLOAT'), '123.5');
        test.assertEqual(formatMeasure(123456.7, 'FLOAT'), '123,456.7');
        test.assertEqual(formatMeasure(1234567890.0, 'FLOAT'), '1,234,567,890.0');

        test.assertEqual(formatMeasure(0.0, 'PERCENT'), '0.0%');
        test.assertEqual(formatMeasure(1.0, 'PERCENT'), '1.0%');
        test.assertEqual(formatMeasure(1.3, 'PERCENT'), '1.3%');
        test.assertEqual(formatMeasure(1.34, 'PERCENT'), '1.3%');
        test.assertEqual(formatMeasure(50.89, 'PERCENT'), '50.9%');
        test.assertEqual(formatMeasure(100.0, 'PERCENT'), '100.0%');

        test.assertEqual(formatMeasure(0, 'WORK_DUR'), '0');
        test.assertEqual(formatMeasure(5 * ONE_DAY, 'WORK_DUR'), '5d');
        test.assertEqual(formatMeasure(2 * ONE_HOUR, 'WORK_DUR'), '2h');
        test.assertEqual(formatMeasure(ONE_MINUTE, 'WORK_DUR'), '1min');
        test.assertEqual(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR, 'WORK_DUR'), '5d 2h');
        test.assertEqual(formatMeasure(2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR'), '2h 1min');
        test.assertEqual(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR'), '5d 2h');
        test.assertEqual(formatMeasure(15 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR'), '15d');
        test.assertEqual(formatMeasure(-5 * ONE_DAY, 'WORK_DUR'), '-5d');
        test.assertEqual(formatMeasure(-2 * ONE_HOUR, 'WORK_DUR'), '-2h');
        test.assertEqual(formatMeasure(-1 * ONE_MINUTE, 'WORK_DUR'), '-1min');

        test.assertEqual(formatMeasure(1, 'RATING'), 'A');
        test.assertEqual(formatMeasure(2, 'RATING'), 'B');
        test.assertEqual(formatMeasure(3, 'RATING'), 'C');
        test.assertEqual(formatMeasure(4, 'RATING'), 'D');
        test.assertEqual(formatMeasure(5, 'RATING'), 'E');

        test.assertEqual(formatMeasure('random value', 'RANDOM_TYPE'), 'random value');

        test.assertEqual(formatMeasure(), '');
      })

      .then(function () {
        test.assertEqual(formatMeasureVariation(0, 'INT'), '0');
        test.assertEqual(formatMeasureVariation(1, 'INT'), '+1');
        test.assertEqual(formatMeasureVariation(-1, 'INT'), '-1');
        test.assertEqual(formatMeasureVariation(1529, 'INT'), '+1,529');
        test.assertEqual(formatMeasureVariation(-1529, 'INT'), '-1,529');

        test.assertEqual(formatMeasureVariation(0, 'SHORT_INT'), '0');
        test.assertEqual(formatMeasureVariation(1, 'SHORT_INT'), '+1');
        test.assertEqual(formatMeasureVariation(-1, 'SHORT_INT'), '-1');
        test.assertEqual(formatMeasureVariation(1529, 'SHORT_INT'), '+1.5k');
        test.assertEqual(formatMeasureVariation(-1529, 'SHORT_INT'), '-1.5k');
        test.assertEqual(formatMeasureVariation(10678, 'SHORT_INT'), '+11k');
        test.assertEqual(formatMeasureVariation(-10678, 'SHORT_INT'), '-11k');

        test.assertEqual(formatMeasureVariation(0.0, 'FLOAT'), '0');
        test.assertEqual(formatMeasureVariation(1.0, 'FLOAT'), '+1.0');
        test.assertEqual(formatMeasureVariation(-1.0, 'FLOAT'), '-1.0');
        test.assertEqual(formatMeasureVariation(50.89, 'FLOAT'), '+50.9');
        test.assertEqual(formatMeasureVariation(-50.89, 'FLOAT'), '-50.9');

        test.assertEqual(formatMeasureVariation(0.0, 'PERCENT'), '0%');
        test.assertEqual(formatMeasureVariation(1.0, 'PERCENT'), '+1.0%');
        test.assertEqual(formatMeasureVariation(-1.0, 'PERCENT'), '-1.0%');
        test.assertEqual(formatMeasureVariation(50.89, 'PERCENT'), '+50.9%');
        test.assertEqual(formatMeasureVariation(-50.89, 'PERCENT'), '-50.9%');

        test.assertEqual(formatMeasureVariation(0, 'WORK_DUR'), '0');
        test.assertEqual(formatMeasureVariation(5 * ONE_DAY, 'WORK_DUR'), '+5d');
        test.assertEqual(formatMeasureVariation(2 * ONE_HOUR, 'WORK_DUR'), '+2h');
        test.assertEqual(formatMeasureVariation(ONE_MINUTE, 'WORK_DUR'), '+1min');
        test.assertEqual(formatMeasureVariation(-5 * ONE_DAY, 'WORK_DUR'), '-5d');
        test.assertEqual(formatMeasureVariation(-2 * ONE_HOUR, 'WORK_DUR'), '-2h');
        test.assertEqual(formatMeasureVariation(-1 * ONE_MINUTE, 'WORK_DUR'), '-1min');

        test.assertEqual(formatMeasureVariation('random value', 'RANDOM_TYPE'), 'random value');

        test.assertEqual(formatMeasureVariation(), '');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Severity Comparators'), function (test) {

  function severityComparator (severity) {
    return casper.evaluate(function (severity) {
      return window.severityComparator(severity);
    }, severity);
  }

  function severityColumnsComparator (severity) {
    return casper.evaluate(function (severity) {
      return window.severityColumnsComparator(severity);
    }, severity);
  }

  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        test.assertEqual(severityComparator('BLOCKER'), 0);
        test.assertEqual(severityComparator('CRITICAL'), 1);
        test.assertEqual(severityComparator('MAJOR'), 2);
        test.assertEqual(severityComparator('MINOR'), 3);
        test.assertEqual(severityComparator('INFO'), 4);
      })

      .then(function () {
        test.assertEqual(severityColumnsComparator('BLOCKER'), 0);
        test.assertEqual(severityColumnsComparator('CRITICAL'), 2);
        test.assertEqual(severityColumnsComparator('MAJOR'), 4);
        test.assertEqual(severityColumnsComparator('MINOR'), 1);
        test.assertEqual(severityColumnsComparator('INFO'), 3);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
