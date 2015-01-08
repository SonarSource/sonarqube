define([
    'components/navigator/facets/base-facet',
    'templates/coding-rules'
], function (BaseFacet) {

  return BaseFacet.extend({
    className: 'search-navigator-facet-box',
    template: Templates['coding-rules-base-facet']
  });

});
