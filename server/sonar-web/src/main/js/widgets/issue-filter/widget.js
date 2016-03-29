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
import moment from 'moment';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';

import Template from './templates/widget-issue-filter.hbs';
import AssigneesTemplate from './templates/widget-issue-filter-assignees.hbs';
import ResolutionsTemplate from './templates/widget-issue-filter-resolutions.hbs';
import SeveritiesTemplate from './templates/widget-issue-filter-severities.hbs';
import StatusesTemplate from './templates/widget-issue-filter-statuses.hbs';

import { translate } from '../../helpers/l10n';


const FACET_LIMIT = 15;

const defaultComparator = function (item) {
  return -item.count;
};

const defaultFilter = function (item) {
  const items = this.query[this.property];
  return items == null ||
      (items != null && items.split(',').indexOf(item.val) !== -1);
};

const defaultLabel = function (item) {
  return item.val;
};

const defaultLink = function (item, property, query, index, items, mode) {
  const criterion = {};
  criterion[property] = item.val;
  const r = _.extend({}, query, criterion);
  if (mode === 'debt') {
    r.facetMode = 'debt';
  }
  if (r.componentKey != null) {
    return window.baseUrl + '/component_issues/index?id=' + encodeURIComponent(r.componentKey) +
        '#' + getQuery(_.omit(r, 'componentKey'));
  } else {
    return window.baseUrl + '/issues/search#' + getQuery(r);
  }
};

const byDistributionConf = {
  'types': {
    comparator (item) {
      const order = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
      return order.indexOf(item.val);
    },
    label (item) {
      return translate('issue.type', item.val);
    }
  },
  'severities': {
    template: SeveritiesTemplate,
    comparator (item) {
      const order = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];
      return order.indexOf(item.val);
    }
  },
  'statuses': {
    template: StatusesTemplate,
    comparator (item) {
      const order = ['OPEN', 'REOPENED', 'CONFIRMED', 'RESOLVED', 'CLOSED'];
      return order.indexOf(item.val);
    },
    filter (item) {
      const unresolvedQuery = '' + this.query.resolved === 'false';
      const resolvedStatus = item.val === 'RESOLVED' || item.val === 'CLOSED';
      return !(unresolvedQuery && resolvedStatus);
    }
  },
  'resolutions': {
    template: ResolutionsTemplate,
    comparator (item) {
      const order = ['', 'FALSE-POSITIVE', 'WONTFIX', 'FIXED', 'REMOVED'];
      return order.indexOf(item.val);
    },
    filter (item) {
      if ('' + this.query.resolved === 'false') {
        return item.val === '';
      } else {
        return defaultFilter.call(this, item);
      }
    }
  },
  'rules': {
    label (item, r) {
      if (_.isArray(r.rules)) {
        const rule = _.findWhere(r.rules, { key: item.val });
        if (rule != null) {
          return rule.name;
        }
      }
    }
  },
  'projectUuids': {
    label (item, r) {
      if (_.isArray(r.components)) {
        const project = _.findWhere(r.components, { uuid: item.val });
        if (project != null) {
          return project.name;
        }
      }
    }
  },
  'assignees': {
    template: AssigneesTemplate,
    label (item, r) {
      if (_.isArray(r.users)) {
        const user = _.findWhere(r.users, { login: item.val });
        if (user != null) {
          return user.name;
        }
      }
    },
    filter (item) {
      if ('' + this.query.assigned === 'false') {
        return item.val === '';
      } else {
        return defaultFilter.call(this, item);
      }
    }
  },
  'languages': {
    label (item, r) {
      if (_.isArray(r.languages)) {
        const lang = _.findWhere(r.languages, { key: item.val });
        if (lang != null) {
          return lang.name;
        }
      }
    }
  },
  'createdAt': {
    comparator (item) {
      return -moment(item.val).unix();
    },
    label (item, r, items, index, query) {
      const beginning = moment(item.val);
      const endDate = query.createdBefore != null ? moment(query.createdBefore) : moment();
      const ending = index < items.length - 1 ? moment(items[index + 1].val).subtract(1, 'days') : endDate;
      const isSameDay = ending.diff(beginning, 'days') <= 1;
      return beginning.format('LL') + (isSameDay ? '' : (' – ' + ending.format('LL')));
    },
    link (item, property, query, index, items, mode) {
      const createdAfter = moment(item.val);
      const endDate = query.createdBefore != null ? moment(query.createdBefore) : moment();
      const createdBefore = index < items.length - 1 ? moment(items[index + 1].val).subtract(1, 'days') : endDate;
      const isSameDay = createdBefore.diff(createdAfter, 'days') <= 1;
      if (isSameDay) {
        createdBefore.add(1, 'days');
      }
      const r = _.extend({}, query, {
        createdAfter: createdAfter.format('YYYY-MM-DD'),
        createdBefore: createdBefore.format('YYYY-MM-DD')
      });
      if (mode === 'debt') {
        r.facetMode = 'debt';
      }
      if (r.componentKey != null) {
        return window.baseUrl + '/component_issues/index?id=' + encodeURIComponent(r.componentKey) +
            '#' + getQuery(_.omit(r, 'componentKey'));
      } else {
        return window.baseUrl + '/issues/search#' + getQuery(r);
      }
    }
  }
};

function getQuery (query, separator) {
  separator = separator || '|';
  const route = [];
  _.forEach(query, function (value, property) {
    route.push(`${property}=${encodeURIComponent(value)}`);
  });
  return route.join(separator);
}

export default Marionette.ItemView.extend({

  getTemplate () {
    return this.conf != null && this.conf.template != null ?
        this.conf.template : Template;
  },

  initialize () {
    this.shouldIgnorePeriod = false;
    this.model = new Backbone.Model({
      query: this.options.query,
      parsedQuery: this.getParsedQuery(),
      property: this.options.distributionAxis
    });

    // Ignore the period date if the filter contains any date criteria
    // `this.shouldIgnorePeriod` is set in `this.getParsedQuery()`
    if (!this.shouldIgnorePeriod) {
      this.model.set({ periodDate: this.options.periodDate });
    }

    this.listenTo(this.model, 'change', this.render);
    this.conf = byDistributionConf[this.options.distributionAxis];
    this.query = this.getParsedQuery();
    this.requestIssues();
  },

  getParsedQuery () {
    const queryString = this.options.query || '';
    const query = {};
    queryString.split('|').forEach(function (criterionString) {
      const criterion = criterionString.split('=');
      if (criterion.length === 2) {
        query[criterion[0]] = criterion[1];
      }
    });
    if (this.options.componentKey != null) {
      _.extend(query, { componentKey: this.options.componentKey });
    }
    if (!this.hasDateFilter(query) && this.options.periodDate != null) {
      _.extend(query, { createdAfter: this.options.periodDate });
    } else {
      this.shouldIgnorePeriod = true;
    }
    return query;
  },

  hasDateFilter (query) {
    const q = query || this.model.get('parsedQuery');
    return _.some(['createdAt', 'createdBefore', 'createdAfter', 'createdInLast'], function (p) {
      return q[p] != null;
    });
  },

  sortItems (items) {
    const comparator = this.conf != null && this.conf.comparator != null ? this.conf.comparator : defaultComparator;
    return _.sortBy(items, comparator);
  },

  filterItems (items) {
    const filter = this.conf != null && this.conf.filter != null ? this.conf.filter : defaultFilter;
    return _.filter(items, filter, { query: this.query, property: this.options.distributionAxis });
  },

  withLink (items) {
    const link = this.conf != null && this.conf.link != null ? this.conf.link : defaultLink;
    const property = this.options.distributionAxis;
    const mode = this.options.displayMode;
    const query = this.model.get('parsedQuery');
    return items.map(function (item, index) {
      return _.extend(item, { searchLink: link(item, property, query, index, items, mode) });
    });
  },

  withLabels (items) {
    const label = this.conf != null && this.conf.label != null ? this.conf.label : defaultLabel;
    const r = this.model.get('rawResponse');
    const query = this.model.get('parsedQuery');
    return items.map(function (item, index) {
      return _.extend(item, { label: label(item, r, items, index, query) });
    });
  },

  requestIssues () {
    const that = this;
    const facetMode = this.options.displayMode;
    const url = window.baseUrl + '/api/issues/search';
    const options = _.extend({}, this.query, {
      facetMode,
      ps: 1,
      facets: this.options.distributionAxis,
      additionalFields: '_all'
    });
    if (this.options.componentUuid != null) {
      _.extend(options, { componentUuids: this.options.componentUuid });
    }
    if (this.options.periodDate != null && !this.shouldIgnorePeriod) {
      _.extend(options, { createdAfter: this.options.periodDate });
    }
    return $.get(url, options).done(function (r) {
      if (_.isArray(r.facets) && r.facets.length === 1) {
        // save response object, but do not trigger repaint
        that.model.set({ rawResponse: r }, { silent: true });
        const items = that.sortItems(that.withLabels(that.withLink(that.filterItems(r.facets[0].values))));
        that.model.set({
          items,
          maxResultsReached: items.length >= FACET_LIMIT,
          maxResults: items.length,
          total: facetMode === 'debt' ? r.debtTotal : r.total
        });
      }
    });
  },

  serializeData () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      displayMode: this.options.displayMode
    });
  }
});
