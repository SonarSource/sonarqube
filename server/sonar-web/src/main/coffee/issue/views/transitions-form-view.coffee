define [
  'issue/views/action-options-view'
  'templates/issue'
], (
  ActionOptionsView
) ->

  $ = jQuery
  

  class extends ActionOptionsView
    template: Templates['issue-transitions-form']


    selectInitialOption: ->
      @makeActive @getOptions().first()


    selectOption: (e) ->
      transition = $(e.currentTarget).data 'value'
      @submit transition
      super


    submit: (transition) ->
      p = window.process.addBackgroundProcess()
      $.ajax
        type: 'POST',
        url: baseUrl + '/api/issues/do_transition',
        data:
          issue: @model.get('key')
          transition: transition
      .done =>
        @options.view.resetIssue {}, p
      .fail =>
        window.process.failBackgroundProcess p
