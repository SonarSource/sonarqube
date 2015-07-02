define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Issues Page', function () {
    bdd.describe('Saved Searches', function () {
      bdd.it('should show list of saved searches', function () {
        return this.remote
            .get(require.toUrl('test/medium/base.html'))
            .setFindTimeout(5000)
            .mockFromString('/api/l10n/index', '{}')
            .mockFromFile('/api/issue_filters/app', 'issues-spec/app.json')
            .mockFromFile('/api/issues/search', 'issues-spec/search.json')
            .startApp('issues')
            .assertElementCount('.js-filter', 2)
            .assertElementCount('.js-filter[data-id="31"]', 1)
            .assertElementCount('.js-filter[data-id="32"]', 1);
      });

      bdd.it('should load a saved search', function () {
        return this.remote
            .get(require.toUrl('test/medium/base.html'))
            .setFindTimeout(5000)
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
            .setFindTimeout(5000)
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
  });

});
