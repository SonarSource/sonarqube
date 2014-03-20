define [
  'backbone.marionette',
  'coding-rules/views/coding-rules-detail-view',
  'templates/coding-rules'
], (
  Marionette,
  CodingRulesDetailView,
  Templates
) ->

  class CodingRulesListItemView extends Marionette.ItemView
    tagName: 'li'
    template: Templates['coding-rules-list-item']
    activeClass: 'active'


    events: ->
      'click': 'showDetail'


    showDetail: ->
      @$el.siblings().removeClass @activeClass
      @$el.addClass @activeClass

      @options.app.layout.showSpinner 'detailsRegion'
      jQuery.ajax
        url: "#{baseUrl}/api/codingrules/show"
      .done (r) =>
        @model.set r.codingrule
        detailView = new CodingRulesDetailView
          app: @options.app
          model: @model
        @options.app.layout.detailsRegion.show detailView


    serializeData: ->
      _.extend super,
        qualityProfile: @options.app.getActiveQualityProfile()
        qualityProfileName: @options.app.activeInFilter.view.renderValue()
