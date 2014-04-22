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
      'click .component-viewer-workspace-item [data-key]': 'goToWorkspaceItem'


    onRender: ->
      @delegateEvents()


    goToWorkspaceItem: (e) ->
      key = $(e.currentTarget).data 'key'
      workspace = @options.main.workspace
      workspaceItem = workspace.findWhere key: key
      workspaceItemIndex = workspace.indexOf workspaceItem
      workspace.reset workspace.initial(workspace.length - workspaceItemIndex)
      @options.main.addTransition workspaceItem.get('key'), workspaceItem.get('transition')


    serializeData: ->
      _.extend super,
        workspace: @options.main.workspace.toJSON()
        settings: @options.main.settings.toJSON()

