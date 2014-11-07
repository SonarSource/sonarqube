define [
  'templates/issue'
  'common/popup'
], (
  Templates
  PopupView
) ->

  class extends PopupView
    template: Templates['issue-changelog']


    collectionEvents:
      'sync': 'render'


    serializeData: ->
      _.extend super,
        issue: @options.issue.toJSON()
