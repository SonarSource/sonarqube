define [
  'api-documentation/views/api-documentation-web-service-view'
], (
  ApiDocumentationWebServiceView
) ->

  class extends Marionette.CollectionView
    className: 'list-group'
    itemView: ApiDocumentationWebServiceView


    itemViewOptions: (model) ->
      app: @options.app
      highlighted: model.get('path') == @highlighted


    highlight: (path) ->
      @highlighted = path
      @render()
