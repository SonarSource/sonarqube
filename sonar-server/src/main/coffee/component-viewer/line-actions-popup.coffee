define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/popup'
  'issues/manual-issue-view'
], (
  Marionette
  Templates
  Popup
  ManualIssueView
) ->

  $ = jQuery


  class extends Popup
    template: Templates['line-options-popup']


    events:
      'click .js-add-manual-issue': 'addManualIssue'


    addManualIssue: (e) ->
      e.preventDefault()
      line = @options.row.data 'line-number'
      component = @options.main.component.get 'key'
      manualIssueView = new ManualIssueView
        line: line
        component: component
      manualIssueView.render().$el.appendTo @options.row.find('.line')
      manualIssueView.on 'add', (issue) =>
        issues = @options.main.source.get('issues') || []
        activeIssues = @options.main.source.get('activeIssues') || []
        showIssues = @options.main.settings.get 'issues'
        issues.push issue
        if showIssues then activeIssues.push issue else activeIssues = [issue]
        @options.main.source.set 'issues', issues
        @options.main.source.set 'activeIssues', activeIssues
        @options.main.settings.set 'issues', true
        @options.main.sourceView.render()


