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
  'api-documentation/models/web-service-action-response'
  'api-documentation/views/api-documentation-action-response-view'
  'templates/api-documentation'
], (
  WebServiceActionResponse,
  ApiDocumentationActionResponseView
) ->

  class ApiDocumentationActionView extends Marionette.ItemView
    className: 'api-documentation-action'
    template: Templates['api-documentation-action']
    spinner: '<i class="spinner"></i>'

    ui:
      displayLink: '.example-response'

    fetchExampleResponse: (event) ->
      exampleResponse = new WebServiceActionResponse
        controller: @model.get('path').substring(0, @model.get('path').length - @model.get('key').length - 1)
        action: @model.get('key')
      @listenTo(exampleResponse, 'change', @appendExampleView)
      exampleResponse.fetch()

    appendExampleView: (model) ->
      @ui.displayLink.hide()
      exampleView = new ApiDocumentationActionResponseView
        model: model
      exampleView.render()
      @$el.append exampleView.$el

    events:
      'click .example-response': 'fetchExampleResponse'
