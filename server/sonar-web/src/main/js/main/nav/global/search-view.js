/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import SelectableCollectionView from '../../../components/common/selectable-collection-view';
import SearchItemTemplate from '../templates/nav-search-item.hbs';
import EmptySearchTemplate from '../templates/nav-search-empty.hbs';
import SearchTemplate from '../templates/nav-search.hbs';
import RecentHistory from '../component/RecentHistory';
import { translate } from '../../../helpers/l10n';
import { collapsedDirFromPath, fileFromPath } from '../../../helpers/path';

const SearchItemView = Marionette.ItemView.extend({
  tagName: 'li',
  template: SearchItemTemplate,

  select () {
    this.$el.addClass('active');
  },

  deselect () {
    this.$el.removeClass('active');
  },

  submit () {
    this.$('a')[0].click();
  },

  onRender () {
    this.$('[data-toggle="tooltip"]').tooltip({
      container: 'body',
      html: true,
      placement: 'left',
      delay: { show: 500, hide: 0 }
    });
  },

  onDestroy () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  serializeData () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      index: this.options.index
    });
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
    'submit': 'onSubmit',
    'keydown .js-search-input': 'onKeyDown',
    'keyup .js-search-input': 'onKeyUp'
  },

  initialize () {
    const that = this;
    this.results = new Backbone.Collection();
    this.favorite = [];
    if (window.SS.user) {
      this.fetchFavorite().always(function () {
        that.resetResultsToDefault();
      });
    } else {
      this.resetResultsToDefault();
    }
    this.resultsView = new SearchResultsView({ collection: this.results });
    this.debouncedSearch = _.debounce(this.search, 250);
    this._bufferedValue = '';
  },

  onRender () {
    const that = this;
    this.resultsRegion.show(this.resultsView);
    setTimeout(function () {
      that.$('.js-search-input').focus();
    }, 0);
  },

  onKeyDown (e) {
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

  onKeyUp () {
    const value = this.$('.js-search-input').val();
    if (value === this._bufferedValue) {
      return;
    }
    this._bufferedValue = this.$('.js-search-input').val();
    this.searchRequest = this.debouncedSearch(value);
  },

  onSubmit () {
    return false;
  },

  fetchFavorite () {
    const that = this;
    return $.get(window.baseUrl + '/api/favourites').done(function (r) {
      that.favorite = r.map(function (f) {
        const isFile = ['FIL', 'UTS'].indexOf(f.qualifier) !== -1;
        return {
          url: window.baseUrl + '/dashboard/index?id=' + encodeURIComponent(f.key) + window.dashboardParameters(true),
          name: isFile ? collapsedDirFromPath(f.lname) + fileFromPath(f.lname) : f.name,
          icon: 'favorite'
        };
      });
      that.favorite = _.sortBy(that.favorite, 'name');
    });
  },

  resetResultsToDefault () {
    const recentHistory = RecentHistory.get();
    const history = recentHistory.map(function (historyItem, index) {
      const url = window.baseUrl + '/dashboard/index?id=' + encodeURIComponent(historyItem.key) +
          window.dashboardParameters(true);
      return {
        url,
        name: historyItem.name,
        q: historyItem.icon,
        extra: index === 0 ? translate('browsed_recently') : null
      };
    });
    const favorite = _.first(this.favorite, 6).map(function (f, index) {
      return _.extend(f, { extra: index === 0 ? translate('favorite') : null });
    });
    const qualifiers = this.model.get('qualifiers')
        .filter(q => q !== 'TRK')
        .map(function (q, index) {
          return {
            url: window.baseUrl + '/all_projects?qualifier=' + encodeURIComponent(q),
            name: translate('qualifiers.all', q),
            extra: index === 0 ? '' : null
          };
        });
    this.results.reset([].concat(history, favorite, qualifiers));
  },

  search (q) {
    if (q.length < 2) {
      this.resetResultsToDefault();
      return;
    }
    const that = this;
    const url = window.baseUrl + '/api/components/suggestions';
    const options = { s: q };
    return $.get(url, options).done(function (r) {
      // if the input value has changed since we sent the request,
      // just ignore the output, because another request already sent
      if (q !== that._bufferedValue) {
        return;
      }

      const collection = [];
      r.results.forEach(function (domain) {
        domain.items.forEach(function (item, index) {
          collection.push(_.extend(item, {
            q: domain.q,
            extra: index === 0 ? domain.name : null,
            url: window.baseUrl + '/dashboard/index?id=' + encodeURIComponent(item.key) +
            window.dashboardParameters(true)
          }));
        });
      });
      that.results.reset([].concat(
          that.getNavigationFindings(q),
          that.getGlobalDashboardFindings(q),
          that.getFavoriteFindings(q),
          collection
      ));
    });
  },

  getNavigationFindings (q) {
    const DEFAULT_ITEMS = [
      { name: translate('issues.page'), url: window.baseUrl + '/issues/search' },
      { name: translate('layout.measures'), url: window.baseUrl + '/measures/search?qualifiers[]=TRK' },
      { name: translate('coding_rules.page'), url: window.baseUrl + '/coding_rules' },
      { name: translate('quality_profiles.page'), url: window.baseUrl + '/profiles' },
      { name: translate('quality_gates.page'), url: window.baseUrl + '/quality_gates' }
    ];
    const customItems = [];
    if (window.SS.isUserAdmin) {
      customItems.push({ name: translate('layout.settings'), url: window.baseUrl + '/settings' });
    }
    const findings = [].concat(DEFAULT_ITEMS, customItems).filter(function (f) {
      return f.name.match(new RegExp(q, 'i'));
    });
    if (findings.length > 0) {
      findings[0].extra = translate('navigation');
    }
    return _.first(findings, 6);
  },

  getGlobalDashboardFindings (q) {
    const dashboards = this.model.get('globalDashboards') || [];
    const items = dashboards.map(function (d) {
      return { name: d.name, url: window.baseUrl + '/dashboard/index?did=' + encodeURIComponent(d.key) };
    });
    const findings = items.filter(function (f) {
      return f.name.match(new RegExp(q, 'i'));
    });
    if (findings.length > 0) {
      findings[0].extra = translate('dashboard.global_dashboards');
    }
    return _.first(findings, 6);
  },

  getFavoriteFindings (q) {
    const findings = this.favorite.filter(function (f) {
      return f.name.match(new RegExp(q, 'i'));
    });
    if (findings.length > 0) {
      findings[0].extra = translate('favorite');
    }
    return _.first(findings, 6);
  }
});
