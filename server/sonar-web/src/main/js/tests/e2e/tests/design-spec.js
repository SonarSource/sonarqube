var lib = require('../lib'),
    testName = lib.testName('Design');

lib.initMessages();
lib.changeWorkingDirectory('design-spec');


casper.test.begin(testName('Base'), function suite(test) {
  casper
      .start(lib.buildUrl('design'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/dependencies', 'dependencies.json');
      })

      .then(function () {
        casper.waitWhileSelector('.spinner');
      })

      .then(function () {
        test.assertSelectorContains('.dsm-body', 'src/test/java/com/maif/sonar/cobol/metrics');
        test.assertSelectorContains('.dsm-body', 'src/test/java/com/maif/sonar/cobol/repository');
        test.assertElementCount('.dsm-body-cell-dependency', 12);
        test.assertElementCount('.dsm-body-cell-cycle', 1);
        test.assertSelectorContains('.dsm-body-cell-cycle', '6');
      })

      .then(function () {
        casper.mouse.doubleclick('.dsm-body-cell-cycle');
        casper.waitForSelector('.spinner', function () {
          casper.waitWhileSelector('.spinner');
        })
      })

      .then(function () {
        test.assertElementCount('.dsm-info tr', 7);
        test.assertSelectorContains('.dsm-info', 'src/main/java/com/maif/sonar/cobol/api/MaifCobolMeasureProvider.java');
        test.assertSelectorContains('.dsm-info', 'src/main/java/com/maif/sonar/cobol/metrics/BusinessRuleCounter.java ');
        test.assertSelectorContains('.dsm-info', 'src/main/java/com/maif/sonar/cobol/metrics/TableMetricsVisitor.java ');
      })

      .run(function () {
        test.done();
      });
});
