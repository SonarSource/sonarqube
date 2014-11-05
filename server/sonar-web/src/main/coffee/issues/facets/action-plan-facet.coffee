define [
  'issues/facets/base-facet'
], (
  BaseFacet
) ->


  class extends BaseFacet

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


    serializeData: ->
      _.extend super,
        values: @getValuesWithLabels()
