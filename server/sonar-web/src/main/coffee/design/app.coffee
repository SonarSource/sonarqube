#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

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
