define([
  'widgets/issue-filter/widget',
  './templates'
], function (IssueFilter) {

  var $ = jQuery;

  Handlebars.registerHelper('issuesHomeLink', function (property, value) {
    return baseUrl + '/issues/search#resolved=false|createdInLast=1w|' +
        property + '=' + (encodeURIComponent(value));
  });

  Handlebars.registerHelper('myIssuesHomeLink', function (property, value) {
    return baseUrl + '/issues/search#resolved=false|createdInLast=1w|assignees=__me__|' +
        property + '=' + (encodeURIComponent(value));
  });

  Handlebars.registerHelper('issueFilterHomeLink', function (id) {
    return baseUrl + '/issues/search#id=' + id;
  });

  return Marionette.ItemView.extend({
    template: Templates['issues-workspace-home'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-barchart rect': 'selectBar',
      'click .js-my-barchart rect': 'selectMyBar'
    },

    initialize: function () {
      this.model = new Backbone.Model();
      this.requestIssues();
      this.requestMyIssues();
    },

    _getProjects: function (r) {
      var projectFacet = _.findWhere(r.facets, { property: 'projectUuids' });
      if (projectFacet != null) {
        var values = _.head(projectFacet.values, 3);
        values.forEach(function (v) {
          var project = _.findWhere(r.projects, { uuid: v.val });
          v.label = project.longName;
        });
        return values;
      }
    },

    _getAuthors: function (r) {
      var authorFacet = _.findWhere(r.facets, { property: 'authors' });
      if (authorFacet != null) {
        return _.head(authorFacet.values, 3);
      }
    },

    _getTags: function (r) {
      var MIN_SIZE = 10,
          MAX_SIZE = 24,
          tagFacet = _.findWhere(r.facets, { property: 'tags' });
      if (tagFacet != null) {
        var values = _.head(tagFacet.values, 10),
            minCount = _.min(values, function (v) {
              return v.count;
            }).count,
            maxCount = _.max(values, function (v) {
              return v.count;
            }).count,
            scale = d3.scale.linear().domain([minCount, maxCount]).range([MIN_SIZE, MAX_SIZE]);
        values.forEach(function (v) {
          v.size = scale(v.count);
        });
        return values;
      }
    },

    requestIssues: function () {
      var that = this;
      var url = baseUrl + '/api/issues/search',
          options = {
            resolved: false,
            createdInLast: '1w',
            ps: 1,
            facets: 'createdAt,projectUuids,authors,tags'
          };
      return $.get(url, options).done(function (r) {
        var createdAt = _.findWhere(r.facets, { property: 'createdAt' });
        that.model.set({
          createdAt: createdAt != null ? createdAt.values : null,
          projects: that._getProjects(r),
          authors: that._getAuthors(r),
          tags: that._getTags(r)
        });
      });
    },

    requestMyIssues: function () {
      var that = this;
      var url = baseUrl + '/api/issues/search',
          options = {
            resolved: false,
            createdInLast: '1w',
            assignees: '__me__',
            ps: 1,
            facets: 'createdAt,projectUuids,authors,tags'
          };
      return $.get(url, options).done(function (r) {
        var createdAt = _.findWhere(r.facets, { property: 'createdAt' });
        return that.model.set({
          myCreatedAt: createdAt != null ? createdAt.values : null,
          myProjects: that._getProjects(r),
          myTags: that._getTags(r)
        });
      });
    },

    onRender: function () {
      var values = this.model.get('createdAt'),
          myValues = this.model.get('myCreatedAt');
      if (values != null) {
        this.$('.js-barchart').barchart(values);
      }
      if (myValues != null) {
        this.$('.js-my-barchart').barchart(myValues);
      }
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
    },

    selectBar: function (e) {
      var periodStart = $(e.currentTarget).data('period-start'),
          periodEnd = $(e.currentTarget).data('period-end');
      this.options.app.state.setQuery({
        resolved: false,
        createdAfter: periodStart,
        createdBefore: periodEnd
      });
    },

    selectMyBar: function (e) {
      var periodStart = $(e.currentTarget).data('period-start'),
          periodEnd = $(e.currentTarget).data('period-end');
      this.options.app.state.setQuery({
        resolved: false,
        assignees: '__me__',
        createdAfter: periodStart,
        createdBefore: periodEnd
      });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        user: window.SS.user,
        filters: _.sortBy(this.options.app.filters.toJSON(), 'name')
      });
    }
  });

});
