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

export default Backbone.Model.extend({
  idAttribute: 'key',

  defaults: {
    activeRuleCount: 0,
    projectCount: 0
  },

  fetch () {
    const that = this;
    this.fetchChanged = {};
    return $.when(
        this.fetchProfileRules(),
        this.fetchInheritance()
    ).done(function () {
      that.set(that.fetchChanged);
    });
  },

  fetchProfileRules () {
    const that = this;
    const url = '/api/rules/search';
    const key = this.id;
    const options = {
      ps: 1,
      facets: 'types',
      qprofile: key,
      activation: 'true'
    };
    return $.get(url, options).done(function (r) {
      const typesFacet = _.findWhere(r.facets, { property: 'types' });
      if (typesFacet != null) {
        const order = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
        const types = typesFacet.values;
        const typesComparator = function (t) {
          return order.indexOf(t.val);
        };
        const sortedTypes = _.sortBy(types, typesComparator);
        _.extend(that.fetchChanged, { rulesTypes: sortedTypes });
      }
    });
  },

  fetchInheritance () {
    const that = this;
    const url = '/api/qualityprofiles/inheritance';
    const options = { profileKey: this.id };
    return $.get(url, options).done(function (r) {
      _.extend(that.fetchChanged, r.profile, {
        ancestors: r.ancestors,
        children: r.children
      });
    });
  },

  fetchChangelog (options) {
    const that = this;
    const url = '/api/qualityprofiles/changelog';
    const opts = _.extend({}, options, { profileKey: this.id });
    return $.get(url, opts).done(function (r) {
      that.set({
        events: r.events,
        eventsPage: r.p,
        totalEvents: r.total,
        eventsParameters: _.clone(options)
      });
    });
  },

  fetchMoreChangelog () {
    const that = this;
    const url = '/api/qualityprofiles/changelog';
    const page = this.get('eventsPage') || 0;
    const parameters = this.get('eventsParameters') || {};
    const opts = _.extend({}, parameters, { profileKey: this.id, p: page + 1 });
    return $.get(url, opts).done(function (r) {
      const events = that.get('events') || [];
      that.set({
        events: [].concat(events, r.events),
        eventsPage: r.p,
        totalEvents: r.total
      });
    });
  },

  resetChangelog () {
    this.unset('events', { silent: true });
    this.unset('eventsPage', { silent: true });
    this.unset('totalEvents');
  },

  compareWith (withKey) {
    const that = this;
    const url = '/api/qualityprofiles/compare';
    const options = { leftKey: this.id, rightKey: withKey };
    return $.get(url, options).done(function (r) {
      const comparison = _.extend(r, {
        inLeftSize: _.size(r.inLeft),
        inRightSize: _.size(r.inRight),
        modifiedSize: _.size(r.modified)
      });
      that.set({
        comparison,
        comparedWith: withKey
      });
    });
  },

  resetComparison () {
    this.unset('comparedWith', { silent: true });
    this.unset('comparison');
  }
});
