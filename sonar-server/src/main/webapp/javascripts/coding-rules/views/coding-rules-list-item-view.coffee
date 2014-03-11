define [
  'backbone.marionette',
  'coding-rules/views/coding-rules-detail-view',
  'common/handlebars-extensions'
], (
  Marionette,
  CodingRulesDetailView
) ->

  class CodingRulesListItemView extends Marionette.ItemView
    tagName: 'li'
    template: getTemplate '#coding-rules-list-item-template'
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
      _.extend super, qualityProfile: @options.app.getActiveQualityProfile()
