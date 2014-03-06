define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette
) ->

  class CodingRulesStatusView extends Marionette.ItemView
    template: getTemplate '#coding-rules-status-template'


    collectionEvents:
      'all': 'render'


    serializeData: ->
      _.extend super,
        paging: @collection.paging
