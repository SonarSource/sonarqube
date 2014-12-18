define [
  'backbone.marionette',
  'coding-rules-old/views/coding-rules-detail-view',
  'templates/coding-rules-old'
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
      tags = _.union @model.get('sysTags'), @model.get('tags')
      _.extend super,
        manualRuleLabel: t 'coding_rules.manual_rule'
        allTags: tags
        showDetails: (@model.get('status') != 'READY') || (_.isArray(tags) && tags.length > 0)
