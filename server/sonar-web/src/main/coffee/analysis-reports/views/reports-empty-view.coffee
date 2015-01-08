define [
  'templates/analysis-reports'
], ->

  class extends Marionette.ItemView
    className: 'analysis-reports-no-results'
    tagName: 'li'
    template: Templates['analysis-reports-empty']
