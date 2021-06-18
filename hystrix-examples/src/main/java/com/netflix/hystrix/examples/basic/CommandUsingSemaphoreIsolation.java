/**
 * Copyright 2012 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.examples.basic;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import org.junit.Test;

/**
 * Example of {@link HystrixCommand} defaulting to use a semaphore isolation strategy
 * when its run() method will not perform network traffic.
 */
// DONE
public class CommandUsingSemaphoreIsolation extends HystrixCommand<String> {

    private final int id;

    public CommandUsingSemaphoreIsolation(int id) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
                // since we're doing work in the run() method that doesn't involve network traffic
                // and executes very fast with low risk we choose SEMAPHORE isolation
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
//                        .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)));
                        .withExecutionIsolationStrategy(ExecutionIsolationStrategy.THREAD)));
        this.id = id;
    }

    @Override
    protected String run() {
        // a real implementation would retrieve data from in memory data structure
        // or some other similar non-network involved work
        if (id == 10) {
            System.out.println("睡一会");
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return "ValueFromHashMap_" + id;
    }

//    @Override
//    protected String getFallback() {
//        if (id == 10) {
//            System.out.println("睡二会");
//            try {
//                Thread.sleep(1000L);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        return "sss";
//    }

    public static class UnitTest {

        @Test
        public void case001() {
            new CommandUsingSemaphoreIsolation(10).execute();
        }

    }
}
