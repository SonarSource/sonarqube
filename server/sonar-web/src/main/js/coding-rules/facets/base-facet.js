define([
    'components/navigator/facets/base-facet',
    'templates/coding-rules'
], function (BaseFacet, Templates) {

  return BaseFacet.extend({
    className: 'search-navigator-facet-box',
    template: Templates['coding-rules-base-facet']
  });

});
