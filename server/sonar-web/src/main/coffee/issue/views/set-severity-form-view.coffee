define [
  'templates/issue'
  'issue/views/action-options-view'
], (
  Templates
  ActionOptionsView
) ->

  $ = jQuery
  

  class extends ActionOptionsView
    template: Templates['issue-set-severity-form']


    getSeverity: ->
      @model.get 'severity'


    selectInitialOption: ->
      @makeActive @getOptions().filter("[data-value=#{@getSeverity()}]")


    selectOption: (e) ->
      severity = $(e.currentTarget).data 'value'
      @submit severity
      super


    submit: (severity) ->
      _severity = @getSeverity()
      return if severity == _severity
      p = window.process.addBackgroundProcess()
      @model.set severity: severity
      $.ajax
        type: 'POST'
        url: "#{baseUrl}/api/issues/set_severity"
        data:
          issue: @model.id
          severity: severity
      .done =>
        window.process.finishBackgroundProcess p
      .fail =>
        @model.set severity: _severity
        window.process.failBackgroundProcess p


    serializeData: ->
      _.extend super,
        items: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
