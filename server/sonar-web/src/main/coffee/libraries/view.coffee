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
  'templates/libraries'
], ->

  $ = jQuery



  class extends Marionette.ItemView
    template: Templates['libraries']


    ui:
      filter: '.js-libraries-filter'
      collapseAll: '.js-libraries-collapse-all'
      expandAll: '.js-libraries-expand-all'


    events:
      'change .js-test-libraries': 'toggleTestLibraries'
      'click @ui.collapseAll': 'collapseAll'
      'click @ui.expandAll': 'expandAll'
      'click .libraries-tree-with-subtree > a': 'toggleSubTree'


    onRender: ->
      @toggleActions()
      filterFn = _.bind @filter, @
      debouncedFilterFn = _.debounce filterFn, 250
      @ui.filter.off('keyup').on 'keyup', debouncedFilterFn


    filter: (e) ->
      term = $(e.currentTarget).val()
      @expandAll()
      @$('.libraries-tree li').addClass 'libraries-tree-subtree-hidden'
      @$('.libraries-tree li').each (index, el) =>
        name = $(el).children('.libraries-tree-name').text()
        @showTree $(el) if name.match new RegExp(term, 'i')


    showTree: (el) ->
      el.removeClass 'libraries-tree-subtree-hidden'
      el.parents('.libraries-tree-subtree-hidden').removeClass 'libraries-tree-subtree-hidden'


    toggleTestLibraries: ->
      @$('.libraries-tree').toggleClass 'libraries-tree-show-tests'


    collapseAll: ->
      @$('.libraries-tree-with-subtree').addClass 'libraries-tree-subtree-collapsed'
      @toggleActions()


    expandAll: ->
      @$('.libraries-tree-with-subtree').removeClass 'libraries-tree-subtree-collapsed'
      @toggleActions()


    toggleSubTree: (e) ->
      tree = $(e.currentTarget).parent()
      tree.toggleClass 'libraries-tree-subtree-collapsed'
      @toggleActions()


    toggleActions: ->
      subTreesCount = @$('.libraries-tree-with-subtree').length
      subTreesCollapsedCount = @$('.libraries-tree-subtree-collapsed').length
      @ui.collapseAll.toggle subTreesCount > subTreesCollapsedCount
      @ui.expandAll.toggle subTreesCollapsedCount > 0


    serializeData: ->
      _.extend super,
        usagesUrl: "#{baseUrl}/dependencies/index?search=#{window.resourceKey}"

