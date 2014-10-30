define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->

  $ = jQuery


  class extends BaseFacet
    template: Templates['issues-assignee-facet']


    events: ->
      _.extend super,
        'change .js-issues-custom-value': 'addCustomValue'


    onRender: ->
      super

      value = @options.app.state.get('query')['assigned']
      if value? && (!value || value == 'false')
        @$('.js-issues-facet').filter("[data-unassigned]").addClass 'active'

      @$('.js-issues-custom-value').select2
        placeholder: 'Add assignee'
        minimumInputLength: 2
        allowClear: false
        formatNoMatches: -> t 'select2.noMatches'
        formatSearching: -> t 'select2.searching'
        formatInputTooShort: -> tp 'select2.tooShort', 2
        width: '100%'
        ajax:
          quietMillis: 300
          url: "#{baseUrl}/api/users/search?f=s2"
          data: (term, page) -> { s: term, p: page }
          results: (data) -> { more: data.more, results: data.results }


    toggleFacet: (e) ->
      unassigned = $(e.currentTarget).is "[data-unassigned]"
      $(e.currentTarget).toggleClass 'active'
      if unassigned
        checked = $(e.currentTarget).is '.active'
        value = if checked then 'false' else null
        @options.app.state.updateFilter assigned: value, assignees: null
      else
        @options.app.state.updateFilter assigned: null, assignees: @getValue()


    getValuesWithLabels: ->
      values = @model.getValues()
      users = @options.app.facets.users
      values.forEach (v) =>
        login = v.val
        name = ''
        if login
          user = _.findWhere users, login: login
          name = user.name if user?
        v.label = name
      values


    disable: ->
      @options.app.state.updateFilter assigned: null, assignees: null


    addCustomValue: ->
      property = @model.get 'property'
      customValue = @$('.js-issues-custom-value').select2 'val'
      value = @getValue()
      value += ',' if value.length > 0
      value += customValue
      obj = {}
      obj[property] = value
      obj.assigned = null
      @options.app.state.updateFilter obj


    serializeData: ->
      _.extend super,
        values: @getValuesWithLabels()
