/*
 * Copyright 2012 The Netty Project
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

/**
 * The {@link EventExecutor} is a special {@link EventExecutorGroup} which comes
 * with some handy methods to see if a {@link Thread} is executed in a event loop.
 * Besides this, it also extends the {@link EventExecutorGroup} to allow for a generic
 * way to access methods.
 *
 * 事件执行器
 * 扩展了一些方法，比如获取自己所在的EventExecutroGroup、判断当前线程是否在EventLoop中
 *
 * 这里为什么会继承EventExecutorGroup？
 *     个人认为是EventExecutorGroup是一组EventExecutor，因此两个类会有相同的行为，比如判断单个Executor是否停止了
 *      或者一组Executor是否停止了
 */
public interface EventExecutor extends EventExecutorGroup {

    /**
     * Returns a reference to itself.
     * 返回自己 和{@link EventExecutorGroup}一样
     */
    @Override
    EventExecutor next();

    /**
     * Return the {@link EventExecutorGroup} which is the parent of this {@link EventExecutor},
     * 返回自己所属的EventExecutorGroup
     */
    EventExecutorGroup parent();

    /**
     * Calls {@link #inEventLoop(Thread)} with {@link Thread#currentThread()} as argument
     * 判断当前线程是否在EventLoop线程中
     */
    boolean inEventLoop();

    /**
     * Return {@code true} if the given {@link Thread} is executed in the event loop,
     * {@code false} otherwise.
     * 判断给定的线程是否是EventLoop线程，看名字是不是不太像，可以看下子类{@link SingleThreadEventExecutor#inEventLoop(Thread)}}的实现
     */
    boolean inEventLoop(Thread thread);

    /**
     * Return a new {@link Promise}.
     * 后边的文章会进行讲解
     */
    <V> Promise<V> newPromise();

    /**
     * Create a new {@link ProgressivePromise}.
     * 后边的文章会进行讲解
     */
    <V> ProgressivePromise<V> newProgressivePromise();

    /**
     * Create a new {@link Future} which is marked as succeeded already. So {@link Future#isSuccess()}
     * will return {@code true}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     *
     * 创建一个Success Future，所有监听了这个Future的Listener都会接到这个通知
     */
    <V> Future<V> newSucceededFuture(V result);

    /**
     * Create a new {@link Future} which is marked as failed already. So {@link Future#isSuccess()}
     * will return {@code false}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     *
     * 创建一个 Failed Future，所有监听了这个Future的Listener都会接到这个通知
     */
    <V> Future<V> newFailedFuture(Throwable cause);
}
