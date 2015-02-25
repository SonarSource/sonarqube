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
  'api-documentation/collections/web-services',
  'api-documentation/views/api-documentation-list-view',
  'api-documentation/router',
  'api-documentation/layout'
], (
  WebServices,
  ApiDocumentationListView,
  ApiDocumentationRouter,
  ApiDocumentationLayout
) ->

  # Create a Quality Gate Application
  App = new Marionette.Application

  App.webServices = new WebServices

  App.openFirstWebService = ->
    if @webServices.length > 0
      @router.navigate "#{@webServices.models[0].get('path')}", trigger: true
    else
      App.layout.detailsRegion.reset()

  App.refresh = ->
    App.apiDocumentationListView = new ApiDocumentationListView
      collection: App.webServices
      app: App
    App.layout.resultsRegion.show App.apiDocumentationListView
    if (Backbone.history.fragment)
      App.router.show Backbone.history.fragment, trigger: true

  # Construct layout
  App.addInitializer ->
    @layout = new ApiDocumentationLayout app: App
    jQuery('#api-documentation').append @layout.render().el
    jQuery('#footer').addClass 'search-navigator-footer'

  # Construct sidebar
  App.addInitializer ->
    App.refresh()

  # Start router
  App.addInitializer ->
    @router = new ApiDocumentationRouter app: @
    Backbone.history.start()

  # Open first Web Service when page is opened
  App.addInitializer ->
    initial = Backbone.history.fragment == ''
    App.openFirstWebService() if initial

  webServicesXHR = App.webServices.fetch()

  jQuery.when(webServicesXHR)
    .done ->
      # Remove the initial spinner
      jQuery('#api-documentation-page-loader').remove()

      # Start the application
      App.start()
