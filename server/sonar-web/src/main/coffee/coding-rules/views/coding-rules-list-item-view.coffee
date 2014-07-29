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
      @options.listView.selectIssue @$el
      @options.app.showRule @model.get('key')


    serializeData: ->
      _.extend super,
        allTags: _.union @model.get('sysTags'), @model.get('tags')
