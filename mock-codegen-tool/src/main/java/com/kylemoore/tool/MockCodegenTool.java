package com.kylemoore.tool;


import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class MockCodegenTool {

  /**
   * args[0]: compileClasspath (':'-delimited)
   * args[1]: analysisDir
   * args[2]: outputFile (unused)
   * @param args
   */
  public static void main(String[] args) throws Exception {
    System.out.println("Running MockCodegenTool");

    List<URL> urls = new ArrayList<>();
    List<File> classpathAndClassesDir = Arrays.stream(args[0].split(File.pathSeparator)).map(File::new).collect(Collectors.toList());
    File classesDir = new File(args[1]);
    classpathAndClassesDir.add(classesDir);
    for (File file : classpathAndClassesDir) {
      try {
        urls.add(file.toURI().toURL());
      } catch (MalformedURLException e) {
        throw new RuntimeException("Unable to parse classpath entry " + file.getAbsolutePath(), e);
      }
    }

    // 5.2.1: getURLs() -> [gradle-5.2.1/lib/plugins/gradle-workers-5.2.1.jar, etc.]  Only contains Gradle APIs; no user-defined JARs.
    // 5.6.4 + legacy API: getURLs() -> [mock-codegen-tool.jar, mock-codegen-plugin.jar, guava-r06.jar]
    ClassLoader workerDaemonClassLoader = Thread.currentThread().getContextClassLoader();

    URLClassLoader singleUseClassLoader = new URLClassLoader(urls.toArray(new URL[0]), workerDaemonClassLoader);

    executeWithChildClassLoader(singleUseClassLoader, () -> {
      Class<?> lists = Class.forName("com.google.common.collect.Lists", false, Thread.currentThread().getContextClassLoader());
      Method[] methods = lists.getMethods();
      if (Arrays.stream(methods).noneMatch(m -> "reverse".equals(m.getName()))) {
        throw new RuntimeException("Could not find Lists#reverse!!!");
      }
      URL listsLocation = lists.getProtectionDomain().getCodeSource().getLocation();
      if (!listsLocation.getFile().endsWith("guava-r07.jar")) {
        throw new RuntimeException("Wrong version of guava; expected guava-r07.jar, got " + listsLocation.toString());
      }
      return null;
    });

  }

  /**
   * Execute the call within the given class loader... handle setting / reverting to
   * previous class loader in a safe manner.
   *
   * @param classLoader the child class loader to set for the duration of the call
   * @param callable the callable to execute within the context of the provided class loader
   * @return the result of the call
   * @throws Exception whatever callable throws
   */
  public static <T> T executeWithChildClassLoader(ClassLoader classLoader,
                                             Callable<T> callable) throws Exception
  {
    ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
    try
    {
      Thread.currentThread().setContextClassLoader(classLoader);
      return callable.call();
    }
    finally
    {
      Thread.currentThread().setContextClassLoader(previousClassLoader);
    }
  }

}
