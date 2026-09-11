package com.temporal.training.exercise6;

import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

public class MoneyTransferWorkflowTest {

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension =
            TestWorkflowExtension.newBuilder()
                    .registerWorkflowImplementationTypes(MoneyTransferWorkflowImpl.class)
                    // TODO: Register the "AccountId" search attribute with TEXT type
                    // Hint: .registerSearchAttribute("AccountId", IndexedValueType.INDEXED_VALUE_TYPE_TEXT)
                    .setDoNotStart(true)
                    .build();

    @Test
    public void testSuccessfulTransfer(
            TestWorkflowEnvironment testEnv, Worker worker, MoneyTransferWorkflow workflow) {
        // TODO: Create a mock BankingActivities using Mockito
        // Hint: Use Mockito.mock() with withSettings().withoutAnnotations()
        BankingActivities mockActivities = null;

        // TODO: Register the mock activities on the injected Worker, then start the
        // test environment. The extension was built with setDoNotStart(true), so
        // nothing is running until you call testEnv.start().

        TransferRequest request = new TransferRequest("account-123", "account-456", 100.0, "transfer-1");

        // TODO: Register delayed callback to approve the transfer after 1 second
        // Hint: Use testEnv.registerDelayedCallback()

        // TODO: Execute the workflow and verify results
        String result = null;

        // TODO: Add assertions to verify:
        // - Result message is "Transfer completed successfully"
        // - Final status is COMPLETED
        // - withdraw() was called with correct parameters
        // - deposit() was called with correct parameters  
        // - refund() was never called
    }

    @Test
    public void testRejectedTransfer(
            TestWorkflowEnvironment testEnv, Worker worker, MoneyTransferWorkflow workflow) {
        // TODO: Implement test for rejected transfer scenario
        // Similar to testSuccessfulTransfer but:
        // - Send approval(false) instead of approval(true)
        // - Verify result is "Transfer rejected and refunded"
        // - Verify status is CANCELLED
        // - Verify withdraw() and refund() were called, but not deposit()
    }

    @Test
    public void testQueryStatus(
            TestWorkflowEnvironment testEnv, Worker worker, MoneyTransferWorkflow workflow) {
        // TODO: Implement test for query functionality
        // - Register callback to check status is PENDING after 500ms
        // - Register callback to approve after 1 second
        // - Verify final status is COMPLETED
    }
}
