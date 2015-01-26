define([
  'components/navigator/models/state'
], function (State) {

  return State.extend({
    defaults: {
      page: 1,
      maxResultsReached: false,
      query: {},
      facets: ['languages', 'tags'],
      allFacets: ['q', 'languages', 'tags', 'repositories', 'debt_characteristics', 'severities', 'statuses',
                  'available_since', 'is_template', 'qprofile', 'inheritance', 'active_severities'],
      facetsFromServer: ['languages', 'repositories', 'tags', 'severities', 'statuses', 'debt_characteristics'],
      transform: {}
    }
  });

});

