package com.yige.pdf.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

/**
 * @Auther yi-ge
 * @Date 2020-11-09 22:55
 */
public class FilesUtils {
//    /**
//     * 删除指定天数之前的文件
//     * @param fromDir 文件路径
//     * @param howDays 天数
//     * @return 删除的文件个数
//     */
//    public static Integer moveFileToReady(String fromDir, int howDays) {
//        File srcDir = new File(fromDir);
//        if (!srcDir.exists()) {
//            return 0;
//        }
//        File[] files = srcDir.listFiles();
//        if (files == null || files.length <= 0) {
//            return 0;
//        }
//        // 删除文件总数
//        int delTotal = 0;
//        Date today = new Date();
//        for (int i = 0; i < files.length; i++) {
//            if (files[i].isFile()) {
//                try {
//                    File ff = files[i];
//                    long time = ff.lastModified();
//                    Calendar cal = Calendar.getInstance();
//                    cal.setTimeInMillis(time);
//                    Date lastModified = cal.getTime();
//                    //(int)(today.getTime() - lastModified.getTime())/86400000;
//                    long days = getDistDates(today, lastModified);
//                    // 删除多少天前之前文件
//                    if (days >= howDays) {
//                        files[i].delete();
//                        delTotal++;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return delTotal;
//    }

    /**
     * 迭代删除文件夹
     *
     * @param dirPath 文件夹路径
     */
    public static void deleteDir(String dirPath) {
        File file = new File(dirPath);
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            if (files == null) {
                file.delete();
            } else {
                for (File value : files) {
                    deleteDir(value.getAbsolutePath());
                }
                file.delete();
            }
        }
    }

    /**
     * 删除指定目录指定天数之前的文件
     * @param fromDir 文件夹路径
     * @param howDays 天数
     * @return 删除的文件夹个数
     */
    public static Integer moveDirToReady(String fromDir, int howDays) {
        File srcDir = new File(fromDir);
        if (!srcDir.exists()) {
            return 0;
        }
        File[] files = srcDir.listFiles();
        if (files == null || files.length <= 0) {
            return 0;
        }
        // 删除文件总数
        int delTotal = 0;
        Date today = new Date();
        for (File file : files) {
            if (file.isDirectory()) {
                try {
                    long time = file.lastModified();
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(time);
                    Date lastModified = cal.getTime();
                    long days = getDistDates(today, lastModified);
                    // 删除多少天前之前文件
                    if (days >= howDays) {
                        deleteDir(file.getPath());
                        delTotal++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return delTotal;
    }

    /**
     * 获取时间差（天）
     *
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 间隔天数
     */
    public static long getDistDates(Date startDate, Date endDate) {
        long totalDate;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        long timestart = calendar.getTimeInMillis();
        calendar.setTime(endDate);
        long timeend = calendar.getTimeInMillis();
        totalDate = Math.abs((timeend - timestart)) / (1000 * 60 * 60 * 24);
        return totalDate;
    }

    /**
     * 从制定URL下载文件并保存到指定目录
     *
     * @param url      请求的路径
     * @param filePath 文件将要保存的目录
     */
    public static void saveUrlAs(String url, String filePath) {
        FileOutputStream fileOut;
        HttpURLConnection conn;
        InputStream inputStream;

        try {
            URL httpUrl = new URL(url);
            conn = (HttpURLConnection) httpUrl.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
            inputStream = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            fileOut = new FileOutputStream(filePath);
            BufferedOutputStream bos = new BufferedOutputStream(fileOut);

            byte[] buf = new byte[4096];
            int length = bis.read(buf);
            while (length != -1) {
                bos.write(buf, 0, length);
                length = bis.read(buf);
            }
            bos.close();
            bis.close();
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
