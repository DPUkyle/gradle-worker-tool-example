package com.kylemoore.gradle.plugin;

import com.kylemoore.gradle.tool.MockCodegenTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.util.Collections;

public class MockCodegenPlugin implements Plugin<Project> {

  private static final Logger LOG = Logging.getLogger(MockCodegenPlugin.class);

  public static final String MOCK_CODEGEN_TOOL_CONFIGURATION = "mock-codegen-tool";

  @Override
  public void apply(Project project) {
    // create "isolated" tool configuration
    Configuration mockCodegenToolConf = project.getConfigurations().maybeCreate(MOCK_CODEGEN_TOOL_CONFIGURATION);
    mockCodegenToolConf.defaultDependencies(dependencies -> {
      dependencies.add(project.getDependencies().project(Collections.singletonMap("path", ":mock-codegen-tool")));
    });

    // create and configure task
    LOG.lifecycle("Configuring MockCodegenPlugin");

    project.getTasks().register("codegen", MockCodegenTask.class, t -> {
      t.setToolClasspath(project.getConfigurations().getByName(MOCK_CODEGEN_TOOL_CONFIGURATION));
      SourceSet mainSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main");
      FileCollection mainCompileClasspath = mainSourceSet.getCompileClasspath();
      t.setCompileClasspath(mainCompileClasspath);
      t.setClassesToAnalyze(mainSourceSet.getOutput().getClassesDirs());
      t.setOutputFile(new File(project.getBuildDir(), t.getName() + "-output.txt"));
    });

  }
}
