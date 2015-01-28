define [
  'components/navigator/models/state'
], (State) ->
  class extends State

    defaults:
      page: 1
      maxResultsReached: false
      query: {}
      facets: ['severities', 'statuses', 'resolutions']
      isContext: false

      allFacets: ['issues', 'severities', 'statuses', 'resolutions', 'projectUuids', 'moduleUuids', 'directories',
                  'fileUuids', 'assignees', 'reporters', 'rules', 'tags', 'languages', 'actionPlans',
                  'creationDate'],
      facetsFromServer: ['severities', 'statuses', 'resolutions', 'actionPlans', 'projectUuids', 'directories', 'rules',
                         'moduleUuids', 'tags', 'assignees', 'reporters', 'fileUuids', 'languages'],
      transform: {
        'resolved': 'resolutions'
        'assigned': 'assignees'
        'planned': 'actionPlans'
        'createdAt': 'creationDate'
        'createdBefore': 'creationDate'
        'createdAfter': 'creationDate'
      }

