define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
) ->


  class extends BaseFacet
    template: Templates['issues-context-facet']


    serializeData: ->
      _.extend super,
          state: @options.app.state.toJSON()
