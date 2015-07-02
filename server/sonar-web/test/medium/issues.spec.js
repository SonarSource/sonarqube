define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Issues Page', function () {
    bdd.describe('Saved Searches', function () {
      bdd.it('should show list of saved searches', function () {
        return this.remote
            .get(require.toUrl('test/medium/base.html'))
            .mockFromString('/api/l10n/index', '{}')
            .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
            .mockFromFile('/api/issues/search', 'issues-spec/search.json')
            .startApp('issues')
            .checkElementCount('.js-filter', 2)
            .checkElementCount('.js-filter[data-id="31"]', 1)
            .checkElementCount('.js-filter[data-id="32"]', 1);
      });

      bdd.it('should load a saved search', function () {
        return this.remote
            .get(require.toUrl('test/medium/base.html'))
            .mockFromString('/api/l10n/index', '{}')
            .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
            .mockFromFile('/api/issues/search', 'issues-spec/search.json')
            .mockFromFile('/api/issue_filters/show/31', 'issues-spec/filter.json')
            .startApp('issues')
            .clickElement('.search-navigator-filters-show-list')
            .clickElement('.js-filter[data-id="31"]')
            .checkElementCount('.js-filter-copy', 1)
            .checkElementCount('.js-filter-edit', 1)
            .checkElementInclude('.issues-filters-name', 'Critical and Blocker Issues')
            .checkElementCount('.js-facet.active[data-value="BLOCKER"]', 1)
            .checkElementCount('.js-facet.active[data-value="CRITICAL"]', 1)
            .checkElementCount('.js-facet.active[data-unresolved]', 1);
      });

      bdd.it('should load a saved search and then resets it by new search', function () {
        return this.remote
            .get(require.toUrl('test/medium/base.html'))
            .mockFromString('/api/l10n/index', '{}')
            .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
            .mockFromFile('/api/issues/search', 'issues-spec/search.json')
            .mockFromFile('/api/issue_filters/show/31', 'issues-spec/filter.json')
            .startApp('issues')
            .clickElement('.search-navigator-filters-show-list')
            .clickElement('.js-filter[data-id="31"]')
            .checkElementCount('.js-filter-copy', 1)
            .checkElementCount('.js-filter-edit', 1)
            .checkElementInclude('.issues-filters-name', 'Critical and Blocker Issues')
            .checkElementCount('.js-facet.active[data-value="BLOCKER"]', 1)
            .checkElementCount('.js-facet.active[data-value="CRITICAL"]', 1)
            .checkElementCount('.js-facet.active[data-unresolved]', 1)
            .clickElement('.js-new-search')
            .checkElementCount('.js-facet[data-value="BLOCKER"]:not(.active)', 1)
            .checkElementCount('.js-facet[data-value="CRITICAL"]:not(.active)', 1)
            .checkElementCount('.js-facet.active[data-unresolved]', 1)
            .checkElementNotInclude('.issues-filters-name', 'Critical and Blocker Issues');
      });
    });

    bdd.it('should load', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
          .mockFromFile('/api/issues/search', 'issues-spec/search.json')
          .startApp('issues')
          .clickElement('.js-new-search')
          .checkElementExist('.facet[data-value=BLOCKER]')
          .checkElementExist('.facet[data-value=CRITICAL]')
          .checkElementExist('.facet[data-value=MAJOR]')
          .checkElementExist('.facet[data-value=MINOR]')
          .checkElementExist('.facet[data-value=INFO]')

          .checkElementExist('.facet[data-value=OPEN]')
          .checkElementExist('.facet[data-value=REOPENED]')
          .checkElementExist('.facet[data-value=CONFIRMED]')
          .checkElementExist('.facet[data-value=RESOLVED]')
          .checkElementExist('.facet[data-value=CLOSED]')

          .checkElementExist('.facet[data-unresolved]')
          .checkElementExist('.facet[data-value=REMOVED]')
          .checkElementExist('.facet[data-value=FIXED]')
          .checkElementExist('.facet[data-value=FALSE-POSITIVE]')

          .checkElementCount('.issue', 50)
          .checkElementCount('.issue.selected', 1)
        //.checkElementInclude('.issue', '1 more branches need to be covered by unit tests to reach')

          .checkElementExist('.js-new-search')
          .checkElementExist('.js-filter-save-as')

          .checkElementInclude('#issues-total', '4623')
          .checkElementExist('.js-prev')
          .checkElementExist('.js-next')
          .checkElementExist('.js-reload')
          .checkElementExist('.js-bulk-change');
    });

    bdd.it('should show severity facet', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
          .mockFromFile('/api/issues/search', 'issues-spec/search.json')
          .startApp('issues')
          .clickElement('.js-new-search')
          .checkElementCount('.issue', 50)
          .clearMocks()
          .mockFromFile('/api/issues/search', 'issues-spec/search-reopened.json', { data: { severities: 'BLOCKER' } })
          .clickElement('.facet[data-value=BLOCKER]')
          .checkElementCount('.issue', 4);
    });

    bdd.it('should select issues', function () {
      var issueKey = '94357807-fcb4-40cc-9598-9a715f1eee6e',
          issueSelector = '.issue[data-key="' + issueKey + '"]';

      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
          .mockFromFile('/api/issues/search', 'issues-spec/search.json')
          .startApp('issues')
          .clickElement('.js-new-search')
          .checkElementExist('.js-selection')
          .checkElementNotExist('.js-selection.icon-checkbox-checked')
          .checkElementExist('.issue .js-toggle')
          .checkElementCount('.js-toggle', 50)
          .checkElementNotExist(issueSelector + ' .js-toggle .icon-checkbox-checked')
          .clickElement(issueSelector + ' .js-toggle')
          .checkElementExist(issueSelector + ' .js-toggle .icon-checkbox-checked')
          .checkElementExist('.js-selection.icon-checkbox-single.icon-checkbox-checked')
          .clickElement('.js-selection')
          .checkElementNotExist('.js-selection.icon-checkbox-checked')
          .checkElementNotExist('.js-toggle .icon-checkbox-checked')
          .clickElement('.js-selection')
          .checkElementExist('.js-selection.icon-checkbox-checked')
          .checkElementCount('.js-toggle .icon-checkbox-checked', 50);
    });

    bdd.it('should bulk change issues', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
          .mockFromFile('/api/issues/search', 'issues-spec/search.json')
          .mockFromString('/issues/bulk_change_form*',
          '<div id="bulk-change-form">bulk change form</div>', { contentType: 'text/plain' })
          .startApp('issues')
          .clickElement('.js-new-search')
          .clickElement('#issues-bulk-change')
          .clickElement('.js-bulk-change')
          .checkElementExist('#bulk-change-form')
          .checkElementInclude('#bulk-change-form', 'bulk change form');
    });

    bdd.it('should bulk change selected issues', function () {
      var issueKey = '94357807-fcb4-40cc-9598-9a715f1eee6e',
          issueSelector = '.issue[data-key="' + issueKey + '"]';

      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
          .mockFromFile('/api/issues/search', 'issues-spec/search.json')
          .mockFromString('/issues/bulk_change_form*',
          '<div id="bulk-change-form">bulk change form</div>', { contentType: 'text/plain' })
          .startApp('issues')
          .clickElement('.js-new-search')
          .checkElementExist('.js-selection')
          .checkElementNotExist('.js-selection.icon-checkbox-checked')
          .checkElementExist('.issue .js-toggle')
          .checkElementNotExist(issueSelector + ' .js-toggle .icon-checkbox-checked')
          .clickElement(issueSelector + ' .js-toggle')
          .checkElementExist(issueSelector + ' .js-toggle .icon-checkbox-checked')
          .checkElementExist('.js-selection.icon-checkbox-single.icon-checkbox-checked')
          .clickElement('#issues-bulk-change')
          .clickElement('.js-bulk-change-selected')
          .checkElementExist('#bulk-change-form')
          .checkElementInclude('#bulk-change-form', 'bulk change form')
          .clearMocks()
          .mockFromFile('/api/issues/search', 'issues-spec/search-changed.json')
          .execute(function () {
            window.onBulkIssues();
          })
          .checkElementExist(issueSelector + ' .js-issue-set-severity .icon-severity-blocker')
          .checkElementExist(issueSelector + ' .js-toggle .icon-checkbox-checked');
    });

    bdd.it('should filter similar issues', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
          .mockFromFile('/api/issues/search',
          'issues-spec/search-filter-similar-issues-severities.json', { data: { severities: 'MAJOR' } })
          .mockFromFile('/api/issues/search', 'issues-spec/search-filter-similar-issues.json')
          .startApp('issues')
          .clickElement('.js-new-search')
          .clickElement('.issue.selected .js-issue-filter')
          .checkElementExist('.bubble-popup')
          .checkElementExist('.bubble-popup [data-property="severities"][data-value="MAJOR"]')
          .checkElementExist('.bubble-popup [data-property="statuses"][data-value="CONFIRMED"]')
          .checkElementExist('.bubble-popup [data-property="resolved"][data-value="false"]')
          .checkElementExist('.bubble-popup [data-property="rules"][data-value="squid:S1214"]')
          .checkElementExist('.bubble-popup [data-property="assigned"][data-value="false"]')
          .checkElementExist('.bubble-popup [data-property="planned"][data-value="false"]')
          .checkElementExist('.bubble-popup [data-property="tags"][data-value="bad-practice"]')
          .checkElementExist('.bubble-popup [data-property="tags"][data-value="brain-overload"]')
          .checkElementExist('.bubble-popup [data-property="projectUuids"][data-value="69e57151-be0d-4157-adff-c06741d88879"]')
          .checkElementExist('.bubble-popup [data-property="moduleUuids"][data-value="7feef7c3-11b9-4175-b5a7-527ca3c75cb7"]')
          .checkElementExist('.bubble-popup [data-property="fileUuids"][data-value="b0517331-0aaf-4091-b5cf-8e305dd0337a"]')
          .clickElement('.bubble-popup [data-property="severities"]')
          .checkElementCount('.issue', 17);
    });
  });

});
