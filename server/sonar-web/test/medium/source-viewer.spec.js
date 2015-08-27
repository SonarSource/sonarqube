define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Source Viewer', function () {
    bdd.describe('Issues', function () {
      bdd.it('should show precise issue location', function () {
        return this.remote
            .open()
            .mockFromFile('/api/components/app', 'source-viewer-spec/app.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/sources/lines', 'source-viewer-spec/lines.json', { data: { uuid: 'uuid' } })
            .mockFromFile('/api/issues/search',
            'source-viewer-spec/issues-with-precise-location.json',
            { data: { componentUuids: 'uuid' } })
            .startApp('source-viewer/app')

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
            .startApp('source-viewer/app')
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
            .startApp('source-viewer/app')
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
    });
  });
});
