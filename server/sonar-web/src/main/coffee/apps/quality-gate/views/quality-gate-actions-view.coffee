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
  '../models/quality-gate'
  '../templates'
], (
  QualityGate
) ->

  class QualityGateActionsView extends Marionette.ItemView
    template: Templates['quality-gate-actions']


    events:
      'click #quality-gate-add': 'add'


    add: ->
      qualityGate = new QualityGate()
      @options.app.qualityGateEditView.method = 'create'
      @options.app.qualityGateEditView.model = qualityGate
      @options.app.qualityGateEditView.show()


    serializeData: ->
      _.extend super, canEdit: @options.app.canEdit
