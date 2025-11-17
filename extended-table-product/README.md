<!--
Dear developer!     

When you create your very valuable documentation, please be aware that this Readme.md is not only published on github. This documentation is also processed automatically and published on our website. For this to work, the two headings "Demo" and "Setup" must not be changed. Do also not change the order of the headings. Feel free to add sub-sections wherever you want.
-->

# Extended Table

Extended Table is a JSF component for Axon.ivy that extends the `p:dataTable` component from PrimeFaces. Its main goal is to provide users with the ability to save the state of a table (such as filters, sorting, pagination, and column order) and restore it at any time and from anywhere within the application. This utility:
- Allows users to persist and restore table states (filters, sorting, pagination, column order)
- Enhances user experience by allowing personalized table views to be saved and reused
- Seamlessly integrates with Axon.ivy and PrimeFaces components
- Provides flexible persistence options through customizable controllers
- Supports you with an easy-to-use demo implementation to reduce your integration effort

<!--
The explanations under "MY-RRODUCT-NAME" are displayed  e.g. for the Connector A-Trust here: https://market.axonivy.com/a-trust#tab-description   
-->

## Projects
- *extended-table* the main component library
- *extended-table-demo* the demo project showcasing the component features
- *extended-table-test* JUnit tests for the extended table component

## Release Notes

### 0.0.1
This is the initial version

## Demo

## Features
- Use of the Extended Table component in the default mode (Ivy User controller) + database controller
- [JUnit](https://junit.org/junit5/) tests for the main module unit testing
- [Hypersonic SQL DB](http://hsqldb.org/) as the embedded database for the demo project (Demo data + database controller)
- [Persistence Utils](https://market.axonivy.com/persistence-utils) library for robust database access and entity management
- Code viewer displays source code for each demo page. This feature helps developers refer/copy the related code easily.


## Table State Persistence
In the background, the table state is persisted as a JSON string value, together with the key formed by some factors (formId, tableId, state name). The persistence mechanism is decided by the given controller - the default controller will persist in the user's property map.

## Use Cases
The Extended Table component is ideal for:
- Applications with complex data tables requiring frequent filtering and sorting
- Multi-user environments where each user needs personalized table views
- Any scenario where table state persistence improves user productivity

<!--
We use all entries under the heading "Demo" for the demo-Tab on our Website, e.g. for the Connector A-Trust here: https://market.axonivy.com/a-trust#tab-demo  
-->

## Setup

To use the Extended Table component in your Axon.ivy project, simply include the dependency in your project:

```xml
<dependency>
  <groupId>com.axonivy.market.extendedtable</groupId>
  <artifactId>extended-table</artifactId>
  <version>0.0.1</version>
  <type>iar</type>
</dependency>
```

And start using the component in your XHTML pages:

Example XHTML usage:

```xhtml
<!-- register namespaces as required by your project -->
<html ...
	xmlns:ic="http://ivyteam.ch/jsf/component"
	...>

<h:body>
  <h:form id="myForm">
    <ic:com.axonivy.market.extendedtable.ExtendedTable
    id="extendedTable"
    value="#{myBean.items}"
    tableId="productsTable"
    paginator="true"
    rows="10"
    resizableColumns="true"
    ...
    >

    <f:facet name="event">
        <p:ajax event="rowSelect" listener="#{data.bean.onRowSelect}"
            update="form:msgs" />
    </f:facet>

    <p:column headerText="ID" sortBy="#{item.id}">
      <h:outputText value="#{item.id}" />
    </p:column>

    <p:column headerText="Name" filterBy="#{item.name}"
        sortBy="#{item.name}" filterMatchMode="contains">
        <f:facet name="filter">
            <p:inputText id="nameFilter" styleClass="custom-filter" onchange="filterTable()" />
        </f:facet>

        <h:outputText value="#{item.name}" />
    </p:column>
    ...

    </ic:com.axonivy.market.extendedtable.ExtendedTable>
  </h:form>
</h:body>
</html>
```

Notes:
- formId/tableId/stateName form the persistence key.
- controller is optional; omit to use the default Ivy user controller or provide a custom controller bean to change persistence storage.
- The extended component supports all p:dataTable features and tags (filters, sorting, pagination, column ordering).

The component extends PrimeFaces `p:dataTable` and provides additional attributes for managing table state persistence.

### Share Link Feature

The Extended Table component includes a **share link feature** that allows users to generate shareable URLs containing the current table state (filters, sorting, pagination, etc.). This is useful for:
- Sharing specific table views with your bussiness partners
- Creating bookmarks for frequently used configurations
- Providing deep links in emails or documentation

**Important:** For the share link feature to work properly, you **must** include the `initialStateName` attribute. This attribute allows the table to automatically load the shared state when users click on the shared link.

**Usage:**

```xml
<ic:com.axonivy.market.extendedtable.ExtendedTable
    tableId="myTable" 
    widgetVar="myTableWidget"
    value="#{data.items}"
    showShareButton="true"
    initialStateName="#{param.stateName}">
    <!-- columns here -->
</ic:com.axonivy.market.extendedtable.ExtendedTable>
```

**Required Attributes:**
- `initialStateName`: **Mandatory for share feature** - Must be set to `#{param.stateName}` or similar URL parameter binding to enable loading of shared states
- `showShareButton`: Set to `true` to display the share button (default: `false`, hidden)

When enabled, users can click the share button to get a URL that preserves the current table configuration. The `initialStateName` attribute ensures the shared state is automatically restored when the link is opened.

### Code Viewer Feature (Demo Project)

The **extended-table-demo** project includes a developer-friendly code viewer that displays source code for each demo page. This feature helps developers refer/copy the related code easily.

**Features:**
- Floating "View Source" button on demo pages
- Syntax-highlighted XHTML and Java code
- Copy-to-clipboard functionality
- Automatic detection of related source files
- Modal dialog with tabbed interface

<!--
The entries under the heading "Setup" are filled in this tab, e.g. for the Connector A-Trust here: https://market.axonivy.com/a-trust#tab-setup. 
-->
