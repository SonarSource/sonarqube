define [
  'components/navigator/workspace-header-view'
  'templates/issues'
], (
  WorkspaceHeaderView
) ->

  $ = jQuery


  class extends WorkspaceHeaderView
    template: Templates['issues-workspace-header']


    events: ->
      _.extend super,
        'click .js-back': 'returnToList'
        'click .js-new-search': 'newSearch'


    initialize: ->
      super
      @_onBulkIssues = window.onBulkIssues
      window.onBulkIssues = =>
        $('#modal').dialog 'close'
        @options.app.controller.fetchList()


    onClose: ->
      window.onBulkIssues = @_onBulkIssues


    returnToList: ->
      @options.app.controller.closeComponentViewer()


    newSearch: ->
      @options.app.controller.newSearch()


    bulkChange: ->
      query = @options.app.controller.getQuery '&'
      url = "#{baseUrl}/issues/bulk_change_form?#{query}"
      openModalWindow url, {}
