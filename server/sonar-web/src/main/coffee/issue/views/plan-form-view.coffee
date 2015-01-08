define [
  'issue/views/action-options-view'
  'templates/issue'
], (
  ActionOptionsView
) ->

  $ = jQuery


  class extends ActionOptionsView
    template: Templates['issue-plan-form']


    getActionPlan: ->
      @model.get('actionPlan') || ''


    getActionPlanName: ->
      @model.get 'actionPlanName'


    selectInitialOption: ->
      @makeActive @getOptions().filter("[data-value=#{@getActionPlan()}]")


    selectOption: (e) ->
      actionPlan = $(e.currentTarget).data 'value'
      actionPlanName = $(e.currentTarget).data 'text'
      @submit actionPlan, actionPlanName
      super


    submit: (actionPlan, actionPlanName) ->
      _actionPlan = @getActionPlan()
      _actionPlanName = @getActionPlanName()
      return if actionPlan == _actionPlan
      p = window.process.addBackgroundProcess()
      if actionPlan == ''
        @model.set actionPlan: null, actionPlanName: null
      else
        @model.set actionPlan: actionPlan, actionPlanName: actionPlanName
      $.ajax
        type: 'POST'
        url: "#{baseUrl}/api/issues/plan"
        data:
          issue: @model.id
          plan: actionPlan
      .done =>
        window.process.finishBackgroundProcess p
      .fail =>
        @model.set assignee: _actionPlan, assigneeName: _actionPlanName
        window.process.failBackgroundProcess p


    getActionPlans: ->
      [{ key: '', name: t 'issue.unplanned' }].concat @collection.toJSON()


    serializeData: ->
      _.extend super,
        items: @getActionPlans()
