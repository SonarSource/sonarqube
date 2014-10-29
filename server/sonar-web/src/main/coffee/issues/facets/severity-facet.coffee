define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->


  class extends BaseFacet
    template: Templates['issues-severity-facet']


    sortValues: (values) ->
      order = ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR']
      _.sortBy values, (v) -> order.indexOf v.val


    serializeData: ->
      _.extend super,
        values: @sortValues @model.getValues()

