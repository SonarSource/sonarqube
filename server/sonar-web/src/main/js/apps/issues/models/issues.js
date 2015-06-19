define([
  './issue'
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

    parseIssues: function (r) {
      var that = this;
      var find = function (source, key, keyField) {
        var searchDict = {};
        searchDict[keyField || 'key'] = key;
        return _.findWhere(source, searchDict) || key;
      };
      return r.issues.map(function (issue, index) {
        var component = find(r.components, issue.component),
            project = find(r.projects, issue.project),
            subProject = find(r.components, issue.subProject),
            rule = find(r.rules, issue.rule);
        _.extend(issue, { index: index });
        if (component) {
          _.extend(issue, {
            componentUuid: component.uuid,
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
        if (subProject) {
          _.extend(issue, {
            subProjectLongName: subProject.longName,
            subProjectUuid: subProject.uuid
          });
        }
        if (rule) {
          _.extend(issue, {
            ruleName: rule.name
          });
        }
        issue = that._injectRelational(issue, r.users, 'assignee', 'login');
        return issue;
      });
    },

    setIndex: function () {
      return this.forEach(function (issue, index) {
        return issue.set({ index: index });
      });
    },

    selectByKeys: function (keys) {
      var that = this;
      keys.forEach(function (key) {
        var issue = that.get(key);
        if (issue) {
          issue.set({ selected: true });
        }
      });
    }
  });

});
