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


    initialize: ->
      super()
      that = @
      @options.collection.each (facet) ->
        property = facet.get 'property'
        facet.set 'property_message', 'coding_rules.facets.' + property
        _.each(facet.get('values'), (value) ->
          value.text = that.options.app.facetLabel(property, value.val)
        )

    selectOption: (e) ->
      option = jQuery(e.currentTarget)
      option.toggleClass 'active'
      @applyOptions()


    applyOptions: ->
      @options.app.fetchFirstPage true


    getQuery: ->
      q = {}
      if @ui.facets.each
        @ui.facets.each ->
          property = jQuery(@).data 'property'
          activeOptions = jQuery(@).find('.active').map(-> jQuery(@).data 'key').get()
          q[property] = activeOptions.join ',' if activeOptions.length > 0
      q
