# PRD: Dice Selection and Roll

## Problem statement
The DiceRoller app currently has no functionality -- it only shows a default "Hello Android" greeting. Users need to be able to select a dice type and roll it to get a random result. This is the core feature that makes the app useful.

## Goals & success metrics
- Users can select one of five dice types (D4, D6, D8, D12, D20) from a clear selection UI.
- Users can roll the selected dice and immediately see a random numeric result.
- The roll result is a uniformly random integer between 1 and the number of faces (inclusive).
- The feature is the entire main screen of the app (replaces the current placeholder greeting).

## User stories
- As a user, I want to choose a dice by its number of faces, so that I can roll the type of dice I need.
- As a user, I want to tap a "Roll" button after selecting a dice, so that I get a random result.
- As a user, I want to clearly see the result of my roll as a number, so that I can use it in my game.

## Functional requirements
1. The main screen displays five selectable dice options: D4, D6, D8, D12, D20.
2. One dice is selected by default (D6, as the most common).
3. A "Roll" button triggers the roll of the currently selected dice.
4. The result is displayed prominently as a plain number (e.g., "14").
5. Rolling again replaces the previous result.

## Out of scope
- Rolling animations or visual effects.
- Rolling multiple dice at once.
- Roll history or logging.
- Custom dice (arbitrary number of faces).
- Sound effects.
- Dice images or 3D representations.

## Related docs
- [UI Design Spec](../design/main-screen.md)
- [Architecture Plan](../architecture/dice-rolling.md)
