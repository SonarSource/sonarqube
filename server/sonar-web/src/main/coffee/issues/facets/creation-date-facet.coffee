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

        'click .js-all': 'onAllClick'
        'click .js-last-week': 'onLastWeekClick'
        'click .js-last-month': 'onLastMonthClick'
        'click .js-last-year': 'onLastYearClick'


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

      values = @model.getValues()
      if _.isArray(values) && values.length > 0
        @$('.js-barchart').barchart values
      else
        @$('.js-barchart').addClass 'hidden'


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


    onAllClick: ->
      @disable()


    onLastWeekClick: ->
      createdAfter = moment().subtract(1, 'weeks').format 'YYYY-MM-DD'
      @options.app.state.updateFilter createdAfter: createdAfter, createdBefore: null, createdAt: null


    onLastMonthClick: ->
      createdAfter = moment().subtract(1, 'months').format 'YYYY-MM-DD'
      @options.app.state.updateFilter createdAfter: createdAfter, createdBefore: null, createdAt: null


    onLastYearClick: ->
      createdAfter = moment().subtract(1, 'years').format 'YYYY-MM-DD'
      @options.app.state.updateFilter createdAfter: createdAfter, createdBefore: null, createdAt: null


    serializeData: ->
      _.extend super,
        periodStart: @options.app.state.get('query').createdAfter
        periodEnd: @options.app.state.get('query').createdBefore
        createdAt: @options.app.state.get('query').createdAt
