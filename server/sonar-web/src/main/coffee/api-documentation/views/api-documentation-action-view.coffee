define [
  'api-documentation/models/web-service-action-response'
  'api-documentation/views/api-documentation-action-response-view'
  'templates/api-documentation'
], (
  WebServiceActionResponse,
  ApiDocumentationActionResponseView
) ->

  class ApiDocumentationActionView extends Marionette.ItemView
    tagName: 'div'
    className: 'api-documentation-action'
    template: Templates['api-documentation-action']
    spinner: '<i class="spinner"></i>'

    ui:
      displayLink: '.example-response'

    fetchExampleResponse: (event) ->
      exampleResponse = new WebServiceActionResponse
        controller: @model.get('path').substring(0, @model.get('path').length - @model.get('key').length - 1)
        action: @model.get('key')
      @listenTo(exampleResponse, 'change', @appendExampleView)
      exampleResponse.fetch()

    appendExampleView: (model) ->
      @ui.displayLink.hide()
      exampleView = new ApiDocumentationActionResponseView
        model: model
      exampleView.render()
      @$el.append exampleView.$el

    events:
      'click .example-response': 'fetchExampleResponse'
