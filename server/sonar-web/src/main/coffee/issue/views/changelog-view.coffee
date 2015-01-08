define [
  'common/popup'
  'templates/issue'
], (
  PopupView
) ->

  class extends PopupView
    template: Templates['issue-changelog']


    collectionEvents:
      'sync': 'render'


    serializeData: ->
      _.extend super,
        issue: @options.issue.toJSON()
