define [
  'backbone.marionette',
  'templates/coding-rules'
], (
  Marionette,
  Templates
) ->

  class CodingRulesHeaderView extends Marionette.ItemView
    template: Templates['coding-rules-header']


    events:
      'click #coding-rules-new-search': 'newSearch'


    newSearch: ->
      @options.app.router.navigate '', trigger: true
