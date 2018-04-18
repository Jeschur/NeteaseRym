package com.netease.mail.datasearch.crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netease.mail.utils.Utils4FileRead;

public class ResourceTasksSubmit{
    private static final Logger logger = LoggerFactory.getLogger(ResourceTasksSubmit.class);
    private static final String CURRENT_PATH = ResourceTasksSubmit.class.getResource("/").getPath();
    static{
        PropertyConfigurator.configure(CURRENT_PATH + "log4j.properties");
    }
    
    
    public static List<String> finishedSeeds(List<String> seeds, String dataPath){
        List<String> finishedList = new ArrayList<>();
        for(String seed:seeds) {
            try {
                String nosFname = DigestUtils.md5Hex(seed);
                String outFileName = dataPath + "/" + nosFname;
                File file = new File(outFileName);
                if(file.exists()) {
                    finishedList.add(seed);
                    continue;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return finishedList;
    }
    
    public static void main(String[] args) {
//        String batchTime = null;
        String dataPath = null;
        String urlsFileName = "urls.list";
        String curlUrl = "http://10.240.130.249:9090/resource-taobaoCategory/TaobaoCategoryServlet/";
        int threadNum = 20;
        boolean isPreProcess = false; //是否在开始处理前先检测本地已经完成部分
        if(args.length<3) {
            System.out.println("java -jar ResourceTasksSubmit.jar dataPath urlsFileName curlUrl [threadNum] [isPreProcess]");
            System.exit(-1);
        }
        if(args.length >= 3) {
            dataPath = args[0];
            urlsFileName = args[1];
            curlUrl = args[2];
        }
        if(args.length >= 4) {
            threadNum = Integer.parseInt(args[3]);
        }
        if(args.length >= 5) {
            isPreProcess = Boolean.parseBoolean(args[4].trim());
        }
        logger.info("dataPath:{}, urlsFileName:{}, curlUrl:{}, threadNum:{}, isPreProcess:{}",
            dataPath, urlsFileName, curlUrl, threadNum, isPreProcess);
        
//        String dataPath = System.getProperty("user.home") + "/dataTomcat/resource-taobaoCategory/" + batchTime;
//        logger.info("dataPath:{}", dataPath);
        File file = new File(dataPath);
        if(!file.exists()) {
            file.mkdirs();
        }
        ResourceDataCheck.curlUrl = curlUrl;
        ResourceDataCheck.dataPath = dataPath;
        
        List<String> seeds = Utils4FileRead.convertFile2List(urlsFileName);
        logger.info("seeds size:{}", seeds.size());
        int tryCount = 5; //多次提交，补充前次爬取中遗漏的数据，最多重复提交tryCount次
        while(tryCount>=0) {
            tryCount --;
            logger.info("tryCount in main:{}", tryCount);
            if(isPreProcess) {
                seeds.removeAll(finishedSeeds(seeds, dataPath));
                logger.info("seeds after preProces, size:{}", seeds.size());
            }
            
            ExecutorService  excutor = Executors.newFixedThreadPool(threadNum);
            for(String seed:seeds) {
                ResourceDataCheck oneCheck = new ResourceDataCheck(seed);
                excutor.submit(oneCheck);
                try {
                    TimeUnit.MILLISECONDS.sleep(5*1000);
                } catch (InterruptedException e) {}
            }
            logger.info("submit over!");
            excutor.shutdown();
            int countTmp = 0;
            while(!excutor.isTerminated()) {
                countTmp ++;
                if(countTmp%12==0) {
                    logger.info("countTmp:{}, continue to wait", countTmp);
                }
                try {
                    TimeUnit.SECONDS.sleep(300);
                } catch (Exception e) {}
            }
            logger.info("countTmp:{}, excutor is terminated", countTmp);
        }
        
        
    }

}