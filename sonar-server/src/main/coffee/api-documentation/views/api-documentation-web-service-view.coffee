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

  class ApiDocumentationWebServiceView extends Marionette.ItemView
    tagName: 'li'
    template: Templates['api-documentation-web-service']
    spinner: '<i class="spinner"></i>'

    modelEvents:
      'change': 'render'


    events:
      'click': 'showWebService'


    onRender: ->
      @$el.toggleClass 'active', @options.highlighted


    showWebService: ->
      @options.app.router.navigate "#{@model.get('path')}", trigger: true
