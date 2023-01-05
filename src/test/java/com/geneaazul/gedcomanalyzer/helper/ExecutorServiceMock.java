package com.geneaazul.gedcomanalyzer.helper;

import org.apache.commons.collections4.IterableUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

public class ExecutorServiceMock implements ExecutorService {

    @Override
    public Future<?> submit(Runnable task) {
        task.run();
        return CompletableFuture.completedFuture(null);
    }

    /*
     * Other methods
     */

    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        return true;
    }

    @Override
    public boolean isTerminated() {
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return true;
    }

    @SneakyThrows
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return CompletableFuture.completedFuture(task.call());
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        return tasks
                .stream()
                .map(this::submit)
                .toList();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
        return invokeAll(tasks);
    }

    @SneakyThrows
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
        return IterableUtils.first(tasks).call();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
        return invokeAny(tasks);
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }

}
