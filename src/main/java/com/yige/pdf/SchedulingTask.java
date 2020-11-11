package com.yige.pdf;

import com.yige.pdf.utils.FilesUtils;
import com.yige.pdf.utils.ResponseFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Auther yi-ge
 * @Date 2020-11-09 22:54
 */
@Slf4j
@Component
@PropertySource(value = "classpath:config.yml", encoding = "utf-8", factory = ResponseFactory.class)
public class SchedulingTask {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Value("${config.public-path}")
    private String PUBLIC_PATH;

    @Scheduled(cron = "${config.cron}")
    @Async("taskScheduler")
    public void checkClenFile() {
        log.info("======定时清理文件夹任务开始于：{}", sdf.format(new Date()));
        // 删除多少天之前的文件夹
        int delCount = FilesUtils.moveDirToReady(PUBLIC_PATH, 1);
        if (delCount > 0) {
            log.info("======本次从：{}" + PUBLIC_PATH + "下清理" + delCount + "个文件夹");
        } else {
            log.info("======暂时没有要清理的文件夹");
        }
        log.info("======定时清理文件文件夹任务结束于：{}", sdf.format(new Date()));
    }
}
