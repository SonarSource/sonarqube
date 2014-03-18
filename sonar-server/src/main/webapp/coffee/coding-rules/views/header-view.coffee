define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette
) ->

  class CodingRulesHeaderView extends Marionette.ItemView
    template: getTemplate '#coding-rules-header-template'


    events:
      'click #coding-rules-new-search': 'newSearch'


    newSearch: ->
      @options.app.router.navigate '', trigger: true
