define [
  'backbone.marionette'
  'templates/issues'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class extends Marionette.ItemView
    template: Templates['issues-filters']


    events:
      'click .js-toggle-filters': 'toggleFilters'
      'click .js-filter': 'applyFilter'
      'click .js-new-search': 'newSearch'
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


    newSearch: ->
      @options.app.controller.newSearch()


    saveAs: ->
      query = @options.app.controller.getQuery '&'
      url = "#{baseUrl}/issues/save_as_form?#{query}"
      openModalWindow url, {}


    save: ->
      query = @options.app.controller.getQuery '&'
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
