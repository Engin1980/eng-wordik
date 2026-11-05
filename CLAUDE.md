# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

The repository contains only a specification, `task.md` (written in Czech). There is no source code, build system, test setup or git history yet, so there are no build, lint or test commands to document. When the project is scaffolded, add those commands here.

## What is being built (from `task.md`)

An Android app for children to practise vocabulary ("slovíčka"). The default language pair is CZ-EN, but the design should support any foreign language.

- **Target platform:** Android phones, minimum Android 8 (API 26). A feature may require a higher version if it needs one.
- **Word sets:** the user gets sets of words into the app in some way that is not yet specified ("nějak"). The import mechanism is an open design decision. The user picks a set to be tested on. Words are shown in random order.
- **Test settings:** each test is configured with a parameter `N`. Its meaning depends on the test mode (see below).

### Test modes

1. **Multiple choice.**
   - The app picks a random word and a random side of it (Czech or foreign).
   - It shows N other options from the same set, and the user taps the correct one.
   - The test ends when either the user has gone through all words N times, or the user reaches a target success rate of N % after going through the whole set.
2. **Letter assembly.**
   - The app picks a random word and shows its Czech form.
   - It offers the letters the target word is made of, optionally plus N extra random letters.
   - The user taps letters to build the word.
   - Evaluation is configurable in one of two ways:
     - Evaluate the whole word when the user presses a confirm button, giving success or failure.
     - Evaluate per letter: a correctly placed letter counts as a success, a wrong one as a failure.

Note that `N` is used for several different things in the spec (number of distractors, number of passes, success-rate percentage, number of extra letters). Model them as separate, named settings rather than one shared field.
