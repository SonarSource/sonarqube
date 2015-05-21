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
  './custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    getUrl: ->
      q = @options.app.state.get 'contextComponentQualifier'
      if q == 'VW' || q == 'SVW'
        "#{baseUrl}/api/components/search"
      else
        "#{baseUrl}/api/resources/search?f=s2&q=TRK&display_uuid=true"


    prepareSearch: ->
      q = @options.app.state.get 'contextComponentQualifier'
      if q == 'VW' || q == 'SVW'
        @prepareSearchForViews()
      else super


    prepareSearchForViews: ->
      componentUuid = this.options.app.state.get 'contextComponentUuid'
      @$('.js-custom-value').select2
        placeholder: 'Search...'
        minimumInputLength: 2
        allowClear: false
        formatNoMatches: -> t 'select2.noMatches'
        formatSearching: -> t 'select2.searching'
        formatInputTooShort: -> tp 'select2.tooShort', 2
        width: '100%'
        ajax:
          quietMillis: 300
          url: @getUrl()
          data: (term, page) ->
            q: term
            componentUuid: componentUuid
            p: page
            ps: 25
          results: (data) ->
            more: data.p * data.ps < data.total,
            results: data.components.map (c) -> id: c.uuid, text: c.name


    getValuesWithLabels: ->
      values = @model.getValues()
      projects = @options.app.facets.projects
      values.forEach (v) =>
        uuid = v.val
        label = ''
        if uuid
          project = _.findWhere projects, uuid: uuid
          label = project.longName if project?
        v.label = label
      values


    serializeData: ->
      _.extend super,
        values: @sortValues @getValuesWithLabels()
