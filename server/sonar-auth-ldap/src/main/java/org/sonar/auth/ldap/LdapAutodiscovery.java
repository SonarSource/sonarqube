/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth.ldap;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * @author Evgeny Mandrikov
 */
@ServerSide
public class LdapAutodiscovery {

  private static final Logger LOG = Loggers.get(LdapAutodiscovery.class);

  /**
   * Get the DNS domain name (eg: example.org).
   *
   * @return DNS domain
   * @throws java.net.UnknownHostException if unable to determine DNS domain
   */
  public static String getDnsDomainName() throws UnknownHostException {
    return getDnsDomainName(InetAddress.getLocalHost().getCanonicalHostName());
  }

  /**
   * Extracts DNS domain name from Fully Qualified Domain Name.
   *
   * @param fqdn Fully Qualified Domain Name
   * @return DNS domain name or null, if can't be extracted
   */
  public static String getDnsDomainName(String fqdn) {
    if (fqdn.indexOf('.') == -1) {
      return null;
    }
    return fqdn.substring(fqdn.indexOf('.') + 1);
  }

  /**
   * Get the DNS DN domain (eg: dc=example,dc=org).
   *
   * @param domain DNS domain
   * @return DNS DN domain
   */
  public static String getDnsDomainDn(String domain) {
    StringBuilder result = new StringBuilder();
    String[] domainPart = domain.split("[.]");
    for (int i = 0; i < domainPart.length; i++) {
      result.append(i > 0 ? "," : "").append("dc=").append(domainPart[i]);
    }
    return result.toString();
  }

  /**
   * Get LDAP server(s) from DNS.
   *
   * @param domain DNS domain
   * @return LDAP server(s) or empty if unable to determine
   */
  public List<LdapSrvRecord> getLdapServers(String domain) {
    try {
      return getLdapServers(new InitialDirContext(), domain);
    } catch (NamingException e) {
      LOG.error("Unable to determine LDAP server(s) from DNS", e);
      return Collections.emptyList();
    }
  }

  List<LdapSrvRecord> getLdapServers(DirContext context, String domain) throws NamingException {
    Attributes lSrvAttrs = context.getAttributes("dns:/_ldap._tcp." + domain, new String[] {"srv"});
    Attribute serversAttribute = lSrvAttrs.get("srv");
    NamingEnumeration<?> lEnum = serversAttribute.getAll();
    SortedSet<LdapSrvRecord> result = new TreeSet<>();
    while (lEnum.hasMore()) {
      String srvRecord = (String) lEnum.next();
      // priority weight port target
      String[] srvData = srvRecord.split(" ");

      int priority = NumberUtils.toInt(srvData[0]);
      int weight = NumberUtils.toInt(srvData[1]);
      String port = srvData[2];
      String target = srvData[3];

      if (target.endsWith(".")) {
        target = target.substring(0, target.length() - 1);
      }
      String server = "ldap://" + target + ":" + port;
      result.add(new LdapSrvRecord(server, priority, weight));
    }
    return new ArrayList<>(result);
  }

  public static class LdapSrvRecord implements Comparable<LdapSrvRecord> {
    private final String serverUrl;
    private final int priority;
    private final int weight;

    public LdapSrvRecord(String serverUrl, int priority, int weight) {
      this.serverUrl = serverUrl;
      this.priority = priority;
      this.weight = weight;
    }

    @Override
    public int compareTo(LdapSrvRecord o) {
      if (this.priority == o.priority) {
        return Integer.compare(o.weight, this.weight);
      }
      return Integer.compare(this.priority, o.priority);
    }

    String getServerUrl() {
      return serverUrl;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      return this.serverUrl.equals(((LdapSrvRecord) obj).serverUrl);
    }

    @Override
    public int hashCode() {
      return this.serverUrl.hashCode();
    }
  }

}
