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
  'issues/facets/custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    prepareSearch: ->
      url = "#{baseUrl}/api/issues/tags?ps=10"
      tags = @options.app.state.get('query').tags
      if tags?
        url += "&tags=#{tags}"
      @$('.js-custom-value').select2
        placeholder: 'Search...'
        minimumInputLength: 0
        allowClear: false
        formatNoMatches: -> t 'select2.noMatches'
        formatSearching: -> t 'select2.searching'
        width: '100%'
        ajax:
          quietMillis: 300
          url: url
          data: (term, page) -> { q: term, ps: 10 }
          results: (data) ->
            results = data.tags.map (tag) ->
              id: tag, text: tag
            { more: false, results: results }


    getValuesWithLabels: ->
      values = @model.getValues()
      tags = @options.app.facets.tags
      values.forEach (v) =>
        v.label = v.val
        v.extra = ''
      values


    serializeData: ->
      _.extend super,
        values: @sortValues @getValuesWithLabels()
