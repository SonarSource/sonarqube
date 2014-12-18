define [
  'issues/facets/custom-values-facet'
  'templates/issues'
], (
  CustomValuesFacet
  Templates
) ->

  $ = jQuery


  class extends CustomValuesFacet
    template: Templates['issues-assignee-facet']


    getUrl: ->
      "#{baseUrl}/api/users/search?f=s2"


    onRender: ->
      super
      value = @options.app.state.get('query')['assigned']
      if value? && (!value || value == 'false')
        @$('.js-facet').filter("[data-unassigned]").addClass 'active'


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
      customValue = @$('.js-custom-value').select2 'val'
      value = @getValue()
      value += ',' if value.length > 0
      value += customValue
      obj = {}
      obj[property] = value
      obj.assigned = null
      @options.app.state.updateFilter obj


    sortValues: (values) ->
      # put "unassigned" first
      _.sortBy values, (v) ->
        x = if v.val == '' then -999999 else -v.count
        x


    serializeData: ->
      _.extend super,
        values: @sortValues @getValuesWithLabels()
