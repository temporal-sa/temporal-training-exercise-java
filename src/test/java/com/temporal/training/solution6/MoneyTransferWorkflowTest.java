package com.temporal.training.solution6;

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
                    .registerSearchAttribute("AccountId", IndexedValueType.INDEXED_VALUE_TYPE_TEXT)
                    .registerWorkflowImplementationTypes(MoneyTransferWorkflowImpl.class)
                    .setDoNotStart(true)
                    .build();

    @Test
    public void testSuccessfulTransfer(
            TestWorkflowEnvironment testEnv, Worker worker, MoneyTransferWorkflow workflow) {
        BankingActivities mockActivities = Mockito.mock(
                BankingActivities.class,
                withSettings().withoutAnnotations()
        );

        worker.registerActivitiesImplementations(mockActivities);
        testEnv.start();

        TransferRequest request = new TransferRequest("account-123", "account-456", 100.0, "transfer-1");

        testEnv.registerDelayedCallback(
            Duration.ofSeconds(1),
            () -> workflow.approve(true)
        );

        String result = workflow.transfer(request);

        assertEquals("Transfer completed successfully", result);
        assertEquals(TransferStatus.COMPLETED, workflow.getStatus());

        verify(mockActivities).withdraw("account-123", 100.0);
        verify(mockActivities).deposit("account-456", 100.0);
        verify(mockActivities, never()).refund(anyString(), anyDouble());
    }

    @Test
    public void testRejectedTransfer(
            TestWorkflowEnvironment testEnv, Worker worker, MoneyTransferWorkflow workflow) {
        BankingActivities mockActivities = Mockito.mock(
                BankingActivities.class,
                withSettings().withoutAnnotations()
        );

        worker.registerActivitiesImplementations(mockActivities);
        testEnv.start();

        TransferRequest request = new TransferRequest("account-123", "account-456", 100.0, "transfer-2");

        testEnv.registerDelayedCallback(
            Duration.ofSeconds(1),
            () -> workflow.approve(false)
        );

        String result = workflow.transfer(request);

        assertEquals("Transfer rejected and refunded", result);
        assertEquals(TransferStatus.CANCELLED, workflow.getStatus());

        verify(mockActivities).withdraw("account-123", 100.0);
        verify(mockActivities).refund("account-123", 100.0);
        verify(mockActivities, never()).deposit(anyString(), anyDouble());
    }

    @Test
    public void testQueryStatus(
            TestWorkflowEnvironment testEnv, Worker worker, MoneyTransferWorkflow workflow) {
        BankingActivities mockActivities = Mockito.mock(
                BankingActivities.class,
                withSettings().withoutAnnotations()
        );

        worker.registerActivitiesImplementations(mockActivities);
        testEnv.start();

        TransferRequest request = new TransferRequest("account-123", "account-456", 100.0, "transfer-3");

        testEnv.registerDelayedCallback(
            Duration.ofMillis(500),
            () -> assertEquals(TransferStatus.PENDING, workflow.getStatus())
        );

        testEnv.registerDelayedCallback(
            Duration.ofSeconds(1),
            () -> workflow.approve(true)
        );

        workflow.transfer(request);
        assertEquals(TransferStatus.COMPLETED, workflow.getStatus());
    }
}
