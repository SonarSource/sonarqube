var lib = require('../lib'),
    testName = lib.testName('Component Viewer', 'Duplications');

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
        lib.mockRequestFromFile('/api/duplications/show', 'duplications.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-header-tab-duplications');
        casper.waitForSelector('.duplication-exists', function () {
          test.assertExists('.js-filter-duplications.active');
          test.assertElementCount('.component-viewer-source .row', 39);
        });
      })

      .then(function () {
        casper.click('.js-filter-duplications');
        test.assertElementCount('.component-viewer-source .row', 520);
      })

      .run(function () {
        test.done();
      });
});




casper.test.begin(testName('Cross-Project'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/duplications/show', 'cross-project-duplications.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-header-tab-duplications');
        casper.waitForSelector('.js-filter-duplications', function () {
          casper.click('.js-filter-duplications');
          casper.waitForSelector('.duplication-exists', function () {
            casper.click('.duplication-exists');
            casper.waitForSelector('.bubble-popup', function () {
              test.assertSelectorContains('.bubble-popup', 'JavaScript');
              test.assertSelectorContains('.bubble-popup', 'JavaScript :: Sonar Plugin');
              test.assertExists('a[data-key="org.codehaus.sonar-plugins.javascript:sonar-javascript-plugin:src/main/java/org/sonar/plugins/javascript/core/JavaScript.java"]');
              test.assertSelectorContains('.bubble-popup', 'src/main/java/org/sonar/plugins/javascript/core/JavaScript.java');
              test.assertSelectorContains('.bubble-popup', '455'); // Line from
              test.assertSelectorContains('.bubble-popup', '470'); // Line to
            });
          });
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('In Deleted Files'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/duplications/show', 'duplications-in-deleted-files.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-toggle-duplications');
        casper.waitForSelector('.duplication-exists', function () {
          test.assertExists('.js-duplications-in-deleted-files');
        });
      })

      .run(function () {
        test.done();
      });
});

