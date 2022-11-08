/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.Gson;
import org.sonar.core.sarif.Sarif210;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SarifSerializerTest {

  private static final String SARIF_JSON = "{\"message\" : \"A sarif in json format as String.\"}";
  private static final String SARIF_JSON_ENCODED = "H4sIAAAAAAAAAKtWyk0tLk5MT1VSsFJQclQoTizKTFPIzFPIKs7PU0jLL8pNLFFILFYILinKzEvXU6oFACgK7/YxAAAA";

  @Mock
  private Gson gson;

  @InjectMocks
  private SarifSerializer serializer;

  @Test
  public void serializeAndEncode_should_compressInGZipAndEncodeBase64() {
    when(gson.toJson(any(Sarif210.class))).thenReturn(SARIF_JSON);
    Sarif210 sarif210 = mock(Sarif210.class);

    String encoded = serializer.serializeAndEncode(sarif210);

    assertThat(encoded).isEqualTo(SARIF_JSON_ENCODED);
  }

}
