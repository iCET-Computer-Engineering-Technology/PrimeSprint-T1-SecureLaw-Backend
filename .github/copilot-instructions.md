# PrimeSprint Backend - Copilot Review Instructions

When reviewing pull requests in this repository, in addition to the usual copilot review , also strictly enforce the following code quality standards for our Java/Spring Boot backend. Flag any violations clearly:

## 1. Clean Code & Logging
* **NO Print Statements:** Strictly flag any use of `System.out.println()`, `System.err.println()`, or `e.printStackTrace()`. Demand the use of our standard logging framework (SLF4J/Logback).
* **Unused Variables:** Identify and flag any unused, redundant, or unnecessary variables, imports, or methods. 

## 2. Formatting & Syntax
* **Naming Conventions:** Enforce strict standard Java camelCase for variables/methods and PascalCase for classes. Flag spelling errors in variable names or comments.
* **Readability:** Point out inconsistent indentation, unnecessary blank lines, trailing whitespace, or missing code documentation for complex logic.

## 3. Best Practices & Architecture
* **Spring Boot Standards:** Flag tight coupling (enforce `@Autowired`/constructor injection), missing standard annotations, or business logic improperly placed inside Controllers instead of Service layers.
* **Code Smells:** Highlight deeply nested `if/else` statements, hardcoded values/magic numbers (these should be in config files or enums), and overly complex methods that require refactoring.
