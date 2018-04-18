package com.netease.mail.datasearch.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResourceDataCheck implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ResourceDataCheck.class);
    private static final String CURRENT_PATH = ResourceDataCheck.class.getResource("/").getPath();
    static{
        PropertyConfigurator.configure(CURRENT_PATH + "log4j.properties");
    }
    
    public static String dataPath = null;
    public static String curlUrl = null;
    
    private static final String OS_NAME = System.getProperty("os.name");
    private static final int SLEEP_TIME = 4*60;
    private static final long TIME_OUT = 60*60*1000;
    private static ObjectMapper om = new ObjectMapper();
    
    private String seed;
    private boolean isGenDataLocal;
    public ResourceDataCheck(String seed) {
        this(seed, true);
    }
    
    public ResourceDataCheck(String seed, boolean isGenLocal) {
        this.seed = seed;
        this.isGenDataLocal = isGenLocal;
    }
    
    /**
     * 获取cmd命令的返回结果，按行存入list中
     * @param cmd
     * @return
     * @throws IOException 
     */
    public static List<String> getCmdOutput(String cmd) throws IOException{
        List<String> results = new ArrayList<>();
        if(OS_NAME.startsWith("Windows")) {
            cmd = "cmd /c " + cmd;
        }
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            if(!line.isEmpty())
                results.add(line);
        }
        bufferedReader.close();
        return results;
    }
    
    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        try {
            String cmd = String.format("curl -X POST -d %s %s", seed, curlUrl);
            logger.info("cmd to exe:{}", cmd);
            Runtime.getRuntime().exec(cmd);
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException e) {}
        } catch (IOException e) {
            logger.error("sendRequest failed, seed:{}, e:", seed, e);
            return;
        }
        try {
            String taskId = DigestUtils.md5Hex(seed);
            while(true) {
                try {
                    String cmd = String.format("curl %s?task=%s", curlUrl, taskId);
                    String cpuOutput = getCmdOutput(cmd).get(0);
                    boolean isCheck = false;
                    if(cpuOutput.indexOf("cannot find task")>-1) {
                        logger.warn("maybe tomcat restart, seed:{}", seed);
                        isCheck = true;
                    }
                    else {
                        String result = cpuOutput.split("result\":\"")[1].split("\"")[0];
                        if(!result.isEmpty())
                            isCheck = true;
                        else{
                            String error = cpuOutput.split("error\":\"")[1].split("\"")[0];
                            if(!error.isEmpty()) {
                                logger.info("parse seed {} failed", seed);
                                return;
                            }
                        }
                    }
                    if(isCheck) {
                        if(isGenDataLocal) {
                            NosData nosData = null;
                            try {
                                nosData = om.readValue(NeteaseObjectStorage.download(taskId), NosData.class);
                            } catch (Exception e) {
                                continue;
                            }
                            
                            String outFileName = dataPath + "/" + taskId;
                            PrintWriter pw = null;
                            try {
                                pw = new PrintWriter(outFileName, "UTF-8");
                                pw.println(om.writeValueAsString(nosData));
                                pw.flush();
                            } catch (Exception e) {
                                logger.info("write seed {} failed, e:", seed, e);
                                continue;
                            }
                            finally {
                                if(null!=pw)
                                    try {
                                        pw.close();
                                    } catch (Exception e2) {}
                            }
                            logger.info("seed finished:{}, outFileName:{}", seed, outFileName);
                        }
                        else {
                            logger.info("seed finished:{}", seed);
                        }
                        return;
                    }
                } catch (Exception e) {}
                
                if((System.currentTimeMillis()-startTime)>TIME_OUT){
                    logger.info("timeout, seed:{}", seed);
                    return;
                }
                
                try {
                    TimeUnit.SECONDS.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {}
                
            }
        } catch (Exception e) {
            logger.error("run error, e:", e);
            return;
        }
    }
    
}