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

define [
  'api-documentation/collections/web-service-actions',
  'api-documentation/views/api-documentation-actions-list-view',
], (
  WebServiceActions,
  ApiDocumentationActionsListView
) ->

  class ApiDocumentationRouter extends Backbone.Router

    routes:
      '*path': 'show'


    initialize: (options) ->
      @app = options.app


    show: (path) ->
      webService = @app.webServices.get path
      if webService
        @app.apiDocumentationListView.highlight path

        actions = new WebServiceActions webService.get('actions'), path: path
        actionsListView = new ApiDocumentationActionsListView
          app: @app
          collection: actions
          model: webService

        @app.layout.detailsRegion.show actionsListView
