package com.kylemoore.gradle.tool;

import com.kylemoore.tool.MockCodegenTool;
import org.gradle.workers.WorkAction;

public abstract class MockCodegenRunner implements WorkAction<MockCodegenToolParameters> {

  @Override
  public void execute() {
    try {
      MockCodegenTool.main(new String[]{getParameters().getCompileClasspath().getAsPath(),
              getParameters().getAnalysisDir().getAsFile().get().getAbsolutePath(),
              getParameters().getOutputFile().getAsFile().get().getAbsolutePath()});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
