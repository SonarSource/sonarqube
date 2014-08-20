requirejs.config
  baseUrl: "#{baseUrl}/js"

  paths:
    'backbone': 'third-party/backbone'
    'backbone.marionette': 'third-party/backbone.marionette'
    'handlebars': 'third-party/handlebars'

  shim:
    'backbone.marionette':
      deps: ['backbone']
      exports: 'Marionette'
    'backbone':
      exports: 'Backbone'
    'handlebars':
      exports: 'Handlebars'


requirejs [
  'backbone', 'backbone.marionette'
  'libraries/view'
  'common/handlebars-extensions'
], (
  Backbone, Marionette
  LibrariesView
) ->

  $ = jQuery
  RESOURCES_URL = "#{baseUrl}/api/resources"
  DEPENDENCY_TREE_URL = "#{baseUrl}/api/dependency_tree"
  App = new Marionette.Application


  App.addInitializer ->
    $.get RESOURCES_URL, resource: window.resourceKey, scopes: 'PRJ', depth: -1, (rawData) ->
      components = new Backbone.Collection rawData
      requests = components.map (component) ->
        id = component.get 'id'
        $.get DEPENDENCY_TREE_URL, resource: id, scopes: 'PRJ', (data) ->
          component.set 'libraries', data

      $.when.apply($, requests).done =>
        @view = new LibrariesView app: @, collection: components
        $('#project-libraries').empty().append @view.render().el


  # Message bundles
  l10nXHR = window.requestMessages()


  jQuery.when(l10nXHR).done -> App.start()
