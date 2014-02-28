define [
  'backbone',
  'quality-gate/models/quality-gate',
  'quality-gate/views/quality-gate-detail-view',
  'quality-gate/views/quality-gate-detail-header-view',
  'quality-gate/views/quality-gate-new-view'
], (
  Backbone,
  QualityGate,
  QualityGateDetailView,
  QualityGateDetailHeaderView,
  QualityGateNewView
) ->

  class QualityGateRouter extends Backbone.Router

    routes:
      'show/:id': 'show'


    initialize: (options) ->
      @app = options.app


    show: (id) ->
      qualityGate = @app.qualityGates.get id
      if qualityGate
        @app.qualityGateSidebarListView.highlight id

        qualityGateDetailHeaderView = new QualityGateDetailHeaderView
          app: @app
          model: qualityGate
        @app.headerRegion.show qualityGateDetailHeaderView

        qualityGateDetailView = new QualityGateDetailView
          app: @app
          model: qualityGate
        @app.detailsRegion.show qualityGateDetailView
        qualityGateDetailView.$el.hide()

        qualityGateDetailHeaderView.showSpinner()
        qualityGate.fetch().done ->
          qualityGateDetailView.$el.show()
          qualityGateDetailHeaderView.hideSpinner()
