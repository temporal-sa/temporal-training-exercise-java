package com.temporal.training.exercise2;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * Worker starter for Exercise 2.
 * TODO: Register workflow and activity implementations.
 */
public class StartWorker {
    
    public static final String TASK_QUEUE = "money-transfer-signals-task-queue";
    
    public static void main(String[] args) {
        // TODO: Complete the worker setup:
        // 1. Create WorkflowServiceStubs
        // 2. Create WorkflowClient
        // 3. Create WorkerFactory
        // 4. Create Worker with TASK_QUEUE
        // 5. Register workflow and activity implementations
        // 6. Start the worker
        
        System.out.println("Worker started for task queue: " + TASK_QUEUE);
        
        // TODO: Implement worker setup and start
    }
}