define [
  'components/navigator/models/state'
], (State) ->
  class extends State

    defaults:
      page: 1
      maxResultsReached: false
      query: {}
      facets: ['severities', 'resolutions', 'createdAt', 'rules', 'tags', 'projectUuids']
      isContext: false

      allFacets: ['issues', 'severities', 'resolutions', 'createdAt', 'rules', 'tags', 'statuses', 'projectUuids',
                  'moduleUuids', 'directories', 'fileUuids', 'assignees', 'reporters', 'authors', 'languages',
                  'actionPlans'],
      facetsFromServer: ['severities', 'statuses', 'resolutions', 'actionPlans', 'projectUuids', 'directories', 'rules',
                         'moduleUuids', 'tags', 'assignees', 'reporters', 'authors', 'fileUuids', 'languages',
                         'createdAt'],
      transform: {
        'resolved': 'resolutions'
        'assigned': 'assignees'
        'planned': 'actionPlans'
        'createdBefore': 'createdAt'
        'createdAfter': 'createdAt'
      }

