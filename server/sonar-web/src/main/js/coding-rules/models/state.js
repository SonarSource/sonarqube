define([
  'components/navigator/models/state'
], function (State) {

  return State.extend({
    defaults: {
      page: 1,
      maxResultsReached: false,
      query: {},
      facets: []
    }
  });

});

