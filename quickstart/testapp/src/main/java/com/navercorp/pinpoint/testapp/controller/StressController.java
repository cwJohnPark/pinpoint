package com.navercorp.pinpoint.testapp.controller;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.navercorp.pinpoint.testapp.service.DataSourceStressService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.navercorp.pinpoint.testapp.util.Description;

@Controller
public class StressController {

    private final DataSourceStressService dataSourceStressService;

    public StressController(DataSourceStressService dataSourceStressService) {
        this.dataSourceStressService = dataSourceStressService;
    }

    @RequestMapping("/consumeCpu")
    @ResponseBody
    @Description("Call that consumes a lot of cpu time.")
    public Map<String, Object> consumeCpu() throws InterruptedException {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        int threadSize = Math.max(1, cpuCount - 1);

        long limitTime = 10000;
        CountDownLatch latch = new CountDownLatch(threadSize);

        for (int i = 0; i < threadSize; i++) {
            Thread thread = new Thread(new ConsumeCpu(latch, limitTime));
            thread.setDaemon(true);

            thread.start();
        }

        latch.await();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("message", "ok");

        return map;
    }

    class ConsumeCpu implements Runnable {

        private final CountDownLatch latch;
        private final long limitTime;

        public ConsumeCpu(CountDownLatch latch, long limitTime) {
            this.latch = latch;
            this.limitTime = limitTime;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();

            try {
                BigDecimal decimal = new BigDecimal(0);
                for (int num = 1; num < Integer.MAX_VALUE; num++) {
                    long currentTimeMillis = System.currentTimeMillis();

                    if (currentTimeMillis - startTime > limitTime) {
                        break;
                    }

                    decimal.add(new BigDecimal(num));
                }
            } finally {
                latch.countDown();
            }
        }
    }

    @RequestMapping("/consumeMemory")
    @ResponseBody
    @Description("Call that consumes some memory that may trigger a few garbage collections.")
    public Map<String, Object> consumeMemory() throws InterruptedException {
        consumeMemory(1024 * 16, 20);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("message", "ok");

        return map;
    }

    @RequestMapping("/consumeMemoryLarge")
    @ResponseBody
    @Description("Call that consumes a large amount of memory that will most likely trigger multiple garbage collections.")
    public Map<String, Object> consumeMemoryLarge() throws InterruptedException {
        consumeMemory(1024 * 16, 100);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("message", "ok");

        return map;
    }
    
    private void consumeMemory(int byteArraySize, int createMaxHeapCount) {
        long heapSize = Runtime.getRuntime().maxMemory();
        
        int count = (int) Math.max(1, (heapSize/byteArraySize) * createMaxHeapCount);
        
        List<WeakReference<byte[]>> weakReferece = new ArrayList<WeakReference<byte[]>>();
        for (int i = 0; i < count; i++) {
            weakReferece.add(new WeakReference<byte[]>(new byte[byteArraySize]));
        }

        System.gc();
    }

    /**
     * DATASOURCE CONNECTION USAGE RATE
     * Triggered when the application's DataSource connection usage(%) exceeds the configured threshold.
     */
    @RequestMapping("/consumeDataSourceUsage")
    @ResponseBody
    @Description("Call that the application's DataSource connection usage(%) exceeds the configured threshold.")
    public Map<String, Object> consumeDataSourceUsage() throws InterruptedException, SQLException {
        boolean executed = dataSourceStressService.occupyDataSource();
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("message", executed ? "ok" : "failed");

        return map;
    }

    /**
     * DEADLOCK OCCURRENCE
     */
    @RequestMapping("/occurDeadlock")
    @ResponseBody
    @Description("Call that occurs thread deadlocks in application.")
    public Map<String, Object> occurDeadlock() throws InterruptedException {

        final String resource1 = "resource 1";
        final String resource2 = "resource 2";

        Thread t1 = new Thread(() -> {
            synchronized (resource1) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                synchronized (resource2) {
                    //Thread.sleep(100);
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (resource2) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                synchronized (resource1) {
                    // Thread.sleep(100);
                }
            }
        });
        t1.start();
        t2.start();

        Thread.sleep(1000);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("message", "ok");

        return map;
    }

}
