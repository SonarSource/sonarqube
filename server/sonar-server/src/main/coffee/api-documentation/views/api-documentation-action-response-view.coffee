define [
  'backbone.marionette',
  'templates/api-documentation'
], (
  Marionette,
  Templates
) ->

  class ApiDocumentationActionResponseView extends Marionette.ItemView
    tagName: 'div'
    className: 'example-response-content'
    template: Templates['api-documentation-action-response']
    spinner: '<i class="spinner"></i>'
