define [
  'backbone.marionette',
  'templates/api-documentation',
  'api-documentation/collections/web-service-actions'
  'api-documentation/views/api-documentation-actions-list-view'
], (
  Marionette,
  Templates,
  WebServiceActions,
  ApiDocumentationActionsListView
) ->

  class ApiDocumentationWebServiceView extends Marionette.Layout
    tagName: 'tr'
    template: Templates['api-documentation-web-service']
    spinner: '<i class="spinner"></i>'

    regions:
      actionsRegion: '.web-service-actions'

    onRender: ->
      @showActions()


    showActions: ->
      actions = new WebServiceActions @model.get('actions')
      view = new ApiDocumentationActionsListView
        app: @options.app
        collection: actions
        webService: @model
      @actionsRegion.show view
