define [
  'components/navigator/models/state'
], (State) ->
  class extends State

    defaults:
      page: 1
      maxResultsReached: false
      query: {}
      facets: ['severities', 'resolutions', 'rules', 'tags', 'projectUuids']
      isContext: false

      allFacets: ['issues', 'severities', 'resolutions', 'rules', 'tags', 'statuses', 'projectUuids', 'moduleUuids',
                  'directories', 'fileUuids', 'assignees', 'reporters', 'authors', 'languages', 'actionPlans',
                  'creationDate'],
      facetsFromServer: ['severities', 'statuses', 'resolutions', 'actionPlans', 'projectUuids', 'directories', 'rules',
                         'moduleUuids', 'tags', 'assignees', 'reporters', 'authors', 'fileUuids', 'languages'],
      transform: {
        'resolved': 'resolutions'
        'assigned': 'assignees'
        'planned': 'actionPlans'
        'createdAt': 'creationDate'
        'createdBefore': 'creationDate'
        'createdAfter': 'creationDate'
      }

