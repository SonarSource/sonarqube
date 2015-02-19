define [
  'api-documentation/models/web-service-action'
  'api-documentation/views/api-documentation-action-view'
  'templates/api-documentation'
], (
  WebServiceAction,
  ApiDocumentationActionView
) ->

  $ = jQuery

  class extends Marionette.CompositeView
    className: 'api-documentation-actions'
    template: Templates['api-documentation-actions']
    itemView: ApiDocumentationActionView
    itemViewContainer: '.search-navigator-workspace-list'

    onRender: ->
      top = $('.search-navigator').offset().top
      @$('.search-navigator-workspace-header').css top: top
