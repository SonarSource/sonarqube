package org.sonar.server.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.updatecenter.common.Release;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.sonar.core.util.FileUtils.deleteQuietly;

public class AbstractPluginDownloader implements Startable {
  private static final Logger LOG = Loggers.get(AbstractPluginDownloader.class);

  protected static final String TMP_SUFFIX = "tmp";
  protected static final String PLUGIN_EXTENSION = "jar";

  protected final HttpDownloader downloader;
  protected final File downloadDir;

  protected AbstractPluginDownloader(File downloadDir, HttpDownloader downloader) {
    this.downloadDir = downloadDir;
    this.downloader = downloader;
  }

  /**
   * Deletes the temporary files remaining from previous downloads
   */
  @Override
  public void start() {
    try {
      forceMkdir(downloadDir);
      for (File tempFile : listTempFile(this.downloadDir)) {
        deleteQuietly(tempFile);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the directory: " + downloadDir, e);
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  public void cancelDownloads() {
    try {
      if (downloadDir.exists()) {
        org.sonar.core.util.FileUtils.cleanDirectory(downloadDir);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to clean the plugin downloads directory: " + downloadDir, e);
    }
  }

  boolean hasDownloads() {
    return !getDownloadedPluginFilenames().isEmpty();
  }

  List<String> getDownloadedPluginFilenames() {
    List<String> names = new ArrayList<>();
    for (File file : listPlugins(this.downloadDir)) {
      names.add(file.getName());
    }
    return names;
  }

  /**
   * @return the list of download plugins as {@link PluginInfo} instances
   */
  public Collection<PluginInfo> getDownloadedPlugins() {
    return listPlugins(this.downloadDir)
      .stream()
      .map(PluginInfo::create)
      .collect(MoreCollectors.toList());
  }
  
  protected void download(Release release) {
    try {
      downloadRelease(release);
    } catch (Exception e) {
      String message = String.format("Fail to download the plugin (%s, version %s) from %s (error is : %s)",
        release.getArtifact().getKey(), release.getVersion().getName(), release.getDownloadUrl(), e.getMessage());
      LOG.debug(message, e);
      throw new IllegalStateException(message, e);
    }
  }

  private void downloadRelease(Release release) throws URISyntaxException, IOException {
    String url = release.getDownloadUrl();

    URI uri = new URI(url);
    if (url.startsWith("file:")) {
      // used for tests
      File file = toFile(uri.toURL());
      copyFileToDirectory(file, downloadDir);
    } else {
      String filename = substringAfterLast(uri.getPath(), "/");
      if (!filename.endsWith("." + PLUGIN_EXTENSION)) {
        filename = release.getKey() + "-" + release.getVersion() + "." + PLUGIN_EXTENSION;
      }
      File targetFile = new File(downloadDir, filename);
      File tempFile = new File(downloadDir, filename + "." + TMP_SUFFIX);
      downloader.download(uri, tempFile);
      copyFile(tempFile, targetFile);
      deleteQuietly(tempFile);
    }
  }

  private static Collection<File> listTempFile(File dir) {
    return FileUtils.listFiles(dir, new String[] {TMP_SUFFIX}, false);
  }

  private static Collection<File> listPlugins(File dir) {
    return FileUtils.listFiles(dir, new String[] {PLUGIN_EXTENSION}, false);
  }
}
