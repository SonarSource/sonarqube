define [
  'backbone.marionette'
  'templates/issues'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class extends Marionette.ItemView
    template: Templates['issues-workspace-header']


    collectionEvents:
      'all': 'render'


    events:
      'click .js-back': 'returnToList'
      'click #issues-bulk-change': 'bulkChange'
      'click #issues-reload': 'reloadIssues'
      'click .js-issues-next': 'selectNextIssue'
      'click .js-issues-prev': 'selectPrevIssue'


    initialize: (options) ->
      @listenTo options.app.state, 'change', @render
      @_onBulkIssues = window.onBulkIssues
      window.onBulkIssues = =>
        $('#modal').dialog 'close'
        @options.app.controller.fetchIssues()


    onClose: ->
      window.onBulkIssues = @_onBulkIssues


    returnToList: ->
      @options.app.controller.closeComponentViewer()


    bulkChange: ->
      query = @options.app.controller.getQuery '&'
      url = "#{baseUrl}/issues/bulk_change_form?#{query}"
      openModalWindow url, {}


    reloadIssues: ->
      @options.app.controller.fetchIssues()


    selectNextIssue: ->
      @options.app.controller.selectNextIssue()


    selectPrevIssue: ->
      @options.app.controller.selectPreviousIssue()


    serializeData: ->
      _.extend super,
        state: @options.app.state.toJSON()

