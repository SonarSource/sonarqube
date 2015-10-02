define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Source Viewer', function () {
    var file = { uuid: 'uuid', key: 'key' };

    bdd.describe('Issues', function () {
      bdd.it('should show precise issue location', function () {
        return this.remote
            .open()
            .mockFromFile('/api/components/app', 'source-viewer-spec/app.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/sources/lines', 'source-viewer-spec/lines.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/issues/search',
            'source-viewer-spec/issues-with-precise-location.json',
            { data: { componentUuids: 'uuid' } })
            .startAppBrowserify('source-viewer', { file: file })

            .checkElementExist('.source-line-code[data-line-number="3"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="3"] .source-line-code-issue', '14 So')

            .checkElementExist('.source-line-code[data-line-number="9"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="9"] .source-line-code-issue', 'sion')

            .checkElementExist('.source-line-code[data-line-number="18"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="18"] .source-line-code-issue',
            'ranklin Street, Fifth Floor, Boston, MA  02110-1301, USA.')
            .checkElementExist('.source-line-code[data-line-number="19"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="19"] .source-line-code-issue', ' */');
      });

      bdd.it('should show secondary issue locations on the same line', function () {
        return this.remote
            .open()
            .mockFromFile('/api/components/app', 'source-viewer-spec/app.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/sources/lines', 'source-viewer-spec/lines.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/issues/search',
            'source-viewer-spec/issues-with-precise-location.json',
            { data: { componentUuids: 'uuid' } })
            .startAppBrowserify('source-viewer', { file: file })
            .checkElementExist('.source-line-code[data-line-number="3"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="3"] .source-line-code-issue', '14 So')
            .clickElement('.source-line-with-issues[data-line-number="3"]')
            .clickElement('.js-issue-locations')
            .checkElementCount('.source-line-code[data-line-number="3"] .source-viewer-issue-location', 1)
            .checkElementCount('.source-line-code[data-line-number="3"] .source-line-code-secondary-issue', 2)
            .checkElementInclude('.source-line-code[data-line-number="3"] .source-line-code-secondary-issue', ') 200')
            .checkElementInclude('.source-line-code[data-line-number="3"] .source-line-code-secondary-issue', '14 So');
      });

      bdd.it('should show secondary issue locations on the different lines', function () {
        return this.remote
            .open()
            .mockFromFile('/api/components/app', 'source-viewer-spec/app.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/sources/lines', 'source-viewer-spec/lines.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/issues/search',
            'source-viewer-spec/issues-with-precise-location.json',
            { data: { componentUuids: 'uuid' } })
            .startAppBrowserify('source-viewer', { file: file })
            .checkElementExist('.source-line-code[data-line-number="9"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="9"] .source-line-code-issue', 'sion')
            .clickElement('.source-line-with-issues[data-line-number="9"]')
            .clickElement('.js-issue-locations')
            .checkElementCount('.source-line-code[data-line-number="8"] .source-viewer-issue-location', 1)
            .checkElementNotExist('.source-line-code[data-line-number="9"] .source-viewer-issue-location')
            .checkElementCount('.source-line-code[data-line-number="8"] .source-line-code-secondary-issue', 1)
            .checkElementCount('.source-line-code[data-line-number="9"] .source-line-code-secondary-issue', 1)
            .checkElementInclude('.source-line-code[data-line-number="8"] .source-line-code-secondary-issue', 'ense ')
            .checkElementInclude('.source-line-code[data-line-number="9"] .source-line-code-secondary-issue', 'sion');
      });

      bdd.it('should show the number of issues on a line', function () {
        return this.remote
            .open()
            .mockFromFile('/api/components/app', 'source-viewer-spec/app.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/sources/lines', 'source-viewer-spec/lines.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/issues/search', 'source-viewer-spec/several-issues-on-a-line.json')
            .startAppBrowserify('source-viewer', { file: file })
            .checkElementExist('.source-line-issues[data-line-number="3"] .icon-severity-critical')
            .checkElementExist('.source-line-issues[data-line-number="3"] .source-line-issues-counter')
            .checkElementInclude('.source-line-issues[data-line-number="3"] .source-line-issues-counter', 2)
            .checkElementExist('.source-line-issues[data-line-number="5"] .icon-severity-critical')
            .checkElementNotExist('.source-line-issues[data-line-number="5"] .source-line-issues-counter');
      });
    });
  });
});
