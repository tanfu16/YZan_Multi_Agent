# Skills Overview

This directory contains the first version of the project's skill documents.

These skills are designed as prompt modules for progressive disclosure, so that MCP-related instructions do not need to be embedded into the main prompt all the time.

## Current Skills

### 1. material-store-search-skill

File:
- `material-store-search-skill.md`

Purpose:
- search for nearby offline stores for decoration materials

Backed by:
- AMap MCP

Java-side mapping:
- `AmapMcpConfig`
- `AmapMcpMaterialSearchClient`

### 2. furniture-search-skill

File:
- `furniture-search-skill.md`

Purpose:
- search for furniture products on e-commerce platforms

Backed by:
- Playwright MCP

Java-side mapping:
- `PlaywrightMcpConfig`
- `PlaywrightMcpFurnitureSearchClient`

## Recommended Usage

Use these skills as progressive prompt disclosure modules:
- only expose `material-store-search-skill` when the user asks for offline material purchasing locations
- only expose `furniture-search-skill` when the user asks for online product search

This keeps the main prompt lighter and prevents MCP usage instructions from polluting unrelated tasks.
