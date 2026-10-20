/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.plugins;

import com.google.common.util.concurrent.Striped;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.platform.PluginInfo;

/**
 * Inventory of the static resources shipped by the installed plugins, served over
 * {@code /static/{pluginKey}/{path}} and advertised by {@code api/plugins/installed}.
 * <p>
 * A plugin jar is read on first request and a successful result kept for the lifetime of this
 * component: resources can be large, so hashing them all eagerly would lengthen every startup, and
 * the set of installed plugins cannot change without a restart. A jar that could not be read is not
 * cached, so the next request scans it again.
 */
public class ServerPluginStaticResources {

  private static final Logger LOG = LoggerFactory.getLogger(ServerPluginStaticResources.class);

  private static final String STATIC_FOLDER = "static/";
  private static final String ZIP_RESOURCES_FOLDER = "resources/";
  private static final String PARENT_FOLDER = "..";
  private static final int SCAN_LOCK_STRIPES = 16;

  private final Map<String, List<PluginStaticResource>> resourcesByPluginKey = new ConcurrentHashMap<>();
  // hashing a jar is too slow to run inside ConcurrentHashMap#computeIfAbsent, which would hold the bin lock throughout
  private final Striped<Lock> scanLocks = Striped.lazyWeakLock(SCAN_LOCK_STRIPES);

  public List<PluginStaticResource> getStaticResources(ServerPlugin plugin) {
    PluginInfo pluginInfo = plugin.getPluginInfo();
    String pluginKey = pluginInfo.getKey();
    List<PluginStaticResource> cached = resourcesByPluginKey.get(pluginKey);
    if (cached != null) {
      return cached;
    }
    Lock scanLock = scanLocks.get(pluginKey);
    scanLock.lock();
    try {
      // resources are read from a deploy-dir file, not the originally-installed one (PluginInfo#getNonNullJarFile),
      // since that one can be moved away by an uninstall while the server is still up: plugin.getJar() for a plain
      // jar, or the resources archive copied alongside it for a ZIP-packaged plugin, whose plugin.getJar() is the
      // extracted scanner-side jar and has no resources/ entries at all.
      boolean zipPackaging = pluginInfo.isZipPackaging();
      File archive = zipPackaging ? plugin.getResourcesArchive() : plugin.getJar().getFile();
      if (archive == null) {
        return List.of();
      }
      String resourceFolder = zipPackaging ? ZIP_RESOURCES_FOLDER : STATIC_FOLDER;
      return Optional.ofNullable(resourcesByPluginKey.get(pluginKey))
        .orElseGet(() -> scan(pluginKey, archive, resourceFolder)
          .map(resources -> {
            resourcesByPluginKey.put(pluginKey, resources);
            return resources;
          })
          .orElseGet(List::of));
    } finally {
      scanLock.unlock();
    }
  }

  private static Optional<List<PluginStaticResource>> scan(String pluginKey, File archive, String resourceFolder) {
    List<PluginStaticResource> resources = new ArrayList<>();
    // not a JarFile: the manifest is not needed here and JarFile would verify the signature of every entry
    try (ZipFile zip = new ZipFile(archive)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (isStaticResource(entry, resourceFolder)) {
          resources.add(toStaticResource(zip, entry, resourceFolder));
        }
      }
    } catch (IOException e) {
      LOG.warn("Failed to list the static resources of plugin {}", pluginKey, e);
      return Optional.empty();
    }
    resources.sort(Comparator.comparing(PluginStaticResource::path));
    return Optional.of(List.copyOf(resources));
  }

  private static boolean isStaticResource(ZipEntry entry, String resourceFolder) {
    String name = entry.getName();
    return !entry.isDirectory()
      && name.startsWith(resourceFolder)
      && name.length() > resourceFolder.length()
      // StaticResourcesServlet refuses to serve those, so they must not be advertised
      && !name.contains(PARENT_FOLDER);
  }

  private static PluginStaticResource toStaticResource(ZipFile zip, ZipEntry entry, String resourceFolder) throws IOException {
    String hash;
    try (InputStream in = zip.getInputStream(entry)) {
      hash = DigestUtils.sha256Hex(in);
    }
    // read from the central directory, hence always known here
    return new PluginStaticResource(entry.getName().substring(resourceFolder.length()), hash, entry.getSize());
  }
}
