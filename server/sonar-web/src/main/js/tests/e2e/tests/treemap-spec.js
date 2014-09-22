var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('treemap-spec');


casper.test.begin('Treemap', function (test) {
  var treemapData = JSON.parse(fs.read('treemap.json'));

  casper.start(lib.buildUrl('treemap'), function () {
    lib.mockRequestFromFile('/api/resources/index', 'treemap-resources.json');

    casper.evaluate(function (treemapData) {
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
    }, treemapData)
  });

  casper
      .then(function () {
        casper.waitWhileSelector('.spinner', function() {
          test.assertElementCount('.treemap-cell', 30);
          test.assertSelectorHasText('.treemap-cell', 'SonarQube');
          test.assertMatch(casper.getElementAttribute('.treemap-link', 'href'), /dashboard\/index/,
              'Treemap cells have links to dashboards');
        });
      })
      .then(function () {
        casper.evaluate(function () {
          var evt = document.createEvent('MouseEvents');
          evt.initMouseEvent('click', true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
          d3.select('.treemap-cell').node().dispatchEvent(evt);
        });
      })
      .then(function () {
        casper.wait(500, function () {
          test.assertSelectorHasText('.treemap-cell', 'Server');
          test.assertElementCount('.treemap-cell', 25);
        });
      });

  casper.run(function() {
    test.done();
  });
});
