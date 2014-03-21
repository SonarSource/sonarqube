define ['jquery.mockjax'], ->

  jQuery.mockjaxSettings.contentType = 'text/json';
  jQuery.mockjaxSettings.responseTime = 250;

  # GET /api/codingrules/app
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/app"
    responseText: JSON.stringify
      qualityprofiles: [
        { key: 'sonarway', name: 'Sonar Way', lang: 'Java', parent: null },
        { key: 'qualityprofile1', name: 'Quality Profile 1', lang: 'Java', parent: 'sonarway' },
        { key: 'qualityprofile2', name: 'Quality Profile 2', lang: 'JavaScript', parent: 'sonarway' },
        { key: 'qualityprofile3', name: 'Quality Profile 3', lang: 'Java', parent: null },
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
        'bold': 'Bold'
        'bulk_change': 'Bulk Change'
        'bulleted_point': 'Bulleted point'
        'cancel': 'Cancel'
        'change': 'Change'
        'code': 'Code'
        'delete': 'Delete'
        'done': 'Done'
        'edit': 'Edit'
        'markdown.helplink': 'Markdown Help'
        'moreCriteria': '+ More Criteria'
        'search_verb': 'Search'
        'severity': 'Severity'
        'update': 'Update'

        'severity.BLOCKER': 'Blocker'
        'severity.CRITICAL': 'Critical'
        'severity.MAJOR': 'Major'
        'severity.MINOR': 'Minor'
        'severity.INFO': 'Info'

        'coding_rules.activate': 'Activate'
        'coding_rules.activate_in': 'Activate In'
        'coding_rules.activate_in_quality_profile': 'Activate In Quality Profile'
        'coding_rules.add_note': 'Add Note'
        'coding_rules.available_since': 'Available Since'
        'coding_rules.bulk_change': 'Bulk Change'
        'coding_rules.change_severity': 'Change Severity'
        'coding_rules.change_severity_in': 'Change Severity In'
        'coding_rules.extend_description': 'Extend Description'
        'coding_rules.deactivate_in': 'Deactivate In'
        'coding_rules.deactivate_quality_profile': 'Deactivate'
        'coding_rules.deactivate_in_quality_profile': 'Deactivate In Quality Profile'
        'coding_rules.found': 'Found'
        'coding_rules._inherits': 'inherits'
        'coding_rules.key': 'Key:'
        'coding_rules.new_search': 'New Search'
        'coding_rules.no_results': 'No Coding Rules'
        'coding_rules.order': 'Order'
        'coding_rules.ordered_by': 'Ordered By'
        'coding_rules.original': 'Original:'
        'coding_rules.page': 'Coding Rules'
        'coding_rules.parameters': 'Parameters'
        'coding_rules.parameters.default_value': 'Default Value:'
        'coding_rules.quality_profiles': 'Quality Profiles'
        'coding_rules.quality_profile': 'Quality Profile'
        'coding_rules.repository': 'Repository:'
        'coding_rules.revert_to_parent_definition': 'Revert to Parent Definition'
        'coding_rules._rules': 'rules'
        'coding_rules.select_tag': 'Select Tag'

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
          status: 'DEPRECATED'
        },
        {
          name: 'Avoid Array Loops'
          language: 'Java'
          severity: 'CRITICAL'
          status: 'READY'
        },
        {
          name: 'Bad practice - Abstract class defines covariant compareTo() method'
          language: 'Java'
          severity: 'MAJOR'
          status: 'READY'
        },
        {
          name: 'Correctness - Use of class without a hashCode() method in a hashed data structure'
          language: 'Java'
          severity: 'MINOR'
          status: 'BETA'
        },
        {
          name: 'Useless Operation On Immutable'
          language: 'Java'
          severity: 'MAJOR'
          status: 'READY'
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
        tags: ['bug', 'comment', 'java8']
        creationDate: '2013-10-15'
        fCreationDate: 'Oct 15, 2013'
        status: 'DEPRECATED'
        repository: 'squid'
        characteristic: 'Reliability'
        subcharacteristic: 'Data related reliability'
        key: 'S1190'
        parameters: [
          { key: 'someParameter', type: 'INT', default: 4, description: 'Some parameter description' }
          { key: 'boolParameter', type: 'BOOL', description: 'Bool parameter description' }
        ]
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
        extra: '''<p>This note is here <b>only for test purposes</b>.</p>'''
        extraRaw: '''This note is here *only for test purposes*.'''

        qualityProfiles: [
          {
            name: 'SonarWay'
            key: 'sonarway'
            severity: 'MINOR'
            canDeactivate: true
            canUpdate: true
            parameters: [
              { key: 'someParameter', value: 8 }
            ]
          },
          {
            name: 'Quality Profile 1'
            key: 'qualityprofile1'
            severity: 'MAJOR'
            canDeactivate: false
            canUpdate: false
            parameters: [
              { key: 'someParameter', value: 6 }
            ]
            inherits: 'sonarway'
            note:
              username: 'Admin Admin'
              html: '''<p>This note is here <b>only for test purposes</b>.</p>'''
              raw: '''This note is here *only for test purposes*.'''
              fCreationDate: 'less than a minute'
          }
        ]



  # POST /api/codingrules/extend_description
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/extend_description"
    responseText: JSON.stringify
      extra: '''<p>This note is here <i>only for test purposes</i>.</p>'''
      extraRaw: '''This note is here *only for test purposes*.'''


  # POST /api/codingrules/bulk_change
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/bulk_change"


  # POST /api/codingrules/set_tags
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/set_tags"


  # POST /api/codingrules/activate
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/activate"


  # POST /api/codingrules/note
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/note"
    responseText: JSON.stringify
      note:
        username: 'Admin Admin'
        html: '''<p>This note is here <b>only for test purposes</b>.</p>'''
        raw: '''This note is here *only for test purposes*.'''
        fCreationDate: 'less than a minute'


  # GET /api/qualityprofiles/list
  jQuery.mockjax
    url: "#{baseUrl}/api/qualityprofiles/list"
    responseText: JSON.stringify
      more: false
      results: [
        { id: 'sonarway', text: 'Sonar Way', category: 'Java', parent: null },
        { id: 'qp1', text: 'Quality Profile 1', category: 'Java', parent: 'sonarway' },
        { id: 'qp2', text: 'Quality Profile 2', category: 'JavaScript', parent: 'sonarway' },
        { id: 'qp3', text: 'Quality Profile 3', category: 'Java', parent: null },
      ]


  # GET /api/qualityprofiles/show
  jQuery.mockjax
    url: "#{baseUrl}/api/qualityprofiles/show"
    responseText: JSON.stringify
      qualityprofile:
        id: 'sonarway', text: 'Sonar Way', category: 'Java', parent: null

