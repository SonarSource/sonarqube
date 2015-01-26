define [
  'components/navigator/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
) ->

  class extends BaseFacet
    template: Templates['issues-base-facet']

    onRender: ->
      super
      @$('[data-toggle="tooltip"]').tooltip container: 'body'


    onClose: ->
      @$('[data-toggle="tooltip"]').tooltip 'destroy'
