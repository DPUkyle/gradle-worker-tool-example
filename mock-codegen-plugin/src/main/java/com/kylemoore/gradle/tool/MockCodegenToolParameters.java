package com.kylemoore.gradle.tool;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.workers.WorkParameters;

public interface MockCodegenToolParameters extends WorkParameters {
  ConfigurableFileCollection getCompileClasspath();
  DirectoryProperty getAnalysisDir();
  RegularFileProperty getOutputFile();
}
