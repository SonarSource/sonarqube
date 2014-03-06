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
      @model.fetch().done =>
        detailView = new CodingRulesDetailView model: @model
        @options.app.layout.detailsRegion.show detailView
