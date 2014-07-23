var lib = require('../lib');


lib.initMessages();
lib.changeWorkingDirectory('component-viewer-spec');


casper.test.begin('Component Viewer Base Tests', function (test) {

  casper.options.onResourceRequested = function (instance, requestData) {
    if (requestData.url.indexOf('require') >= 0) {
      lib.mockRequest('../api/l10n/index', '{}');
      lib.mockRequestFromFile('../api/components/app', 'app.json');
      lib.mockRequestFromFile('../api/sources/show', 'source.json');
    }
  };


  casper.start(lib.buildUrl('component-viewer.html#component=component'), function () {
    lib.setDefaultViewport();
  });

  casper.wait(1000, function () {

    // Check header elements
    test.assertElementCount('.component-viewer-header', 1);
    test.assertSelectorContains('.component-viewer-header-component-project', 'SonarQube');
    test.assertSelectorContains('.component-viewer-header-component-project', 'SonarQube :: Batch');
    test.assertSelectorContains('.component-viewer-header-component-name',
        'src/main/java/org/sonar/batch/index/Cache.java');
    test.assertElementCount('.component-viewer-header-favorite', 1);
    test.assertElementCount('.component-viewer-header-actions', 1);

    // Check main measures
    test.assertSelectorContains('.js-header-tab-basic', '379');
    test.assertSelectorContains('.js-header-tab-issues', 'A');
    test.assertSelectorContains('.js-header-tab-issues', '3h 30min');
    test.assertSelectorContains('.js-header-tab-issues', '6');
    test.assertSelectorContains('.js-header-tab-coverage', '74.3%');
    test.assertElementCount('.js-header-tab-scm', 1);

    // Check source
    test.assertElementCount('.component-viewer-source .row', 520);
    test.assertSelectorContains('.component-viewer-source', 'public class Cache');
  });

  casper.run(function () {
    test.done();
  });
});
