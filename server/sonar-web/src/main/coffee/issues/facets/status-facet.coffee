define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
) ->


  class extends BaseFacet
    template: Templates['issues-status-facet']


    sortValues: (values) ->
      order = ['OPEN', 'REOPENED', 'CONFIRMED', 'RESOLVED', 'CLOSED']
      _.sortBy values, (v) -> order.indexOf v.val
