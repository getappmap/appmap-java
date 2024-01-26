package com.appland.appmap.util;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.util.FileUtils.relativizeGitPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.config.Properties;

public class GitUtil implements AutoCloseable {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  private Git git;
  private Path fsBase;
  private ObjectId tree; // HEAD^{tree}

  // Map from the Git path of a source root (that starts with
  // src/{main,test}/java) to its real location in the filesystem.
  private static Map<String, Path> sourceRoots = new HashMap<>();

  // Cache the resolution of the partial path in a CodeObject to a full Git path
  // under a source root, or to an empty Optional if resolution previously
  // failed.
  private static Map<String, Optional<String>> sourcePaths = new HashMap<>();

  private GitUtil(Git git, ObjectId tree, Path fsBase) {
    this.git = git;
    this.tree = tree;
    this.fsBase = fsBase;
  }

  public static GitUtil open() throws IOException {
    if (Properties.DisableGit) {
      return null;
    }

    try {
      FileRepositoryBuilder builder = new FileRepositoryBuilder()
          .readEnvironment();

        builder.findGitDir();
        if (builder.getGitDir() == null) {
          logger.debug("Working directory {}, not in a git repo", () -> Paths.get("").toAbsolutePath());
          return null;
        }

      Repository repository = builder.build();
      if (repository.isBare()) {
        logger.warn(
            "Current git repository is an unsupported configuration. No git metadata will be collected, and source paths may be incorrect.");
        return null;
      }

      Path fsBase = AppMapConfig.get().configFile.toAbsolutePath().getParent();
      ObjectId tree = repository.resolve(Constants.HEAD + "^{tree}");
      if (tree == null) {
        logger.warn("Couldn't resolve HEAD to a tree in {}, source paths may be incorrect", fsBase);
        return null;
      }

      return new GitUtil(new Git(repository), tree, fsBase);
    } catch (IOException e) {
      logger.warn(e);
    }
    return null;
  }

  @Override
  public void close() {
    git.close();
  }

  public Repository getRepository() {
    return git.getRepository();
  }

  public String getRepositoryURL() {
    try {
      List<RemoteConfig> remotes = git.remoteList().call();
      Optional<RemoteConfig> originConfig = remotes.stream().filter(r -> r.getName().equals("origin")).findFirst();
      List<URIish> uris = originConfig.isPresent() ? originConfig.get().getURIs() : remotes.get(0).getURIs();
      return uris.get(0).toASCIIString();
    } catch (GitAPIException e) {
      logger.warn(e);
    }
    return "";
  }

  public String getBranch() {
    try {
      return getRepository().getBranch();
    } catch (IOException e) {
      logger.warn(e);
    }
    return "";
  }

  public String getCommit() {
    try {
      return getRepository().resolve(HEAD).name();
    } catch (RevisionSyntaxException | IOException e) {
      logger.warn(e);
    }
    return "";
  }

  // In the code below, the "fs" prefix indicates that the variable holds a
  // filesystem path. The "git" prefix indicates that the variable holds a git
  // path. Hopefully this will make mistakes easier to spot.

  private static final byte[] GIT_MAIN_JAVA = "src/main/java".getBytes();
  private static int GIT_MAIN_JAVA_LEN = GIT_MAIN_JAVA.length;
  private static final byte[] GIT_TEST_JAVA = "src/test/java".getBytes();
  private static int GIT_TEST_JAVA_LEN = GIT_TEST_JAVA.length;


  /**
   * If we're in a git repo, walk all the directories under the directory that
   * contains the config, looking for source directories (i.e. those that start
   * "src/main/java" or "src/test/java").
   *
   * If we're not in a git repo, do nothing.
   *
   * @throws IOException if something goes wrong accessing the git repo
   */
  public static void findSourceRoots() throws IOException {

    try (GitUtil git = GitUtil.open()) {
      if (git == null) {
        return; // not in a repo, nothing to do
      }

      Repository repository = git.getRepository();
      Path fsRoot = repository.getWorkTree().toPath();
      String gitBase = fsRoot.relativize(git.fsBase).toString().replace(File.separator, "/");
      logger.debug("repoRoot: {}, gitCwd: {}", fsRoot, gitBase);
      try (TreeWalk treeWalk = new TreeWalk(repository)) {
        treeWalk.addTree(git.tree);

        if (gitBase.length() > 0) {
          // Ignore the parts of the repo that aren't under the base directory.
          treeWalk.setFilter(PathFilter.create(gitBase));
        }

        // treeWalk.next() returns directories iff setRecursive(false)
        treeWalk.setRecursive(false);

        while (treeWalk.next()) {
          if (treeWalk.isPathSuffix(GIT_MAIN_JAVA, GIT_MAIN_JAVA_LEN)
              || treeWalk.isPathSuffix(GIT_TEST_JAVA, GIT_TEST_JAVA_LEN)) {
            String gitSourceRoot = treeWalk.getPathString();

            // Take advantage of the fact that Path treats '/' as a valid
            // separator, even on Windows:
            Path fsSourceRoot = fsRoot.resolve(Paths.get(gitSourceRoot));

            // Once we've determined the full git path, strip off the base (if
            // necessary):
            if (gitBase.length() > 0) {
              gitSourceRoot = relativizeGitPath(gitBase, gitSourceRoot);
            }

            logger.debug("found {} => {}", gitSourceRoot, fsSourceRoot);
            sourceRoots.put(gitSourceRoot, fsSourceRoot);
          } else if (treeWalk.isSubtree()) {
            treeWalk.enterSubtree();
          }
        }
      }
    }
  }

  /**
   * Given the partial path from a CodeObject, append it to each of the source
   * roots filesystem paths in turn. If the resulting filesystem path refers to an
   * existing file, return the full git path. If no matching filesystem path is
   * found, the original path is returned unmodified
   * 
   * This method caches the resolution of partial paths, to minimize filesystem
   * access.
   *
   * @param gitPartialPath the partial path guessed for a CodeObject
   * @return a git path, as described above
   */
  public static String resolveSourcePath(String gitPartialPath) {
    Optional<String> cached;
    if ((cached = sourcePaths.get(gitPartialPath)) != null) {
      logger.trace("cache hit, {} => {}", gitPartialPath, cached);
      return cached.orElse(gitPartialPath);
    }

    logger.trace("cache miss, {}", gitPartialPath);
    String gitFullPath = null;
    for (Map.Entry<String, Path> sourceRoot : sourceRoots.entrySet()) {
      String gitRoot = sourceRoot.getKey();
      Path fsRoot = sourceRoot.getValue();
      // As above, rely on Path accepting '/' as separator:
      Path fsFullPath = fsRoot.resolve(gitPartialPath);
      boolean exists = Files.exists(fsFullPath);
      logger.trace("{} exists under project root {}? {}", gitPartialPath, fsRoot, exists);
      if (exists) {
        gitFullPath = gitRoot + "/" + gitPartialPath;
        logger.debug("found: {}", gitFullPath);
        break;
      }
    }

    logger.trace("found: {}", gitFullPath);
    sourcePaths.put(gitPartialPath, Optional.ofNullable(gitFullPath));
    return gitFullPath != null ? gitFullPath : gitPartialPath;
  }
}