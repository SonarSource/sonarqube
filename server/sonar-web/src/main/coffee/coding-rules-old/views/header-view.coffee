define [
  'backbone.marionette',
  'templates/coding-rules-old'
], (
  Marionette,
  Templates
) ->

  class CodingRulesHeaderView extends Marionette.ItemView
    template: Templates['coding-rules-header']


    events:
      'click #coding-rules-new-search': 'newSearch'
      'click #coding-rules-create-rule': 'createRule'


    newSearch: ->
      @options.app.router.emptyQuery()


    createRule: ->
      @options.app.createManualRule()


    serializeData: ->
      _.extend super,
        'canWrite': @options.app.canWrite
