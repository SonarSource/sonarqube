import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import '../templates';

export default Marionette.ItemView.extend({
  template: Templates['coding-rules-rule-issues'],

  initialize: function () {
    var that = this;
    this.total = null;
    this.projects = [];
    this.requestIssues().done(function () {
      that.render();
    });
  },

  requestIssues: function () {
    var that = this,
        url = baseUrl + '/api/issues/search',
        options = {
          rules: this.model.id,
          resolved: false,
          ps: 1,
          facets: 'projectUuids'
        };
    return $.get(url, options).done(function (r) {
      var projectsFacet = _.findWhere(r.facets, { property: 'projectUuids' }),
          projects = projectsFacet != null ? projectsFacet.values : [];
      projects = projects.map(function (project) {
        var projectBase = _.findWhere(r.components, { uuid: project.val });
        return _.extend(project, {
          name: projectBase != null ? projectBase.longName : ''
        });
      });
      that.projects = projects;
      that.total = r.total;
    });
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      total: this.total,
      projects: this.projects,
      baseSearchUrl: baseUrl + '/issues/search#resolved=false|rules=' + encodeURIComponent(this.model.id)
    });
  }
});


