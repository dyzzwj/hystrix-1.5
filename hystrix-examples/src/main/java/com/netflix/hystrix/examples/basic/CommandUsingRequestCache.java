/**
 * Copyright 2012 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.examples.basic;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Sample {@link HystrixCommand} showing how implementing the {@link #getCacheKey()} method
 * enables request caching for eliminating duplicate calls within the same request context.
 */
// DONE
public class CommandUsingRequestCache extends HystrixCommand<Boolean> {

    private final int value;

    private boolean sleep = false;

    protected CommandUsingRequestCache(int value) {
//        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"), Integer.MAX_VALUE);
//        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
//                // since we're doing work in the run() method that doesn't involve network traffic
//                // and executes very fast with low risk we choose SEMAPHORE isolation
//                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
//                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE).withExecutionTimeoutInMilliseconds(Integer.MAX_VALUE)));
        this.value = value;
    }

    protected CommandUsingRequestCache(int value, boolean sleep) {
        this(value);
        this.sleep = sleep;
    }

    @Override
    protected Boolean run() {
        if (sleep) {
            try {
                Thread.sleep(1 * 1000);
            } catch (InterruptedException e) {
            }
        }
        System.out.println("run:" + value);
        if (false) {
            throw new RuntimeException("日");
        }
        return value == 0 || value % 2 == 0;
    }

    @Override
    protected String getCacheKey() {
        return String.valueOf(value);
    }

    public static class UnitTest {

        @Test
        public void testWithoutCacheHits() {
            HystrixRequestContext context = HystrixRequestContext.initializeContext();
            try {
                assertTrue(new CommandUsingRequestCache(2).execute());
                assertTrue(new CommandUsingRequestCache(2).execute());
                assertFalse(new CommandUsingRequestCache(1).execute());
                assertTrue(new CommandUsingRequestCache(0).execute());
                assertTrue(new CommandUsingRequestCache(58672).execute());
            } finally {
                context.shutdown();
            }
        }

        @Test
        public void testWithCacheHitsByYunai001() {
            HystrixRequestContext context = HystrixRequestContext.initializeContext();
            try {
                assertTrue(new CommandUsingRequestCache(2).execute());
                assertTrue(new CommandUsingRequestCache(2).execute());
            } finally {
                context.shutdown();
            }
        }

        @Test
        public void testWithCacheHitsByYunai002() {
            HystrixRequestContext context = HystrixRequestContext.initializeContext();
            assertTrue(new CommandUsingRequestCache(2).execute());
            context.shutdown();

//            context = HystrixRequestContext.initializeContext();
            assertTrue(new CommandUsingRequestCache(2).execute());
        }

        // 测试并发场景
        @Test
        public void testWithCacheHitsByYunai003() throws InterruptedException {
            final HystrixRequestContext context = HystrixRequestContext.initializeContext();
            final int numbers = 10;
            final CountDownLatch latch = new CountDownLatch(numbers);
            for (int i = 0; i < numbers; i++) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HystrixRequestContext.setContextOnCurrentThread(context);
                            Boolean result = new CommandUsingRequestCache(2, true).execute();
                            System.out.println("result:" + result);
                        } finally {
                            latch.countDown();
                        }
                    }
                }).start();
            }
            latch.await();
            context.shutdown();
        }

        @Test
        public void testWithCacheHitsByYunai004() throws InterruptedException {
            final HystrixRequestContext context = HystrixRequestContext.initializeContext();
            ExecutorService executor = Executors.newFixedThreadPool(1000);
//            final int latchCount = Short.MAX_VALUE;
            final int latchCount = 1000;
            final CountDownLatch latch = new CountDownLatch(latchCount);
            for (int i = 0; i < latchCount; i++) {
//                Thread.sleep(10);
//                Thread.sleep(100);
//                Thread.sleep(1000);
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HystrixRequestContext.setContextOnCurrentThread(context);
                            new CommandUsingRequestCache(2, false).execute();
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            latch.await();
//            new CommandUsingRequestCache(2, false).execute();
            context.shutdown();
        }

        @Test
        public void testWithCacheHitsByYunai005() throws InterruptedException {
            final HystrixRequestContext context = HystrixRequestContext.initializeContext();
            Observable<Boolean> o = new CommandUsingRequestCache(2, false).toObservable();
            o.subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    System.out.println("finish");
                }
            });
            Thread.sleep(Long.MAX_VALUE);
        }

        @Test
        public void testWithCacheHitsByYunai006() {
            CommandUsingRequestCache cache = new CommandUsingRequestCache(2, false);
            cache.execute();
            cache.execute();
        }

        private volatile int numbers = 0;

        private class Counter implements Callable<Void> {

            private final boolean incr;

            private Counter(boolean incr) {
                this.incr = incr;
            }

            @Override
            public Void call() throws Exception {
                if (incr) {
                    numbers++;
                } else {
                    numbers--;
                }
                return null;
            }
        }

        /**
         * 模拟 {@link com.netflix.hystrix.HystrixCachedObservable#outstandingSubscriptions} 是否可能不正确
         * @throws InterruptedException 异常
         */
        @Test
        public void testVolatile() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(500);
            List<Counter> runners = new LinkedList<Counter>();
            for (int i = 0; i < Short.MAX_VALUE * 100; i++) {
                runners.add(new Counter(true));
                runners.add(new Counter(false));
            }
            executor.invokeAll(runners);
            System.out.println("result:" + numbers);
        }

        @Test
        public void testWithCacheHits() {
            HystrixRequestContext context = HystrixRequestContext.initializeContext();
            try {
                CommandUsingRequestCache command2a = new CommandUsingRequestCache(2);
                CommandUsingRequestCache command2b = new CommandUsingRequestCache(2);

                assertTrue(command2a.execute());
                // this is the first time we've executed this command with the value of "2" so it should not be from cache
                assertFalse(command2a.isResponseFromCache());

                assertTrue(command2b.execute());
                // this is the second time we've executed this command with the same value so it should return from cache
                assertTrue(command2b.isResponseFromCache());
            } finally {
                context.shutdown();
            }

            // start a new request context
            context = HystrixRequestContext.initializeContext();
            try {
                CommandUsingRequestCache command3b = new CommandUsingRequestCache(2);
                assertTrue(command3b.execute());
                // this is a new request context so this should not come from cache
                assertFalse(command3b.isResponseFromCache());
            } finally {
                context.shutdown();
            }
        }
    }

}
