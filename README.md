# Extended Table

## Description

Extended Table is a JSF component for Axon.ivy that extends the `p:dataTable` component from PrimeFaces. Its main goal is to provide users with the ability to save the state of a table (such as filters, sorting, pagination, and column order) and restore it at any time and from anywhere within the application. This feature enhances user experience by allowing personalized table views to be persisted and reused.

In the background, the table state is persisted as a JSON string value, together with the key formed by some factors (formId, tableId, state name), and the persistence are decided by the given controller, the default controller will persist in the user's property map.

## Features

- Save table state (filters, sorting, pagination, column order, etc.)
- Restore saved table state on demand
- Delete saved states from the list
- Reset the table to its default state
- **Share link feature** to generate shareable URLs with table state
- **Customizable button icons** with show/hide controls
- Seamless integration with Axon.ivy and PrimeFaces

## Pre-loading a Saved State

You can automatically load a saved table state when the component is first rendered by using the `initialStateName` attribute. This is useful when you want to open a view with a specific table configuration (pre-applied filters, sorting, etc.).

### Quick Example

```xml
<!-- In your process, set a parameter with the state name -->
<ic:com.axonivy.market.extendedtable.ExtendedTable
    tableId="myTable" 
    widgetVar="myTableWidget"
    value="#{data.items}"
    initialStateName="#{param.stateName}">
    <!-- columns here -->
</ic:com.axonivy.market.extendedtable.ExtendedTable>
```

Then you can navigate to the page with a URL parameter:
```
yourpage.xhtml?stateName=MyFilteredView
```

Or pass it from a process:
```java
in.stateName = "MyFilteredView";
```

The table will automatically load and apply the saved state on first render, showing the user the pre-configured view with all filters, sorting, and pagination already applied.

## Icon Customization

The Extended Table component supports full customization of button icons. You can:
- Override default icons with custom PrimeIcons or Font Awesome icons
- Show/hide icons individually for each button
- Mix icon configurations (some visible, some hidden, some custom)

### Quick Example

```xml
<ic:com.axonivy.market.extendedtable.ExtendedTable
    tableId="myTable" 
    widgetVar="myTableWidget"
    value="#{data.items}"
    saveStateIcon="pi pi-cloud-upload"
    restoreStateIcon="pi pi-undo"
    showDeleteStateIcon="false"
    resetTableIcon="pi pi-sync">
    <!-- columns here -->
</ic:com.axonivy.market.extendedtable.ExtendedTable>
```

For complete documentation and more examples, see the [Icon Customization Guide](docs/ICON_CUSTOMIZATION.md).

## Share Link Feature

The Extended Table component includes a share link feature that allows users to generate a URL containing the current table state. This makes it easy to share specific table configurations with other users or bookmark particular views.


### Quick Example

```xml
<ic:com.axonivy.market.extendedtable.ExtendedTable
    tableId="myTable" 
    widgetVar="myTableWidget"
    value="#{data.items}"
    initialStateName="#{param.stateName}"
    showShareButton="true">
    <!-- columns here -->
</ic:com.axonivy.market.extendedtable.ExtendedTable>
```


## LIMITATIONS & NOTES TBC

- Var declaration
- multipleViewState
- Change ids => impact persisted state
- Dynamic columns

## TODOS

- Check group ID with Peter
- Lazy approach for all showcases
- Import/Export states

