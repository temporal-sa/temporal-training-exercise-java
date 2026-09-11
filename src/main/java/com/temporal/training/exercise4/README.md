# Exercise 4: Visibility & Monitoring with Custom Search Attributes (30 min)

## Learning Objectives
- Create and use Custom Search Attributes for workflow filtering
- Upsert Search Attributes from within workflows
- Understand how Search Attributes enable workflow discovery
- Query workflows using custom attributes

## Tasks

### 1. Add Search Attribute Import (2 min)
- Import `SearchAttributeKey` from `io.temporal.common`
- Review the SearchAttributeKey API

### 2. Implement Search Attribute Upsert (15 min)
- Add `Workflow.upsertTypedSearchAttributes()` call in the transfer method
- Use `SearchAttributeKey.forKeyword("AccountId")` to create the key
- Set the value to `request.fromAccount()` for filtering by source account
- Place the upsert call early in the workflow execution

### 3. Test Search Attribute Functionality (13 min)
- Run the workflow and verify it executes successfully
- Use Temporal Web UI to view the workflow with custom search attributes
- Verify the AccountId search attribute appears in the workflow details

## Key Concepts
- **Search Attributes**: Custom metadata for workflow filtering and discovery
- **Keyword Search Attributes**: Text-based attributes for exact matching
- **Workflow.upsertTypedSearchAttributes()**: Method to set search attributes from workflows
- **SearchAttributeKey**: Type-safe way to define search attribute keys

## Prerequisites
Make sure Temporal server is started with the AccountId search attribute:
```bash
temporal server start-dev --search-attribute AccountId=Text
```

## Testing
1. Start the worker: `./gradlew execute -PmainClass=com.temporal.training.exercise4.StartWorker`
2. Run the workflow: `./gradlew execute -PmainClass=com.temporal.training.exercise4.StartWorkflow`
3. Open Temporal Web UI (http://localhost:8233) and verify the AccountId search attribute

## Expected Behavior
- Workflow executes normally with all previous functionality
- AccountId search attribute is visible in Temporal Web UI
- Can filter workflows by AccountId in the Web UI

## Next Steps
Exercise 6 will focus on testing strategies with TestWorkflowExtension and time manipulation.
