define [
  'backbone.marionette',
  'api-documentation/views/api-documentation-web-service-view'
], (
  Marionette,
  ApiDocumentationWebServiceView
) ->

  class ApiDocumentationListView extends Marionette.CollectionView
    tagName: 'table'
    className: 'web-services-list'
    itemView: ApiDocumentationWebServiceView


    itemViewOptions: (model) ->
      app: @options.app
