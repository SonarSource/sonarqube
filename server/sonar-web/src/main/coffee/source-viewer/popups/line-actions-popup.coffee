define [
  'backbone.marionette'
  'templates/source-viewer'
  'common/popup'
  'issue/manual-issue-view'
], (
  Marionette
  Templates
  Popup
  ManualIssueView
) ->


  class extends Popup
    template: Templates['source-viewer-line-options-popup']


    events:
      'click .js-get-permalink': 'getPermalink'
      'click .js-add-manual-issue': 'addManualIssue'


    getPermalink: (e) ->
      e.preventDefault()
      url = "#{baseUrl}/component/index#component=#{encodeURIComponent(@model.key())}&line=#{@options.line}"
      windowParams = 'resizable=1,scrollbars=1,status=1'
      window.open url, @model.get('name'), windowParams


    addManualIssue: (e) ->
      e.preventDefault()
      line = @options.line
      component = @model.key()
      manualIssueView = new ManualIssueView
        line: line
        component: component
        rules: @model.get 'manual_rules'
      manualIssueView.render().$el.appendTo @options.row.find('.source-line-code')
      manualIssueView.on 'add', (issue) =>
        @trigger 'onManualIssueAdded', issue
