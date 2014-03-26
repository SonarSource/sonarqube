define [
  'backbone.marionette'
  'templates/coding-rules'
], (
  Marionette,
  Templates
) ->

  class CodingRulesFacetsView extends Marionette.ItemView
    template: Templates['coding-rules-facets']


    ui:
      facets: '.navigator-facets-list-item'
      options: '.navigator-facets-list-item-option'


    events:
      'click @ui.options': 'selectOption'


    selectOption: (e) ->
      option = jQuery(e.currentTarget)
      option.toggleClass 'active'
      @applyOptions()


    applyOptions: ->
      @options.app.fetchFirstPage true


    getQuery: ->
      q = {}
      @ui.facets.each ->
        property = jQuery(@).data 'property'
        activeOptions = jQuery(@).find('.active').map(-> jQuery(@).data 'key').get()
        q[property] = activeOptions.join ',' if activeOptions.length > 0
      q
