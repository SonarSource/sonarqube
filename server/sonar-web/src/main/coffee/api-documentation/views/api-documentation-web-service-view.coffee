define [
  'api-documentation/collections/web-service-actions'
  'api-documentation/views/api-documentation-actions-list-view'
  'templates/api-documentation'
], (
  WebServiceActions,
  ApiDocumentationActionsListView
) ->

  class extends Marionette.ItemView
    tagName: 'a'
    className: 'list-group-item'
    template: Templates['api-documentation-web-service']


    modelEvents:
      'change': 'render'


    events:
      'click': 'showWebService'


    onRender: ->
      @$el.toggleClass 'active', @options.highlighted


    showWebService: ->
      @options.app.router.navigate "#{@model.get('path')}", trigger: true
