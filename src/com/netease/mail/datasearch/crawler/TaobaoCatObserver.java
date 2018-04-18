package com.netease.mail.datasearch.crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netease.mail.utils.Utils4FileRead;

public class TaobaoCatObserver implements Observer{
    private static final Logger logger = LoggerFactory.getLogger(TaobaoCatObserver.class);
    private static final String CURRENT_PATH = TaobaoCatObserver.class.getResource("/").getPath();
    static{
        PropertyConfigurator.configure(CURRENT_PATH + "log4j.properties");
        logger.info("load TaobaoCatObserver log4j.properties succeed");
    }
    public static String curlUrl = null;
    
    private int threadNum = 20;
    private List<String> seeds;
    private List<String> needCheckNos;
    
    public TaobaoCatObserver(List<String> seeds) {
        this(20, seeds);
    }
    
    public TaobaoCatObserver(int threadNum, List<String> seeds) {
        this.threadNum = threadNum;
        this.seeds = seeds;
        this.needCheckNos = new ArrayList<>();
    }
    
    
    @Override
    public void update(Observable o, Object arg) {
        needCheckNos.clear();
        NosCheckObservable observable = (NosCheckObservable)o;
        needCheckNos.addAll(observable.getCheckSet());
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (Exception e) {}
    }
    
    private void sendRequest(String seed) {
        try {
//            String cmd = String.format("curl -X POST -d %s http://localhost:9090/resource-taobaoCategory/TaobaoCategoryServlet/", seed);
            String cmd = String.format("curl -X POST -d %s %s", seed, curlUrl);
            logger.info("cmd to exe:{}", cmd);
            Runtime.getRuntime().exec(cmd);
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {}
        } catch (IOException e) {
            logger.error("sendRequest failed, seed:{}, e:", seed, e);
        }
    }
    
    public List<String> genCheckList() {
//        logger.info("needCheckNos.size:{}, needCheckNos:{}", needCheckNos.size(), needCheckNos);
        if(needCheckNos.size()<threadNum && seeds.size()>0) {
            int gap = threadNum - needCheckNos.size();
            if(gap>seeds.size())
                gap = seeds.size();
            for(int i=gap-1; i>=0; i--) {
                String seed = seeds.remove(i);
                sendRequest(seed);
                needCheckNos.add(seed);
            }
        }
        logger.info("needCheckNos.size:{}, needCheckNos:{}", needCheckNos.size(), needCheckNos);
        return needCheckNos;
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
        try {
            String batchTime = null;
            String urlsFileName = "urls.list";
            String curlUrl = "http://10.240.130.249:9090/resource-taobaoCategory/TaobaoCategoryServlet/";
            int threadNum = 20;
            boolean isPreProcess = false; //是否在开始处理前先检测本地已经完成部分
            if(args.length==0) {
                System.out.println("java -jar TaobaoCatObserver.jar batchTime urlsFileName curlUrl [threadNum] [isPreProcess]");
                System.exit(-1);
            }
            if(args.length >= 3) {
                batchTime = args[0];
                urlsFileName = args[1];
                curlUrl = args[2];
            }
            if(args.length >= 4) {
                threadNum = Integer.parseInt(args[3]);
            }
            if(args.length >= 5) {
                isPreProcess = Boolean.parseBoolean(args[4].trim());
            }
            logger.info("batchTime:{}, urlsFileName:{}, curlUrl:{}, threadNum:{}, isPreProcess:{}",
                    batchTime, urlsFileName, curlUrl, threadNum, isPreProcess);
            
            String dataPath = System.getProperty("user.home") + "/dataTomcat/resource-taobaoCategory/" + batchTime;
            File file = new File(dataPath);
            if(!file.exists()) {
                file.mkdirs();
            }
            TaobaoCatObserver.curlUrl = curlUrl;
            NosCheckObservable.batchTime = batchTime;
            NosCheckObservable.dataPath = dataPath;
            
            List<String> seeds = Utils4FileRead.convertFile2List(urlsFileName);
            logger.info("seeds size:{}", seeds.size());
            if(isPreProcess) {
                seeds.removeAll(finishedSeeds(seeds, dataPath));
                logger.info("seeds after preProces, size:{}", seeds.size());
            }
            TaobaoCatObserver observer = new TaobaoCatObserver(threadNum, seeds);
            int sleepTime = 5*60; //每sleepTime轮询一次
            while(true) {
                List<String> checkList = new ArrayList<>(observer.genCheckList());
                if(checkList.isEmpty()) {
                    logger.info("finished parse");
                    break;
                }
                NosCheckObservable observable = NosCheckObservable.getInstance(checkList);
                logger.info("observable checklist.size:{}, {}", observable.getCheckSet().size(), observable.getCheckSet());
                observable.addObserver(observer);
                observable.updateInfo();
                TimeUnit.SECONDS.sleep(sleepTime);
            }
            logger.info("done!");
        } catch (Exception e) {
            logger.error("main function error:", e);
            System.exit(-1);
        }
        
    }

}