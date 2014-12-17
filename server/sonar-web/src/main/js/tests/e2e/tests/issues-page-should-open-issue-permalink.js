/* globals casper: false */

var lib = require('../lib');


lib.initMessages();
lib.changeWorkingDirectory('issues-page-should-open-issue-permalink');


var issueKey = 'some-issue-key';


casper.test.begin('issues-page-should-open-issue-permalink', function (test) {
  casper
      .start(lib.buildUrl('issues#issues=' + encodeURI(issueKey)), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json', { data: { issues: issueKey } });
      })

      .then(function () {
        casper.waitForSelector('.issue', function () {
          test.assertElementCount('.issue', 1);
          test.assertExist('.issue[data-key="' + issueKey + '"]');
        });
      })

      .run(function () {
        test.done();
      });
});
