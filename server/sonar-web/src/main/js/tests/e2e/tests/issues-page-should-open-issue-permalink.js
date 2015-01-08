/* globals casper: false */

var lib = require('../lib');


lib.initMessages();
lib.changeWorkingDirectory('issues-page-should-open-issue-permalink');


var issueKey = 'some-issue-key';


casper.test.begin('issues-page-should-open-issue-permalink', 3, function (test) {
  casper
      .start(lib.buildUrl('issues#issues=' + encodeURI(issueKey)), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequest('/api/issues/search', '{}', { data: { issues: issueKey, p: 2 } });
        lib.mockRequestFromFile('/api/issues/search', 'search.json', { data: { issues: issueKey } });
        lib.mockRequestFromFile('/api/components/app', 'components-app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line', function () {
          test.assertSelectorContains('.source-viewer', 'public void executeOn(Project project, SensorContext context');
          test.assertElementCount('.issue', 1);
          test.assertExist('.issue[data-key="' + issueKey + '"]');
        });
      })

      .run(function () {
        test.done();
      });
});
