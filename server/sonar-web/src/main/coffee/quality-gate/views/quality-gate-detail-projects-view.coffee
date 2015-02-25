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
  'common/select-list'
  'templates/quality-gates'
], ->

  class QualityGateDetailProjectsView extends Marionette.ItemView
    template: Templates['quality-gate-detail-projects']


    onRender: ->
      unless @model.get('default')
        new SelectList
          el: @$('#select-list-projects')
          width: '100%'
          readOnly: !@options.app.canEdit
          focusSearch: false
          format: (item) -> item.name
          searchUrl: "#{baseUrl}/api/qualitygates/search?gateId=#{@options.gateId}"
          selectUrl: "#{baseUrl}/api/qualitygates/select"
          deselectUrl: "#{baseUrl}/api/qualitygates/deselect"
          extra:
            gateId: @options.gateId
          selectParameter: 'projectId'
          selectParameterValue: 'id'
          labels:
            selected: t('quality_gates.projects.with')
            deselected: t('quality_gates.projects.without')
            all: t('quality_gates.projects.all')
            noResults: t('quality_gates.projects.noResults')
          tooltips:
            select: t('quality_gates.projects.select_hint')
            deselect: t('quality_gates.projects.deselect_hint')

    serializeData: ->
      _.extend super, canEdit: @options.app.canEdit
