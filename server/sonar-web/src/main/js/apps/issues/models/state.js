define([
  'components/navigator/models/state'
], function (State) {

  return State.extend({
    defaults: {
      page: 1,
      maxResultsReached: false,
      query: {},
      facets: ['severities', 'resolutions'],
      isContext: false,
      allFacets: [
        'issues',
        'severities',
        'resolutions',
        'statuses',
        'createdAt',
        'rules',
        'tags',
        'projectUuids',
        'moduleUuids',
        'directories',
        'fileUuids',
        'assignees',
        'reporters',
        'authors',
        'languages',
        'actionPlans'
      ],
      facetsFromServer: [
        'severities',
        'statuses',
        'resolutions',
        'actionPlans',
        'projectUuids',
        'directories',
        'rules',
        'moduleUuids',
        'tags',
        'assignees',
        'reporters',
        'authors',
        'fileUuids',
        'languages',
        'createdAt'
      ],
      transform: {
        'resolved': 'resolutions',
        'assigned': 'assignees',
        'planned': 'actionPlans',
        'createdBefore': 'createdAt',
        'createdAfter': 'createdAt',
        'createdInLast': 'createdAt'
      }
    }
  });

});
