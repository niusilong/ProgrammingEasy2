package com.test.sopa.thread;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
	public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20, 100, 60L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(), 
		      new TysxThreadFactory("Tysx"),
		      new ThreadPoolExecutor.AbortPolicy());
}
