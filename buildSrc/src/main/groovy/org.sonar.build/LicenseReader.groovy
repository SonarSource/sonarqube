package org.sonar.build

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class LicenseReader extends FilterReader {

  LicenseReader(Reader fileReader) {
    super(build(fileReader))
  }

  private static Reader build(Reader fileReader) {
    def json = new JsonSlurper().parse(fileReader)

    json.dependencies.each { dependency ->
      if (dependency.licenses.size() > 1) {
        def idx = dependency.licenses.findIndexOf { it.name == "Elastic License 2.0" }
        if (idx >= 0) {
          dependency.licenses = [dependency.licenses[idx]]
        }
      }
    }

    json.dependencies.sort { it.name }

    def jsonText = JsonOutput.toJson(json)
    return new StringReader(JsonOutput.prettyPrint(jsonText))
  }
}
