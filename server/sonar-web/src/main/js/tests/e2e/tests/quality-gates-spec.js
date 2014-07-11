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
    curFilePath.push('quality-gates-spec');
    fs.changeWorkingDirectory(curFilePath.join(fs.separator));
}

casper.test.begin('App is setup correctly', function suite(test) {

  // Load MockJax responses from FS
  var appResponse = fs.read('app.json');
  var listResponse = fs.read('list.json');
  var showResponse = fs.read('show.json');

  // Register mockjax requests on loading of requirejs
  casper.options.onResourceRequested = function(instance, requestData, networkRequest) {

    // Here, instance is the running instance of casperjs
    if (requestData.url.indexOf('require') >= 0) {
      instance.page.includeJs('../js/third-party/jquery.mockjax.js', function injectReponses() {
        // Inject response values to page scope
        instance.page.evaluate(function setupMockJax(appResponse, listResponse, showResponse) {
          jQuery.mockjaxSettings.contentType = 'text/json';
          jQuery.mockjaxSettings.responseTime = 250;

          jQuery.mockjax({
            url: '../api/l10n/index',
            responseText: JSON.stringify({
              'quality_gates.page': 'Quality Gates'
            })
          });

          jQuery.mockjax({
            url: '../api/qualitygates/app',
            responseText: appResponse
          });


          jQuery.mockjax({
            url: "../api/qualitygates/list",
            responseText: listResponse
          });

          jQuery.mockjax({
            url: "../api/qualitygates/show?id=1",
            responseText: showResponse
          });

        }, appResponse, listResponse, showResponse);
      });
    }
  };

  // See API at http://docs.casperjs.org/en/latest/modules/index.html

  casper.start('http://localhost:3000/pages/quality-gates.html', function() {
    test.assertTitle('Quality Gates');
  });

  casper.waitWhileSelector("div#quality-gates-loader", function() {

    casper.waitForSelector('li.active', function() {
      test.assertElementCount('li.active', 1);
      test.assertSelectorHasText('ol.navigator-results-list li', 'Default Gate');
    });

    casper.waitForSelector('div.navigator-header', function() {
      test.assertSelectorHasText('div.navigator-header h1', 'Default Gate');
    });

    casper.waitForSelector('table.quality-gate-conditions tbody tr:nth-child(9)', function() {
      test.assertElementCount('table.quality-gate-conditions tbody tr', 9);
    });
  });

  casper.run(function() {
    test.done();
  });
});
