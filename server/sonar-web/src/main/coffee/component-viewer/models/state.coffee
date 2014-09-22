define [
  'backbone'
], (
  Backbone
) ->


  class State extends Backbone.Model

    defaults:
      hasMeasures: false
      hasIssues: false
      hasCoverage: false
      hasITCoverage: false
      hasDuplications: false
      hasTests: false
      hasSCM: false

      activeHeaderTab: null