var lib = require('../lib'),
    testName = lib.testName('Component Viewer', 'Issues');

lib.initMessages();
lib.changeWorkingDirectory('component-viewer-spec');


casper.test.begin(testName('Filters'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-header-tab-issues');
        casper.waitForSelector('.js-filter-unresolved-issues');
      })

      .then(function () {
        casper.click('.js-filter-unresolved-issues');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 6);
        });
      })

      .then(function () {
        casper.click('.js-filter-open-issues');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 1);
        });
      })

      .then(function () {
        casper.click('.js-filter-fixed-issues');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 11);
        });
      })

      .then(function () {
        casper.click('.js-filter-false-positive-issues');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 1);
        });
      })

      .then(function () {
        casper.click('.js-filter-MAJOR-issues');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 1);
        });
      })

      .then(function () {
        casper.click('.js-filter-MINOR-issues');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 1);
        });
      })

      .then(function () {
        casper.click('.js-filter-INFO-issues');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 4);
        });
      })

      .then(function () {
        casper.click('.js-filter-rule[data-rule="common-java:DuplicatedBlocks"]');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 1);
          test.assertSelectorContains('.code-issue', '2 duplicated blocks of code.');
        });
      })

      .then(function () {
        casper.click('.js-filter-rule[data-rule="squid:S1192"]');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 1);
          test.assertSelectorContains('.code-issue', 'Define a constant instead of duplicating this literal');
        });
      })

      .then(function () {
        casper.click('.js-filter-rule[data-rule="squid:S1135"]');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 4);
          test.assertSelectorContains('.code-issue', 'Complete the task associated to this TODO comment');
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('On File Level'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-header-tab-issues');
        casper.waitForSelector('.js-filter-unresolved-issues');
      })

      .then(function () {
        casper.click('.js-filter-unresolved-issues');
        casper.waitForSelector('.code-issue');
      })

      .then(function () {
        test.assertVisible('.component-viewer-source .row[data-line-number="0"]');
        test.assertExists('.code-issue[data-issue-key="20002ec7-b647-44da-bdf5-4d9fbf4b7c58"]');
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Bulk Change Link Exists'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-header-tab-issues');
        casper.waitForSelector('.js-filter-unresolved-issues');
      })

      .then(function () {
        test.assertExists('.js-issues-bulk-change');
      })

      .run(function () {
        test.done();
      });
});
