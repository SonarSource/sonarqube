define [
  'issues/workspace-list-item-view'
], (
  IssueView
) ->

  class extends IssueView

    onRender: ->
      super
      @$el.removeClass 'issue-navigate-right'


    serializeData: ->
      _.extend super,
        showComponent: false
