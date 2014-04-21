define [
  'backbone.marionette'
  'templates/component-viewer'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class HeaderView extends Marionette.ItemView
    template: Templates['header']


    events:
      'click .component-viewer-workspace-item [data-key]': 'goToWorkspaceItem'
      'click [data-option=coverage]': 'toggleCoverage'


    onRender: ->
      @delegateEvents()


    goToWorkspaceItem: (e) ->
      key = $(e.currentTarget).data 'key'
      workspace = @options.main.workspace
      workspaceItem = workspace.findWhere key: key
      workspaceItemIndex = workspace.indexOf workspaceItem
      workspace.reset workspace.initial(workspace.length - workspaceItemIndex)
      @options.main.addTransition workspaceItem.get('key'), workspaceItem.get('transition')


    toggleCoverage: (e) ->
      el = $(e.currentTarget)
      active = el.is '.active'
      el.toggleClass 'active'
      if active then @options.main.hideCoverage() else @options.main.showCoverage()


    serializeData: ->
      _.extend super,
        workspace: @options.main.workspace.toJSON()
        settings: @options.main.settings.toJSON()

