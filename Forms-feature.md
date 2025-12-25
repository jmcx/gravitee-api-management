# Instructions for the new Forms feature

The new feature is called "Forms", and will go into its own left nav item below "Homepage".

In this page, you'll provide a way to create new forms, that will be show to the end user viewing the rendered portal. 

You'll need to support form creation, edition, deletion. 

Users will be able to create multiple forms.

there will be an option to select which form is currently active.

When creating or editing a form, the user will need to manipulate the schema of the form.

This schema will be edited using a simple text editor (e.g. one that looks like VSCode's Monaco editor). When creating a new form, it will come with a default example schema that shows all the possibilities, and the user can edit from there.

The meta schema for the forms is as follows:

The schema is restricted to these primitive types:
String Schema
{
  "type": "string",
  "title": "Display Name",
  "description": "Description text",
  "minLength": 3,
  "maxLength": 50,
  "pattern": "^[A-Za-z]+$",
  "format": "email",
  "default": "user@example.com"
}
Supported formats: email, uri, date, date-time
Number Schema
{
  "type": "number", // or "integer"
  "title": "Display Name",
  "description": "Description text",
  "minimum": 0,
  "maximum": 100,
  "default": 50
}
Boolean Schema
{
  "type": "boolean",
  "title": "Display Name",
  "description": "Description text",
  "default": false
}
Enum Schema
Single-select enum (without titles):
{
  "type": "string",
  "title": "Color Selection",
  "description": "Choose your favorite color",
  "enum": ["Red", "Green", "Blue"],
  "default": "Red"
}
Single-select enum (with titles):
{
  "type": "string",
  "title": "Color Selection",
  "description": "Choose your favorite color",
  "oneOf": [
    { "const": "#FF0000", "title": "Red" },
    { "const": "#00FF00", "title": "Green" },
    { "const": "#0000FF", "title": "Blue" }
  ],
  "default": "#FF0000"
}
Multi-select enum (without titles):
{
  "type": "array",
  "title": "Color Selection",
  "description": "Choose your favorite colors",
  "minItems": 1,
  "maxItems": 2,
  "items": {
    "type": "string",
    "enum": ["Red", "Green", "Blue"]
  },
  "default": ["Red", "Green"]
}
Multi-select enum (with titles):
{
  "type": "array",
  "title": "Color Selection",
  "description": "Choose your favorite colors",
  "minItems": 1,
  "maxItems": 2,
  "items": {
    "anyOf": [
      { "const": "#FF0000", "title": "Red" },
      { "const": "#00FF00", "title": "Green" },
      { "const": "#0000FF", "title": "Blue" }
    ]
  },
  "default": ["#FF0000", "#00FF00"]
}

Multi-select enum (dynamic from environment dictionary):
{
  "type": "array",
  "title": "Requested features",
  "minItems": 1,
  "items": {
    "x-gravitee-dictionary": "subscription_features"
  }
}

Notes:
- `x-gravitee-dictionary` is a Gravitee extension that lets you source multi-select options from an environment dictionary.
- The dictionary is looked up by its **key** in the current environment.
- Dictionary `properties` are expanded into `items.anyOf` with:
  - `const` = property key
  - `title` = property value