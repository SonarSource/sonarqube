define [
  'backbone.marionette'
  'templates/issues'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class extends Marionette.ItemView
    className: 'issues-facet-box'
    template: Templates['issues-base-facet']


    modelEvents: ->
      'change': 'render'


    events: ->
      'click .js-issues-facet-toggle': 'toggle'
      'click .js-issues-facet': 'toggleFacet'


    onRender: ->
      console.log @model.id, @model.get 'enabled'
      @$el.toggleClass 'issues-facet-box-collapsed', !@model.get('enabled')

      property = @model.get 'property'
      value = @options.app.state.get('query')[property]
      if typeof value == 'string'
        value.split(',').forEach (s) =>
          @$('.js-issues-facet').filter("[data-value='#{s}']").addClass 'active'


    toggle: ->
      @model.toggle()


    getValue: ->
      @$('.js-issues-facet.active').map(-> $(@).data 'value').get().join()


    toggleFacet: (e) ->
      $(e.currentTarget).toggleClass 'active'
      property = @model.get 'property'
      value = @getValue()
      obj = {}
      obj[property] = value
      @options.app.state.updateFilter obj

