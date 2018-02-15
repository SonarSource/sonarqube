package org.sonar.db.source;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

import static org.sonar.db.source.FileSourceDto.LINES_HASHES_SPLITTER;

public class LineHashesWithKeyDto {
  private String kee;
  private String lineHashes;

  public String getKey() {
    return kee;
  }

  public String getRawLineHashes() {
    return lineHashes;
  }

  public void setRawLineHashes(@Nullable String lineHashes) {
    this.lineHashes = lineHashes;
  }

  public List<String> getLineHashes() {
    if (lineHashes == null) {
      return Collections.emptyList();
    }
    return LINES_HASHES_SPLITTER.splitToList(lineHashes);
  }
}
