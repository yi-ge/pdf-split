package com.yige.pdf;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfSplitter;
import com.yige.pdf.utils.CryptoUtils;
import com.yige.pdf.utils.FilesUtils;
import com.yige.pdf.utils.ResponseFactory;
import com.yige.pdf.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

@Slf4j
@RestController
@PropertySource(value = "classpath:config.yml", encoding = "utf-8", factory = ResponseFactory.class)
public class Controller {
    private static String PUBLIC_PATH;
    private static String HOST;

    Controller(@Value("${config.public-path}") String PUBLIC_DIR, @Value("${config.host}") String HOSTNAME) {
        PUBLIC_PATH = PUBLIC_DIR;
        HOST = HOSTNAME;
        File file = new File(PUBLIC_PATH);
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("Can't mkdir public path.");
        }
    }

    private static String splitPDF(String perpage, String url) throws IOException {
        String hash = CryptoUtils.hashKeyForDisk(perpage + "-" + url); // 根据perpage和url获取hash值
        String pageCountFilePath = PUBLIC_PATH + "/" + hash + "/pageCount.txt";
        String filePath = PUBLIC_PATH + "/" + hash + "/" + hash + ".pdf";
        List<String> urlList = new ArrayList<>();
        File file = new File(PUBLIC_PATH + "/" + hash);

        if (!file.exists() && !file.isDirectory()) { // 如果文件不存在
            if (!file.mkdir()) {
                return "";
            }

            FilesUtils.saveUrlAs(url, filePath); // 从URL下载文件

            final int maxPageCount = Integer.parseInt(perpage); // create a new PDF per X pages from the original file
            PdfDocument pdfDocument = new PdfDocument(new PdfReader(new File(filePath)));
            PdfSplitter pdfSplitter = new PdfSplitter(pdfDocument) { // PDF分割
                int partNumber = 1;

                @Override
                protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                    try {
                        String filePath = PUBLIC_PATH + "/" + hash + "/" + partNumber++ + ".pdf";
                        urlList.add(filePath.replace(PUBLIC_PATH, HOST));
                        return new PdfWriter(filePath);
                    } catch (final FileNotFoundException ignored) {
                        throw new RuntimeException();
                    }
                }
            };

            pdfSplitter.splitByPageCount(maxPageCount, (pdfDoc, pageRange) -> pdfDoc.close()); // 根据perpage分割
//        pdfSplitter.splitByPageNumbers(maxPageCount); // 根据pageNumber分割
            int pageCount = pdfDocument.getNumberOfPages(); // 总页数

            pdfDocument.close();

            File pageCountFile = new File(pageCountFilePath);
            if (!pageCountFile.exists() && !pageCountFile.createNewFile()) {
                throw new RuntimeException("Can't createNewFile 'pageCount.txt'.");
            }
            FileWriter fw = new FileWriter(pageCountFilePath);
            fw.write(String.valueOf(pageCount));
            fw.close();

            Map<String, Object> out = new HashMap<>();
            out.put("totalPage", pageCount);
            out.put("urlList", urlList);
            return JSON.toJSONString(out);
        } else { // 如果文件已经存在
            int totalPage = 0;
            File pageCountFile = new File(pageCountFilePath);
            if (pageCountFile.exists()) { // 读取总页数记录
                InputStream is = new FileInputStream(pageCountFilePath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                totalPage = Integer.parseInt(line);
                is.close();
                reader.close();
            }

            File[] fileArray = file.listFiles();
            String fileFirstPath = file.toPath().toString().replace(PUBLIC_PATH, HOST) + "/";
            if (fileArray != null) {
                for (File value : fileArray) { // 还原下载地址
                    if (value.isFile() && !value.getName().equals("pageCount.txt") && !value.getName().equals(hash + ".pdf")) {
                        urlList.add(fileFirstPath + value.getName());
                    }
                }
            }

            urlList.sort(Comparator.comparingInt(str -> Integer.parseInt(str.replace(fileFirstPath, "").replace(".pdf", ""))));

            Map<String, Object> out = new HashMap<>();
            out.put("totalPage", totalPage);
            out.put("urlList", urlList);
            return JSON.toJSONString(out);
        }
    }

    @Bean("taskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(10);
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        return threadPoolTaskScheduler;
    }

    @GetMapping(value = "/", produces = {"application/json;charset=utf-8"})
    public String get(@RequestParam(value = "perpage", defaultValue = "5") String perpage, @RequestParam(value = "url") String url) throws IOException {
        return splitPDF(perpage, url);
    }

    @PostMapping(value = "/", produces = {"application/json;charset=utf-8"})
    public String post(@RequestBody JSONObject body) throws IOException {
        String perpage = body.getString("perpage");
        String url = body.getString("url");
        return splitPDF(perpage, url);
    }

    @GetMapping(value = "/{hash}/{name}")
    public ResponseEntity<FileSystemResource> getToolByToolName(@PathVariable("hash") String hash, @PathVariable("name") String name) {
        File file = new File(PUBLIC_PATH + "/" + hash + "/" + name);
        if (file.exists()) {
            return ResponseUtils.exportFile(file);
        }
        return null;
    }
}
