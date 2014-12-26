define([
      'components/navigator/facets-view',
      'coding-rules/facets/base-facet',
      'coding-rules/facets/query-facet',
      'coding-rules/facets/language-facet',
      'coding-rules/facets/repository-facet',
      'coding-rules/facets/quality-profile-facet',
      'coding-rules/facets/characteristic-facet',
      'coding-rules/facets/severity-facet',
      'coding-rules/facets/status-facet',
      'coding-rules/facets/available-since-facet',
      'coding-rules/facets/inheritance-facet',
      'coding-rules/facets/active-severity-facet'
    ],
    function (FacetsView,
              BaseFacet,
              QueryFacet,
              LanguageFacet,
              RepositoryFacet,
              QualityProfileFacet,
              CharacteristicFacet,
              SeverityFacet,
              StatusFacet,
              AvailableSinceFacet,
              InheritanceFacet,
              ActiveSeverityFacet) {

      var viewsMapping = {
        q: QueryFacet,
        languages: LanguageFacet,
        repositories: RepositoryFacet,
        qprofile: QualityProfileFacet,
        debt_characteristics: CharacteristicFacet,
        severities: SeverityFacet,
        statuses: StatusFacet,
        available_since: AvailableSinceFacet,
        inheritance: InheritanceFacet,
        active_severities: ActiveSeverityFacet
      };

      return FacetsView.extend({

        getItemView: function (model) {
          var view = viewsMapping[model.get('property')];
          return view ? view : BaseFacet;
        }

      });

    });
