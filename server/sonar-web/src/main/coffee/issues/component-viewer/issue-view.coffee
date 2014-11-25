define [
  'issues/workspace-list-item-view'
], (
  IssueView
) ->

  class extends IssueView

    onRender: ->
      super
      @$el.removeClass 'issue-navigate-right'
      @$el.addClass 'issue-navigate-left'

    serializeData: ->
      _.extend super,
        showComponent: false
