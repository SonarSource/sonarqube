requirejs.config
  baseUrl: "#{baseUrl}/js"

  paths:
    'jquery': 'third-party/jquery'
    'backbone': 'third-party/backbone'
    'backbone.marionette': 'third-party/backbone.marionette'
    'handlebars': 'third-party/handlebars'
    'moment': 'third-party/moment'

  shim:
    'backbone.marionette':
      deps: ['backbone']
      exports: 'Marionette'
    'backbone':
      exports: 'Backbone'
    'handlebars':
      exports: 'Handlebars'
    'moment':
      exports: 'moment'


requirejs [
  'backbone', 'backbone.marionette', 'handlebars',
  'api-documentation/collections/web-services',
  'api-documentation/views/api-documentation-list-view',
  'api-documentation/layout',
  'common/handlebars-extensions'
], (
  Backbone, Marionette, Handlebars,
  WebServices,
  ApiDocumentationListView,
  ApiDocumentationLayout
) ->

  # Create a Quality Gate Application
  App = new Marionette.Application


  App.webServices = new WebServices

  # Construct layout
  App.addInitializer ->
    @layout = new ApiDocumentationLayout app: @
    jQuery('#body').append @layout.render().el

  # Construct sidebar
  App.addInitializer ->
    @apiDocumentationListView = new ApiDocumentationListView
      collection: @webServices
      app: @
    @layout.resultsRegion.show @apiDocumentationListView

  webServicesXHR = App.webServices.fetch()

  jQuery.when(webServicesXHR)
    .done ->
      # Remove the initial spinner
      jQuery('#api-documentation-page-loader').remove()

      # Start the application
      App.start()
