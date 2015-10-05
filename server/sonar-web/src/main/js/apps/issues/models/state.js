import _ from 'underscore';
import State from 'components/navigator/models/state';

export default State.extend({
  defaults: {
    page: 1,
    maxResultsReached: false,
    query: {},
    facets: ['facetMode', 'severities', 'resolutions'],
    isContext: false,
    allFacets: [
      'facetMode',
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
  },

  getFacetMode: function () {
    var query = this.get('query');
    return query.facetMode || 'count';
  },

  toJSON: function () {
    return _.extend({ facetMode: this.getFacetMode() }, this.attributes);
  }
});


