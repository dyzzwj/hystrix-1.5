package com.netflix.hystrix;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

public class HystrixCachedObservable<R> {
    /**
     * 订阅
     */
    protected final Subscription originalSubscription;
    /**
     * 缓存 cachedObservable
     */
    protected final Observable<R> cachedObservable;
    /**
     * TODO 【2006】【outstandingSubscriptions】
     */
    private volatile int outstandingSubscriptions = 0;
//    private AtomicInteger outstandingSubscriptions2 = new AtomicInteger(0);

    protected HystrixCachedObservable(final Observable<R> originalObservable) {
        ReplaySubject<R> replaySubject = ReplaySubject.create();
        this.originalSubscription = originalObservable
                .subscribe(replaySubject);

        this.cachedObservable = replaySubject
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        outstandingSubscriptions--; // TODO 芋艿，这是为啥
//                        outstandingSubscriptions2.decrementAndGet();
                        System.out.println("【sub】Thread:" + Thread.currentThread() + "===" + outstandingSubscriptions + "result:" + originalSubscription.isUnsubscribed());
                        if (outstandingSubscriptions == 0) {
//                        if (outstandingSubscriptions2.decrementAndGet() == 0) {
//                            if (!originalSubscription.isUnsubscribed()) {
//                                synchronized (originalSubscription) {
//                                    if (!originalSubscription.isUnsubscribed()) {
//                                        originalSubscription.unsubscribe();
//                                        System.err.println("【666666】【sub】Thread:" + Thread.currentThread() + "===" + outstandingSubscriptions);
//                                    }
//                                }
//                            }
                            System.err.println("[1]" + cachedObservable);
                            System.err.println("[1]" + originalSubscription.isUnsubscribed());
                            originalSubscription.unsubscribe();
//                            System.err.println("【666666】【sub】Thread:" + Thread.currentThread() + "===" + outstandingSubscriptions);
//                            System.err.println("【666666】【sub】Thread:" + Thread.currentThread() + "===" + outstandingSubscriptions2.get());
                        }
//                        System.out.println("【sub】Thread:" + Thread.currentThread() + "===" + outstandingSubscriptions2.get());
//                        System.out.println("[1]" + cachedObservable);
                    }
                })
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        outstandingSubscriptions++;
//                        System.out.println("【plus】Thread:" + Thread.currentThread() + "===" + outstandingSubscriptions);
//                        outstandingSubscriptions2.incrementAndGet();
//                        System.out.println("【plus】Thread:" + Thread.currentThread() + "===" + outstandingSubscriptions2.get());
//                        System.out.println("[0]" + cachedObservable);
                    }
                });
    }

    public static <R> HystrixCachedObservable<R> from(Observable<R> o, AbstractCommand<R> originalCommand) {
        return new HystrixCommandResponseFromCache<R>(o, originalCommand);
    }

    public static <R> HystrixCachedObservable<R> from(Observable<R> o) {
        return new HystrixCachedObservable<R>(o);
    }

    public Observable<R> toObservable() {
        return cachedObservable;
    }

    public void unsubscribe() {
        originalSubscription.unsubscribe();
    }
}
