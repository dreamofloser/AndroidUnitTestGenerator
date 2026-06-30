# Stage 1 Design

## Objective

Stage 1 creates the minimum useful framework loop:

```text
source files -> parser -> class model -> test generator -> test files -> report
```

## Supported Input

- Java files under `src/main/java` by default.
- Public top-level classes.
- Public non-abstract, non-native methods.

## Generated Output

- JUnit4 Java test classes.
- Tests are generated into the same package as the source class.
- Static methods are called directly.
- Instance methods are called on a generated target instance.
- Unknown constructor and method arguments use conservative placeholder values.

## Current Limitations

- No Kotlin parser yet.
- No Android component templates yet.
- No inferred assertions for primitive return values.
- No mocking yet.
- Unknown object parameters are passed as `null`.

These limitations are intentional. They keep Stage 1 small and make the next stages clear.
