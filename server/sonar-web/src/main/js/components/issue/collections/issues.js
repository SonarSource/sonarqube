define([
  '../models/issue'
], function (Issue) {

  return Backbone.Collection.extend({
    model: Issue,

    url: function () {
      return baseUrl + '/api/issues/search';
    },

    parse: function (r) {
      function find (source, key, keyField) {
        var searchDict = {};
        searchDict[keyField || 'key'] = key;
        return _.findWhere(source, searchDict) || key;
      }

      this.paging = {
        p: r.p,
        ps: r.ps,
        total: r.total,
        maxResultsReached: r.p * r.ps >= r.total
      };

      return r.issues.map(function (issue) {
        var component = find(r.components, issue.component),
            project = find(r.projects, issue.project),
            rule = find(r.rules, issue.rule)
        if (component) {
          _.extend(issue, {
            componentLongName: component.longName,
            componentQualifier: component.qualifier
          });
        }
        if (project) {
          _.extend(issue, {
            projectLongName: project.longName,
            projectUuid: project.uuid
          });
        }
        if (rule) {
          _.extend(issue, { ruleName: rule.name });
        }
        return issue;
      });
    }
  });

});
