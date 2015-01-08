define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
) ->


  class extends BaseFacet
    template: Templates['issues-issue-key-facet']


    onRender: ->
      @$el.toggleClass 'hidden', !@options.app.state.get('query').issues


    disable: ->
      @options.app.state.updateFilter issues: null


    serializeData: ->
      _.extend super,
        issues: @options.app.state.get('query').issues
