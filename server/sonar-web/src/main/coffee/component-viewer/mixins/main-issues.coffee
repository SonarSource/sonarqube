define [
  'component-viewer/utils'
], (
  utils
) ->

  $ = jQuery
  API_COMPONENT = "#{baseUrl}/api/components/app"
  API_ISSUES = "#{baseUrl}/api/issues/search"
  LINES_AROUND_ISSUE = 4


  class IssuesMixin

    requestIssues: (key) ->
      options =
        components: key
        ignorePaging: true
        extra_fields: 'actions,transitions,assigneeName,actionPlanName'
      $.get API_ISSUES, options, (data) =>
        @state.set 'hasIssues', true
        issues = _.sortBy data.issues, (issue) -> "#{issue.rule}_____#{issue.message}"
        @source.set issues: issues
        @resetIssues()


    resetIssues: ->
      issues = @source.get('issues')
      if _.isArray issues
        @source.set
          activeIssues: issues.filter (issue) -> !issue.resolution


    showIssues: (store = false, issue) ->
      @settings.set 'issues', true
      @storeSettings() if store
      if issue?
        @currentIssue = issue.key
        @source.set 'issues', [issue]
        @filterByCurrentIssue()
        @headerView.render()
        @trigger 'sized'
      else
        unless @state.get 'hasIssues'
          @requestIssues(@key).done =>
            @sourceView.render()
            @trigger 'sized'
        else
          @sourceView.render()
          @trigger 'sized'


    hideIssues: (store = false) ->
      @settings.set 'issues', false
      @storeSettings() if store
      @sourceView.render()


    requestIssuesPeriod: (key, period) ->
      params = key: key
      params.period = period if period?
      $.get API_COMPONENT, params, (data) =>
        rules = data.rules.map (r) -> key: r[0], name: r[1], count: r[2]
        severities = data.severities.map (r) -> key: r[0], name: r[1], count: r[2]
        @state.set
          rules: _.sortBy(rules, 'name')
          severities: utils.sortSeverities(severities)


    filterLinesByIssues: ->
      issues = @source.get 'issues'
      @sourceView.resetShowBlocks()
      issues.forEach (issue) =>
        line = issue.line || 0
        if issue.status == 'CLOSED'
          @sourceView.addShowBlock 0, 0, true
        else
          @sourceView.addShowBlock line - LINES_AROUND_ISSUE, line + LINES_AROUND_ISSUE, line == 0
      @sourceView.render()


    filterByIssues: (predicate, requestIssues = true) ->
      period = @state.get('period')
      if period
        p = predicate
        predicate = (issue) =>
          creationDate = new Date moment(issue.creationDate).format()
          (creationDate >= period.get('sinceDate')) && p issue

      if requestIssues && !@state.get 'hasIssues'
        @requestIssues(@key).done => @_filterByIssues(predicate)
      else
        @_filterByIssues(predicate)


    _filterByIssues: (predicate) ->
      issues = @source.get 'issues'
      @settings.set 'issues', true
      @sourceView.resetShowBlocks()
      activeIssues = []
      issues.forEach (issue) =>
        if predicate issue
          line = issue.line || 0
          if issue.status == 'CLOSED'
            @sourceView.addShowBlock 0, 0, true
          else
            @sourceView.addShowBlock line - LINES_AROUND_ISSUE, line + LINES_AROUND_ISSUE, line == 0
          activeIssues.push issue
      @source.set 'activeIssues', activeIssues
      @sourceView.render()


    # Current Issue
    filterByCurrentIssue: -> @filterByIssues ((issue) => issue.key == @currentIssue), false


    # All Issues
    filterByAllIssues: -> @filterByIssues -> true

    # Resolved Issues
    filterByFixedIssues: -> @filterByIssues (issue) -> issue.resolution == 'FIXED'

    # Unresolved Issues
    filterByUnresolvedIssues: -> @filterByIssues (issue) -> !issue.resolution

    # False Positive
    filterByFalsePositiveIssues: -> @filterByIssues (issue) -> issue.resolution == 'FALSE-POSITIVE'

    # Open/Reopened Issues
    filterByOpenIssues: -> @filterByIssues (issue) -> issue.status == 'OPEN' || issue.status == 'REOPENED'

    # Rule
    filterByRule: (rule) -> @filterByIssues (issue) -> issue.rule == rule && !issue.resolution

    # Severity
    filterByBlockerIssues: -> @filterByIssues (issue) -> issue.severity == 'BLOCKER' && !issue.resolution
    filterByCriticalIssues: -> @filterByIssues (issue) -> issue.severity == 'CRITICAL' && !issue.resolution
    filterByMajorIssues: -> @filterByIssues (issue) -> issue.severity == 'MAJOR' && !issue.resolution
    filterByMinorIssues: -> @filterByIssues (issue) -> issue.severity == 'MINOR' && !issue.resolution
    filterByInfoIssues: -> @filterByIssues (issue) -> issue.severity == 'INFO' && !issue.resolution
