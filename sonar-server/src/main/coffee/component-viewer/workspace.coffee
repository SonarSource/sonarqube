define [
  'backbone.marionette'
  'templates/component-viewer'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class WorkspaceView extends Marionette.ItemView
    template: Templates['workspace']


    events:
      'click .js-toggle-workspace': 'toggleWorkspace'

      'click .component-viewer-workspace-item > a[data-key]': 'goToWorkspaceItem'
      'click .component-viewer-workspace-option > a[data-key]': 'goToWorkspaceOption'


    onRender: ->
      @delegateEvents()


    toggleWorkspace: ->
      @options.main.toggleWorkspace true


    goToWorkspaceItem: (e) ->
      key = $(e.currentTarget).data 'key'
      workspace = @options.main.workspace
      workspaceItem = workspace.findWhere key: key
      workspaceItem.set 'active', true
      workspaceItemOptions = workspaceItem.get 'options'
      workspaceItemOptions.forEach (option) -> option.active = false
      @options.main._open key


    goToWorkspaceOption: (e) ->
      workspaceKey = $(e.currentTarget).data 'workspace-key'
      key = $(e.currentTarget).data 'key'
      workspace = @options.main.workspace
      workspaceItem = workspace.findWhere key: workspaceKey
      workspaceItem.set 'active', false
      workspaceItemOptions = workspaceItem.get 'options'
      workspaceItemOptions.forEach (option) -> option.active = option.key == key
      @options.main._open key


    serializeData: ->
      _.extend super,
        workspace: @options.main.workspace.toJSON()
        settings: @options.main.settings.toJSON()

