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



casper.test.begin(testName('Stress Test'), function suite(test) {
  casper.options.waitTimeout = 1000000;

  casper
      .exit(0) // Remove this to enable the test

      .start(lib.buildUrl('design'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequest('/api/resources', generateBigResourcesFile(1000));
      })

      .then(function () {
        casper.waitWhileSelector('.spinner');
      })

      .then(function () {
        test.assertElementCount('.dsm-body tr', 1000);
      })

      .run(function () {
        test.done();
      });
});


function generateBigResourcesFile (limit) {
  var s = '[{"id":6924,"key":"com.maif.sonar:maif-cobol-plugin","name":"MAIF :: Cobol plugin","scope":"PRJ","qualifier":"TRK","date":"2014-07-21T23:04:05+0600","creationDate":null,"lname":"MAIF :: Cobol plugin","version":"2.7-SNAPSHOT","description":"","msr":[{"key":"dsm","data":"[',
      i, j;

  for (i = 0; i < limit; i++) {
    s += '{\\\"i\\\":';
    s += i;
    s += ',\\\"n\\\":\\\"src/test/java/com/maif/sonar/cobol/metrics';
    s += i;
    s += '\\\",\\\"q\\\":\\\"DIR\\\",\\\"v\\\":[';

    for (j = 0; j < limit; j++) {
      s += '{}';
      if (j < limit - 1) {
        s += ',';
      }
    }

    s += ']}';
    if (i < limit - 1) {
      s += ',';
    }
  }

  s += ']"}]}]';
  return s;
}
