define [
  'backbone.marionette'
  'templates/coding-rules-old'
], (
  Marionette,
  Templates
) ->

  class CodingRulesFacetsView extends Marionette.ItemView
    template: Templates['coding-rules-facets']


    ui:
      facets: '.navigator-facets-list-item'
      options: '.facet'


    events:
      'click @ui.options': 'selectOption'


    initialize: ->
      super()
      that = @
      @options.collection.each (facet) ->
        property = facet.get 'property'
        facet.set 'property_message', t 'coding_rules.facets.' + property
        facet.set 'limitReached', facet.get('values').length >= 10
        _.each(facet.get('values'), (value) ->
          value.text = that.options.app.facetLabel(property, value.val)
        )

    selectOption: (e) ->
      option = jQuery(e.currentTarget)
      option.toggleClass 'active'
      property = option.closest('.navigator-facets-list-item').data('property')
      value = option.data('key')
      @options.app.filterBarView.toggle(property, value)
      @applyOptions()


    applyOptions: ->
      @options.app.fetchFirstPage()


    restoreFromQuery: (params) ->
      @ui.options.each ->
        jQuery(@).removeClass('active')
      @ui.facets.each ->
        property = jQuery(@).data 'property'
        if !!params[property]
          _(params[property].split(',')).map (value) ->
            jQuery('.navigator-facets-list-item[data-property="' + property + '"] .facet[data-key="' + value + '"]').addClass 'active'
