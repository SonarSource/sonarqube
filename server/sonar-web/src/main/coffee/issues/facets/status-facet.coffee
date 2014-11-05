define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->


  class extends BaseFacet
    template: Templates['issues-status-facet']


    sortValues: (values) ->
      order = ['OPEN', 'RESOLVED', 'REOPENED', 'CLOSED', 'CONFIRMED']
      _.sortBy values, (v) -> order.indexOf v.val
