import State from 'components/navigator/models/state';

export default State.extend({
  defaults: {
    page: 1,
    maxResultsReached: false,
    query: {},
    facets: ['languages', 'tags'],
    allFacets: [
      'q', 'rule_key', 'languages', 'tags', 'repositories', 'debt_characteristics', 'severities',
      'statuses', 'available_since', 'is_template', 'qprofile', 'inheritance', 'active_severities'
    ],
    facetsFromServer: [
      'languages', 'repositories', 'tags', 'severities', 'statuses', 'debt_characteristics',
      'active_severities'
    ],
    transform: {
      'has_debt_characteristic': 'debt_characteristics'
    }
  }
});



