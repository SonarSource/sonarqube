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
  'issue/issue-view'
  'issues/issue-filter-view'
  'templates/issues'
], (
  IssueView
  IssueFilterView
) ->

  $ = jQuery

  SHOULD_NULL =
    any: ['issues']
    resolutions: ['resolved']
    resolved: ['resolutions']
    assignees: ['assigned']
    assigned: ['assignees']
    actionPlans: ['planned']
    planned: ['actionPlans']

  class extends IssueView
    filterTemplate: Templates['issues-issue-filter']

    events: ->
      _.extend super,
        'click': 'selectCurrent'
        'dblclick': 'openComponentViewer'
        'click .js-issue-navigate': 'openComponentViewer'
        'click .js-issue-filter': 'onIssueFilterClick'


    initialize: (options) ->
      super
      @listenTo options.app.state, 'change:selectedIndex', @select


    onRender: ->
      super
      @select()
      @addFilterSelect()
      @$el.addClass 'issue-navigate-right'


    onIssueFilterClick: (e) ->
      e.preventDefault()
      e.stopPropagation()
      $('body').click()
      @popup = new IssueFilterView
        triggerEl: $(e.currentTarget)
        bottomRight: true
        model: @model
      @popup.on 'select', (property, value) =>
        obj = {}
        obj[property] = '' + value
        SHOULD_NULL.any.forEach (p) -> obj[p] = null
        if SHOULD_NULL[property]?
          SHOULD_NULL[property].forEach (p) -> obj[p] = null
        @options.app.state.updateFilter obj
        @popup.close()
      @popup.render()



    addFilterSelect: ->
      @$('.issue-table-meta-cell-first').find('.issue-meta-list').append @filterTemplate @model.toJSON()


    select: ->
      selected = @model.get('index') == @options.app.state.get 'selectedIndex'
      @$el.toggleClass 'selected', selected


    selectCurrent: ->
      @options.app.state.set selectedIndex: @model.get('index')


    resetIssue: (options) ->
      key = @model.get 'key'
      componentUuid = @model.get 'componentUuid'
      index = @model.get 'index'
      @model.clear silent: true
      @model.set { key: key, componentUuid: componentUuid, index: index }, { silent: true }
      @model.fetch(options)
      .done =>
        @trigger 'reset'


    openComponentViewer: ->
      @options.app.state.set selectedIndex: @model.get('index')
      if @options.app.state.has 'component'
        @options.app.controller.closeComponentViewer()
      else
        @options.app.controller.showComponentViewer @model


    serializeData: ->
      _.extend super,
        showComponent: true
