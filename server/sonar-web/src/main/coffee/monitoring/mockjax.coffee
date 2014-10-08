define ['third-party/jquery.mockjax'], ->

  jQuery.mockjaxSettings.contentType = 'text/json';
  jQuery.mockjaxSettings.responseTime = 250;

  # GET /api/codingrules/app
  jQuery.mockjax
    url: "#{baseUrl}/api/reports/search"
    responseText: JSON.stringify
      paging:
        pageIndex: 1
        pageSize: 5
        total: 206
        pages: 42
      reports: [
        {
          id: 84
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:11:33+06:00"
          status: "PENDING"
        }
        {
          id: 83
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some additional text or stack trace."
        }
        {
          id: 82
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some additional text or stack trace."
        }
        {
          id: 81
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some additional text or stack trace."
        }
        {
          id: 80
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some additional text or stack trace."
        }
        {
          id: 79
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some additional text or stack trace."
        }
        {
          id: 78
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some additional text or stack trace."
        }
        {
          id: 77
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some additional text or stack trace."
        }
        {
          id: 76
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some additional text or stack trace."
        }
        {
          id: 75
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some asdditional text or stack trace."
        }
        {
          id: 74
          project: "org.codehaus.sonar:sonar"
          projectName: "SonarQube"
          startDate: "2014-07-19T23:10:33+06:00"
          endDate: "2014-07-19T23:12:01+06:00"
          status: "DONE"
          extra: "Some additional text or stack trace."
        }
      ]
