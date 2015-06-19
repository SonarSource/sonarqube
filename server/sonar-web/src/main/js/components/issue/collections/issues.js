define([
  '../models/issue'
], function (Issue) {

  return Backbone.Collection.extend({
    model: Issue,

    url: function () {
      return baseUrl + '/api/issues/search';
    },

    _injectRelational: function (issue, source, baseField, lookupField) {
      var baseValue = issue[baseField];
      if (baseValue != null && _.size(source)) {
        var lookupValue = _.find(source, function (candidate) {
          return candidate[lookupField] === baseValue;
        });
        if (lookupValue != null) {
          Object.keys(lookupValue).forEach(function (key) {
            var newKey = baseField + key.charAt(0).toUpperCase() + key.slice(1);
            issue[newKey] = lookupValue[key];
          });
        }
      }
      return issue;
    },

    parse: function (r) {
      var that = this;
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
            rule = find(r.rules, issue.rule);
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
        issue = that._injectRelational(issue, r.users, 'assignee', 'login');
        issue = that._injectRelational(issue, r.users, 'reporter', 'login');
        return issue;
      });
    }
  });

});
