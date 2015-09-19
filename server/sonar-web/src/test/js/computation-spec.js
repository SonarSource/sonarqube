/* globals casper: false */
var lib = require('../lib'),
    testName = lib.testName('Computation');

lib.initMessages();
lib.changeWorkingDirectory('computation-spec');
lib.configureCasper();

casper.test.begin(testName('List'), 9, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        lib.mockRequestFromFile('/api/ce/queue', 'queue.json');
        lib.mockRequestFromFile('/api/ce/activity', 'history.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/computation/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#computation-list li');
      })

      .then(function () {
        test.assertElementCount('#computation-list li[data-id]', 1);
        test.assertSelectorContains('#computation-list', 'SonarQube');
        test.assertSelectorContains('#computation-list-footer', '1');
        test.assertExists('.js-queue.selected');
      })

      .then(function () {
        casper.click('.js-history');
        casper.waitForSelectorTextChange('#computation-list-footer');
      })

      .then(function () {
        test.assertElementCount('#computation-list li[data-id]', 3);
        test.assertSelectorContains('#computation-list', 'Duration');
        test.assertExists('.js-history.selected');
        test.assertExists('.panel-danger[data-id="3"]');
      })

      .then(function () {
        casper.click('.js-queue');
        casper.waitForSelectorTextChange('#computation-list-footer');
      })

      .then(function () {
        test.assertElementCount('#computation-list li[data-id]', 1);
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Show More'), 2, function (test) {
  casper
      .start(lib.buildUrl('base#past'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/ce/activity', 'history-big-1.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/computation/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#computation-list li');
      })

      .then(function () {
        test.assertElementCount('#computation-list li[data-id]', 2);
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/ce/activity', 'history-big-2.json',
            { data: { p: '2' } });
        casper.click('#computation-fetch-more');
        casper.waitForSelectorTextChange('#computation-list-footer');
      })

      .then(function () {
        test.assertElementCount('#computation-list li[data-id]', 3);
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});
