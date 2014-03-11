define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette,
) ->

  class CodingRulesDetailView extends Marionette.ItemView
    template: getTemplate '#coding-rules-detail-template'


    onRender: ->
      qp = @options.app.getActiveQualityProfile()
      @$('.coding-rules-detail-quality-profile').first().addClass 'active' if qp?
