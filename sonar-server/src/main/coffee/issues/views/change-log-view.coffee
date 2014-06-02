define [
  'backbone.marionette'
  'templates/issues'
], (
  Marionette
  Templates
) ->

  class IssueDetailChangeLogView extends Marionette.ItemView
    template: Templates['change-log']

    collectionEvents:
      'sync': 'render'