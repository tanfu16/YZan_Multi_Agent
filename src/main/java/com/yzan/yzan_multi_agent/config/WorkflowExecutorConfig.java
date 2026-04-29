package com.yzan.yzan_multi_agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class WorkflowExecutorConfig {

    @Bean(name = "agentWorkflowThreadPoolExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor agentWorkflowThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                4,
                4,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(32),
                runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName("agent-workflow-" + thread.threadId());
            return thread;
        },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
