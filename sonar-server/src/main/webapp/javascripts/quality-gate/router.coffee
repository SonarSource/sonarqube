define [
  'backbone',
  'models/quality-gate',
  'views/quality-gate-detail-view',
  'views/quality-gate-new-view'
], (
  Backbone,
  QualityGate,
  QualityGateDetailView,
  QualityGateNewView
) ->

  class QualityGateRouter extends Backbone.Router

    routes:
      'show/:id': 'show'
      'new': 'new'


    initialize: (options) ->
      @app = options.app


    show: (id) ->
      qualityGate = @app.qualityGates.get id
      if qualityGate
        @app.qualityGateSidebarListView.highlight id
        qualityGateDetailView = new QualityGateDetailView
          app: @app
          model: qualityGate
        @app.contentRegion.show qualityGateDetailView
        qualityGateDetailView.$el.addClass 'navigator-fetching'
        qualityGate.fetch().done ->
          qualityGateDetailView.$el.removeClass 'navigator-fetching'


    new: ->
      qualityGateNewView = new QualityGateNewView
        app: @app
        model: new QualityGate
      @app.contentRegion.show qualityGateNewView
