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
  'quality-gate/collections/conditions',
  'quality-gate/views/quality-gate-detail-header-view',
  'quality-gate/views/quality-gate-detail-conditions-view',
  'quality-gate/views/quality-gate-detail-projects-view'
  'templates/quality-gates',
], (
  Conditions,
  QualityGateDetailHeaderView,
  QualityGateDetailConditionsView,
  QualityGateDetailProjectsView
) ->

  class QualityGateDetailView extends Marionette.Layout
    template: Templates['quality-gate-detail']


    regions:
      conditionsRegion: '#quality-gate-conditions'
      projectsRegion: '#quality-gate-projects'


    modelEvents:
      'change': 'render'


    onRender: ->
      @showConditions()
      @showProjects()


    showConditions: ->
      conditions = new Conditions @model.get('conditions')
      view = new QualityGateDetailConditionsView
        app: @options.app
        collection: conditions
        gateId: @model.id
        qualityGate: @model
      @conditionsRegion.show view


    showProjects: ->
      view = new QualityGateDetailProjectsView
        app: @options.app
        model: @model
        gateId: @model.id
      @projectsRegion.show view
