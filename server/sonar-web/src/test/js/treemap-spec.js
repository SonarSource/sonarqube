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
var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('treemap-spec');
lib.configureCasper();


casper.test.begin('Treemap', function (test) {
  var treemapData = JSON.parse(fs.read('treemap.json'));

  casper.start(lib.buildUrl('base'), function () {
    lib.mockRequestFromFile('/api/resources/index', 'treemap-resources.json');

    casper.evaluate(function (treemapData) {
      var widget = new SonarWidgets.Treemap();
      widget
          .metrics(treemapData.metrics)
          .metricsPriority(['coverage', 'ncloc'])
          .components(treemapData.components)
          .options({
            heightInPercents: 55,
            maxItems: 30,
            maxItemsReachedMessage: '',
            baseUrl: '/dashboard/index/',
            noData: '',
            resource: ''
          })
          .render('#content');
    }, treemapData);
  });

  casper
      .then(function () {
        casper.waitWhileSelector('.spinner', function() {
          test.assertElementCount('.treemap-cell', 30);
          test.assertSelectorHasText('.treemap-cell', 'SonarQube');
          test.assertMatch(casper.getElementAttribute('.treemap-link', 'href'), /dashboard\/index/,
              'Treemap cells have links to dashboards');
        });
      })
      .then(function () {
        casper.evaluate(function () {
          var evt = document.createEvent('MouseEvents');
          evt.initMouseEvent('click', true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
          d3.select('.treemap-cell').node().dispatchEvent(evt);
        });
      })
      .then(function () {
        casper.wait(500, function () {
          test.assertSelectorHasText('.treemap-cell', 'Server');
          test.assertElementCount('.treemap-cell', 25);
        });
      })
      .then(function () {
        lib.sendCoverage();
      });

  casper.run(function() {
    test.done();
  });
});
