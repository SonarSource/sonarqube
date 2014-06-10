package org.sonar.server.log.index;

import org.elasticsearch.action.search.SearchResponse;
import org.sonar.core.log.Log;
import org.sonar.server.search.Index;
import org.sonar.server.search.Result;

import java.util.Map;

/**
 * @since 4.4
 */
public class LogResult extends Result<Log> {

  public LogResult(Index<Log, ?, ?> index, SearchResponse response) {
    super(index, response);
  }

  public LogResult(SearchResponse response) {
    super(response);
  }

  @Override
  protected Log getSearchResult(Map<String, Object> fields) {
    return new LogDoc(fields);
  }
}
