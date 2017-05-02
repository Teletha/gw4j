/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import bee.Bee;
import bee.TaskFailure;
import bee.api.Command;
import bee.api.Scope;
import bee.api.Task;
import bee.util.Java;
import bee.util.Java.JVM;
import filer.Filer;
import kiss.I;

/**
 * @version 2017/04/01 19:53:40
 */
public class Test extends Task {

    @Command("Test product codes.")
    public void test() {
        Compile compile = require(Compile.class);
        compile.source();
        compile.test();

        try {
            Path report = project.getOutput().resolve("test-reports");
            Files.createDirectories(report);

            Java.with()
                    .classPath(project.getClasses())
                    .classPath(project.getTestClasses())
                    .classPath(project.getDependency(Scope.Test))
                    .classPath(Bee.class)
                    .enableAssertion()
                    .encoding(project.getEncoding())
                    .workingDirectory(project.getRoot())
                    .run(Junit.class, project.getTestClasses(), report);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2017/04/01 19:53:45
     */
    private static final class Junit extends JVM {

        /**
         * {@inheritDoc}
         */
        @Override
        public void process() throws Exception {
            Path classes = Filer.locate(args[0]);
            List<Path> tests = Filer.walk(classes, "**Test.class");

            if (tests.isEmpty()) {
                ui.talk("Nothing to test");
                return;
            }

            // execute test classes
            int runs = 0;
            int skips = 0;
            boolean shows = false;
            long times = 0;
            List<Failure> fails = new ArrayList();
            List<Failure> errors = new ArrayList();

            JUnitCore core = new JUnitCore();
            ui.talk("Run\t\tFail\t\tError \tSkip\t\tTime(sec)");

            for (Path path : tests) {
                String fqcn = classes.relativize(path).toString();
                fqcn = fqcn.substring(0, fqcn.length() - 6).replace(File.separatorChar, '.');

                try {
                    Class clazz = Class.forName(fqcn);

                    if (!validate(clazz)) {
                        continue;
                    }

                    Result result = core.run(clazz);
                    List<Failure> failures = result.getFailures();
                    List<Failure> fail = new ArrayList();
                    List<Failure> error = new ArrayList();

                    for (Failure failure : failures) {
                        if (failure.getException() instanceof AssertionError) {
                            fail.add(failure);
                        } else {
                            error.add(failure);
                        }
                    }

                    boolean show = !fail.isEmpty() || !error.isEmpty() || result.getIgnoreCount() != 0;

                    if (!shows) {
                        shows = show;
                    }

                    ui.talk(buildResult(result.getRunCount(), fail.size(), error.size(), result.getIgnoreCount(), result
                            .getRunTime(), fqcn) + (show ? "" : "\r"));

                    runs += result.getRunCount();
                    skips += result.getIgnoreCount();
                    times += result.getRunTime();
                    fails.addAll(fail);
                    errors.addAll(error);
                } catch (Error e) {
                    ui.talk(buildResult(0, 0, 0, 0, 0, ""));
                } catch (ClassNotFoundException e) {
                    throw I.quiet(e);
                }
            }

            if (shows) ui.talk("Run\t\tFail\t\tError \tSkip\t\tTime(sec)");
            ui.talk(buildResult(runs, fails.size(), errors.size(), skips, times, "TOTAL (" + tests.size() + " classes)"));

            if (fails.size() != 0 || errors.size() != 0) {
                TaskFailure failure = new TaskFailure("Test has failed.");
                buildFailure(failure, errors);
                buildFailure(failure, fails);
                throw failure;
            }
        }

        /**
         * <p>
         * Validate the target class which is test case or not.
         * </p>
         * 
         * @param test A target class.
         * @return A result.
         */
        private boolean validate(Class test) {
            for (Method method : test.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && method.isAnnotationPresent(org.junit.Test.class)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * <p>
         * Build result message.
         * </p>
         */
        private String buildResult(int tests, int fails, int errors, int ignores, long time, String name) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%-8d\t%-8d\t%-8d\t%-8d\t%.3f\t\t\t%s", tests, fails, errors, ignores, (float) time / 1000, name));

            if (errors != 0) {
                builder.append("  <<<  ERROR!");
            } else if (fails != 0) {
                builder.append("  <<<  FAILURE!");
            }
            return builder.toString();
        }

        /**
         * <p>
         * Build {@link TaskFailure}.
         * </p>
         * 
         * @param failure A current resolver.
         * @param list A list of test results.
         */
        private void buildFailure(TaskFailure failure, List<Failure> list) {
            for (Failure fail : list) {
                Description desc = fail.getDescription();
                Class test = desc.getTestClass();
                StackTraceElement element = fail.getException().getStackTrace()[0];
                int line = element.getClassName().equals(test.getName()) ? element.getLineNumber() : 0;

                String target = desc.getClassName() + "#" + desc.getMethodName();

                if (line != 0) {
                    target = target + "(" + test.getSimpleName() + ".java:" + line + ")";
                }
                failure.solve("Fix " + target + "\r\n  >>>  " + fail.getMessage().trim().split("[\\r\\n]")[0]);
            }
        }
    }
}
