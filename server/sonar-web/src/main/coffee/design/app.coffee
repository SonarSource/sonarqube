requirejs.config
  baseUrl: "#{baseUrl}/js"


requirejs [
  'design/view'
], (
  DesignView
) ->

  $ = jQuery
  RESOURCES_URL = "#{baseUrl}/api/resources"
  App = new Marionette.Application


  App.noDataAvailable = ->
    message = t 'design.noData'
    $('#project-design').html "<p class=\"message-alert\"><i class=\"icon-alert-warn\"></i> #{message}</p>"


  App.addInitializer ->
    packageTangles = {}

    packageTanglesXHR = $.get RESOURCES_URL, resource: window.resourceKey, depth: 1, metrics: 'package_tangles', (data) ->
      data.forEach (component) ->
        packageTangles[component.id] = component.msr[0].frmt_val

    dsmXHR = $.get RESOURCES_URL, resource: window.resourceKey, metrics: 'dsm'
    dsmXHR.fail -> App.noDataAvailable()

    $.when(packageTanglesXHR, dsmXHR).done ->
      rawData = dsmXHR.responseJSON
      unless _.isArray(rawData) && rawData.length == 1 && _.isArray(rawData[0].msr)
        App.noDataAvailable()
        return
      data = JSON.parse rawData[0].msr[0].data
      data.forEach (row, rowIndex) ->
        row.v.forEach (cell, columnIndex) ->
          if cell.w? && cell.w > 0
            if rowIndex < columnIndex
              cell.status = 'cycle'
            else
              cell.status = 'dependency'
      data = data.map (row) ->
        _.extend row, empty: row.q == 'DIR' && row.v.every (item) -> !item.w?
      collection = new Backbone.Collection data
      collection.forEach (model) ->
        model.set 'pt', packageTangles[model.get 'i']
      @view = new DesignView app: @, collection: collection
      $('#project-design').empty().append @view.render().el


  # Message bundles
  l10nXHR = window.requestMessages()


  jQuery.when(l10nXHR).done -> App.start()
