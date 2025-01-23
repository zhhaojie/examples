package com.qimiao.jobrunrexample;

import jakarta.annotation.Resource;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RunExample {
    @Resource
    private JobScheduler jobScheduler;

    @Resource
    private CleanDirtyTask cleanDirtyTask;

    @Resource
    CreateProductTask createProductTask;

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) throws InterruptedException {
        // 每隔一段时间生产一些产品
        jobScheduler.scheduleRecurrently(Cron.every15seconds(), createProductTask::createProduct);

        // 每隔一段时间，有一些任务要处理
        for (int i = 0; i < 100 * 1000; i++) {
            if (i % 100 == 0) {
                TimeUnit.SECONDS.sleep(1);
            }
            jobScheduler.enqueue(cleanDirtyTask::clean);
        }
    }
}
