define ['jquery.mockjax'], ->

  jQuery.mockjaxSettings.contentType = 'text/json';
  jQuery.mockjaxSettings.responseTime = 250;

  # GET /api/codingrules/app
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/app"
    responseText: JSON.stringify
      qualityprofiles: [
        { key: 'sonarway', name: 'Sonar Way', parent: null },
        { key: 'qp1', name: 'Quality Profile 1', parent: 'sonarway' },
        { key: 'qp2', name: 'Quality Profile 2', parent: 'sonarway' },
        { key: 'qp3', name: 'Quality Profile 3', parent: null },
      ]
      languages:
        java: 'Java'
        javascript: 'JavaScript'
      repositories:
        'checkstyle': 'Checkstyle'
        'common-java': 'Common SonarQube'
        'findbugs': 'FindBugs'
        'pmd': 'PMD'
        'pmd-unit-tests': 'PMD Unit Tests'
        'squid': 'SonarQube'
      statuses:
        'BETA': 'Beta'
        'DEPRECATED': 'Deprecated'
        'READY': 'Ready'
      tags:
        'brain-overload': 'brain-overload'
        'bug': 'bug'
        'comment': 'comment'
        'convention': 'convention'
        'error-handling': 'error-handling'
        'formatting': 'formatting'
        'java8': 'java8'
        'multithreading': 'multithreading'
        'naming': 'naming'
        'pitfall': 'pitfall'
        'security': 'security'
        'size': 'size'
        'unused': 'unused'
        'unused-code': 'unused-code'
      messages:
        'all': 'All'
        'any': 'Any'
        'apply': 'Apply'
        'bulk_change': 'Bulk Change'
        'cancel': 'Cancel'
        'moreCriteria': '+ More Criteria'
        'search_verb': 'Search'
        'update': 'Update'

        'severity.BLOCKER': 'Blocker'
        'severity.CRITICAL': 'Critical'
        'severity.MAJOR': 'Major'
        'severity.MINOR': 'Minor'
        'severity.INFO': 'Info'

        'coding_rules.activate_quality_profile': 'Activate Quality Profile'
        'coding_rules.bulk_change': 'Bulk Change'
        'coding_rules.deactivate_quality_profile': 'Deactivate'
        'coding_rules.found': 'Found'
        'coding_rules.new_search': 'New Search'
        'coding_rules.no_results': 'No Coding Rules'
        'coding_rules.order': 'Order'
        'coding_rules.ordered_by': 'Ordered By'
        'coding_rules.page': 'Coding Rules'
        'coding_rules.quality_profiles': 'Quality Profiles'

        'coding_rules.filters.availableSince': 'Available Since'
        'coding_rules.filters.description': 'Description'
        'coding_rules.filters.in_quality_profile': 'In Quality Profile'
        'coding_rules.filters.inheritance': 'Inheritance'
        'coding_rules.filters.inheritance.inactive': 'Inheritance criteria is available when an inherited quality profile is selected'
        'coding_rules.filters.inheritance.any': 'Any'
        'coding_rules.filters.inheritance.not_inherited': 'Not Inherited'
        'coding_rules.filters.inheritance.inherited': 'Inherited'
        'coding_rules.filters.inheritance.overriden': 'Overriden'
        'coding_rules.filters.key': 'Key'
        'coding_rules.filters.language': 'Language'
        'coding_rules.filters.name': 'Name'
        'coding_rules.filters.out_of_quality_profile': 'Out of Quality Profile'
        'coding_rules.filters.repository': 'Repository'
        'coding_rules.filters.severity': 'Severity'
        'coding_rules.filters.status': 'Status'
        'coding_rules.filters.tag': 'Tag'

        'coding_rules.sort.creation_date': 'Creation Date'
        'coding_rules.sort.name': 'Name'


  # GET /api/codingrules/search
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/search"
    responseText: JSON.stringify
      codingrules: [
        {
          name: 'Array designators "[]" should be located after the type in method signatures'
          language: 'Java'
          severity: 'MAJOR'
        },
        {
          name: 'Avoid Array Loops'
          language: 'Java'
          severity: 'CRITICAL'
        },
        {
          name: 'Bad practice - Abstract class defines covariant compareTo() method'
          language: 'Java'
          severity: 'MAJOR'
        },
        {
          name: 'Correctness - Use of class without a hashCode() method in a hashed data structure'
          language: 'Java'
          severity: 'MINOR'
        },
        {
          name: 'Useless Operation On Immutable'
          language: 'Java'
          severity: 'MAJOR'
        }
      ]
      paging:
        total: 5
        fTotal: '5'



  # GET /api/codingrules/show
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/show"
    responseText: JSON.stringify
      codingrule:
        name: 'Array designators "[]" should be located after the type in method signatures'
        language: 'Java'
        description: '''
            <p>
            According to the Java Language Specification:
            </p>

            <pre>For compatibility with older versions of the Java SE platform,
            the declaration of a method that returns an array is allowed to place (some or all of)
            the empty bracket pairs that form the declaration of the array type after
            the formal parameter list. This obsolescent syntax should not be used in new code.
            </pre>

            <p>The following code snippet illustrates this rule:</p>

            <pre>public int getVector()[] { /* ... */ }    // Non-Compliant

            public int[] getVector() { /* ... */ }    // Compliant

            public int[] getMatrix()[] { /* ... */ }  // Non-Compliant

            public int[][] getMatrix() { /* ... */ }  // Compliant
            </pre>'''

        qualityProfiles: [
          {
            name: 'SonarWay'
            severity: 'MINOR'
            canDeactivate: true
            canUpdate: true
            parameters: [
              { key: 'max', value: 8 }
            ]

          },
          {
            name: 'Quality Profile 1'
            severity: 'MAJOR'
            canDeactivate: false
            canUpdate: false
            parameters: [
              { key: 'max', value: 6 }
            ]

          }
        ]



  # POST /api/codingrules/bulk_change
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/bulk_change"


  # GET /api/qualityprofiles/list
  jQuery.mockjax
    url: "#{baseUrl}/api/qualityprofiles/list"
    responseText: JSON.stringify
      more: false
      results: [
        { id: 'sonarway', text: 'Sonar Way', parent: null },
        { id: 'qp1', text: 'Quality Profile 1', parent: 'sonarway' },
        { id: 'qp2', text: 'Quality Profile 2', parent: 'sonarway' },
        { id: 'qp3', text: 'Quality Profile 3', parent: null },
      ]


  # GET /api/qualityprofiles/show
  jQuery.mockjax
    url: "#{baseUrl}/api/qualityprofiles/show"
    responseText: JSON.stringify
      qualityprofile:
        id: 'sonarway', text: 'Sonar Way', parent: null

