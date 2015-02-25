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
  'analysis-reports/router'
  'analysis-reports/layout'
  'analysis-reports/models/reports'
  'analysis-reports/views/reports-view'
  'analysis-reports/views/actions-view'
], (
  Router
  Layout
  Reports
  ReportsView
  ActionsView
) ->

  # Add html class to mark the page as navigator page
  jQuery('html').addClass 'navigator-page'


  # Create an Application
  App = new Marionette.Application


  App.fetchReports = ->
    fetch = if @state.get 'active' then @reports.fetchActive() else @reports.fetchHistory()
    @layout.showSpinner 'actionsRegion'
    @layout.resultsRegion.reset()
    fetch.done =>
      @state.set page: @reports.paging.page
      @reportsView = new ReportsView
        app: @
        collection: @reports
      @layout.resultsRegion.show @reportsView

      unless @state.get('active') || @reports.paging.maxResultsReached
        @reportsView.bindScrollEvents() unless @state.get 'active'

      @actionsView = new ActionsView
        app: @
        collection: @reports
      @layout.actionsRegion.show @actionsView


  App.fetchNextPage = ->
    @reports.fetchHistory
      data:
        p: @state.get('page') + 1
      remove: false
    .done =>
      @state.set page: @reports.paging.page


  App.addInitializer ->
    @state = new Backbone.Model()
    @state.on 'change:active', => @fetchReports()


  App.addInitializer ->
    @layout = new Layout app: @
    jQuery('#analysis-reports').empty().append @layout.render().el


  App.addInitializer ->
    @reports = new Reports()
    @router = new Router app: @
    Backbone.history.start()


  l10nXHR = window.requestMessages()
  jQuery.when(l10nXHR).done -> App.start()
