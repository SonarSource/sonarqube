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
  'dashboard/collections/widgets'
  'dashboard/views/widgets-view'
  'dashboard/mockjax'
], (
  Widgets
  WidgetsView
) ->

  $ = jQuery
  App = new Marionette.Application()
  App.dashboard = window.did
  App.resource = window.resource
  App.state = new Backbone.Model configure: false


  App.saveDashboard = ->
    layout = @widgetsView.getLayout()
    data =
      did: App.dashboard.id
      layout: layout
    $.post "#{baseUrl}/api/dashboards/save", data


  App.addInitializer ->
    @widgetsView = new WidgetsView
      collection: @widgets
      dashboard: @dashboard
      el: $('#dashboard')
      app: @
    @widgetsView.render();


  requestDetails = ->
    $.get "#{baseUrl}/api/dashboards/show", key: App.dashboard, (data) ->
      App.dashboard = new Backbone.Model _.omit data, 'widgets'
      App.widgets = new Widgets data.widgets


  $.when(requestDetails(), window.requestMessages()).done -> App.start()

