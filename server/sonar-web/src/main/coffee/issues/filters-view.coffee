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
  'templates/issues'
], ->

  $ = jQuery


  class extends Marionette.ItemView
    template: Templates['issues-filters']


    events:
      'click .js-toggle-filters': 'toggleFilters'
      'click .js-filter': 'applyFilter'
      'click .js-filter-save-as': 'saveAs'
      'click .js-filter-save': 'save'
      'click .js-filter-copy': 'copy'
      'click .js-filter-edit': 'edit'


    initialize: (options) ->
      @listenTo options.app.state, 'change:filter', @render
      @listenTo options.app.state, 'change:changed', @render
      @listenTo options.app.filters, 'all', @render
      window.onSaveAs = window.onCopy = window.onEdit = (id) =>
        $('#modal').dialog 'close'
        @options.app.controller.fetchFilters().done =>
          filter = @collection.get id
          filter.fetch().done => @options.app.controller.applyFilter filter


    onRender: ->
      @$el.toggleClass 'search-navigator-filters-selected', @options.app.state.has('filter')


    toggleFilters: (e) ->
      e.stopPropagation()
      @$('.search-navigator-filters-list').toggle()
      $('body').on 'click.issues-filters', =>
        $('body').off 'click.issues-filters'
        @$('.search-navigator-filters-list').hide()


    applyFilter: (e) ->
      id = $(e.currentTarget).data 'id'
      filter = @collection.get id
      filter.fetch().done => @options.app.controller.applyFilter filter


    getSaveQuery: ->
      query = @options.app.controller.getQuery '&'
      facets = @options.app.state.get('facets').join ','
      if facets.length > 0
        facetsQuery = "facets=#{encodeURIComponent(facets)}"
        query = [query, facetsQuery].join('&')
      query


    saveAs: ->
      query = @getSaveQuery()
      url = "#{baseUrl}/issues/save_as_form?#{query}"
      openModalWindow url, {}


    save: ->
      query = @getSaveQuery()
      url = "#{baseUrl}/issues/save/#{@options.app.state.get('filter').id}?#{query}"
      $.post(url).done =>
        @options.app.state.set changed: false


    copy: ->
      url = "#{baseUrl}/issues/copy_form/#{@options.app.state.get('filter').id}"
      openModalWindow url, {}


    edit: ->
      url = "#{baseUrl}/issues/edit_form/#{@options.app.state.get('filter').id}"
      openModalWindow url, {}


    serializeData: ->
      _.extend super,
        state: @options.app.state.toJSON()
        filter: @options.app.state.get('filter')?.toJSON()
        currentUser: window.SS.user
