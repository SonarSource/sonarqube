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
        'click .js-new-search': 'newSearch'
        'click .js-back': 'returnToList'


    initialize: ->
      super
      @_onBulkIssues = window.onBulkIssues
      window.onBulkIssues = =>
        $('#modal').dialog 'close'
        @options.app.controller.fetchList()


    onClose: ->
      window.onBulkIssues = @_onBulkIssues


    newSearch: ->
      @options.app.controller.newSearch()


    returnToList: ->
      @options.app.controller.closeComponentViewer()


    bulkChange: ->
      query = @options.app.controller.getQuery '&'
      url = "#{baseUrl}/issues/bulk_change_form?#{query}"
      openModalWindow url, {}
