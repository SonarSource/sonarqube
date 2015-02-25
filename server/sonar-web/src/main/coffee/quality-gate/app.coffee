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
  'quality-gate/collections/quality-gates',
  'quality-gate/views/quality-gate-sidebar-list-view',
  'quality-gate/views/quality-gate-actions-view',
  'quality-gate/views/quality-gate-edit-view',
  'quality-gate/router',
  'quality-gate/layout'
], (
  QualityGates,
  QualityGateSidebarListItemView,
  QualityGateActionsView,
  QualityGateEditView,
  QualityGateRouter,
  QualityGateLayout
) ->

  # Create a Quality Gate Application
  App = new Marionette.Application


  App.qualityGates = new QualityGates


  App.openFirstQualityGate = ->
    if @qualityGates.length > 0
      @router.navigate "show/#{@qualityGates.models[0].get('id')}", trigger: true
    else
      App.layout.detailsRegion.reset()


  App.deleteQualityGate = (id) ->
    App.qualityGates.remove id
    App.openFirstQualityGate()


  App.unsetDefaults = (id) ->
    App.qualityGates.each (gate) ->
      gate.set('default', false) unless gate.id == id


  # Construct layout
  App.addInitializer ->
    @layout = new QualityGateLayout app: @
    jQuery('#quality-gates').append @layout.render().el
    jQuery('#footer').addClass 'search-navigator-footer'


  # Construct actions bar
  App.addInitializer ->
    @codingRulesHeaderView = new QualityGateActionsView
      app: @
    @layout.actionsRegion.show @codingRulesHeaderView


  # Construct sidebar
  App.addInitializer ->
    @qualityGateSidebarListView = new QualityGateSidebarListItemView
      collection: @qualityGates
      app: @
    @layout.resultsRegion.show @qualityGateSidebarListView


  # Construct edit view
  App.addInitializer ->
    @qualityGateEditView = new QualityGateEditView app: @
    @qualityGateEditView.render()


  # Start router
  App.addInitializer ->
    @router = new QualityGateRouter app: @
    Backbone.history.start()


  # Open first quality gate when come to the page
  App.addInitializer ->
    initial = Backbone.history.fragment == ''
    App.openFirstQualityGate() if initial


  # Call app, Load metrics and the list of quality gates before start the application
  appXHR = jQuery.ajax
    url: "#{baseUrl}/api/qualitygates/app"
  .done (r) =>
      App.canEdit = r.edit
      App.periods = r.periods
      App.metrics = r.metrics

  qualityGatesXHR = App.qualityGates.fetch()

  # Message bundles
  l10nXHR = window.requestMessages()

  jQuery.when(qualityGatesXHR, appXHR, l10nXHR)
    .done ->
      # Start the application
      App.start()
