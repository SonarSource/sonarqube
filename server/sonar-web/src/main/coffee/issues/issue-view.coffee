define [
  'issue/issue-view'
  'templates/issues'
], (
  IssueView
  Templates
) ->


  class extends IssueView
    template: Templates['issues-issue']
