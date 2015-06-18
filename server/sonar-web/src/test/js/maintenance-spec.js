/* global describe:false, it:false */
var lib = require('../lib');

describe('Maintenance App', function () {

  it('should exist', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/system/status', 'status-up.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/maintenance/app'], function (App) {
              App.start({ el: '#content', setup: false });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.maintenance-title');
        })

        .then(function () {
          test.assertExists('.maintenance-title');
          test.assertExists('.maintenance-text');
        });
  });

  it('should change status', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/system/status', 'status-up.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/maintenance/app'], function (App) {
              App.start({ el: '#content', setup: false });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.maintenance-title');
        })

        .then(function () {
          test.assertDoesntExist('.maintenance-title.text-danger');
          lib.clearRequestMocks();
          lib.fmock('/api/system/status', 'status-down.json');
          casper.waitForSelector('.maintenance-title.text-danger');
        });
  });

});
