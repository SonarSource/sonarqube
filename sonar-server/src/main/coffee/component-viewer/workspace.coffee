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
      'click .component-viewer-workspace-item > a[data-key]': 'goToWorkspaceItem'
      'click .component-viewer-workspace-option > a[data-key]': 'goToWorkspaceOption'


    onRender: ->
      @delegateEvents()


    goToWorkspaceItem: (e) ->
      key = $(e.currentTarget).data 'key'
      workspace = @options.main.workspace
      workspaceItem = workspace.findWhere key: key
      workspaceItemIndex = workspace.indexOf workspaceItem
      workspace.reset workspace.initial(workspace.length - workspaceItemIndex)
      @options.main.addTransition workspaceItem.get('key'), workspaceItem.get('transition')


    goToWorkspaceOption: (e) ->
      workspaceKey = $(e.currentTarget).data 'workspace-key'
      key = $(e.currentTarget).data 'key'
      name = $(e.currentTarget).text()

      workspace = @options.main.workspace
      workspaceItem = workspace.findWhere key: workspaceKey
      workspaceItemOptions = workspaceItem.get 'options'
      workspaceItemOptions.forEach (option) -> option.active = option.name == name

      @options.main.addTransition workspaceItem.get('key'), workspaceItem.get('transition'), null, [
        { key: key, name: name }
      ]


    serializeData: ->
      _.extend super,
        workspace: @options.main.workspace.toJSON()
        settings: @options.main.settings.toJSON()

