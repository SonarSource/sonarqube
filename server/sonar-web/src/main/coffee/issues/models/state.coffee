define [
  'components/navigator/models/state'
], (
  State
) ->

  class extends State

    defaults:
      page: 1
      maxResultsReached: false
      query: {}
      facets: ['severities', 'statuses', 'resolutions']

