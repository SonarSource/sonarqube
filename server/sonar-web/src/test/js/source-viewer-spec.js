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


casper.test.begin(testName('Base'), 14, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues.json', { data: { componentUuids: 'uuid' } });
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
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
          test.assertSelectorContains('.source-viewer-header-measure', '378');
          test.assertSelectorContains('.source-viewer-header-measure', 'A');
          test.assertSelectorContains('.source-viewer-header-measure', '2h 10min');
          test.assertSelectorContains('.source-viewer-header-measure', '6');
          test.assertSelectorContains('.source-viewer-header-measure', '74.3%');
          test.assertSelectorContains('.source-viewer-header-measure', '5.8%');

          // Check source
          test.assertElementCount('.source-line', 519);
          test.assertSelectorContains('.source-viewer', 'import com.google.common.collect.Sets');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Decoration'), 8, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues.json', { data: { componentUuids: 'uuid' } });
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
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


casper.test.begin(testName('Test File'), 1, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/components/app', 'tests/app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'tests/lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
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


casper.test.begin(testName('Highlight Usages'), 6, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues.json', { data: { componentUuids: 'uuid' } });
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        test.assertElementCount('.sym-9999', 3);
        test.assertElementCount('.sym.highlighted', 0);

        casper.click('.sym-9999');
        test.assertElementCount('.sym-9999.highlighted', 3);
        test.assertElementCount('.sym.highlighted', 3);

        casper.click('.sym-9999');
        test.assertElementCount('.sym-9999.highlighted', 0);
        test.assertElementCount('.sym.highlighted', 0);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Line Permalink'), 1, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues.json', { data: { componentUuids: 'uuid' } });
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        casper.click('.source-line-number[data-line-number="3"]');
        casper.waitForSelector('.js-get-permalink');
      })

      .then(function () {
        casper.click('.js-get-permalink');

        // TODO check raw url
        test.assert(true);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Link to Raw'), 1, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues.json', { data: { componentUuids: 'uuid' } });
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        casper.click('.js-actions');
        casper.waitForSelector('.js-raw-source');
      })

      .then(function () {
        casper.click('.js-raw-source');

        // TODO check raw url
        test.assert(true);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Details'), 15, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues-details.json', { data: { componentUuids: 'uuid' } });
        lib.mockRequestFromFile('/api/metrics/search', 'metrics.json');
        lib.mockRequestFromFile('/api/resources', 'measures.json',
            { data: { resource: 'org.codehaus.sonar:sonar-batch:src/main/java/org/sonar/batch/index/Cache.java' } });
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        casper.click('.js-actions');
        casper.waitForSelector('.js-measures');
      })

      .then(function () {
        casper.click('.js-measures');
        casper.waitForSelector('.measure[data-metric="function_complexity"]');
      })

      .then(function () {
        test.assertSelectorContains('.measure[data-metric="lines"]', '92');
        test.assertSelectorContains('.measure[data-metric="ncloc"]', '57');
        test.assertSelectorContains('.measure[data-metric="comment_lines"]', '19.7% / 14');
        test.assertSelectorContains('.measure[data-metric="complexity"]', '22');
        test.assertSelectorContains('.measure[data-metric="function_complexity"]', '4.9');

        test.assertSelectorContains('.measure[data-metric="sqale_rating"]', 'A');
        test.assertSelectorContains('.measure[data-metric="sqale_index"]', '0');
        test.assertSelectorContains('.measure[data-metric="violations"]', '6');

        test.assertSelectorContains('.measure[data-metric="coverage"]', '11.3%');
        test.assertSelectorContains('.measure[data-metric="lines_to_cover"]', '6/38');
        test.assertSelectorContains('.measure[data-metric="conditions_to_cover"]', '6/38');
        test.assertSelectorContains('.measure[data-metric="it_lines_to_cover"]', '31/37');
        test.assertSelectorContains('.measure[data-metric="it_conditions_to_cover"]', '30/35');
      })

      .then(function () {
        test.assertNotVisible('.measure[data-metric="file_complexity"]');
        casper.click('.js-show-all-measures');
        test.assertVisible('.measure[data-metric="file_complexity"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Details', 'Tests'), 41, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app-test.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues-details.json', { data: { componentUuids: 'uuid' } });
        lib.mockRequestFromFile('/api/metrics/search', 'metrics.json');
        lib.mockRequestFromFile('/api/resources', 'measures-test.json',
            { data: { resource: 'org.codehaus.sonar:sonar-batch:src/main/java/org/sonar/batch/index/Cache.java' } });
        lib.mockRequestFromFile('/api/tests/list', 'tests.json', { data: { testFileUuid: 'uuid' } });
        lib.mockRequestFromFile('/api/tests/covered_files', 'covered-files.json',
            { data: { testUuid: 'test-uuid-1' } });
        lib.mockRequestFromFile('/api/tests/covered_files', 'covered-files.json',
            { data: { testUuid: 'test-uuid-3' } });
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        casper.click('.js-actions');
        casper.waitForSelector('.js-measures');
      })

      .then(function () {
        casper.click('.js-measures');
        casper.waitForSelector('.measure[data-metric="tests"]');
      })

      .then(function () {
        test.assertSelectorContains('.measure[data-metric="tests"]', '1');
        test.assertSelectorContains('.measure[data-metric="test_execution_time"]', '15 ms');

        casper.waitForSelector('.source-viewer-test-name');
      })

      .then(function () {
        test.assertElementCount('.source-viewer-test-name', 5);

        test.assertElementCount('.source-viewer-test-status .icon-test-status-ok', 2);
        test.assertElementCount('.source-viewer-test-status .icon-test-status-failure', 1);
        test.assertElementCount('.source-viewer-test-status .icon-test-status-error', 1);
        test.assertElementCount('.source-viewer-test-status .icon-test-status-skipped', 1);
      })

      .then(function () {
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(2)', 'test4');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(3)', 'test3');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(4)', 'test1');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(5)', 'test2');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(6)', 'test5');

        casper.click('.js-sort-tests-by-status');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(2)', 'test5');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(3)', 'test2');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(4)', 'test1');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(5)', 'test3');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(6)', 'test4');
        casper.click('.js-sort-tests-by-status');

        casper.click('.js-sort-tests-by-duration');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(2)', 'test4');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(3)', 'test2');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(4)', 'test3');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(5)', 'test1');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(6)', 'test5');

        casper.click('.js-sort-tests-by-duration');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(2)', 'test5');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(3)', 'test1');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(4)', 'test3');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(5)', 'test2');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(6)', 'test4');

        casper.click('.js-sort-tests-by-name');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(2)', 'test5');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(3)', 'test4');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(4)', 'test3');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(5)', 'test2');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(6)', 'test1');

        casper.click('.js-sort-tests-by-name');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(2)', 'test1');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(3)', 'test2');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(4)', 'test3');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(5)', 'test4');
        test.assertSelectorContains('.source-viewer-tests-list tr:nth-child(6)', 'test5');
      })

      .then(function () {
        casper.click('.js-show-test[data-id="test-uuid-1"]');
        casper.waitForText('src/main/java/com/sonar/CoveredFile1.java');
      })

      .then(function () {
        test.assertSelectorContains('.js-selected-test', 'src/main/java/com/sonar/CoveredFile1.java');
        test.assertSelectorContains('.js-selected-test', '2');
      })

      .then(function () {
        casper.click('.js-show-test[data-id="test-uuid-3"]');
        casper.waitForText('Failure Message');
      })

      .then(function () {
        test.assertSelectorContains('.js-selected-test', 'Failure Message');
        test.assertSelectorContains('.js-selected-test', 'Failure Stacktrace');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
