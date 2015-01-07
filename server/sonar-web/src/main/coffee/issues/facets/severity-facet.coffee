define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
) ->


  class extends BaseFacet
    template: Templates['issues-severity-facet']


    sortValues: (values) ->
      order = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
      _.sortBy values, (v) -> order.indexOf v.val

