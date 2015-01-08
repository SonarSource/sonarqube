define [
  'components/navigator/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
) ->

  class extends BaseFacet
    template: Templates['issues-base-facet']
