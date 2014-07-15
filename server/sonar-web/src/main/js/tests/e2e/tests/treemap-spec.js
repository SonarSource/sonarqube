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
  curFilePath.push('treemap-spec');
  fs.changeWorkingDirectory(curFilePath.join(fs.separator));
}


casper.test.begin('Treemap', function suite(test) {

  // Load MockJax responses from FS
  var treemapData = JSON.parse(fs.read('treemap.json'));
  var resourceResponse = fs.read('treemap-resources.json');


  casper.start('http://localhost:3000/pages/treemap.html', function () {
    casper.evaluate(function (treemapData, resourceResponse) {
      jQuery.mockjax({ url: '/api/resources/index', responseText: resourceResponse });
      var widget = new SonarWidgets.Treemap();
      widget
          .metrics(treemapData.metrics)
          .metricsPriority(['coverage', 'ncloc'])
          .components(treemapData.components)
          .options({
            heightInPercents: 55,
            maxItems: 30,
            maxItemsReachedMessage: '',
            baseUrl: '/dashboard/index/',
            noData: '',
            resource: ''
          })
          .render('#container');
    }, treemapData, resourceResponse)
  });

  casper
      .then(function () {
        casper.waitWhileSelector('.spinner', function() {
          test.assertElementCount('.treemap-cell', 30);
          test.assertSelectorHasText('.treemap-cell', 'SonarQube');
          test.assertMatch(casper.getElementAttribute('.treemap-link', 'href'), /dashboard\/index/,
              'Treemap cells have links to dashboards');
        });
      });

  casper.run(function() {
    test.done();
  });
});
