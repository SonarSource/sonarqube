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
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
) ->


  class extends BaseFacet
    template: Templates['issues-custom-values-facet']


    events: ->
      _.extend super,
        'change .js-custom-value': 'addCustomValue'


    getUrl: ->


    onRender: ->
      super
      @prepareSearch()


    prepareSearch: ->
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
          data: (term, page) -> { s: term, p: page }
          results: (data) -> { more: data.more, results: data.results }


    addCustomValue: ->
      property = @model.get 'property'
      customValue = @$('.js-custom-value').select2 'val'
      value = @getValue()
      value += ',' if value.length > 0
      value += customValue
      obj = {}
      obj[property] = value
      @options.app.state.updateFilter obj
