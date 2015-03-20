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
    testName = lib.testName('Workspace');


lib.initMessages();
lib.changeWorkingDirectory('workspace');
lib.configureCasper();


casper.test.begin(testName('Open From Component Viewer'), 8, function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.localStorage.removeItem('sonarqube-workspace');
          require(['/js/source-viewer/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        casper.click('.js-actions');
        casper.waitForSelector('.js-workspace', function () {
          casper.click('.js-workspace');
        });
      })

      .then(function () {
        casper.waitForSelector('.workspace-viewer .source-line');
      })

      .then(function () {
        test.assertElementCount('.workspace-nav-item', 1);
        test.assertSelectorContains('.workspace-nav-item', 'Cache.java');
        test.assertExists('.workspace-nav-item .icon-qualifier-fil');

        test.assertSelectorContains('.workspace-viewer-name', 'Cache.java');
        test.assertExists('.workspace-viewer-name .icon-qualifier-fil');

        test.assertExists('.workspace-viewer .source-viewer');
        test.assertElementCount('.workspace-viewer .source-line', 11);
      })

      .then(function () {
        casper.click('.workspace-viewer .js-close');
        test.assertDoesntExist('.workspace-viewer');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Load From Local Storage'), 7, function (test) {
  casper
      .start(lib.buildUrl('nav'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.localStorage.setItem('sonarqube-workspace',
              '[{"uuid":"12345","type":"component","name":"Cache.java","q":"FIL"}]');
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          require(['/js/nav/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.workspace-nav-item');
      })

      .then(function () {
        test.assertElementCount('.workspace-nav-item', 1);
        test.assertSelectorContains('.workspace-nav-item', 'Cache.java');
        test.assertExists('.workspace-nav-item .icon-qualifier-fil');
      })

      .then(function () {
        casper.click('.workspace-nav-item');
        casper.waitForSelector('.workspace-viewer .source-line');
      })

      .then(function () {
        test.assertSelectorContains('.workspace-viewer-name', 'Cache.java');
        test.assertExists('.workspace-viewer-name .icon-qualifier-fil');

        test.assertExists('.workspace-viewer .source-viewer');
        test.assertElementCount('.workspace-viewer .source-line', 11);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Close From Nav'), 2, function (test) {
  casper
      .start(lib.buildUrl('nav'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.localStorage.setItem('sonarqube-workspace',
              '[{"uuid":"12345","type":"component","name":"Cache.java","q":"FIL"}]');
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          require(['/js/nav/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.workspace-nav-item');
      })

      .then(function () {
        casper.click('.workspace-nav-item');
        casper.waitForSelector('.workspace-viewer .source-line');
      })

      .then(function () {
        casper.click('.workspace-nav-item .js-close');
        test.assertDoesntExist('.workspace-nav-item');
        test.assertDoesntExist('.workspace-viewer');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Minimize'), 2, function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.localStorage.removeItem('sonarqube-workspace');
          require(['/js/source-viewer/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        casper.click('.js-actions');
        casper.waitForSelector('.js-workspace', function () {
          casper.click('.js-workspace');
        });
      })

      .then(function () {
        casper.waitForSelector('.workspace-viewer .source-line');
      })

      .then(function () {
        casper.click('.workspace-viewer .js-minimize');
        test.assertDoesntExist('.workspace-viewer');
        test.assertElementCount('.workspace-nav-item', 1);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Full Screen'), 8, function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.localStorage.removeItem('sonarqube-workspace');
          require(['/js/source-viewer/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        casper.click('.js-actions');
        casper.waitForSelector('.js-workspace', function () {
          casper.click('.js-workspace');
        });
      })

      .then(function () {
        casper.waitForSelector('.workspace-viewer .source-line');
      })

      .then(function () {
        test.assertVisible('.workspace-viewer .js-full-screen');
        test.assertNotVisible('.workspace-viewer .js-normal-size');

        casper.click('.workspace-viewer .js-full-screen');
        test.assertExists('.workspace-viewer.workspace-viewer-full-screen');
        test.assertNotVisible('.workspace-viewer .js-full-screen');
        test.assertVisible('.workspace-viewer .js-normal-size');

        casper.click('.workspace-viewer .js-normal-size');
        test.assertDoesntExist('.workspace-viewer.workspace-viewer-full-screen');
        test.assertVisible('.workspace-viewer .js-full-screen');
        test.assertNotVisible('.workspace-viewer .js-normal-size');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
