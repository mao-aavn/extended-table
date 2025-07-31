# Extended Table

## Description

Extended Table is a JSF component for Axon.ivy that extends the `p:dataTable` component from PrimeFaces. Its main goal is to provide users with the ability to save the state of a table (such as filters, sorting, pagination, and column order) and restore it at any time and from anywhere within the application. This feature enhances user experience by allowing personalized table views to be persisted and reused.

In the background, the table state is persisted as a JSON string in the user's session property map.

## Features

- Save table state (filters, sorting, pagination, column order, etc.)
- Restore saved table state on demand
- Delete saved states from the list
- Reset the table to its default state
- Seamless integration with Axon.ivy and PrimeFaces
