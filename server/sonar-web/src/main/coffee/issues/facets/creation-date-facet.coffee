define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
) ->

  $ = jQuery


  class extends BaseFacet
    template: Templates['issues-creation-date-facet']


    events: ->
      _.extend super,
        'change input': 'applyFacet'
        'click .js-select-period-start': 'selectPeriodStart'
        'click .js-select-period-end': 'selectPeriodEnd'


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

      @$('.js-barchart').barchart @model.getValues()

      @$('select').select2
        width: '100%'
        minimumResultsForSearch: 999


    selectPeriodStart: ->
      @$('.js-period-start').datepicker 'show'


    selectPeriodEnd: ->
      @$('.js-period-end').datepicker 'show'



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
        periodStart: @options.app.state.get('query').createdAfter
        periodEnd: @options.app.state.get('query').createdBefore
        createdAt: @options.app.state.get('query').createdAt
