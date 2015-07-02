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
            .waitForElementCount('.js-filter', 2)
            .waitForElementCount('.js-filter[data-id="31"]', 1)
            .waitForElementCount('.js-filter[data-id="32"]', 1);
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
            .assertElementCount('.js-filter-copy', 1)
            .assertElementCount('.js-filter-edit', 1)
            .assertElementInclude('.issues-filters-name', 'Critical and Blocker Issues')
            .assertElementCount('.js-facet.active[data-value="BLOCKER"]', 1)
            .assertElementCount('.js-facet.active[data-value="CRITICAL"]', 1)
            .assertElementCount('.js-facet.active[data-unresolved]', 1);
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
            .assertElementCount('.js-filter-copy', 1)
            .assertElementCount('.js-filter-edit', 1)
            .assertElementInclude('.issues-filters-name', 'Critical and Blocker Issues')
            .assertElementCount('.js-facet.active[data-value="BLOCKER"]', 1)
            .assertElementCount('.js-facet.active[data-value="CRITICAL"]', 1)
            .assertElementCount('.js-facet.active[data-unresolved]', 1)
            .clickElement('.js-new-search')
            .assertElementCount('.js-facet[data-value="BLOCKER"]:not(.active)', 1)
            .assertElementCount('.js-facet[data-value="CRITICAL"]:not(.active)', 1)
            .assertElementCount('.js-facet.active[data-unresolved]', 1)
            .assertElementNotInclude('.issues-filters-name', 'Critical and Blocker Issues');
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
          .assertElementExist('.facet[data-value=BLOCKER]')
          .assertElementExist('.facet[data-value=CRITICAL]')
          .assertElementExist('.facet[data-value=MAJOR]')
          .assertElementExist('.facet[data-value=MINOR]')
          .assertElementExist('.facet[data-value=INFO]')

          .assertElementExist('.facet[data-value=OPEN]')
          .assertElementExist('.facet[data-value=REOPENED]')
          .assertElementExist('.facet[data-value=CONFIRMED]')
          .assertElementExist('.facet[data-value=RESOLVED]')
          .assertElementExist('.facet[data-value=CLOSED]')

          .assertElementExist('.facet[data-unresolved]')
          .assertElementExist('.facet[data-value=REMOVED]')
          .assertElementExist('.facet[data-value=FIXED]')
          .assertElementExist('.facet[data-value=FALSE-POSITIVE]')

          .assertElementCount('.issue', 50)
          .assertElementCount('.issue.selected', 1)
        //.assertElementInclude('.issue', '1 more branches need to be covered by unit tests to reach')

          .assertElementExist('.js-new-search')
          .assertElementExist('.js-filter-save-as')

          .assertElementInclude('#issues-total', '4623')
          .assertElementExist('.js-prev')
          .assertElementExist('.js-next')
          .assertElementExist('.js-reload')
          .assertElementExist('.js-bulk-change');
    });

    bdd.it('should show severity facet', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
          .mockFromFile('/api/issues/search', 'issues-spec/search.json')
          .startApp('issues')
          .clickElement('.js-new-search')
          .waitForElementCount('.issue', 50)
          .clearMocks()
          .mockFromFile('/api/issues/search', 'issues-spec/search-reopened.json', { data: { severities: 'BLOCKER' } })
          .clickElement('.facet[data-value=BLOCKER]')
          .waitForElementCount('.issue', 4);
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
          .assertElementExist('.js-selection')
          .assertElementNotExist('.js-selection.icon-checkbox-checked')
          .assertElementExist('.issue .js-toggle')
          .assertElementCount('.js-toggle', 50)
          .assertElementNotExist(issueSelector + ' .js-toggle .icon-checkbox-checked')
          .clickElement(issueSelector + ' .js-toggle')
          .assertElementExist(issueSelector + ' .js-toggle .icon-checkbox-checked')
          .assertElementExist('.js-selection.icon-checkbox-single.icon-checkbox-checked')
          .clickElement('.js-selection')
          .assertElementNotExist('.js-selection.icon-checkbox-checked')
          .assertElementNotExist('.js-toggle .icon-checkbox-checked')
          .clickElement('.js-selection')
          .assertElementExist('.js-selection.icon-checkbox-checked')
          .assertElementCount('.js-toggle .icon-checkbox-checked', 50);
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
          .assertElementExist('#bulk-change-form')
          .assertElementInclude('#bulk-change-form', 'bulk change form');
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
          .assertElementExist('.js-selection')
          .assertElementNotExist('.js-selection.icon-checkbox-checked')
          .assertElementExist('.issue .js-toggle')
          .assertElementNotExist(issueSelector + ' .js-toggle .icon-checkbox-checked')
          .clickElement(issueSelector + ' .js-toggle')
          .assertElementExist(issueSelector + ' .js-toggle .icon-checkbox-checked')
          .assertElementExist('.js-selection.icon-checkbox-single.icon-checkbox-checked')
          .clickElement('#issues-bulk-change')
          .clickElement('.js-bulk-change-selected')
          .assertElementExist('#bulk-change-form')
          .assertElementInclude('#bulk-change-form', 'bulk change form')
          .clearMocks()
          .mockFromFile('/api/issues/search', 'issues-spec/search-changed.json')
          .execute(function () {
            window.onBulkIssues();
          })
          .assertElementExist(issueSelector + ' .js-issue-set-severity .icon-severity-blocker')
          .assertElementExist(issueSelector + ' .js-toggle .icon-checkbox-checked');
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
          .assertElementExist('.bubble-popup')
          .assertElementExist('.bubble-popup [data-property="severities"][data-value="MAJOR"]')
          .assertElementExist('.bubble-popup [data-property="statuses"][data-value="CONFIRMED"]')
          .assertElementExist('.bubble-popup [data-property="resolved"][data-value="false"]')
          .assertElementExist('.bubble-popup [data-property="rules"][data-value="squid:S1214"]')
          .assertElementExist('.bubble-popup [data-property="assigned"][data-value="false"]')
          .assertElementExist('.bubble-popup [data-property="planned"][data-value="false"]')
          .assertElementExist('.bubble-popup [data-property="tags"][data-value="bad-practice"]')
          .assertElementExist('.bubble-popup [data-property="tags"][data-value="brain-overload"]')
          .assertElementExist('.bubble-popup [data-property="projectUuids"][data-value="69e57151-be0d-4157-adff-c06741d88879"]')
          .assertElementExist('.bubble-popup [data-property="moduleUuids"][data-value="7feef7c3-11b9-4175-b5a7-527ca3c75cb7"]')
          .assertElementExist('.bubble-popup [data-property="fileUuids"][data-value="b0517331-0aaf-4091-b5cf-8e305dd0337a"]')
          .clickElement('.bubble-popup [data-property="severities"]')
          .waitForElementCount('.issue', 17);
    });
  });

});
