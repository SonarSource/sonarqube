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


    # Used to parse /api/issues/search response
    parseIssues: (r) ->
      find = (source, key, keyField) ->
        searchDict = {}
        searchDict[keyField || 'key'] = key
        _.findWhere(source, searchDict) || key

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

        if _.isArray(issue.sources) && issue.sources.length > 0
          source = ''
          issue.sources.forEach (line) ->
            source = line[1] if line[0] == issue.line
          _.extend issue, source: source


        if _.isArray(issue.scm) && issue.scm.length > 0
          scmAuthor = ''
          scmDate = ''

          issue.scm.forEach (line) ->
            if line[0] == issue.line
              scmAuthor = line[1]
              scmDate = line[2]

          _.extend issue,
            scmAuthor: scmAuthor
            scmDate: scmDate

        issue
