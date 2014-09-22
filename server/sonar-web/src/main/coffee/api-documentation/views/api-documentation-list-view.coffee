define [
  'backbone.marionette',
  'api-documentation/views/api-documentation-web-service-view'
], (
  Marionette,
  ApiDocumentationWebServiceView
) ->

  class ApiDocumentationListView extends Marionette.CollectionView
    tagName: 'ol'
    className: 'navigator-results-list'
    itemView: ApiDocumentationWebServiceView

    itemViewOptions: (model) ->
      app: @options.app
      highlighted: model.get('path') == @highlighted


    highlight: (path) ->
      @highlighted = path
      @render()
