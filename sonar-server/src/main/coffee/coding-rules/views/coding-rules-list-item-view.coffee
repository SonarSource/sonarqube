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

      @options.app.layout.showSpinner 'detailsRegion'
      jQuery.ajax
        url: "#{baseUrl}/api/rules/show"
        data:
          key: @model.get('key')
          actives: true
      .done (r) =>
        @model.set r.rule
        @options.app.codingRulesQualityProfileActivationView.rule = @model
        @options.app.detailView = new CodingRulesDetailView
          app: @options.app
          model: @model
          actives: r.actives
        @options.app.layout.detailsRegion.show @options.app.detailView
