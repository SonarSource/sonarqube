define [
  'issues/workspace-list-item-view'
], (
  IssueView
) ->

  class extends IssueView

    serializeData: ->
      _.extend super,
        showComponent: false
