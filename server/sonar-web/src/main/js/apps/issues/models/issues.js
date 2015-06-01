define([
  'components/issue/models/issue'
], function (Issue) {

  return Backbone.Collection.extend({
    model: Issue,

    url: function () {
      return baseUrl + '/api/issues/search';
    },

    parseIssues: function (r) {
      var find = function (source, key, keyField) {
        var searchDict = {};
        searchDict[keyField || 'key'] = key;
        return _.findWhere(source, searchDict) || key;
      };
      return r.issues.map(function (issue, index) {
        var component = find(r.components, issue.component),
            project = find(r.projects, issue.project),
            subProject = find(r.components, issue.subProject),
            rule = find(r.rules, issue.rule),
            assignee = find(r.users, issue.assignee, 'login');
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
        if (assignee) {
          _.extend(issue, {
            assigneeEmail: assignee.email
          });
        }
        return issue;
      });
    },

    setIndex: function () {
      return this.forEach(function (issue, index) {
        return issue.set({ index: index });
      });
    }
  });

});
