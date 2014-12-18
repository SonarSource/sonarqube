define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->

  $ = jQuery


  class extends BaseFacet
    template: Templates['issues-resolution-facet']


    onRender: ->
      super

      value = @options.app.state.get('query')['resolved']
      if value? && (!value || value == 'false')
        @$('.js-facet').filter("[data-unresolved]").addClass 'active'


    toggleFacet: (e) ->
      unresolved = $(e.currentTarget).is "[data-unresolved]"
      $(e.currentTarget).toggleClass 'active'
      if unresolved
        checked = $(e.currentTarget).is '.active'
        value = if checked then 'false' else null
        @options.app.state.updateFilter resolved: value, resolutions: null
      else
        @options.app.state.updateFilter resolved: null, resolutions: @getValue()


    disable: ->
      @options.app.state.updateFilter resolved: null, resolutions: null


    sortValues: (values) ->
      order = ['', 'FIXED', 'FALSE-POSITIVE', 'MUTED', 'REMOVED']
      _.sortBy values, (v) -> order.indexOf v.val

