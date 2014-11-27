var lib = require('../lib'),
    testName = lib.testName('Component Viewer', 'Select Tab by Metric');

lib.initMessages();
lib.changeWorkingDirectory('component-viewer-spec');


casper.test.begin(testName('sqale_index'), function (test) {
  casper
      .start(lib.buildUrl('file-dashboard'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
        casper.evaluate(function () {
          window.fileKey = 'some-key';
          window.metric = 'sqale_index';
        });
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .source-line');
      })

      .then(function () {
        casper.waitForSelector('.issue', function () {
          test.assertExists('.js-toggle-issues.active');
          test.assertExists('.component-viewer-header-expanded-bar.active');
          test.assertExists('.js-filter-unresolved-issues.active');
          test.assertElementCount('.component-viewer-source .source-line', 56);
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('minor_violations'), function (test) {
  casper
      .start(lib.buildUrl('file-dashboard'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
        casper.evaluate(function () {
          window.fileKey = 'some-key';
          window.metric = 'minor_violations';
        });
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .source-line');
      })

      .then(function () {
        casper.waitForSelector('.issue', function () {
          test.assertExists('.js-toggle-issues.active');
          test.assertExists('.component-viewer-header-expanded-bar.active');
          test.assertExists('.js-filter-MINOR-issues.active');
          test.assertElementCount('.component-viewer-source .source-line', 11);
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('line_coverage'), function (test) {
  casper
      .start(lib.buildUrl('file-dashboard'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/coverage/show', 'coverage.json');
        casper.evaluate(function () {
          window.fileKey = 'some-key';
          window.metric = 'line_coverage';
        });
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .source-line');
      })

      .then(function () {
        casper.waitForSelector('.source-line-covered', function () {
          test.assertExists('.js-toggle-coverage.active');
          test.assertExists('.component-viewer-header-expanded-bar.active');
          test.assertExists('.js-filter-lines-to-cover.active');
          test.assertElementCount('.component-viewer-source .source-line', 369);
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('duplicated_lines'), function (test) {
  casper
      .start(lib.buildUrl('file-dashboard'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/duplications/show', 'duplications.json');
        casper.evaluate(function () {
          window.fileKey = 'some-key';
          window.metric = 'duplicated_lines';
        });
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .source-line');
      })

      .then(function () {
        casper.waitForSelector('.source-line-duplicated', function () {
          test.assertExists('.js-toggle-duplications.active');
          test.assertExists('.component-viewer-header-expanded-bar.active');
          test.assertExists('.js-filter-duplications.active');
          test.assertElementCount('.component-viewer-source .source-line', 39);
        });
      })

      .run(function () {
        test.done();
      });
});
