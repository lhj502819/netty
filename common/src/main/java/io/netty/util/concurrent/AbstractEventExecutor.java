/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util.concurrent;

import io.netty.util.internal.UnstableApi;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for {@link EventExecutor} implementations.
 *
 * 对EventExecutor进行了部分默认实现
 */
public abstract class AbstractEventExecutor extends AbstractExecutorService implements EventExecutor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractEventExecutor.class);

    static final long DEFAULT_SHUTDOWN_QUIET_PERIOD = 2;
    static final long DEFAULT_SHUTDOWN_TIMEOUT = 15;

    /**
     * 自己所属的EventExecutorGroup，初始化时指定
     */
    private final EventExecutorGroup parent;
    /**
     * 只有自己，遍历时使用{@link #iterator()}
     */
    private final Collection<EventExecutor> selfCollection = Collections.<EventExecutor>singleton(this);

    protected AbstractEventExecutor() {
        this(null);
    }

    protected AbstractEventExecutor(EventExecutorGroup parent) {
        this.parent = parent;
    }

    @Override
    public EventExecutorGroup parent() {
        return parent;
    }

    /**
     * next获取自己
     */
    @Override
    public EventExecutor next() {
        return this;
    }

    /**
     * 判断当前线程是否在EventLoop中，不指定线程就使用当前线程，具体的判断逻辑由子类实现，这里还只是进行抽象实现
     */
    @Override
    public boolean inEventLoop() {
        return inEventLoop(Thread.currentThread());
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return selfCollection.iterator();
    }

    /**
     * 设置默认的shutdown参数,实现自{@link EventExecutorGroup}
     *  优雅关闭，方法被调用后isShuttingDown会返回true，quietPeriod时间内没有任务被提交才会真正的shutdown，如果quietPeriod时间内有任务提交进来，那quietPeriod就重新计算，
     *   但也不是无限的等待，可以设置最大等待时间timeout，超过timeout无论是否有任务提交进来都shutdown
     */
    @Override
    public Future<?> shutdownGracefully() {
        return shutdownGracefully(DEFAULT_SHUTDOWN_QUIET_PERIOD, DEFAULT_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    public abstract void shutdown();

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }


    /**
     * 还是进行默认实现，至于 Promise 是啥，我们后边会有单独的文章进行讲解
     */
    @Override
    public <V> Promise<V> newPromise() {
        return new DefaultPromise<V>(this);
    }

    /**
     * 还是进行默认实现，至于 ProgressivePromise 是啥，我们后边会有单独的文章进行讲解
     */
    @Override
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return new DefaultProgressivePromise<V>(this);
    }

    /**
     * 这里就不是默认实现了，就是具体实现了，把自己作为Executor、result传了进去，既然是SucceededFuture，那肯定是需要执行结果的
     */
    @Override
    public <V> Future<V> newSucceededFuture(V result) {
        return new SucceededFuture<V>(this, result);
    }

    /**
     * 这里就不是默认实现了，就是具体实现了，把自己作为Executor、result传了进去，既然是FailedFuture，那肯定是需要失败原因的
     */
    @Override
    public <V> Future<V> newFailedFuture(Throwable cause) {
        return new FailedFuture<V>(this, cause);
    }

    /**
     * 这里调用父类 {@link AbstractExecutorService#submit(Runnable)}提交任务，具体代码我贴到下边了，方便大家观看（是不是很贴心 哈哈）
     *
     * public Future<?> submit(Runnable task) {
     *     if (task == null) throw new NullPointerException();
     *     RunnableFuture<Void> ftask = newTaskFor(task, null);
     *     execute(ftask);
     *     return ftask;
     *}
     * 其中newTaskFor被当前类重写了，execute方法并没有被当前类重写，但肯定也是被Netty的某个实现类重写了，不要慌，等会儿我们就知道了
     *
     */
    @Override
    public Future<?> submit(Runnable task) {
        return (Future<?>) super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return (Future<T>) super.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return (Future<T>) super.submit(task);
    }

    /**
     * 重写父类{@link AbstractExecutorService#newTaskFor} (Runnable)}，创建PromiseTask
     * 这里不禁感叹Netty作者的牛逼，把Doug lea大神的代码吃的透透的
     */
    @Override
    protected final <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new PromiseTask<T>(this, runnable, value);
    }

    @Override
    protected final <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new PromiseTask<T>(this, callable);
    }

    /**
     * 没有做定时的实现，因为有专门干这个的 {@link AbstractScheduledEventExecutor }，这就是传说中的单一职责喽..
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay,
                                       TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    /**
     * Try to execute the given {@link Runnable} and just log if it throws a {@link Throwable}.
     *
     * 安全的执行任务，异常了就只打印个日志。。
     */
    protected static void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            logger.warn("A task raised an exception. Task: {}", task, t);
        }
    }

    /**
     * Like {@link #execute(Runnable)} but does not guarantee the task will be run until either
     * a non-lazy task is executed or the executor is shut down.
     *
     * This is equivalent to submitting a {@link AbstractEventExecutor.LazyRunnable} to
     * {@link #execute(Runnable)} but for an arbitrary {@link Runnable}.
     *
     * The default implementation just delegates to {@link #execute(Runnable)}.
     */
    @UnstableApi
    public void lazyExecute(Runnable task) {
        execute(task);
    }

    /**
     * Marker interface for {@link Runnable} to indicate that it should be queued for execution
     * but does not need to run immediately.
     */
    @UnstableApi
    public interface LazyRunnable extends Runnable { }
}
