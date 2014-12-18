define [
  'components/navigator/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->

  class extends BaseFacet
    template: Templates['issues-base-facet']
