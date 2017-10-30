# Kottage
[![Build Status](https://travis-ci.org/noway1979/kottage.svg?branch=master)](https://travis-ci.org/noway1979/kottage)

Kottage is a JUnit 5 extension aimed at integration/component tests.  Integration tests target test scenarios where real resource interaction is desired to some or full extent.
Kottage provides a pluggable component model to design APIs to any external resource being used in tests.

## Test Resource Lifecycle
All registered resource components take part in JUnit 5's clearly-defined lifecycle allowing to setup and tear down resources. These lifecycle callbacks are executed isolated with any failures being aggregated.

## Test Resource Configuration
Kottage supports a dynamic, hierarchical test configuration allowing to customize test resource configuration from global and host-specific settings down to specific overrides on a singular test.
A static test-independent configuration is given via [Typesafe/Lightbend Config](https://lightbend.github.io/config/) files. It may contain testsite-specific configuration values, such configuration of required external services (URLs, credentials). These global configuration values may be overridden on a per-host basis.
Tests on the other hand may define test resource-specific annotations to define resource-specific setup/behavior or override static configuration values.

## Managing side effects with reversible operations
Integration tests with external resources are inherently stateful and may be even persistent. These side effects need to be reversed to not interfere with any subsequent test's expectations regarding state. Therefore, Kottage offers a generic notion of reversible operations which can be executed by either a test resource implementation or a test. Inverse operations are executed in LIFO order, honoring possible dependencies. Kottage also considers whether an operation belongs to a test class setup or a specific test and accordingly reverts within the corresponding scope.

## Test Resource Parameterization
TODO

## Technical Foundation
Builds upon JUnit Jupiter 5, Typesafe Config and implemented in Kotlin.
