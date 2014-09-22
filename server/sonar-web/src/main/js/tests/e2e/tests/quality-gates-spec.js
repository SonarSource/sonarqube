var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('quality-gates-spec');


casper.test.begin('Quality Gates', function suite(test) {
  casper.start(lib.buildUrl('quality-gates'), function() {
    lib.setDefaultViewport();

    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
    lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
    lib.mockRequestFromFile('/api/qualitygates/show?id=1', 'show.json');
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
