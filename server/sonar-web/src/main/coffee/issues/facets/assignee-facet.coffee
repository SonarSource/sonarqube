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


    onRender: ->
      super

      value = @options.app.state.get('query')['assigned']
      if value? && (!value || value == 'false')
        @$('.js-issues-facet').filter("[data-unassigned]").addClass 'active'


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


    serializeData: ->
      _.extend super,
        values: @getValuesWithLabels()
