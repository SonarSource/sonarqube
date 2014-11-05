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
      'click .js-issues-toggle-filters': 'toggleFilters'
      'click .js-issues-filter': 'applyFilter'
      'click #issues-new-search': 'newSearch'
      'click #issues-filter-save-as': 'saveAs'
      'click #issues-filter-save': 'save'
      'click #issues-filter-copy': 'copy'
      'click #issues-filter-edit': 'edit'


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
      @$el.toggleClass 'issues-filters-selected', @options.app.state.has('filter')


    toggleFilters: (e) ->
      e.stopPropagation()
      @$('.issues-filters-list').toggle()
      $('body').on 'click.issues-filters', =>
        $('body').off 'click.issues-filters'
        @$('.issues-filters-list').hide()


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
