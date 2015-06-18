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
    testName = lib.testName('Handlebars Helpers');

lib.initMessages();
lib.configureCasper();


function helper (name) {
  var args = Array.prototype.slice.call(arguments, 1);
  return casper.evaluate(function (name, args) {
    args.push({});
    return Handlebars.helpers[name].apply(this, args);
  }, name, args);
}

function blockHelper (name, fn, inverse) {
  var args = Array.prototype.slice.call(arguments, 3);
  return casper.evaluate(function (name, args, fn, inverse) {
    args.push({ fn: fn, inverse: inverse });
    return Handlebars.helpers[name].apply(this, args);
  }, name, args, fn, inverse);
}

function emptyFn () {
  // do nothing
}

function returnX () {
  return 'x';
}

function returnY () {
  return 'y';
}


casper.test.begin(testName(), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        test.assertEqual(helper('capitalize', ''), '');
        test.assertEqual(helper('capitalize', 'a'), 'A');
        test.assertEqual(helper('capitalize', 'abcd'), 'Abcd');
      })

      .then(function () {
        test.assertEqual(blockHelper('gt', returnX, returnY, 1, 2), 'y');
        test.assertEqual(blockHelper('gt', returnX, returnY, 2, 1), 'x');
        test.assertEqual(blockHelper('gt', returnX, returnY, 1, 1), 'y');
      })

      .then(function () {
        test.assertEqual(blockHelper('lt', returnX, returnY, 1, 2), 'x');
        test.assertEqual(blockHelper('lt', returnX, returnY, 2, 1), 'y');
        test.assertEqual(blockHelper('lt', returnX, returnY, 1, 1), 'y');
      })

      .then(function () {
        test.assertEqual(blockHelper('ifLength', returnX, returnY, null, 7), 'y');
        test.assertEqual(blockHelper('ifLength', returnX, returnY, [], 0), 'x');
        test.assertEqual(blockHelper('ifLength', returnX, returnY, [], 1), 'y');
        test.assertEqual(blockHelper('ifLength', returnX, returnY, ['a'], 1), 'x');
        test.assertEqual(blockHelper('ifLength', returnX, returnY, ['a'], 2), 'y');
      })

      .then(function () {
        test.assertEqual(helper('numberShort', 0), '0');
        test.assertEqual(helper('numberShort', 1), '1');
        test.assertEqual(helper('numberShort', 999), '999');
        test.assertEqual(helper('numberShort', 1000), '1,000');
        test.assertEqual(helper('numberShort', 1529), '1,529');
        test.assertEqual(helper('numberShort', 10000), '10k');
        test.assertEqual(helper('numberShort', 10678), '10.7k');
        test.assertEqual(helper('numberShort', 1234567890), '1b');
      })

      .then(function () {
        test.assertEqual(helper('limitString', ''), '');
        test.assertEqual(helper('limitString', 'abcd'), 'abcd');
        test.assertEqual(helper('limitString', 'aaaa aaaa aaaa aaaa aaaa aaaa '), 'aaaa aaaa aaaa aaaa aaaa aaaa ');
        test.assertEqual(helper('limitString', 'aaaa aaaa aaaa aaaa aaaa aaaa a'), 'aaaa aaaa aaaa aaaa aaaa aaaa ...');
        test.assertEqual(helper('limitString', 'aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa '),
            'aaaa aaaa aaaa aaaa aaaa aaaa ...');
      })

      .then(function () {
        test.assertEqual(helper('withSign', 0), '+0');
        test.assertEqual(helper('withSign', 1), '+1');
        test.assertEqual(helper('withSign', 2), '+2');
        test.assertEqual(helper('withSign', -1), '-1');
        test.assertEqual(helper('withSign', -2), '-2');
      })

      .then(function () {
        test.assertEqual(helper('formatMeasure', 50.89, 'PERCENT'), '50.9%');
        test.assertEqual(helper('formatMeasureVariation', 50.89, 'PERCENT'), '+50.9%');
      })

      .then(function () {
        test.assertEqual(blockHelper('repeat', returnX, emptyFn, 3), 'xxx');
      })

      .then(function () {
        test.assertEqual(blockHelper('eqComponents', returnX, returnY, null, null), 'x');
        test.assertEqual(blockHelper('eqComponents', returnX, returnY, {}, null), 'x');
        test.assertEqual(blockHelper('eqComponents', returnX, returnY, null, {}), 'x');
        test.assertEqual(blockHelper('eqComponents', returnX, returnY,
            { project: 'A' }, { project: 'A' }), 'x');
        test.assertEqual(blockHelper('eqComponents', returnX, returnY,
            { project: 'A' }, { project: 'B' }), 'y');
        test.assertEqual(blockHelper('eqComponents', returnX, returnY,
            { project: 'A', subProject: 'D' }, { project: 'A', subProject: 'D' }), 'x');
        test.assertEqual(blockHelper('eqComponents', returnX, returnY,
            { project: 'A', subProject: 'D' }, { project: 'A', subProject: 'E' }), 'y');
        test.assertEqual(blockHelper('eqComponents', returnX, returnY,
            { project: 'A', subProject: 'D' }, { project: 'B', subProject: 'D' }), 'y');
        test.assertEqual(blockHelper('eqComponents', returnX, returnY,
            { project: 'A', subProject: 'D' }, { project: 'B', subProject: 'E' }), 'y');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
