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
var lib = require('../lib'),
    testName = lib.testName('Design');

lib.initMessages();
lib.changeWorkingDirectory('design-spec');
lib.configureCasper();


casper.test.begin(testName('Base'), function suite(test) {
  casper
      .start(lib.buildUrl('design'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/dependencies', 'dependencies.json');
      })

      .then(function () {
        casper.waitWhileSelector('.spinner');
      })

      .then(function () {
        test.assertSelectorContains('.dsm-body', 'src/test/java/com/maif/sonar/cobol/metrics');
        test.assertSelectorContains('.dsm-body', 'src/test/java/com/maif/sonar/cobol/repository');
        test.assertElementCount('.dsm-body-cell-dependency', 12);
        test.assertElementCount('.dsm-body-cell-cycle', 1);
        test.assertSelectorContains('.dsm-body-cell-cycle', '6');
      })

      .then(function () {
        casper.mouse.doubleclick('.dsm-body-cell-cycle');
        casper.waitForSelector('.spinner', function () {
          casper.waitWhileSelector('.spinner');
        })
      })

      .then(function () {
        test.assertElementCount('.dsm-info tr', 7);
        test.assertSelectorContains('.dsm-info', 'src/main/java/com/maif/sonar/cobol/api/MaifCobolMeasureProvider.java');
        test.assertSelectorContains('.dsm-info', 'src/main/java/com/maif/sonar/cobol/metrics/BusinessRuleCounter.java ');
        test.assertSelectorContains('.dsm-info', 'src/main/java/com/maif/sonar/cobol/metrics/TableMetricsVisitor.java ');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
