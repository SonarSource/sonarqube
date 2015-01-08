define [
  'templates/api-documentation'
], ->

  class ApiDocumentationActionResponseView extends Marionette.ItemView
    tagName: 'div'
    className: 'example-response-content'
    template: Templates['api-documentation-action-response']
    spinner: '<i class="spinner"></i>'
