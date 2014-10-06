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
  'design/view'
  'common/handlebars-extensions'
], (
  Backbone, Marionette
  DesignView
) ->

  $ = jQuery
  RESOURCES_URL = "#{baseUrl}/api/resources"
  App = new Marionette.Application


  App.addInitializer ->
    packageTangles = {}

    packageTanglesXHR = $.get RESOURCES_URL, resource: window.resourceKey, depth: 1, metrics: 'package_tangles', (data) ->
      data.forEach (component) ->
        packageTangles[component.id] = component.msr[0].frmt_val

    dsmXHR = $.get RESOURCES_URL, resource: window.resourceKey, metrics: 'dsm'

    $.when(packageTanglesXHR, dsmXHR).done ->
      rawData = dsmXHR.responseJSON
      data = JSON.parse rawData[0].msr[0].data
      data.forEach (row, rowIndex) ->
        row.v.forEach (cell, columnIndex) ->
          if cell.w? && cell.w > 0
            if rowIndex < columnIndex
              cell.status = 'cycle'
            else
              cell.status = 'dependency'
      collection = new Backbone.Collection data
      collection.forEach (model) ->
        model.set 'pt', packageTangles[model.get 'i']
      @view = new DesignView app: @, collection: collection
      $('#project-design').empty().append @view.render().el


  # Message bundles
  l10nXHR = window.requestMessages()


  jQuery.when(l10nXHR).done -> App.start()
