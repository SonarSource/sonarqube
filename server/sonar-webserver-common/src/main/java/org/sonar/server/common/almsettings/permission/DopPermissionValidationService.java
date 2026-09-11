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
package org.sonar.server.common.almsettings.permission;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabServerException;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

/**
 * Validates whether a configured DevOps Platform instance grants the write permissions the SonarQube Remediation Agent
 * needs to clone a repository, push a branch and open a pull/merge request. GitLab and Azure DevOps reuse the existing
 * per-platform validators — the same checks that run when minting an SCM token for the orchestrator — and GitHub goes
 * through {@link GithubRemediationPermissionChecker}, which also inspects what each installation actually granted
 * (SONAR-32166). All of them return a structured {@link DopPermissionCheck} instead of throwing, so the outcome can be
 * surfaced in the UI (SONAR-31626).
 *
 * <p>Only GitHub, GitLab and Azure DevOps are supported; Bitbucket is out of scope. {@link #check(AlmSettingDto)} and
 * {@link #checkAll(List)} always validate live. {@link #checkCached(AlmSettingDto)} and {@link #checkAllCached(List)}
 * (SONAR-31641) wrap those with an in-memory, per-node cache keyed by the {@code alm_setting}'s unique {@code key}, so
 * that repeated reads of one DevOps Platform configuration share one verdict instead of each triggering a live
 * external call. {@link #checkRefreshed(AlmSettingDto)} is the explicit re-check that bypasses and then updates that
 * cache.
 *
 * <p>{@link #checkForProject(AlmSettingDto, String)} answers the narrower, per-project question, and for GitHub has a
 * cache of its own: a project's verdict comes from the installation covering that project's repository, so it is
 * keyed by configuration <em>and</em> repository rather than by configuration alone (SONAR-32166).
 */
@ServerSide
public class DopPermissionValidationService {

  private static final Logger LOG = LoggerFactory.getLogger(DopPermissionValidationService.class);

  // Short TTL: enough to collapse a burst of near-simultaneous requests for the same configuration (e.g. several
  // connection cards on the DevOps Platform settings page, loaded within the same page render) without keeping a
  // stale verdict around for long. An administrator who has just fixed a configuration does not have to wait it out —
  // checkRefreshed() re-checks that one configuration on demand.
  private static final Duration CACHE_TTL = Duration.ofSeconds(60);
  private static final long MAX_CACHE_ENTRIES = 500;
  // One entry per bound project rather than per configuration, so this bound is the larger of the two. Entries are
  // small: a project verdict carries no installation list.
  private static final long MAX_PROJECT_CACHE_ENTRIES = 2_000;

  private static final String INSUFFICIENT_SCOPE_MARKER = "insufficient scope";

  private final GithubRemediationPermissionChecker githubRemediationPermissionChecker;
  private final GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  private final AzureDevOpsValidator azureDevOpsValidator;
  private final System2 system2;
  // Not final: rebuilt by createForTesting() with a fake Ticker. See that method's javadoc for why this can't be a
  // second constructor overload instead.
  private Cache<String, TimestampedPermissionCheck> cache;
  private Cache<ProjectCheckKey, TimestampedPermissionCheck> projectCache;

  public DopPermissionValidationService(GithubRemediationPermissionChecker githubRemediationPermissionChecker,
    GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator, AzureDevOpsValidator azureDevOpsValidator, System2 system2) {
    this.githubRemediationPermissionChecker = githubRemediationPermissionChecker;
    this.gitlabGlobalSettingsValidator = gitlabGlobalSettingsValidator;
    this.azureDevOpsValidator = azureDevOpsValidator;
    this.system2 = system2;
    // Cache expiry uses a real monotonic ticker, not System2#now(): Guava's Ticker contract expects a monotonic
    // nanosecond source, and wall-clock time can jump (NTP correction, manual change), which would make entries expire
    // early or linger past the intended TTL. System2 is still used for the checkedAt value returned to API callers —
    // that's a display timestamp callers expect to be wall-clock, a different concern from cache bookkeeping.
    this.cache = buildCache(Ticker.systemTicker(), MAX_CACHE_ENTRIES);
    this.projectCache = buildCache(Ticker.systemTicker(), MAX_PROJECT_CACHE_ENTRIES);
  }

  private static <K> Cache<K, TimestampedPermissionCheck> buildCache(Ticker ticker, long maximumSize) {
    return CacheBuilder.newBuilder()
      .expireAfterWrite(CACHE_TTL)
      .maximumSize(maximumSize)
      .ticker(ticker)
      .build();
  }

  /**
   * Package-private, test-only: builds an instance whose cache is driven by a fake {@link Ticker} instead of a real
   * monotonic clock, so TTL expiry can be exercised deterministically without sleeping past a real TTL.
   *
   * <p>This is a static factory rather than a second constructor overload on purpose: this class is constructor-injected
   * into {@code DefaultPermissionChecksController}, a Spring-managed bean. Spring uses a class's sole constructor
   * automatically, but with two or more constructors and no {@code @Autowired} to disambiguate, it falls back to
   * looking for a no-arg constructor and fails server startup — exactly what broke CI when this class briefly had a
   * second (package-private) constructor for this same purpose.
   */
  static DopPermissionValidationService createForTesting(GithubRemediationPermissionChecker githubRemediationPermissionChecker,
    GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator, AzureDevOpsValidator azureDevOpsValidator, System2 system2, Ticker cacheTicker) {
    DopPermissionValidationService service = new DopPermissionValidationService(githubRemediationPermissionChecker, gitlabGlobalSettingsValidator,
      azureDevOpsValidator, system2);
    service.cache = buildCache(cacheTicker, MAX_CACHE_ENTRIES);
    service.projectCache = buildCache(cacheTicker, MAX_PROJECT_CACHE_ENTRIES);
    return service;
  }

  /**
   * Checks the given DevOps Platform configuration against the Remediation Agent's required write permissions.
   *
   * @throws IllegalArgumentException if the configuration's platform is not supported (Bitbucket).
   */
  public DopPermissionCheck check(AlmSettingDto almSetting) {
    return switch (almSetting.getAlm()) {
      case GITHUB -> githubRemediationPermissionChecker.checkConnection(almSetting);
      case GITLAB -> checkGitlab(almSetting);
      case AZURE_DEVOPS -> checkAzure(almSetting);
      case BITBUCKET, BITBUCKET_CLOUD ->
        throw new IllegalArgumentException("DevOps Platform '" + almSetting.getAlm() + "' is not supported by the Remediation Agent");
    };
  }

  /**
   * Checks several configurations in parallel and returns the results in the same order as the input. Each individual
   * check is time-bounded by the ALM client's connect/read timeouts; running them concurrently keeps the total close to
   * the slowest single platform. Uses a virtual thread per check rather than a fixed pool — these calls are blocking
   * I/O, not CPU-bound, and there are at most a handful of supported platforms per instance, so there's no pool sizing
   * to tune and no thread reuse to lose by not sharing an executor across calls. All settings must be supported
   * platforms (see {@link #check(AlmSettingDto)}).
   */
  public List<DopPermissionCheck> checkAll(List<AlmSettingDto> almSettings) {
    return mapInParallel(almSettings, this::check);
  }

  /**
   * Cached counterpart to {@link #check(AlmSettingDto)}. A cache hit performs no external call. Concurrent misses for
   * the same configuration are coalesced into a single live check (Guava {@code Cache.get(key, Callable)} semantics).
   *
   * <p>A failed check is cached like any other outcome. It is tempting to evict it so the next reader retries, but a
   * GitHub check walks every page of the app's installations: while the platform is unreachable or rate-limiting,
   * evicting would turn each page load into a fresh full scan and make the outage worse. An administrator who has
   * fixed the cause does not have to wait the TTL out — {@link #checkRefreshed(AlmSettingDto)} is exactly that.
   */
  public TimestampedPermissionCheck checkCached(AlmSettingDto almSetting) {
    try {
      // Cache#get(key, Callable) declares ExecutionException for any checked exception the loader might throw; our
      // loader (check(AlmSettingDto)) never declares one, so this branch is unreachable in practice, only required by
      // the method signature. An unsupported platform (Bitbucket) throws IllegalArgumentException, which Guava
      // propagates unwrapped as UncheckedExecutionException rather than through this catch.
      return cache.get(almSetting.getKey(), () -> new TimestampedPermissionCheck(check(almSetting), system2.now()));
    } catch (ExecutionException e) {
      throw new IllegalStateException("Failed to compute permission check for '" + almSetting.getKey() + "'", e.getCause());
    }
  }

  /**
   * Runs a live check for one configuration and replaces its cache entry with the result — the explicit
   * "Re-check permissions" action, which exists precisely because the caller does not trust what is cached
   * (SONAR-32166). The result is written whatever it is, a failure included: this action is the way past a cached
   * verdict, so leaving the old entry in place, or clearing it and letting the next reader pay for a fresh scan,
   * would both defeat it.
   *
   * <p>A successful re-check also drops that configuration's project entries, so an administrator who has just fixed
   * a GitHub App does not see a project keep reporting the old verdict for the rest of its TTL. A failed re-check
   * deliberately leaves them alone: dropping them would send every project reading its permissions back to a platform
   * that has just proven to be unreachable, which is the call storm the cache exists to prevent.
   */
  public TimestampedPermissionCheck checkRefreshed(AlmSettingDto almSetting) {
    TimestampedPermissionCheck result = new TimestampedPermissionCheck(check(almSetting), system2.now());
    cache.put(almSetting.getKey(), result);
    if (almSetting.getAlm() == ALM.GITHUB && result.check().status() != PermissionCheckStatus.CHECK_FAILED) {
      projectCache.asMap().keySet().removeIf(key -> key.configurationKey().equals(almSetting.getKey()));
    }
    return result;
  }

  /**
   * Verdict for one project rather than for its whole configuration.
   *
   * <p>For GitHub this is a different question from {@link #checkCached(AlmSettingDto)}, not a cheaper version of it:
   * two projects sharing one configuration are covered by two different installations, which may have approved
   * different permissions, so each project gets its own verdict from its own installation. That is why it cannot be
   * served from the per-configuration cache, whose entries hold the instance-wide verdict — it has a cache of its own,
   * keyed by configuration and repository together, with the same expiry, coalescing and failure handling, and a
   * larger bound, since it holds one entry per bound project rather than one per configuration.
   *
   * <p>GitLab and Azure DevOps have no per-project equivalent: their credential is the same whatever the project, so
   * they keep using the shared per-configuration verdict.
   *
   * @param repositorySlug the repository the project is bound to ({@code owner/repository} on GitHub), or {@code null}
   */
  public TimestampedPermissionCheck checkForProject(AlmSettingDto almSetting, @Nullable String repositorySlug) {
    if (almSetting.getAlm() != ALM.GITHUB) {
      return checkCached(almSetting);
    }
    ProjectCheckKey key = new ProjectCheckKey(almSetting.getKey(), repositorySlug);
    try {
      // See checkCached: the loader declares no checked exception, so ExecutionException is unreachable in practice.
      return projectCache.get(key,
        () -> new TimestampedPermissionCheck(githubRemediationPermissionChecker.checkProject(almSetting, repositorySlug), system2.now()));
    } catch (ExecutionException e) {
      throw new IllegalStateException("Failed to compute permission check for '" + almSetting.getKey() + "'", e.getCause());
    }
  }

  /**
   * Cached counterpart to {@link #checkAll(List)}. Results are returned in the same order as the input. Cache misses
   * (cold entries, or every entry right after startup) are validated in parallel just like {@link #checkAll(List)}, so
   * a cold cache doesn't serialize N external calls.
   */
  public List<TimestampedPermissionCheck> checkAllCached(List<AlmSettingDto> almSettings) {
    return mapInParallel(almSettings, this::checkCached);
  }

  private static <T> List<T> mapInParallel(List<AlmSettingDto> almSettings, Function<AlmSettingDto, T> mapper) {
    if (almSettings.size() <= 1) {
      return almSettings.stream().map(mapper).toList();
    }
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<CompletableFuture<T>> futures = almSettings.stream()
        .map(almSetting -> CompletableFuture.supplyAsync(() -> mapper.apply(almSetting), executor))
        .toList();
      return futures.stream().map(CompletableFuture::join).toList();
    }
  }

  private DopPermissionCheck checkGitlab(AlmSettingDto almSetting) {
    if (isUnsupportedGitlabToken(almSetting)) {
      return DopPermissionCheck.unsupportedTokenType();
    }
    try {
      return gitlabGlobalSettingsValidator.hasApiScope(almSetting) ? DopPermissionCheck.sufficient() : DopPermissionCheck.insufficient();
    } catch (GitlabServerException e) {
      if (e.getHttpStatus() == HTTP_FORBIDDEN && hasInsufficientScope(e.getMessage())) {
        return DopPermissionCheck.insufficient();
      }
      return DopPermissionCheck.checkFailed();
    } catch (Exception e) {
      return DopPermissionCheck.checkFailed();
    }
  }

  /**
   * Runs independently of the scope-based check above: a GitLab bot token (Project/Group Access Token) can have
   * full read/write scope and still be unusable by the Remediation Agent, which needs to exchange the credential for
   * a short-lived user token (SONAR-31770). Any failure while determining bot status falls through to the
   * {@code hasApiScope()} check above, which already classifies invalid-credential/network failures.
   */
  private boolean isUnsupportedGitlabToken(AlmSettingDto almSetting) {
    try {
      return gitlabGlobalSettingsValidator.isBotToken(almSetting);
    } catch (Exception e) {
      LOG.debug("Could not determine whether the GitLab token for configuration '{}' belongs to a bot user; "
        + "falling back to the scope-based check", almSetting.getKey(), e);
      return false;
    }
  }

  private DopPermissionCheck checkAzure(AlmSettingDto almSetting) {
    try {
      azureDevOpsValidator.validate(almSetting);
      // Azure DevOps exposes no token-scope introspection, so a successful connectivity check cannot confirm write access.
      return DopPermissionCheck.unknown();
    } catch (Exception e) {
      return DopPermissionCheck.checkFailed();
    }
  }

  /**
   * Cache key for a project-scoped GitHub check. A typed key rather than a concatenated string: the repository slug is
   * caller-supplied and contains a {@code /}, so any separator chosen for concatenation could also appear inside a
   * value and let two different projects collide on one key. Records give correct equality over both parts, nulls
   * included — a project bound to a configuration but to no repository is its own key, not a prefix of every other.
   */
  private record ProjectCheckKey(String configurationKey, @Nullable String repositorySlug) {
  }

  private static boolean hasInsufficientScope(@Nullable String message) {
    return message != null && message.toLowerCase(Locale.ENGLISH).contains(INSUFFICIENT_SCOPE_MARKER);
  }
}
