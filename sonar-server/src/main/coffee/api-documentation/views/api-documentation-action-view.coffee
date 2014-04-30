define [
  'backbone.marionette',
  'templates/api-documentation'
], (
  Marionette,
  Templates
) ->

  class ApiDocumentationActionView extends Marionette.ItemView
    tagName: 'div'
    className: 'api-documentation-action'
    template: Templates['api-documentation-action']
    spinner: '<i class="spinner"></i>'

