// Dump log messages
casper.on('remote.message', function(message) {
  this.echo('Log: '+ message, 'LOG');
});

// Dump uncaught errors
casper.on('page.error', function(msg, trace) {
  this.echo('Error: ' + msg, 'ERROR');
});


casper.test.begin('App is setup correctly', function suite(test) {

  // Register mockjax requests on loading of requirejs
  casper.options.onResourceRequested = function(instance, requestData, networkRequest) {
    if (requestData.url.indexOf('require') >= 0) {
      // Here, instance is the running instance of casperjs
      instance.page.includeJs('../js/third-party/jquery.mockjax.js', function() {
        instance.page.evaluate(function() {
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
            responseText: JSON.stringify({
              "edit": false,
              "periods": [
                {
                  "key": 1,
                  "text": "since previous analysis"
                },
                {
                  "key": 2,
                  "text": "over 365 days"
                },
                {
                  "key": 3,
                  "text": "since previous version"
                },
                {
                  "key": 4,
                  "text": "over period 4 - defined at project level"
                },
                {
                  "key": 5,
                  "text": "over period 5 - defined at project level"
                }
              ],
              "metrics": [
                  {
                      "id": 62,
                      "key": "blocker_violations",
                      "name": "Blocker issues",
                      "type": "INT",
                      "domain": "Issues",
                      "hidden": false
                  },
                  {
                      "id": 37,
                      "key": "new_coverage",
                      "name": "Coverage on new code",
                      "type": "PERCENT",
                      "domain": "Tests",
                      "hidden": false
                  },
                  {
                      "id": 63,
                      "key": "critical_violations",
                      "name": "Critical issues",
                      "type": "INT",
                      "domain": "Issues",
                      "hidden": false
                  },
                  {
                      "id": 154,
                      "key": "sqale_effort_to_grade_a",
                      "name": "Effort to rating A",
                      "type": "WORK_DUR",
                      "domain": "SQALE",
                      "hidden": false
                  },
                  {
                      "id": 218,
                      "key": "open_issues",
                      "name": "Open issues",
                      "type": "INT",
                      "domain": "Issues",
                      "hidden": false
                  },
                  {
                      "id": 219,
                      "key": "reopened_issues",
                      "name": "Reopened issues",
                      "type": "INT",
                      "domain": "Issues",
                      "hidden": false
                  },
                  {
                      "id": 32,
                      "key": "skipped_tests",
                      "name": "Skipped unit tests",
                      "type": "INT",
                      "domain": "Tests",
                      "hidden": false
                  },
                  {
                      "id": 31,
                      "key": "test_errors",
                      "name": "Unit test errors",
                      "type": "INT",
                      "domain": "Tests",
                      "hidden": false
                  },
                  {
                      "id": 33,
                      "key": "test_failures",
                      "name": "Unit test failures",
                      "type": "INT",
                      "domain": "Tests",
                      "hidden": false
                  }
              ]
            })
          });


          jQuery.mockjax({
            url: "../api/qualitygates/list",
            responseText: JSON.stringify({
              "qualitygates": [
                {
                  "id": 1,
                  "name": "Default Gate"
                }
              ],
              "default": 1
            })
          });

          jQuery.mockjax({
            url: "../api/qualitygates/show?id=1",
            responseText: {
              "id": 1,
              "name": "Default Gate",
              "conditions": [
                  {
                      "id": 1,
                      "metric": "blocker_violations",
                      "op": "GT",
                      "warning": "",
                      "error": "0"
                  },
                  {
                      "id": 2,
                      "metric": "new_coverage",
                      "op": "LT",
                      "warning": "",
                      "error": "80",
                      "period": 3
                  },
                  {
                      "id": 3,
                      "metric": "critical_violations",
                      "op": "GT",
                      "warning": "",
                      "error": "0"
                  },
                  {
                      "id": 4,
                      "metric": "sqale_effort_to_grade_a",
                      "op": "GT",
                      "warning": "",
                      "error": "0"
                  },
                  {
                      "id": 5,
                      "metric": "open_issues",
                      "op": "GT",
                      "warning": "0",
                      "error": ""
                  },
                  {
                      "id": 6,
                      "metric": "reopened_issues",
                      "op": "GT",
                      "warning": "0",
                      "error": ""
                  },
                  {
                      "id": 7,
                      "metric": "skipped_tests",
                      "op": "GT",
                      "warning": "0",
                      "error": ""
                  },
                  {
                      "id": 8,
                      "metric": "test_errors",
                      "op": "GT",
                      "warning": "",
                      "error": "0"
                  },
                  {
                      "id": 9,
                      "metric": "test_failures",
                      "op": "GT",
                      "warning": "",
                      "error": "0"
                  }
              ]
            }
          });

        });
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
