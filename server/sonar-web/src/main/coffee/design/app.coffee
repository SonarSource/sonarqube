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
    $.get RESOURCES_URL, resource: window.resourceKey, metrics: 'dsm', (rawData) ->
      data = JSON.parse rawData[0].msr[0].data
      data.forEach (row, rowIndex) ->
        row.v.forEach (cell, columnIndex) ->
          if cell.w? && cell.w > 0
            if rowIndex < columnIndex
              cell.status = 'cycle'
            else
              cell.status = 'dependency'
      @view = new DesignView app: @, collection: new Backbone.Collection data
      $('#project-design').empty().append @view.render().el


  # Message bundles
  l10nXHR = window.requestMessages()


  jQuery.when(l10nXHR).done -> App.start()
