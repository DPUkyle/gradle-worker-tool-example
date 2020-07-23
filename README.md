I created this repository as a discussion point to learn about changes in the Gradle Worker API between version 5.2.1 and 5.6+.

# Overview
The example involves three components:
1. A standalone code generator tool with no dependencies (`:mock-codegen-tool`).  
It could be invoked from the CLI using its main method, but historically is invoked via Gradle's `Project#javaexec`.
2. A Gradle plugin which creates a Worker API-enabled task to invoke the tool (`:mock-codegen-plugin`).
Note that the tool is applied to an isolated, named Configuration and is not a direct dependency of the plugin jar.
3. A dummy project to apply and invoke the code generator plugin and execute its task (`:consumer`).

## Implementation details and intent
The intention is to "warm-up" a worker daemon whose classpath contains only the codegen tool.

This daemon should be persistent and long-lived, and able to receive multiple units of work.

A unique attribute of this "tool" is that it receives an additional classpath-like argument, which it proceeds
to classload in a single-use, disposable classloader.  After completion of the unit of work, it is assumed that this
disposable classloader is GC'd and the worker daemon becomes once again available.  This daemon could be invoked dozens 
of times in a large multi-module project; my assumption is that "warming-up" the daemon with the tool and it's hypothetical
dependencies would be a performance advantage when compared to a simple `Project#javaexec`, which works well.

# The problem
When invoked with "Legacy" Worker APIs (`WorkerExecutor#submit`) and Gradle 5.6.4, the classloader contains the 
buildscript classpath of the invoking task's project.  ABI incompatibilities can result, as detailed below.  This 
behavior was not observed in Gradle 5.2.1.

# Steps to reproduce
Please try the following three scenarios.

The trap is that the dummy project `:consumer` is compiled with Guava v7, which added a method `Lists#reverse`.

The mocked code generator will classload the compile classpath of `:consumer` and do a reflective check for this method.

Guava v6 is on the buildscript classpath of `:consumer`; if it is seen by `MockCodegenTool` failures will occur.

## Scenario one: "Legacy" API, Gradle 5.2.1 runtime
`$ git checkout branch-5.2.1`
`$ ./gradlew :mock-codegen-plugin:assemble && ./gradlew :consumer:codegen --rerun-tasks`

Everything is fine; see MockCodegenTool line 38.
The Worker Daemon contains the entire Gradle API, but does not contain other conflicting dependencies.

## Scenario two: "Legacy" API, Gradle 5.6.4 runtime
`$ git checkout branch-5.6.4`
`$ ./gradlew :mock-codegen-plugin:assemble && ./gradlew :consumer:codegen --rerun-tasks`

Kaboom.  The worker daemon's classpath contains `mock-codegen-tool.jar, mock-codegen-plugin.jar, guava-r06.jar]`
The presence of guava-r06 breaks code which was compiled against a higher API verison.

## Scenario two: "New" API, Gradle 5.6.4 runtime
`$ git checkout branch-5.6.4-new`
`$ ./gradlew :mock-codegen-plugin:assemble && ./gradlew :consumer:codegen --rerun-tasks`

TBD

## Debugging
To debug the problem, uncomment `consumer/build.gradle` line 22.  You will then be prompted to connect to the worker 
daemon at localhost:5005.