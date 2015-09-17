import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'key',

  defaults: {
    activeRuleCount: 0,
    projectCount: 0
  },

  fetch: function () {
    var that = this;
    this.fetchChanged = {};
    return $.when(
        this.fetchProfileRules(),
        this.fetchInheritance()
    ).done(function () {
          that.set(that.fetchChanged);
        });
  },

  fetchProfileRules: function () {
    var that = this,
        url = baseUrl + '/api/rules/search',
        key = this.id,
        options = {
          ps: 1,
          facets: 'active_severities',
          qprofile: key,
          activation: 'true'
        };
    return $.get(url, options).done(function (r) {
      var severityFacet = _.findWhere(r.facets, { property: 'active_severities' });
      if (severityFacet != null) {
        var severities = severityFacet.values,
            severityComparator = function (s) {
              return window.severityColumnsComparator(s.val);
            },
            sortedSeverities = _.sortBy(severities, severityComparator);
        _.extend(that.fetchChanged, { rulesSeverities: sortedSeverities });
      }
    });
  },

  fetchInheritance: function () {
    var that = this,
        url = baseUrl + '/api/qualityprofiles/inheritance',
        options = { profileKey: this.id };
    return $.get(url, options).done(function (r) {
      _.extend(that.fetchChanged, r.profile, {
        ancestors: r.ancestors,
        children: r.children
      });
    });
  },

  fetchChangelog: function (options) {
    var that = this,
        url = baseUrl + '/api/qualityprofiles/changelog',
        opts = _.extend({}, options, { profileKey: this.id });
    return $.get(url, opts).done(function (r) {
      that.set({
        events: r.events,
        eventsPage: r.p,
        totalEvents: r.total,
        eventsParameters: _.clone(options)
      });
    });
  },

  fetchMoreChangelog: function () {
    var that = this,
        url = baseUrl + '/api/qualityprofiles/changelog',
        page = this.get('eventsPage') || 0,
        parameters = this.get('eventsParameters') || {},
        opts = _.extend({}, parameters, { profileKey: this.id, p: page + 1 });
    return $.get(url, opts).done(function (r) {
      var events = that.get('events') || [];
      that.set({
        events: [].concat(events, r.events),
        eventsPage: r.p,
        totalEvents: r.total
      });
    });
  },


  resetChangelog: function () {
    this.unset('events', { silent: true });
    this.unset('eventsPage', { silent: true });
    this.unset('totalEvents');
  },

  compareWith: function (withKey) {
    var that = this,
        url = baseUrl + '/api/qualityprofiles/compare',
        options = { leftKey: this.id, rightKey: withKey };
    return $.get(url, options).done(function (r) {
      var comparison = _.extend(r, {
        inLeftSize: _.size(r.inLeft),
        inRightSize: _.size(r.inRight),
        modifiedSize: _.size(r.modified)
      });
      that.set({
        comparison: comparison,
        comparedWith: withKey
      });
    });
  }
});


