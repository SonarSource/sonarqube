define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->

  $ = jQuery


  class extends BaseFacet
    template: Templates['issues-creation-date-facet']


    events: ->
      _.extend super,
        'change input': 'applyFacet'


    onRender: ->
      @$el.toggleClass 'search-navigator-facet-box-collapsed', !@model.get('enabled')

      @$('input').datepicker
        dateFormat: 'yy-mm-dd'
        changeMonth: true
        changeYear: true

      props = ['createdAfter', 'createdBefore', 'createdAt']
      query = @options.app.state.get 'query'
      props.forEach (prop) =>
        value = query[prop]
        @$("input[name=#{prop}]").val value if value?


    applyFacet: ->
      obj = {}
      @$('input').each ->
        property = $(@).prop 'name'
        value = $(@).val()
        obj[property] = value
      @options.app.state.updateFilter obj


    disable: ->
      @options.app.state.updateFilter createdAfter: null, createdBefore: null, createdAt: null


    serializeData: ->
      _.extend super,
        createdAt: @options.app.state.get('query').createdAt
