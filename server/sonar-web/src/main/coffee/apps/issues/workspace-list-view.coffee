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
  'components/navigator/workspace-list-view'
  './workspace-list-item-view'
  './workspace-list-empty-view'
  './templates'
], (
  WorkspaceListView
  IssueView
  EmptyView
) ->

  $ = jQuery

  COMPONENT_HEIGHT = 29
  BOTTOM_OFFSET = 10


  class extends WorkspaceListView
    template: Templates['issues-workspace-list']
    componentTemplate: Templates['issues-workspace-list-component']
    itemView: IssueView
    itemViewContainer: '.js-list'
    emptyView: EmptyView


    bindShortcuts: ->
      doAction = (action) =>
        selectedIssue = @collection.at @options.app.state.get 'selectedIndex'
        return unless selectedIssue?
        selectedIssueView = @children.findByModel selectedIssue
        selectedIssueView.$(".js-issue-#{action}").click()

      super

      key 'right', 'list', =>
        selectedIssue = @collection.at @options.app.state.get 'selectedIndex'
        @options.app.controller.showComponentViewer selectedIssue
        return false

      key 'f', 'list', -> doAction 'transition'
      key 'a', 'list', -> doAction 'assign'
      key 'm', 'list', -> doAction 'assign-to-me'
      key 'p', 'list', -> doAction 'plan'
      key 'i', 'list', -> doAction 'set-severity'
      key 'c', 'list', -> doAction 'comment'
      key 't', 'list', -> doAction 'edit-tags'


    scrollTo: ->
      selectedIssue = @collection.at @options.app.state.get 'selectedIndex'
      return unless selectedIssue?
      selectedIssueView = @children.findByModel selectedIssue
      parentTopOffset = @$el.offset().top
      viewTop = selectedIssueView.$el.offset().top - parentTopOffset
      if selectedIssueView.$el.prev().is('.issues-workspace-list-component')
        viewTop -= COMPONENT_HEIGHT
      viewBottom = selectedIssueView.$el.offset().top + selectedIssueView.$el.outerHeight() + BOTTOM_OFFSET
      windowTop = $(window).scrollTop()
      windowBottom = windowTop + $(window).height()
      if viewTop < windowTop
        $(window).scrollTop viewTop
      if viewBottom > windowBottom
        $(window).scrollTop $(window).scrollTop() - windowBottom + viewBottom


    appendHtml: (compositeView, itemView, index) ->
      $container = this.getItemViewContainer compositeView
      model = @collection.at(index)
      if model?
        prev = @collection.at(index - 1)
        putComponent = !prev?
        if prev?
          fullComponent = [model.get('project'), model.get('component')].join ' '
          fullPrevComponent = [prev.get('project'), prev.get('component')].join ' '
          putComponent = true unless fullComponent == fullPrevComponent
        if putComponent
          $container.append @componentTemplate model.toJSON()
      $container.append itemView.el


    closeChildren: ->
      super
      @$('.issues-workspace-list-component').remove()
