var lib = require('../lib'),
    testName = lib.testName('Metric Filter');


lib.initMessages();


casper.test.begin(testName('Basic Tests'), function suite(test) {

  casper.start(lib.buildUrl('metric-filter'));


  casper.waitForSelector(".navigator-filter-label", function checkMetricFilter() {

    test.assertSelectorHasText('.navigator-filter-label', 'Metric');
    test.assertSelectorHasText('.navigator-filter-value', 'Not set');

    casper.click('.navigator-filter-label');
    casper.waitUntilVisible('.navigator-filter-details', function checkFields() {
      test.assertElementCount('[name="metric"] [label="Issues"] option', 2);
      test.assertElementCount('[name="metric"] [label="Size"] option', 1);

      test.assertElementCount('[name="period"] option', 4);
    });

    casper.waitUntilVisible('.select2-results:nth-child(2) .select2-result-sub', function checkMetricField() {
      test.assertElementCount('.select2-result-sub .select2-match', 3);
      test.assertElementCount('.select2-results-dept-0:nth-child(1) .select2-result', 2);
      test.assertElementCount('.select2-results-dept-0:nth-child(2) .select2-result', 1);

      casper.mouseEvent('mousedown', '.select2-results-dept-0:nth-child(1) .select2-result:nth-child(2) span');
      casper.mouseEvent('mouseup', '.select2-results-dept-0:nth-child(1) .select2-result:nth-child(2) span');
    });

    casper.then(function checkPeriodsForDifferentialMetric() {
      casper.click('.select2-container:nth-child(3) .select2-choice');

      casper.waitUntilVisible('.select2-results', function checkPeriods() {
        // 'New issues' is selected => 'Value' disappears
        test.assertElementCount('[name="period"] option', 3);
      });
    });

    casper.then(function comeBackToNonDifferentialMetric() {
      casper.click('.select2-container:nth-child(1) .select2-choice');

      casper.waitUntilVisible('.select2-results:nth-child(2) .select2-result-sub', function checkMetricField() {
        test.assertElementCount('.select2-result-sub .select2-match', 3);
        test.assertElementCount('.select2-results-dept-0:nth-child(1) .select2-result', 2);
        test.assertElementCount('.select2-results-dept-0:nth-child(2) .select2-result', 1);

        casper.mouseEvent('mousedown', '.select2-results-dept-0:nth-child(1) .select2-result:nth-child(1) span');
        casper.mouseEvent('mouseup', '.select2-results-dept-0:nth-child(1) .select2-result:nth-child(1) span');
      });
    });

    casper.then(function checkPeriodsForDifferentialMetric() {
      casper.click('.select2-container:nth-child(3) .select2-choice');

      casper.waitUntilVisible('.select2-results');
      // 'Issues' is selected => 'Value' is back
      test.assertElementCount('[name="period"] option', 4);
    });
  });

  casper.then(function fillFilter() {
    casper.sendKeys('[name="val"]', '0');

    casper.click('.navigator-filter-label');

    casper.waitWhileVisible('.navigator-filter-details', function checkMetricDropdownNotOpenOnEdition() {
      casper.click('.navigator-filter-label');

      casper.waitUntilVisible('.navigator-filter-details');
      casper.wait(100, function checkDropdownNotOpen() {
        test.assertNotVisible('.select2-results:nth-child(2) .select2-result-sub');
      });
    });
  });

  casper.run(function() {
    test.done();
  });
});
