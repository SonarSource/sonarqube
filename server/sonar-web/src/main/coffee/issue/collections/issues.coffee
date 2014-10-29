define [
  'backbone'
  'issue/models/issue'
], (
  Backbone
  Issue
) ->

  class extends Backbone.Collection
    model: Issue

    url: ->
      "#{baseUrl}/api/issues/search"


    parse: (r) ->
      find = (source, key, keyField) ->
        searchDict = {}
        searchDict[keyField || 'key'] = key
        _.findWhere(source, searchDict) || key

      @paging =
        p: r.p
        ps: r.ps
        total: r.total
        maxResultsReached: r.p * r.ps >= r.total

      r.issues.map (issue) ->
        component = find r.components, issue.component
        project = find r.projects, issue.project
        rule = find r.rules, issue.rule

        if component
          _.extend issue,
            componentLongName: component.longName
            componentQualifier: component.qualifier

        if project
          _.extend issue,
            projectLongName: project.longName

        if rule
          _.extend issue,
            ruleName: rule.name

        issue
