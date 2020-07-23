package com.kylemoore.gradle.tool;

import com.kylemoore.tool.MockCodegenTool;

import javax.inject.Inject;

public class MockCodegenRunner implements Runnable {

  private final String compileClasspath;
  private final String analysisDir;
  private final String outputFile;

  @Inject
  public MockCodegenRunner(String compileClasspath, String analysisDir, String outputFile) {
    this.compileClasspath = compileClasspath;
    this.analysisDir = analysisDir;
    this.outputFile = outputFile;
  }

  @Override
  public void run() {
    try {
      MockCodegenTool.main(new String[]{compileClasspath, analysisDir, outputFile});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
