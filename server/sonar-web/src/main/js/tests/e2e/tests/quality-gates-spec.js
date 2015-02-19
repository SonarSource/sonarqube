/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('quality-gates-spec');
lib.configureCasper();


casper.test.begin('Quality Gates', function suite (test) {
  casper
      .start(lib.buildUrl('quality-gates'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=1', 'show.json');
      })

      .then(function () {
        casper.waitForSelector('.active', function () {
          test.assertElementCount('.active', 1);
          test.assertSelectorHasText('.search-navigator-side .active', 'Default Gate');
        });
      })

      .then(function () {
        casper.waitForSelector('.search-navigator-workspace-header', function () {
          test.assertSelectorHasText('.search-navigator-workspace-header', 'Default Gate');
        });
      })

      .then(function () {
        casper.waitForSelector('table.quality-gate-conditions tbody tr:nth-child(9)', function () {
          test.assertElementCount('table.quality-gate-conditions tbody tr', 9);
        });
      })

      .run(function () {
        test.done();
      });
});
