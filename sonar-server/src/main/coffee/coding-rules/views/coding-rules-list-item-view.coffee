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

      @options.app.showRule @model.get('key')
