/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.auth.saml;

import java.security.PrivateKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

class SamlPrivateKeyConverterTest {

  private static final String VALID_PRIVATE_KEY  = """
    -----BEGIN PRIVATE KEY-----
    MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDUHbeiASLUJZGK
    vuKbBDy0q83Dx+yibRtvhlN/Wo6STPOIrkYEI/mGVN9ABaczVA3rrDsX4yL26qyG
    txWerypKiGzMeg7uJsq4734up3LUcwS+lP6xn9E2Tb3pmOyJ8g921I4urgFwwQ3W
    MLZ2es5VlybsODHhw0DFaS5qAwgqXFZ5ZuMVSRNQg4XMxjq+IKk27saA6M5d/Hvu
    Q6SRKUreA2HYC1SOfUviKGB4BakRYqH4OZkqriUUPcRx/rl7Jye16yxLj3lNxAAQ
    LAISqpVA5fJHUnm+TGFriaBuDgmL+jjlVZDZpVFCNEpq9JRf1CFeEFyNZ2LvBQYn
    a/hoEGiLAgMBAAECggEACr9AtRgapCYNJDr8rz79Nmg4AjHoduhRSgrDw0Ga1KXK
    dwPhb0dFVr2JHCzNJmgdVnIBAnpTJLCKMj8nfMVCPsl/vbJ3fDCB9/HGcmWwhzwW
    ovNYnjZLOtWgiCvj3C9kAjDNzWaqkwRtB4urSwB44NRKOBC20kn6x6XHIL7rlH3G
    hNMKau+W0LFK2xev8Rm3Ze5j8GRIY92TzSPDSxOvwOKBiAqBTyK+lPFo7KbZ9znf
    KFlVJAhoDQvYeApOWcZNkKUDgn87ctt1KcWXW3qMRcYrVIScZv8lvu/Pd8CMIqk8
    varjnYhl76TdvlOrWPbLqRGawfCWWqfZsdwbYegUeQKBgQD6mJXuQMKFLZ2Tz9tD
    2wOwv7zSbRjLA/up60ZODHG1abkPae98xtnhcItaocm5EnczaTe6iaRUHViHMoYB
    WXlmMUsizjLvZ/jl3VgudBgDtChZoCwXhdSjzv7g0H5PQDznkEndWKUlkL5DwwiR
    kbDCf0ZSrCeC2uYwvp2RGFQseQKBgQDYsLQGwcphjXnty4izrNMmhQMC73G9kUi9
    I+etzewPr47KWcCcQA84C+xdm1l03b30JoDZUds0bEiJ7fWCRfDhjktzMEjYOJpC
    ioFmceb4X0IKZRUewKfKtiJASQYKnVs4NLtr7O9njj43cX+PkuDTkRf/MtR07w9F
    h9CBSsP0IwKBgQCq+HmqcKKGTGXrF/An7oApEdfY1TgKIrCL8Iop69GUjQoGmyca
    wWybo7Zf4mwHibKr78hmy0vDW7Yvn2fP+eSatVzm5TnZHt2wroBgSTKtLDgvVdlf
    Px6hmDNK3NSga1piPo42pykdZRB6mND6yCSJvl4TP4NBgul0LkjAWpHrOQKBgEhr
    bg9gDxJhZSGrR2m9VehmxeXiPqI7fxLvgAufacioRNGuA2h1JGD34yw5skETptuF
    TsTSza8MjYXDiKzcUTAhDWhW99GDwVKB1dN2N9pEg97Tf6aCftGJ45KWlTVQ996m
    CQl961kxuetvvFEwpoEu2dVJmKXoC7OSO4Yqhaj/AoGBAMryk5JYtu7HTKNzyJzT
    e9Sup0gum+oCtWopK58Oo9cPBJwGgLljrlGJFcirjkEwpcLZ1SAdMN+iebUia893
    7TjVnhAe+/Y/YzKAZcyggBJo7F2mshT4/0mVsw/hzJCNBbC7OihBtd6qT9YsFsIT
    xkGDoWLmoZBN7qeFmjNAS21W
    -----END PRIVATE KEY-----
    """;

  private static final String VALID_PRIVATE_KEY_WITHOUT_COMMENTS = """
    MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDUHbeiASLUJZGK
    vuKbBDy0q83Dx+yibRtvhlN/Wo6STPOIrkYEI/mGVN9ABaczVA3rrDsX4yL26qyG
    txWerypKiGzMeg7uJsq4734up3LUcwS+lP6xn9E2Tb3pmOyJ8g921I4urgFwwQ3W
    MLZ2es5VlybsODHhw0DFaS5qAwgqXFZ5ZuMVSRNQg4XMxjq+IKk27saA6M5d/Hvu
    Q6SRKUreA2HYC1SOfUviKGB4BakRYqH4OZkqriUUPcRx/rl7Jye16yxLj3lNxAAQ
    LAISqpVA5fJHUnm+TGFriaBuDgmL+jjlVZDZpVFCNEpq9JRf1CFeEFyNZ2LvBQYn
    a/hoEGiLAgMBAAECggEACr9AtRgapCYNJDr8rz79Nmg4AjHoduhRSgrDw0Ga1KXK
    dwPhb0dFVr2JHCzNJmgdVnIBAnpTJLCKMj8nfMVCPsl/vbJ3fDCB9/HGcmWwhzwW
    ovNYnjZLOtWgiCvj3C9kAjDNzWaqkwRtB4urSwB44NRKOBC20kn6x6XHIL7rlH3G
    hNMKau+W0LFK2xev8Rm3Ze5j8GRIY92TzSPDSxOvwOKBiAqBTyK+lPFo7KbZ9znf
    KFlVJAhoDQvYeApOWcZNkKUDgn87ctt1KcWXW3qMRcYrVIScZv8lvu/Pd8CMIqk8
    varjnYhl76TdvlOrWPbLqRGawfCWWqfZsdwbYegUeQKBgQD6mJXuQMKFLZ2Tz9tD
    2wOwv7zSbRjLA/up60ZODHG1abkPae98xtnhcItaocm5EnczaTe6iaRUHViHMoYB
    WXlmMUsizjLvZ/jl3VgudBgDtChZoCwXhdSjzv7g0H5PQDznkEndWKUlkL5DwwiR
    kbDCf0ZSrCeC2uYwvp2RGFQseQKBgQDYsLQGwcphjXnty4izrNMmhQMC73G9kUi9
    I+etzewPr47KWcCcQA84C+xdm1l03b30JoDZUds0bEiJ7fWCRfDhjktzMEjYOJpC
    ioFmceb4X0IKZRUewKfKtiJASQYKnVs4NLtr7O9njj43cX+PkuDTkRf/MtR07w9F
    h9CBSsP0IwKBgQCq+HmqcKKGTGXrF/An7oApEdfY1TgKIrCL8Iop69GUjQoGmyca
    wWybo7Zf4mwHibKr78hmy0vDW7Yvn2fP+eSatVzm5TnZHt2wroBgSTKtLDgvVdlf
    Px6hmDNK3NSga1piPo42pykdZRB6mND6yCSJvl4TP4NBgul0LkjAWpHrOQKBgEhr
    bg9gDxJhZSGrR2m9VehmxeXiPqI7fxLvgAufacioRNGuA2h1JGD34yw5skETptuF
    TsTSza8MjYXDiKzcUTAhDWhW99GDwVKB1dN2N9pEg97Tf6aCftGJ45KWlTVQ996m
    CQl961kxuetvvFEwpoEu2dVJmKXoC7OSO4Yqhaj/AoGBAMryk5JYtu7HTKNzyJzT
    e9Sup0gum+oCtWopK58Oo9cPBJwGgLljrlGJFcirjkEwpcLZ1SAdMN+iebUia893
    7TjVnhAe+/Y/YzKAZcyggBJo7F2mshT4/0mVsw/hzJCNBbC7OihBtd6qT9YsFsIT
    xkGDoWLmoZBN7qeFmjNAS21W
    """;

  private final SamlPrivateKeyConverter samlPrivateKeyConverter = new SamlPrivateKeyConverter();

  @ParameterizedTest
  @ValueSource(strings = {VALID_PRIVATE_KEY, VALID_PRIVATE_KEY_WITHOUT_COMMENTS})
  void toPrivateKey_whenPrivateKeyIsValid_succeeds(String privateKeyString) {
    PrivateKey privateKey = samlPrivateKeyConverter.toPrivateKey(privateKeyString);
    assertThat(privateKey).isNotNull();
  }

  @Test
  void toPrivateKey_whenPrivateKeyIsInvalid_throwsException() {
    assertThatRuntimeException()
      .isThrownBy(() -> samlPrivateKeyConverter.toPrivateKey("invalidKey"))
      .withMessage("bla");
  }


}
