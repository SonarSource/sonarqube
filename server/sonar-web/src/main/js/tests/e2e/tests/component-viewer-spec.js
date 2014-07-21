// Dump log messages
casper.on('remote.message', function(message) {
  this.echo('Log: '+ message, 'LOG');
});

// Dump uncaught errors
casper.on('page.error', function(msg, trace) {
  this.echo('Error: ' + msg, 'ERROR');
});

var fs = require('fs');
var utils = require('utils');

// Since Casper has control, the invoked script is deep in the argument stack
var currentFile = require('system').args[4];
var curFilePath = fs.absolute(currentFile).split(fs.separator);
if (curFilePath.length > 1) {
    curFilePath.pop(); // PhantomJS does not have an equivalent path.baseName()-like method
    curFilePath.push('component-viewer-spec');
    fs.changeWorkingDirectory(curFilePath.join(fs.separator));
}

casper.test.begin('Component Viewer Base Tests', function suite(test) {

  var appResponse = fs.read('app.json'),
      sourceResponse = fs.read('source.json');

  // Register mockjax requests on loading of requirejs
  casper.options.onResourceRequested = function(instance, requestData) {

    // Here, instance is the running instance of casperjs
    if (requestData.url.indexOf('require') >= 0) {
      instance.page.includeJs('../js/third-party/jquery.mockjax.js', function () {
        // Inject response values to page scope
        instance.page.evaluate(function (appResponse, sourceResponse) {
          jQuery.mockjaxSettings.contentType = 'text/json';
          jQuery.mockjaxSettings.responseTime = 250;

          jQuery.mockjax({ url: '../api/l10n/index', responseText: '{}'});
          jQuery.mockjax({ url: '../api/components/app', responseText: appResponse});
          jQuery.mockjax({ url: '../api/sources/show', responseText: sourceResponse});
        }, appResponse, sourceResponse);
      });
    }
  };

  // See API at http://docs.casperjs.org/en/latest/modules/index.html

  casper.start('http://localhost:3000/pages/component-viewer.html#component=component', function () {
    casper.viewport(1200, 800);
  });

  casper.wait(1000, function() {

    // Check header elements
    test.assertElementCount('.component-viewer-header', 1);
    test.assertSelectorHasText('.component-viewer-header-component-project', 'SonarQube');
    test.assertSelectorHasText('.component-viewer-header-component-project', 'SonarQube :: Batch');
    test.assertSelectorHasText('.component-viewer-header-component-name',
        'src/main/java/org/sonar/batch/index/Cache.java');
    test.assertElementCount('.component-viewer-header-favorite', 1);
    test.assertElementCount('.component-viewer-header-actions', 1);

    // Check main measures
    test.assertSelectorHasText('.js-header-tab-basic', '379');
    test.assertSelectorHasText('.js-header-tab-issues', 'A');
    test.assertSelectorHasText('.js-header-tab-issues', '3h 30min');
    test.assertSelectorHasText('.js-header-tab-issues', '6');
    test.assertSelectorHasText('.js-header-tab-coverage', '74.3%');
    test.assertElementCount('.js-header-tab-scm', 1);

    // Check source
    test.assertElementCount('.component-viewer-source .row', 520);
    test.assertSelectorHasText('.component-viewer-source', 'public class Cache');
  });

  casper.run(function() {
    test.done();
  });
});
