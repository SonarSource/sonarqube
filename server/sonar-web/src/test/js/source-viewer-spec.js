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
/* global casper:false */


var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-spec');
lib.configureCasper();


casper.test.begin(testName('Base'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/source-viewer/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line', function () {
          // Check header elements
          test.assertExists('.source-viewer-header');
          test.assertSelectorContains('.source-viewer-header', 'SonarQube');
          test.assertSelectorContains('.source-viewer-header', 'SonarQube :: Batch');
          test.assertSelectorContains('.source-viewer-header', 'src/main/java/org/sonar/batch/index/Cache.java');
          test.assertExists('.source-viewer-header .js-favorite');
          test.assertExists('.source-viewer-header-actions');

          // Check main measures
          // FIXME enable lines check
          //test.assertSelectorContains('.source-viewer-header-measure', '379');
          test.assertSelectorContains('.source-viewer-header-measure', 'A');
          test.assertSelectorContains('.source-viewer-header-measure', '2h 10min');
          test.assertSelectorContains('.source-viewer-header-measure', '6');
          test.assertSelectorContains('.source-viewer-header-measure', '74.3%');
          test.assertSelectorContains('.source-viewer-header-measure', '5.8%');

          // Check source
          // FIXME enable source lines count check
          //test.assertElementCount('.source-line', 518);
          test.assertSelectorContains('.source-viewer', 'public class Cache');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Decoration'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/source-viewer/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        // Check issues decoration
        test.assertElementCount('.has-issues', 6);
      })

      .then(function () {
        // Check coverage decoration
        test.assertElementCount('.source-line-covered', 142);
        test.assertElementCount('.source-line-uncovered', 50);
        test.assertElementCount('.source-line-partially-covered', 2);
      })

      .then(function () {
        // Check duplications decoration
        test.assertElementCount('.source-line-duplicated', 30);
      })

      .then(function () {
        // Check scm decoration
        test.assertElementCount('.source-line-scm-inner', 186);
        test.assertExists('.source-line-scm-inner[data-author="simon.brandhof@gmail.com"]');
        test.assertExists('.source-line-scm-inner[data-author="julien.henry@sonarsource.com"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Test File'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/components/app', 'tests/app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'tests/lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/source-viewer/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        test.assertSelectorContains('.source-viewer-header-measure', '6');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
