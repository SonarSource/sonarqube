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
            .startApp('source-viewer', { file: file })

            .checkElementExist('.source-line-code[data-line-number="3"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="3"] .source-line-code-issue', '14 So')

            .checkElementExist('.source-line-code[data-line-number="11"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="11"] .source-line-code-issue', 'arQub')

            .checkElementExist('.source-line-code[data-line-number="18"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="18"] .source-line-code-issue',
            'ranklin Street, Fifth Floor, Boston, MA  02110-1301, USA.')
            .checkElementExist('.source-line-code[data-line-number="19"] .source-line-code-issue')
            .checkElementInclude('.source-line-code[data-line-number="19"] .source-line-code-issue', ' */');
      });
    });
  });
});
