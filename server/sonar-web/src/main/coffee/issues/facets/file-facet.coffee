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

  $ = jQuery


  class extends BaseFacet
    template: Templates['issues-file-facet']


    onRender: ->
      super
      maxValueWidth = _.max @$('.facet-stat').map(-> $(@).outerWidth()).get()
      @$('.facet-name').css 'padding-right', maxValueWidth


    getValuesWithLabels: ->
      values = @model.getValues()
      source = @options.app.facets.components
      values.forEach (v) =>
        key = v.val
        label = null
        if key
          item = _.findWhere source, uuid: key
          label = item.longName if item?
        v.label = label
      values


    serializeData: ->
      _.extend super,
        values: @sortValues @getValuesWithLabels()
