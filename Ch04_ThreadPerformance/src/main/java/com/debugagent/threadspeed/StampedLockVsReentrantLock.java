/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.debugagent.threadspeed;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

public class StampedLockVsReentrantLock {

    static class Test {
        volatile long sharedData;

        public long runTest(Runnable thread) {
            sharedData = 0;
            Executor executor = Executors.newFixedThreadPool(10);
            for(int iter = 0 ; iter < 10 ; iter++) {
                executor.execute(thread);
            }

            return sharedData;
        }
    }

    public long syncrhonizedLock() {
        final Object LOCK = new Object();
        final Test test = new Test();
        return test.runTest(() -> {
            for(int iter = 0 ; iter < 100 ; iter++) {
                synchronized (LOCK) {
                    test.sharedData += System.currentTimeMillis();
                }
            }
        });
    }

    public long stampedLock() {
        StampedLock lock = new StampedLock();
        final Test test = new Test();
        return test.runTest(() -> {
            for(int iter = 0 ; iter < 100 ; iter++) {
                long val = 0;
                try {
                    val = lock.writeLock();
                    test.sharedData += System.currentTimeMillis();
                } finally {
                    lock.unlockWrite(val);
                }
            }
        });
    }

    public long reentrantLock() {
        ReentrantLock lock = new ReentrantLock();
        final Test test = new Test();
        return test.runTest(() -> {
            for(int iter = 0 ; iter < 100 ; iter++) {
                try {
                    lock.lock();
                    test.sharedData += System.currentTimeMillis();
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    public static void main(String[] args) throws Exception {
        new StampedLockVsReentrantLock().runTests();
    }

    private void runTests() throws Exception {
        System.out.println("Warmup");
        for(int iter = 0 ; iter < 3 ; iter++) {
            syncrhonizedLock();
            stampedLock();
            reentrantLock();
        }
        System.out.println("Warmup Finished");

        long synchronizedTime = System.nanoTime();

        for(int iter = 0 ; iter < 10 ; iter++) {
            syncrhonizedLock();
        }

        synchronizedTime = System.nanoTime() - synchronizedTime;

        long reentrantTime = System.nanoTime();

        for(int iter = 0 ; iter < 10 ; iter++) {
            reentrantLock();
        }

        reentrantTime = System.nanoTime() - reentrantTime;

        long stampedTime = System.nanoTime();

        for(int iter = 0 ; iter < 10 ; iter++) {
            stampedLock();
        }

        stampedTime = System.nanoTime() - stampedTime;

        System.out.println("Synchronized: " + synchronizedTime);
        System.out.println("Reentrant: " + reentrantTime);
        System.out.println("Stamped: " + stampedTime);
        System.exit(0);
    }
}
