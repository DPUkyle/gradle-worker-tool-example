package com.kylemoore.gradle.tool;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

public class MockCodegenTask extends DefaultTask {

  private final WorkerExecutor workerExecutor;

  private FileCollection toolClasspath = getProject().files();
  private FileCollection classesToAnalyze = getProject().files();
  private FileCollection compileClasspath = getProject().files();
  private File outputFile;
  private boolean debugEnabled;

  @Inject
  public MockCodegenTask(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;
  }

  @Classpath
  public FileCollection getToolClasspath() {
    return toolClasspath;
  }

  public void setToolClasspath(FileCollection toolClasspath) {
    this.toolClasspath = toolClasspath;
  }

  @Classpath
  public FileCollection getClassesToAnalyze() {
    return classesToAnalyze;
  }

  public void setClassesToAnalyze(FileCollection classesToAnalyze) {
    this.classesToAnalyze = classesToAnalyze;
  }

  @Classpath
  public FileCollection getCompileClasspath() {
    return compileClasspath;
  }

  public void setCompileClasspath(FileCollection compileClasspath) {
    this.compileClasspath = compileClasspath;
  }

  @OutputFile
  public File getOutputFile() {
    return outputFile;
  }

  public void setOutputFile(File outputFile) {
    this.outputFile = outputFile;
  }

  @Internal
  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  public void setDebugEnabled(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
  }

  @TaskAction
  public void generate() {
      ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
      try {
          Thread.currentThread().setContextClassLoader(getMinimumClassLoader());
          workerExecutor.submit(MockCodegenRunner.class, config -> {
              config.setIsolationMode(IsolationMode.PROCESS);
              config.setClasspath(getToolClasspath());
              config.forkOptions(javaForkOptions -> {
                  javaForkOptions.setDebug(isDebugEnabled());
              });
              config.setDisplayName("Mock Codegen Daemon");
              config.params(getCompileClasspath().getAsPath(), //compileClasspath
                  getClassesToAnalyze().getSingleFile().getAbsolutePath(), //analysisDir
                  getOutputFile().getAbsolutePath()); //outputFile
          });
      } finally {
          Thread.currentThread().setContextClassLoader(originalClassLoader);
      }
  }

  private ClassLoader getMinimumClassLoader() {
      URLClassLoader contextClassloader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
      URL[] urls = Arrays.stream(contextClassloader.getURLs()).filter(url -> isModule(url, "mock-codegen-plugin")).toArray(URL[]::new);
      return new URLClassLoader(urls, contextClassloader.getParent());
  }

  private boolean isModule(URL url, String moduleName) {
      return url.getFile() != null && new File(url.getFile()).getName().startsWith(moduleName);
  }
}
