/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
// @flow
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import debounce from 'lodash/debounce';
import sortBy from 'lodash/sortBy';
import SelectableCollectionView from '../../../../components/common/selectable-collection-view';
import SearchItemTemplate from '../templates/nav-search-item.hbs';
import EmptySearchTemplate from '../templates/nav-search-empty.hbs';
import SearchTemplate from '../templates/nav-search.hbs';
import RecentHistory from '../component/RecentHistory';
import { translate } from '../../../../helpers/l10n';
import { isUserAdmin } from '../../../../helpers/users';
import { getFavorites } from '../../../../api/favorites';
import { getSuggestions } from '../../../../api/components';
import {
  getOrganization,
  areThereCustomOrganizations
} from '../../../../store/organizations/utils';

type Finding = {
  name: string,
  url: string,
  extra?: string
};

const SHOW_ORGANIZATION_FOR_QUALIFIERS = ['TRK', 'VW', 'SVW'];

const SearchItemView = Marionette.ItemView.extend({
  tagName: 'li',
  template: SearchItemTemplate,

  select() {
    this.$el.addClass('active');
  },

  deselect() {
    this.$el.removeClass('active');
  },

  submit() {
    this.$('a')[0].click();
  },

  onRender() {
    this.$('[data-toggle="tooltip"]').tooltip({
      container: 'body',
      html: true,
      placement: 'left',
      delay: { show: 500, hide: 0 }
    });
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      index: this.options.index
    };
  }
});

const SearchEmptyView = Marionette.ItemView.extend({
  tagName: 'li',
  template: EmptySearchTemplate
});

const SearchResultsView = SelectableCollectionView.extend({
  className: 'menu',
  tagName: 'ul',
  childView: SearchItemView,
  emptyView: SearchEmptyView
});

export default Marionette.LayoutView.extend({
  className: 'navbar-search',
  tagName: 'form',
  template: SearchTemplate,

  regions: {
    resultsRegion: '.js-search-results'
  },

  events: {
    submit: 'handleSubmit',
    'keydown .js-search-input': 'onKeyDown',
    'keyup .js-search-input': 'onKeyUp'
  },

  initialize() {
    this.results = new Backbone.Collection();
    this.favorite = [];
    if (this.model.get('currentUser').isLoggedIn) {
      this.fetchFavorite().then(
        () => this.resetResultsToDefault(),
        () => this.resetResultsToDefault()
      );
    } else {
      this.resetResultsToDefault();
    }
    this.resultsView = new SearchResultsView({ collection: this.results });
    this.debouncedSearch = debounce(this.search, 250);
    this._bufferedValue = '';
  },

  onRender() {
    const that = this;
    this.resultsRegion.show(this.resultsView);
    setTimeout(
      () => {
        that.$('.js-search-input').focus();
      },
      0
    );
  },

  onKeyDown(e) {
    if (e.keyCode === 38) {
      this.resultsView.selectPrev();
      return false;
    }
    if (e.keyCode === 40) {
      this.resultsView.selectNext();
      return false;
    }
    if (e.keyCode === 13) {
      this.resultsView.submitCurrent();
      this.destroy();
      return false;
    }
    if (e.keyCode === 27) {
      this.options.hide();
      return false;
    }
  },

  onKeyUp() {
    const value = this.$('.js-search-input').val();
    if (value === this._bufferedValue) {
      return;
    }
    this._bufferedValue = this.$('.js-search-input').val();
    this.searchRequest = this.debouncedSearch(value);
  },

  onSubmit() {
    return false;
  },

  fetchFavorite(): Promise<*> {
    const customOrganizations = areThereCustomOrganizations();
    return getFavorites().then(r => {
      this.favorite = r.favorites.map(f => {
        const showOrganization = customOrganizations && f.organization != null;
        const organization = showOrganization ? getOrganization(f.organization) : null;
        return {
          url: window.baseUrl +
            '/dashboard/index?id=' +
            encodeURIComponent(f.key) +
            window.dashboardParameters(true),
          name: f.name,
          icon: 'favorite',
          organization
        };
      });
      this.favorite = sortBy(this.favorite, 'name');
    });
  },

  resetResultsToDefault() {
    const recentHistory = RecentHistory.get();
    const customOrganizations = areThereCustomOrganizations();
    const history = recentHistory.map((historyItem, index) => {
      const url = window.baseUrl +
        '/dashboard/index?id=' +
        encodeURIComponent(historyItem.key) +
        window.dashboardParameters(true);
      const showOrganization = customOrganizations && historyItem.organization != null;
      // $FlowFixMe flow doesn't check the above condition on `historyItem.organization != null`
      const organization = showOrganization ? getOrganization(historyItem.organization) : null;
      return {
        url,
        organization,
        name: historyItem.name,
        q: historyItem.icon,
        extra: index === 0 ? translate('browsed_recently') : null
      };
    });
    const favorite = this.favorite.slice(0, 6).map((f, index) => {
      return { ...f, extra: index === 0 ? translate('favorite') : null };
    });
    this.results.reset([].concat(history, favorite));
  },

  search(q) {
    if (q.length < 2) {
      this.resetResultsToDefault();
      return;
    }
    return getSuggestions(q).then(r => {
      // if the input value has changed since we sent the request,
      // just ignore the output, because another request already sent
      if (q !== this._bufferedValue) {
        return;
      }

      const customOrganizations = areThereCustomOrganizations();

      const collection = [];
      r.results.forEach(({ items, q }) => {
        items.forEach((item, index) => {
          const showOrganization = customOrganizations &&
            item.organization != null &&
            SHOW_ORGANIZATION_FOR_QUALIFIERS.includes(q);
          const organization = showOrganization ? getOrganization(item.organization) : null;
          collection.push({
            ...item,
            q,
            organization,
            extra: index === 0 ? translate('qualifiers', q) : null,
            url: window.baseUrl + '/dashboard?id=' + encodeURIComponent(item.key)
          });
        });
      });
      this.results.reset([
        ...this.getNavigationFindings(q),
        ...this.getGlobalDashboardFindings(q),
        ...this.getFavoriteFindings(q),
        ...collection
      ]);
    });
  },

  getNavigationFindings(q) {
    const DEFAULT_ITEMS = [
      { name: translate('issues.page'), url: window.baseUrl + '/issues/search' },
      {
        name: translate('layout.measures'),
        url: window.baseUrl + '/measures/search?qualifiers[]=TRK'
      },
      { name: translate('coding_rules.page'), url: window.baseUrl + '/coding_rules' },
      { name: translate('quality_profiles.page'), url: window.baseUrl + '/profiles' },
      { name: translate('quality_gates.page'), url: window.baseUrl + '/quality_gates' }
    ];
    const customItems: Array<Finding> = [];
    if (isUserAdmin(this.model.get('currentUser'))) {
      customItems.push({ name: translate('layout.settings'), url: window.baseUrl + '/settings' });
    }
    const findings = [].concat(DEFAULT_ITEMS, customItems).filter(f => {
      return f.name.match(new RegExp(q, 'i'));
    });
    if (findings.length > 0) {
      findings[0].extra = translate('navigation');
    }
    return findings.slice(0, 6);
  },

  getGlobalDashboardFindings(q) {
    const dashboards = this.model.get('globalDashboards') || [];
    const items = dashboards.map(d => {
      return {
        name: d.name,
        url: window.baseUrl + '/dashboard/index?did=' + encodeURIComponent(d.key)
      };
    });
    const findings = items.filter(f => {
      return f.name.match(new RegExp(q, 'i'));
    });
    if (findings.length > 0) {
      findings[0].extra = translate('dashboard.global_dashboards');
    }
    return findings.slice(0, 6);
  },

  getFavoriteFindings(q) {
    const findings = this.favorite.filter(f => {
      return f.name.match(new RegExp(q, 'i'));
    });
    if (findings.length > 0) {
      findings[0].extra = translate('favorite');
    }
    return findings.slice(0, 6);
  }
});
