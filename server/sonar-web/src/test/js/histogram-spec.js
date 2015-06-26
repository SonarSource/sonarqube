/* global describe:false, it:false */
var lib = require('../lib');

describe('Histogram Widget', function () {

  it('should be displayed', 7, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/measures/search_filter', 'search-filter.json');
        })

        .then(function () {
          casper.evaluate(function () {
            d3.json = function (url, callback) {
              return jQuery.get(url).done(function (r) {
                callback(null, r);
              });
            };

            var widget = new window.SonarWidgets.Widget();
            var metrics = ['sqale_index'];

            widget
                .type('Histogram')
                .source(baseUrl + '/measures/search_filter')
                .metricsPriority(metrics)
                .options({
                  baseUrl: baseUrl + '/dashboard/index',
                  relativeScale: false,
                  maxItemsReachedMessage: '',
                  noData: '',
                  noMainMetric: false
                })
                .render('#content');
          });
        })

        .then(function () {
          casper.waitForSelector('.sonar-d3');
        })

        .then(function () {
          test.assertElementCount('.sonar-d3 .bar', 3);
          test.assertSelectorContains('.sonar-d3 .bar:nth-child(1)', 'A');
          test.assertSelectorContains('.sonar-d3 .bar:nth-child(1)', '17d');
          test.assertSelectorContains('.sonar-d3 .bar:nth-child(2)', 'B');
          test.assertSelectorContains('.sonar-d3 .bar:nth-child(2)', '5d 1h');
          test.assertSelectorContains('.sonar-d3 .bar:nth-child(3)', 'C');
          test.assertSelectorContains('.sonar-d3 .bar:nth-child(3)', '7h 15min');
        });
  });

  it('should display a message when no data', 0, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/measures/search_filter', 'search-filter-no-data.json');
        })

        .then(function () {
          casper.evaluate(function () {
            d3.json = function (url, callback) {
              return jQuery.get(url).done(function (r) {
                callback(null, r);
              });
            };

            var widget = new window.SonarWidgets.Widget();
            var metrics = ['sqale_index'];

            widget
                .type('Histogram')
                .source(baseUrl + '/measures/search_filter')
                .metricsPriority(metrics)
                .options({
                  baseUrl: baseUrl + '/dashboard/index',
                  relativeScale: false,
                  maxItemsReachedMessage: '',
                  noData: '',
                  noMainMetric: 'NO_MAIN_METRIC'
                })
                .render('#content');
          });
        })

        .then(function () {
          casper.waitForText('NO_MAIN_METRIC');
        });
  });

  it('should display a "max results reached" message', 0, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/measures/search_filter', 'search-filter-max-results-reached.json');
        })

        .then(function () {
          casper.evaluate(function () {
            d3.json = function (url, callback) {
              return jQuery.get(url).done(function (r) {
                callback(null, r);
              });
            };

            var widget = new window.SonarWidgets.Widget();
            var metrics = ['sqale_index'];

            widget
                .type('Histogram')
                .source(baseUrl + '/measures/search_filter')
                .metricsPriority(metrics)
                .options({
                  baseUrl: baseUrl + '/dashboard/index',
                  relativeScale: false,
                  maxItemsReachedMessage: 'MAX_RESULTS_REACHED',
                  noData: '',
                  noMainMetric: ''
                })
                .render('#content');
          });
        })

        .then(function () {
          casper.waitForText('MAX_RESULTS_REACHED');
        });
  });

});
