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
  'templates/issues'
], (
  CustomValuesFacet
) ->

  $ = jQuery


  class extends CustomValuesFacet
    template: Templates['issues-assignee-facet']


    getUrl: ->
      "#{baseUrl}/api/users/search?f=s2"


    onRender: ->
      super
      value = @options.app.state.get('query')['assigned']
      if value? && (!value || value == 'false')
        @$('.js-facet').filter("[data-unassigned]").addClass 'active'


    toggleFacet: (e) ->
      unassigned = $(e.currentTarget).is "[data-unassigned]"
      $(e.currentTarget).toggleClass 'active'
      if unassigned
        checked = $(e.currentTarget).is '.active'
        value = if checked then 'false' else null
        @options.app.state.updateFilter assigned: value, assignees: null
      else
        @options.app.state.updateFilter assigned: null, assignees: @getValue()


    getValuesWithLabels: ->
      values = @model.getValues()
      users = @options.app.facets.users
      values.forEach (v) =>
        login = v.val
        name = ''
        if login
          user = _.findWhere users, login: login
          name = user.name if user?
        v.label = name
      values


    disable: ->
      @options.app.state.updateFilter assigned: null, assignees: null


    addCustomValue: ->
      property = @model.get 'property'
      customValue = @$('.js-custom-value').select2 'val'
      value = @getValue()
      value += ',' if value.length > 0
      value += customValue
      obj = {}
      obj[property] = value
      obj.assigned = null
      @options.app.state.updateFilter obj


    sortValues: (values) ->
      # put "unassigned" first
      _.sortBy values, (v) ->
        x = if v.val == '' then -999999 else -v.count
        x


    getNumberOfMyIssues: ->
      @options.app.state.get 'myIssues'


    serializeData: ->
      _.extend super,
        myIssues: @getNumberOfMyIssues()
        values: @sortValues @getValuesWithLabels()
