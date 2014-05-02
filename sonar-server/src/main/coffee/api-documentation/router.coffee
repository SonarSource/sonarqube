define [
  'backbone',
  'api-documentation/collections/web-service-actions',
  'api-documentation/views/api-documentation-actions-list-view',
], (
  Backbone,
  WebServiceActions,
  ApiDocumentationActionsListView
) ->

  class ApiDocumentationRouter extends Backbone.Router

    routes:
      '*path': 'show'


    initialize: (options) ->
      @app = options.app


    show: (path) ->
      webService = @app.webServices.get path
      if webService
        @app.apiDocumentationListView.highlight path

        actions = new WebServiceActions webService.get('actions')
        actionsListView = new ApiDocumentationActionsListView
          app: @app
          collection: actions
          model: webService

        @app.layout.detailsRegion.show actionsListView
