requirejs.config
  baseUrl: "#{baseUrl}/js"


requirejs [
  'libraries/view'
], (
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
        components.reset components.reject (model) ->
          (model.get('id') == window.resourceKey || model.get('key') == window.resourceKey) &&
              model.get('libraries').length == 0

        @view = new LibrariesView app: @, collection: components
        $('#project-libraries').empty().append @view.render().el


  # Message bundles
  l10nXHR = window.requestMessages()


  jQuery.when(l10nXHR).done -> App.start()
