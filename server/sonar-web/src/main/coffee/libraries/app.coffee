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
