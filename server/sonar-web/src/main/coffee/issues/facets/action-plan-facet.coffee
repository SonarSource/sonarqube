define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->

  $ = jQuery


  class extends BaseFacet
    template: Templates['issues-action-plan-facet']


    onRender: ->
      super
      value = @options.app.state.get('query')['planned']
      if value? && (!value || value == 'false')
        @$('.js-facet').filter("[data-unplanned]").addClass 'active'


    toggleFacet: (e) ->
      unplanned = $(e.currentTarget).is "[data-unplanned]"
      $(e.currentTarget).toggleClass 'active'
      if unplanned
        checked = $(e.currentTarget).is '.active'
        value = if checked then 'false' else null
        @options.app.state.updateFilter planned: value, actionPlans: null
      else
        @options.app.state.updateFilter planned: null, actionPlans: @getValue()


    getValuesWithLabels: ->
      values = @model.getValues()
      actionPlans = @options.app.facets.actionPlans
      values.forEach (v) =>
        key = v.val
        label = null
        if key
          actionPlan = _.findWhere actionPlans, key: key
          label = actionPlan.name if actionPlan?
        v.label = label
      values


    disable: ->
      @options.app.state.updateFilter planned: null, actionPlans: null


    serializeData: ->
      _.extend super,
        values: @getValuesWithLabels()
