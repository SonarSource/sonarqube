package org.sonar.server.db.fake;

import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

public interface FakeMapper {

  void insert(FakeDto dto);

  FakeDto selectByKey(@Param("key") String key);

  List<FakeDto> selectAfterDate(@Param("date") Timestamp date);
}
