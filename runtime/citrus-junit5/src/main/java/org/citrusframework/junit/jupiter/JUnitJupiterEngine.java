/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.junit.jupiter;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.citrusframework.TestClass;
import org.citrusframework.TestSource;
import org.citrusframework.junit.jupiter.main.JUnitCitrusTest;
import org.citrusframework.main.AbstractTestEngine;
import org.citrusframework.main.TestRunConfiguration;
import org.citrusframework.main.scan.ClassPathTestScanner;
import org.citrusframework.main.scan.JarFileTestScanner;
import org.citrusframework.util.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

public class JUnitJupiterEngine extends AbstractTestEngine {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(JUnitJupiterEngine.class);

    private final Set<TestExecutionListener> testExecutionListeners = new LinkedHashSet<>();

    private boolean printSummary = true;

    public JUnitJupiterEngine(TestRunConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void run() {
        LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();

        if (getConfiguration().getTestSources().isEmpty()) {
            addTestPackages(requestBuilder, getConfiguration());
        } else {
            addTestClasses(requestBuilder, getConfiguration());
            addTestSources(requestBuilder, getConfiguration());
        }

        LauncherDiscoveryRequest request = requestBuilder.build();

        SummaryGeneratingListener listener = null;
        if (printSummary) {
            listener = new SummaryGeneratingListener();
        }

        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            launcher.registerTestExecutionListeners(testExecutionListeners.toArray(TestExecutionListener[]::new));

            if (printSummary) {
                launcher.registerTestExecutionListeners(listener);
            }

            launcher.execute(request);
        }

        if (printSummary && listener != null) {
            TestExecutionSummary summary = listener.getSummary();
            summary.printTo(new PrintWriter(System.out));
        }
    }

    private void addTestSources(LauncherDiscoveryRequestBuilder requestBuilder, TestRunConfiguration configuration) {
        List<TestSource> testSources = configuration.getTestSources().stream()
                .filter(source -> !"java".equals(source.getType()))
                .toList();

        for (TestSource source : testSources) {
            logger.info(String.format("Running test source %s", source.getName()));

            JUnitCitrusTest.setSourceName(source.getName());
            JUnitCitrusTest.setSource(Optional.ofNullable(source.getFilePath()).orElse(""));
            requestBuilder.selectors(selectClass(JUnitCitrusTest.class));
        }
    }

    private void addTestPackages(LauncherDiscoveryRequestBuilder requestBuilder, TestRunConfiguration configuration) {
        List<String> packagesToRun = configuration.getPackages();
        if (packagesToRun == null || packagesToRun.isEmpty()) {
            packagesToRun = Collections.singletonList("");
            logger.info("Running all tests in project");
        }

        List<DiscoverySelector> selectors = new ArrayList<>();
        for (String packageName : packagesToRun) {
            if (StringUtils.hasText(packageName)) {
                logger.info(String.format("Running tests in package %s", packageName));
            }

            List<TestClass> classesToRun;
            if (configuration.getTestJar() != null) {
                classesToRun = new JarFileTestScanner(configuration.getTestJar(),
                        configuration.getIncludes()).findTestsInPackage(packageName);
            } else {
                classesToRun = new ClassPathTestScanner(Test.class, configuration.getIncludes()).findTestsInPackage(packageName);
            }

            classesToRun.stream()
                    .peek(testClass -> logger.info(String.format("Running test %s",
                            Optional.ofNullable(testClass.getMethod()).map(method -> testClass.getName() + "#" + method)
                                    .orElseGet(testClass::getName))))
                    .map(testClass -> {
                        try {
                            Class<?> clazz;
                            if (configuration.getTestJar() != null) {
                                clazz = Class.forName(testClass.getName(), false,
                                        new URLClassLoader(new URL[]{configuration.getTestJar().toURI().toURL()}, getClass().getClassLoader()));
                            } else {
                                clazz = Class.forName(testClass.getName());
                            }
                            return clazz;
                        } catch (ClassNotFoundException | MalformedURLException e) {
                            logger.warn("Unable to read test class: " + testClass.getName());
                            return Void.class;
                        }
                    })
                    .filter(clazz -> !clazz.equals(Void.class))
                    .forEach(clazz -> selectors.add(selectClass(clazz)));

            requestBuilder.selectors(selectors);

            logger.info(String.format("Found %s test classes to execute", selectors.size()));
        }
    }

    private void addTestClasses(LauncherDiscoveryRequestBuilder requestBuilder, TestRunConfiguration configuration) {
        List<TestClass> testClasses = configuration.getTestSources().stream()
                .filter(source -> "java".equals(source.getType()))
                .map(TestSource::getName)
                .map(TestClass::fromString)
                .toList();

        List<DiscoverySelector> selectors = new ArrayList<>();
        for (TestClass testClass : testClasses) {
            logger.info(String.format("Running test %s",
                    Optional.ofNullable(testClass.getMethod()).map(method -> testClass.getName() + "#" + method)
                            .orElseGet(testClass::getName)));

            try {
                Class<?> clazz;
                if (configuration.getTestJar() != null) {
                    clazz = Class.forName(testClass.getName(), false,
                            new URLClassLoader(new URL[]{configuration.getTestJar().toURI().toURL()}, getClass().getClassLoader()));
                } else {
                    clazz = Class.forName(testClass.getName());
                }

                if (StringUtils.hasText(testClass.getMethod())) {
                    selectors.add(selectMethod(clazz, testClass.getMethod()));
                } else {
                    selectors.add(selectClass(clazz));
                }

            } catch (ClassNotFoundException | MalformedURLException e) {
                logger.warn("Unable to read test class: " + testClass.getName());
            }
        }

        requestBuilder.selectors(selectors);
    }

    public JUnitJupiterEngine addTestListener(TestExecutionListener testExecutionListener) {
        testExecutionListeners.add(testExecutionListener);
        return this;
    }

    public JUnitJupiterEngine withPrintSummary(boolean enabled) {
        this.printSummary = enabled;
        return this;
    }
}